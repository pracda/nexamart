# nexamart-destroy.ps1
# Permanently deletes every AWS resource these scripts created: the App Runner service, the RDS
# instance (and its data), the ECR image, the security group, and the IAM role. Run this ONLY
# after your presentation and grading are fully done -- this is not reversible.

. "$PSScriptRoot\nexamart-aws-config.ps1"

Write-Host "=== NexaMart AWS: DESTROY (permanent) ===" -ForegroundColor Red
Write-Host "This deletes the RDS database (all data), the App Runner service, the ECR image," -ForegroundColor Red
Write-Host "the security group, and the IAM role. This cannot be undone." -ForegroundColor Red
$confirm = Read-Host "Type DELETE to confirm"
if ($confirm -ne "DELETE") {
    Write-Host "Aborted -- nothing was deleted."
    exit 0
}

Test-Prerequisites
$state = Load-State

if ($state.appRunnerServiceArn) {
    Write-Host "`nDeleting App Runner service..." -ForegroundColor Cyan
    try {
        aws apprunner delete-service --service-arn $state.appRunnerServiceArn --region $AWS_REGION | Out-Null
    } catch {
        Write-Host "Could not delete App Runner service (it may already be gone)." -ForegroundColor Yellow
    }
}

Write-Host "`nDeleting RDS instance (no final snapshot)..." -ForegroundColor Cyan
$dbExists = Invoke-AwsQuery { aws rds describe-db-instances --db-instance-identifier $RDS_INSTANCE_ID --region $AWS_REGION }
if ($dbExists) {
    aws rds delete-db-instance --db-instance-identifier $RDS_INSTANCE_ID --skip-final-snapshot --region $AWS_REGION | Out-Null
    Write-Host "RDS deletion requested (finishes in the background over a few minutes)"
} else {
    Write-Host "No RDS instance found, nothing to delete"
}

if ($state.rdsSecurityGroupId) {
    Write-Host "`nDeleting security group (this can fail if RDS is still finishing deletion -- rerun this script in a few minutes if so)..." -ForegroundColor Cyan
    try {
        aws ec2 delete-security-group --group-id $state.rdsSecurityGroupId --region $AWS_REGION | Out-Null
    } catch {
        Write-Host "Could not delete security group yet -- RDS may still be deleting. Rerun this script in a few minutes." -ForegroundColor Yellow
    }
}

Write-Host "`nDeleting ECR repository (and its images)..." -ForegroundColor Cyan
try {
    aws ecr delete-repository --repository-name $ECR_REPO_NAME --force --region $AWS_REGION | Out-Null
} catch {
    Write-Host "Could not delete ECR repository (it may already be gone)." -ForegroundColor Yellow
}

if ($state.appRunnerAccessRoleArn) {
    Write-Host "`nDeleting IAM role..." -ForegroundColor Cyan
    try {
        aws iam detach-role-policy --role-name $IAM_ROLE_NAME --policy-arn "arn:aws:iam::aws:policy/service-role/AWSAppRunnerServicePolicyForECRAccess" | Out-Null
        aws iam delete-role --role-name $IAM_ROLE_NAME | Out-Null
    } catch {
        Write-Host "Could not delete IAM role -- delete it manually from the IAM console if needed (role: $IAM_ROLE_NAME)." -ForegroundColor Yellow
    }
}

Remove-Item $StateFile -ErrorAction SilentlyContinue

Write-Host "`n=== Destroy complete ===" -ForegroundColor Green
Write-Host "Don't forget to also delete the Amplify app from the console if you created one" -ForegroundColor Yellow
Write-Host "(Amplify -> your app -> Actions -> Delete app)." -ForegroundColor Yellow
