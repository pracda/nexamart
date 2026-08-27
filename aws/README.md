# NexaMart AWS scripts — turn it up, test it, turn it down

These scripts deploy NexaMart to AWS for testing, and let you pause/resume the paid pieces with
one command each so you're not paying for idle compute between now and the presentation.

**Architecture:** AWS App Runner (backend, from a Docker image in ECR) + AWS RDS PostgreSQL
(database) + AWS Amplify Hosting (frontend). App Runner and RDS both support a real pause/stop
and resume/start, which is why this combination was chosen over the Elastic-Beanstalk-based
`docs/DEPLOYMENT.md` runbook — a single-instance Elastic Beanstalk environment has no clean
pause/resume; stopping its EC2 instance by hand risks EB's health checks auto-replacing it.

## What each script does

| Script | What it does | When to run it |
|---|---|---|
| `nexamart-bootstrap.ps1` | Creates everything from scratch: ECR repo, builds & pushes the Docker image, IAM role, RDS database, App Runner service | **Once**, right now |
| `nexamart-up.ps1` | Resumes App Runner + starts RDS | Before you test, and again shortly before the Aug 29 presentation |
| `nexamart-down.ps1` | Pauses App Runner + stops RDS (keeps all data) | As soon as you're done testing, every time |
| `nexamart-destroy.ps1` | Permanently deletes everything (asks you to type DELETE to confirm) | Once, after grading is fully over |

In practice `nexamart-up.ps1` and `nexamart-down.ps1` are the two you'll run repeatedly.
Bootstrap runs once at the start; destroy runs once at the very end.

## Prerequisites (do this before running anything)

1. **AWS CLI** installed and configured: run `aws configure` and paste in an access key, secret
   key, and region from your AWS account (IAM → Users → your user → Security credentials →
   Create access key). A personal AWS account with admin-level permissions is fine for this —
   the scripts touch ECR, App Runner, RDS, EC2 (security groups), and IAM.
2. **Docker Desktop** installed and running — you already have this, since the repo's
   `docker-compose.yml` needs it for local development.
3. Copy `aws\.env.aws.example` to `aws\.env.aws` and fill in three values: a strong
   `DB_PASSWORD`, a fresh long random `JWT_SECRET` (don't reuse the local dev default), and your
   real `OPENAI_API_KEY`. **This file is gitignored and will never be committed.**

## Run order

```powershell
cd NexaMart\aws

# 1. One-time setup (~15-20 min, mostly waiting on RDS/App Runner provisioning)
.\nexamart-bootstrap.ps1

# 2. Test the deployed app -- the backend URL is printed at the end of bootstrap,
#    and again every time you run nexamart-up.ps1

# 3. Done testing for now -- stop paying for it
.\nexamart-down.ps1

# ... days pass ...

# 4. Shortly before presenting on Aug 29 -- bring it back up
.\nexamart-up.ps1

# 5. After grading is fully done -- delete everything permanently
.\nexamart-destroy.ps1
```

## The frontend (Amplify) — one manual step, not part of the toggle

The scripts only manage the backend and database, because those are what actually cost money
while idle. AWS Amplify Hosting (the frontend) costs effectively nothing at rest, so it's set up
once, manually, and left running rather than folded into the up/down cycle. Follow
`docs/DEPLOYMENT.md` section 7, pointing `VITE_API_BASE_URL` at the App Runner URL printed by
`nexamart-bootstrap.ps1`. If you run `nexamart-down.ps1`, the Amplify frontend stays live but
shows connection errors until you run `nexamart-up.ps1` again — that's expected between testing
sessions, not a bug.

## Cost expectations

- **App Runner, paused:** effectively $0 — App Runner only bills for provisioned/active compute.
- **App Runner, running:** roughly $0.03–0.06/hour at the smallest instance size used here (1
  vCPU / 2GB) — a few cents per testing session.
- **RDS, stopped:** storage only, about $0.023/GB-month → roughly $0.46/month for the 20GB
  instance, prorated to pennies for a few days.
- **RDS, running:** `db.t4g.micro` runs a few cents/hour.
- **Amplify:** effectively free at this scale (static hosting, low request volume).

Realistic total for bootstrap plus a handful of test sessions plus the presentation: well under
$2, and likely covered by AWS free-tier credits on a new account regardless.

**Important RDS caveat:** AWS automatically restarts a stopped RDS instance after about 7 days —
it isn't allowed to stay stopped forever. If more than a week passes between `nexamart-down.ps1`
and your next `nexamart-up.ps1`, just run `nexamart-down.ps1` again to re-stop it; otherwise it
quietly starts billing compute again on its own.

## Files in this folder

- `nexamart-aws-config.ps1` — shared settings and helper functions, dot-sourced by the other
  scripts. You shouldn't need to run this one directly.
- `nexamart-bootstrap.ps1`, `nexamart-up.ps1`, `nexamart-down.ps1`, `nexamart-destroy.ps1` — see
  the table above.
- `.env.aws.example` — template for your secrets. Copy to `.env.aws` and fill in (gitignored).
- `state.json` — created automatically after bootstrap; records the AWS resource IDs so the
  other scripts don't need you to copy/paste ARNs around. Gitignored — it's specific to your AWS
  account and would be meaningless (or wrong) for anyone else who clones the repo.
- `.gitignore` — excludes `.env.aws` and `state.json` from the repo.

## Troubleshooting

- **"AWS CLI is not configured / not authenticated"** — run `aws configure` first.
- **`nexamart-bootstrap.ps1` fails partway through** — it's safe to just run it again; every
  step checks whether its resource already exists before trying to create it.
- **App Runner stuck showing `OPERATION_IN_PROGRESS`** — normal for the first deploy (up to
  about 5 minutes) and for pause/resume (1–2 minutes). The scripts poll and wait automatically;
  you don't need to intervene.
- **RDS security-group deletion fails in `nexamart-destroy.ps1`** — RDS itself is usually still
  finishing its own deletion in the background. Wait a couple of minutes and run the script
  again.
- **Made a code change and want to redeploy?** Re-run `nexamart-bootstrap.ps1` — it rebuilds and
  pushes a fresh image (the ECR/RDS/App-Runner "already exists" checks skip straight to that
  step), then trigger a new deployment with:
  `aws apprunner start-deployment --service-arn <arn> --region us-east-1`
  (the ARN is saved in `state.json` as `appRunnerServiceArn`). This is intentionally separate
  from the up/down cost-control scripts, which never touch the deployed image.
