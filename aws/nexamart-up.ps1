# nexamart-up.ps1
# Resumes the App Runner service and starts the RDS database. Run this before testing or
# demoing. Takes about 2-5 minutes; the script polls and tells you when it's ready.

. "$PSScriptRoot\nexamart-aws-config.ps1"

Write-Host "=== NexaMart AWS: turning UP ===" -ForegroundColor Cyan
Test-Prerequisites
$state = Load-State

if (-not $state.appRunnerServiceArn -or -not $state.rdsSecurityGroupId) {
    Write-Host "No state.json found (or it's incomplete). Run .\nexamart-bootstrap.ps1 first." -ForegroundColor Red
    exit 1
}

# ---- RDS ----
Write-Host "`nStarting RDS..." -ForegroundColor Cyan
$dbStatus = aws rds describe-db-instances --db-instance-identifier $RDS_INSTANCE_ID --region $AWS_REGION --query "DBInstances[0].DBInstanceStatus" --output text
Write-Host "Current RDS status: $dbStatus"

if ($dbStatus -eq "stopped") {
    aws rds start-db-instance --db-instance-identifier $RDS_INSTANCE_ID --region $AWS_REGION | Out-Null
    Write-Host "Waiting for RDS to become available..."
    aws rds wait db-instance-available --db-instance-identifier $RDS_INSTANCE_ID --region $AWS_REGION
} elseif ($dbStatus -eq "available") {
    Write-Host "RDS already available"
} else {
    Write-Host "RDS is in state '$dbStatus' -- waiting for it to settle..."
    aws rds wait db-instance-available --db-instance-identifier $RDS_INSTANCE_ID --region $AWS_REGION
}

# ---- App Runner ----
Write-Host "`nResuming App Runner service..." -ForegroundColor Cyan
$status = aws apprunner describe-service --service-arn $state.appRunnerServiceArn --region $AWS_REGION --query "Service.Status" --output text
Write-Host "Current App Runner status: $status"

if ($status -eq "PAUSED") {
    aws apprunner resume-service --service-arn $state.appRunnerServiceArn --region $AWS_REGION | Out-Null
} elseif ($status -eq "RUNNING") {
    Write-Host "App Runner already running"
}

do {
    Start-Sleep -Seconds 10
    $status = aws apprunner describe-service --service-arn $state.appRunnerServiceArn --region $AWS_REGION --query "Service.Status" --output text
    Write-Host "  status: $status"
} while ($status -eq "OPERATION_IN_PROGRESS")

if ($status -ne "RUNNING") {
    Write-Host "App Runner did not reach RUNNING (ended at $status) -- check the AWS console." -ForegroundColor Red
    exit 1
}

Write-Host "`n=== Everything is UP ===" -ForegroundColor Green
Write-Host "Backend URL: https://$($state.appRunnerServiceUrl)"
Write-Host "Try it:      https://$($state.appRunnerServiceUrl)/api/products"
Write-Host "`nRemember to run .\nexamart-down.ps1 when you're done testing." -ForegroundColor Yellow
