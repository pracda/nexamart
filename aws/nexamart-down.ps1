# nexamart-down.ps1
# Pauses the App Runner service and stops the RDS database -- this is what actually stops the
# meter. Data is preserved (this is NOT a delete). Run this as soon as you're done testing.

. "$PSScriptRoot\nexamart-aws-config.ps1"

Write-Host "=== NexaMart AWS: turning DOWN ===" -ForegroundColor Cyan
Test-Prerequisites
$state = Load-State

if (-not $state.appRunnerServiceArn -or -not $state.rdsSecurityGroupId) {
    Write-Host "No state.json found. Nothing to turn down (or bootstrap never completed)." -ForegroundColor Yellow
    exit 0
}

# ---- App Runner ----
Write-Host "`nPausing App Runner service..." -ForegroundColor Cyan
$status = aws apprunner describe-service --service-arn $state.appRunnerServiceArn --region $AWS_REGION --query "Service.Status" --output text
Write-Host "Current App Runner status: $status"

if ($status -eq "RUNNING") {
    aws apprunner pause-service --service-arn $state.appRunnerServiceArn --region $AWS_REGION | Out-Null
    do {
        Start-Sleep -Seconds 10
        $status = aws apprunner describe-service --service-arn $state.appRunnerServiceArn --region $AWS_REGION --query "Service.Status" --output text
        Write-Host "  status: $status"
    } while ($status -eq "OPERATION_IN_PROGRESS")
} else {
    Write-Host "App Runner not running (status: $status), nothing to pause"
}

# ---- RDS ----
Write-Host "`nStopping RDS..." -ForegroundColor Cyan
$dbStatus = aws rds describe-db-instances --db-instance-identifier $RDS_INSTANCE_ID --region $AWS_REGION --query "DBInstances[0].DBInstanceStatus" --output text
Write-Host "Current RDS status: $dbStatus"

if ($dbStatus -eq "available") {
    aws rds stop-db-instance --db-instance-identifier $RDS_INSTANCE_ID --region $AWS_REGION | Out-Null
    Write-Host "RDS stop requested (finishes in the background over a few minutes -- you don't need to wait)"
} else {
    Write-Host "RDS not in 'available' state (status: $dbStatus), nothing to stop"
}

Write-Host "`n=== Everything is DOWN ===" -ForegroundColor Green
Write-Host "App Runner: paused (near-zero cost while paused)"
Write-Host "RDS: stopping (storage is still billed, a few cents -- but compute stops)"
Write-Host ""
Write-Host "IMPORTANT: AWS auto-restarts a stopped RDS instance after about 7 days." -ForegroundColor Yellow
Write-Host "If a week passes before your next .\nexamart-up.ps1, just run .\nexamart-down.ps1 again" -ForegroundColor Yellow
Write-Host "to re-stop it, or run .\nexamart-destroy.ps1 once grading is fully done." -ForegroundColor Yellow
