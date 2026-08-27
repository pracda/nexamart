# nexamart-aws-config.ps1
# Shared configuration and helper functions for the NexaMart AWS scripts.
# This file is dot-sourced by the other scripts (bootstrap/up/down/destroy) — you should not
# need to run it directly.

$ErrorActionPreference = "Stop"

# ---- Fixed project settings (safe to edit if you want a different region or resource names,
#      just do it before running bootstrap) ----
$Script:AWS_REGION         = "us-east-1"
$Script:ECR_REPO_NAME       = "nexamart-backend"
$Script:APP_RUNNER_SERVICE  = "nexamart-backend"
$Script:RDS_INSTANCE_ID     = "nexamart-db"
$Script:RDS_DB_NAME         = "nexamart"
$Script:RDS_MASTER_USERNAME = "nexamart"
$Script:RDS_INSTANCE_CLASS  = "db.t4g.micro"
$Script:RDS_STORAGE_GB      = 20
$Script:APP_PORT            = 8081
$Script:IAM_ROLE_NAME       = "AppRunnerECRAccessRole-nexamart"

$Script:ScriptDir  = $PSScriptRoot
$Script:StateFile  = Join-Path $Script:ScriptDir "state.json"
$Script:EnvFile    = Join-Path $Script:ScriptDir ".env.aws"
$Script:RepoRoot   = Split-Path -Parent $Script:ScriptDir
$Script:BackendDir = Join-Path $Script:RepoRoot "nexamart-backend"

function Load-Secrets {
    if (-not (Test-Path $Script:EnvFile)) {
        Write-Host "Missing $($Script:EnvFile)" -ForegroundColor Red
        Write-Host "Copy aws\.env.aws.example to aws\.env.aws and fill in real values first." -ForegroundColor Yellow
        exit 1
    }
    $secrets = @{}
    Get-Content $Script:EnvFile | ForEach-Object {
        $line = $_.Trim()
        if ($line -and -not $line.StartsWith("#") -and $line.Contains("=")) {
            $parts = $line.Split("=", 2)
            $secrets[$parts[0].Trim()] = $parts[1].Trim()
        }
    }
    foreach ($k in @("DB_PASSWORD", "JWT_SECRET", "OPENAI_API_KEY")) {
        if (-not $secrets.ContainsKey($k) -or [string]::IsNullOrWhiteSpace($secrets[$k])) {
            Write-Host "aws\.env.aws is missing a value for $k" -ForegroundColor Red
            exit 1
        }
    }
    return $secrets
}

function Load-State {
    if (Test-Path $Script:StateFile) {
        return Get-Content $Script:StateFile -Raw | ConvertFrom-Json
    }
    return [PSCustomObject]@{
        accountId              = $null
        ecrRepoUri             = $null
        appRunnerAccessRoleArn = $null
        appRunnerServiceArn    = $null
        appRunnerServiceUrl    = $null
        rdsSecurityGroupId     = $null
        vpcId                  = $null
    }
}

function Save-State($state) {
    $state | ConvertTo-Json -Depth 5 | Set-Content $Script:StateFile
}

function Test-Prerequisites {
    $missing = @()
    if (-not (Get-Command aws -ErrorAction SilentlyContinue))    { $missing += "AWS CLI (aws)" }
    if (-not (Get-Command docker -ErrorAction SilentlyContinue)) { $missing += "Docker" }
    if ($missing.Count -gt 0) {
        Write-Host "Missing required tools: $($missing -join ', ')" -ForegroundColor Red
        exit 1
    }
    try {
        aws sts get-caller-identity --region $Script:AWS_REGION | Out-Null
    } catch {
        Write-Host "AWS CLI is not configured / not authenticated. Run 'aws configure' first." -ForegroundColor Red
        exit 1
    }
}

function Get-AccountId {
    return (aws sts get-caller-identity --query Account --output text --region $Script:AWS_REGION)
}

# Runs an AWS CLI "does this exist yet" lookup without letting a routine "not found" (non-zero
# exit code) turn into a terminating exception under $ErrorActionPreference = "Stop" -- this
# matters on PowerShell 7.3+, where native-command failures respect ErrorActionPreference by
# default. Returns $null on any failure instead of throwing.
function Invoke-AwsQuery([scriptblock]$Block) {
    $prevPref = $ErrorActionPreference
    $ErrorActionPreference = "SilentlyContinue"
    try {
        $result = & $Block 2>$null
    } catch {
        $result = $null
    } finally {
        $ErrorActionPreference = $prevPref
    }
    return $result
}
