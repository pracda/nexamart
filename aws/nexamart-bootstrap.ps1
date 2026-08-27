# nexamart-bootstrap.ps1
# Run ONCE to create every AWS resource NexaMart needs: ECR repository, the Docker image,
# an IAM role, the RDS database, and the App Runner service. Safe to re-run if it fails partway
# through — every step checks whether its resource already exists before creating it.
# Takes roughly 15-20 minutes, most of it waiting on RDS provisioning and the first App Runner
# deploy, not active work.

. "$PSScriptRoot\nexamart-aws-config.ps1"

Write-Host "=== NexaMart AWS bootstrap ===" -ForegroundColor Cyan
Test-Prerequisites
$secrets = Load-Secrets
$state = Load-State

if (-not $state.accountId) {
    $state.accountId = Get-AccountId
}
$accountId = $state.accountId
Write-Host "AWS account: $accountId   region: $AWS_REGION"

# ---------------------------------------------------------------------------
# 1. ECR repository
# ---------------------------------------------------------------------------
Write-Host "`n[1/5] ECR repository..." -ForegroundColor Cyan
$ecrExists = Invoke-AwsQuery { aws ecr describe-repositories --repository-names $ECR_REPO_NAME --region $AWS_REGION }
if (-not $ecrExists) {
    aws ecr create-repository --repository-name $ECR_REPO_NAME --region $AWS_REGION | Out-Null
    Write-Host "Created ECR repo $ECR_REPO_NAME"
} else {
    Write-Host "ECR repo already exists"
}
$state.ecrRepoUri = "$accountId.dkr.ecr.$AWS_REGION.amazonaws.com/$ECR_REPO_NAME"
Save-State $state

# ---------------------------------------------------------------------------
# 2. Build & push the Docker image
# ---------------------------------------------------------------------------
Write-Host "`n[2/5] Build & push Docker image..." -ForegroundColor Cyan
aws ecr get-login-password --region $AWS_REGION | docker login --username AWS --password-stdin "$accountId.dkr.ecr.$AWS_REGION.amazonaws.com"
if ($LASTEXITCODE -ne 0) { throw "docker login to ECR failed" }

Push-Location $BackendDir
try {
    docker build -t "${ECR_REPO_NAME}:latest" .
    if ($LASTEXITCODE -ne 0) { throw "docker build failed" }
    docker tag "${ECR_REPO_NAME}:latest" "$($state.ecrRepoUri):latest"
    docker push "$($state.ecrRepoUri):latest"
    if ($LASTEXITCODE -ne 0) { throw "docker push failed" }
} finally {
    Pop-Location
}
Write-Host "Image pushed to $($state.ecrRepoUri):latest"

# ---------------------------------------------------------------------------
# 3. IAM role App Runner uses to pull from ECR
# ---------------------------------------------------------------------------
Write-Host "`n[3/5] IAM access role for App Runner..." -ForegroundColor Cyan
$roleArn = Invoke-AwsQuery { aws iam get-role --role-name $IAM_ROLE_NAME --query "Role.Arn" --output text }
if (-not $roleArn -or $roleArn -eq "None") {
    $trustPolicy = @"
{
  "Version": "2012-10-17",
  "Statement": [{
    "Effect": "Allow",
    "Principal": { "Service": "build.apprunner.amazonaws.com" },
    "Action": "sts:AssumeRole"
  }]
}
"@
    $trustPolicyPath = Join-Path $env:TEMP "nexamart-approle-trust.json"
    $trustPolicy | Set-Content $trustPolicyPath

    aws iam create-role --role-name $IAM_ROLE_NAME --assume-role-policy-document "file://$trustPolicyPath" | Out-Null
    aws iam attach-role-policy --role-name $IAM_ROLE_NAME --policy-arn "arn:aws:iam::aws:policy/service-role/AWSAppRunnerServicePolicyForECRAccess" | Out-Null
    Write-Host "Created IAM role $IAM_ROLE_NAME (waiting 10s for IAM propagation)"
    Start-Sleep -Seconds 10
    $roleArn = aws iam get-role --role-name $IAM_ROLE_NAME --query "Role.Arn" --output text
} else {
    Write-Host "IAM role already exists"
}
$state.appRunnerAccessRoleArn = $roleArn
Save-State $state

# ---------------------------------------------------------------------------
# 4. RDS PostgreSQL database
# ---------------------------------------------------------------------------
Write-Host "`n[4/5] RDS PostgreSQL database..." -ForegroundColor Cyan
$dbStatus = Invoke-AwsQuery { aws rds describe-db-instances --db-instance-identifier $RDS_INSTANCE_ID --region $AWS_REGION --query "DBInstances[0].DBInstanceStatus" --output text }

if (-not $dbStatus -or $dbStatus -eq "None") {
    $defaultVpc = aws ec2 describe-vpcs --filters "Name=isDefault,Values=true" --region $AWS_REGION --query "Vpcs[0].VpcId" --output text
    $state.vpcId = $defaultVpc

    $sgName = "nexamart-db-sg"
    $sgId = Invoke-AwsQuery { aws ec2 describe-security-groups --filters "Name=group-name,Values=$sgName" "Name=vpc-id,Values=$defaultVpc" --region $AWS_REGION --query "SecurityGroups[0].GroupId" --output text }
    if (-not $sgId -or $sgId -eq "None") {
        $sgId = aws ec2 create-security-group --group-name $sgName --description "NexaMart RDS access" --vpc-id $defaultVpc --region $AWS_REGION --query "GroupId" --output text
        aws ec2 authorize-security-group-ingress --group-id $sgId --protocol tcp --port 5432 --cidr 0.0.0.0/0 --region $AWS_REGION | Out-Null
        Write-Host "Created security group $sgId (0.0.0.0/0 on 5432 -- course-project demo posture, see docs/DEPLOYMENT.md)"
    }
    $state.rdsSecurityGroupId = $sgId
    Save-State $state

    Write-Host "Creating RDS instance (this takes 5-10 minutes)..."
    aws rds create-db-instance `
        --db-instance-identifier $RDS_INSTANCE_ID `
        --db-instance-class $RDS_INSTANCE_CLASS `
        --engine postgres `
        --master-username $RDS_MASTER_USERNAME `
        --master-user-password $secrets.DB_PASSWORD `
        --allocated-storage $RDS_STORAGE_GB `
        --db-name $RDS_DB_NAME `
        --vpc-security-group-ids $sgId `
        --publicly-accessible `
        --backup-retention-period 0 `
        --no-multi-az `
        --region $AWS_REGION | Out-Null

    Write-Host "Waiting for RDS to become available..."
    aws rds wait db-instance-available --db-instance-identifier $RDS_INSTANCE_ID --region $AWS_REGION
} else {
    Write-Host "RDS instance already exists (status: $dbStatus)"
    if ($dbStatus -ne "available") {
        Write-Host "Waiting for it to become available..."
        aws rds wait db-instance-available --db-instance-identifier $RDS_INSTANCE_ID --region $AWS_REGION
    }
}

$dbEndpoint = aws rds describe-db-instances --db-instance-identifier $RDS_INSTANCE_ID --region $AWS_REGION --query "DBInstances[0].Endpoint.Address" --output text
Write-Host "RDS endpoint: $dbEndpoint"

# ---------------------------------------------------------------------------
# 5. App Runner service
# ---------------------------------------------------------------------------
Write-Host "`n[5/5] App Runner service..." -ForegroundColor Cyan
$existingArn = Invoke-AwsQuery { aws apprunner list-services --region $AWS_REGION --query "ServiceSummaryList[?ServiceName=='$APP_RUNNER_SERVICE'].ServiceArn" --output text }

if (-not $existingArn -or $existingArn -eq "None" -or $existingArn -eq "") {
    $envVars = @{
        SPRING_PROFILES_ACTIVE = "docker"
        DB_HOST                = $dbEndpoint
        DB_PORT                = "5432"
        DB_NAME                = $RDS_DB_NAME
        DB_USER                = $RDS_MASTER_USERNAME
        DB_PASSWORD             = $secrets.DB_PASSWORD
        JWT_SECRET               = $secrets.JWT_SECRET
        OPENAI_API_KEY           = $secrets.OPENAI_API_KEY
        PORT                     = "$APP_PORT"
    }

    $sourceConfig = @{
        AuthenticationConfiguration = @{ AccessRoleArn = $roleArn }
        AutoDeploymentsEnabled      = $false
        ImageRepository             = @{
            ImageIdentifier     = "$($state.ecrRepoUri):latest"
            ImageRepositoryType = "ECR"
            ImageConfiguration  = @{
                Port                        = "$APP_PORT"
                RuntimeEnvironmentVariables = $envVars
            }
        }
    } | ConvertTo-Json -Depth 10 -Compress
    $sourceConfigPath = Join-Path $env:TEMP "nexamart-apprunner-source.json"
    $sourceConfig | Set-Content $sourceConfigPath

    Write-Host "Creating App Runner service (first deploy takes ~5 minutes)..."
    aws apprunner create-service `
        --service-name $APP_RUNNER_SERVICE `
        --source-configuration "file://$sourceConfigPath" `
        --instance-configuration "Cpu=1024,Memory=2048" `
        --health-check-configuration "Protocol=TCP,Interval=10,Timeout=5,HealthyThreshold=1,UnhealthyThreshold=10" `
        --region $AWS_REGION | Out-Null

    Write-Host "Waiting for the service to finish deploying..."
    do {
        Start-Sleep -Seconds 15
        $svcArn = aws apprunner list-services --region $AWS_REGION --query "ServiceSummaryList[?ServiceName=='$APP_RUNNER_SERVICE'].ServiceArn" --output text
        $status = aws apprunner describe-service --service-arn $svcArn --region $AWS_REGION --query "Service.Status" --output text
        Write-Host "  status: $status"
    } while ($status -eq "OPERATION_IN_PROGRESS")

    if ($status -ne "RUNNING") {
        $state.appRunnerServiceArn = $svcArn
        Save-State $state
        Write-Host "`n=== App Runner deployment FAILED (status: $status) ===" -ForegroundColor Red
        Write-Host "The image built and pushed fine, and RDS is up -- this failure is specific to the" -ForegroundColor Red
        Write-Host "App Runner deployment itself (commonly: the app crashed on startup, often a DB" -ForegroundColor Red
        Write-Host "connection problem, or it didn't respond to the health check in time)." -ForegroundColor Red
        Write-Host ""
        Write-Host "To see the real reason, check the deployment's CloudWatch logs:" -ForegroundColor Yellow
        Write-Host "  aws logs describe-log-groups --region $AWS_REGION --log-group-name-prefix /aws/apprunner/$APP_RUNNER_SERVICE" -ForegroundColor Yellow
        Write-Host "then tail whichever log groups it lists, e.g.:" -ForegroundColor Yellow
        Write-Host "  aws logs tail <log-group-name> --region $AWS_REGION --since 1h" -ForegroundColor Yellow
        Write-Host ""
        Write-Host "Once you know the cause and it's fixed, delete the failed service and rerun this script:" -ForegroundColor Yellow
        Write-Host "  aws apprunner delete-service --service-arn $svcArn --region $AWS_REGION" -ForegroundColor Yellow
        Write-Host "  .\nexamart-bootstrap.ps1" -ForegroundColor Yellow
        exit 1
    }
} else {
    $svcArn = $existingArn
    Write-Host "App Runner service already exists"
}

$state.appRunnerServiceArn = $svcArn
$serviceUrl = aws apprunner describe-service --service-arn $svcArn --region $AWS_REGION --query "Service.ServiceUrl" --output text
$state.appRunnerServiceUrl = $serviceUrl
Save-State $state

Write-Host "`n=== Bootstrap complete ===" -ForegroundColor Green
Write-Host "Backend URL:  https://$serviceUrl"
Write-Host "Try it:       https://$serviceUrl/api/products"
Write-Host ""
Write-Host "Next steps:" -ForegroundColor Yellow
Write-Host "  1. Deploy the frontend once (AWS Amplify Hosting) following docs/DEPLOYMENT.md section 7," -ForegroundColor Yellow
Write-Host "     pointing VITE_API_BASE_URL at https://$serviceUrl" -ForegroundColor Yellow
Write-Host "  2. Test everything end to end." -ForegroundColor Yellow
Write-Host "  3. Run .\nexamart-down.ps1 as soon as you're done to stop paying for it." -ForegroundColor Yellow
