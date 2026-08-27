# NexaMart — AWS Deployment Runbook

Goal: satisfy CS425 rubric criterion 13, Cloud Deployment (up to 2 extra-credit points) —
1 point for a publicly accessible cloud URL, 1 point for the app *and its database* working
correctly in the cloud with credentials managed via environment variables or a secrets service.

## Recommended path: scripted App Runner + RDS (turn it up/down on demand)

Because this deployment is only needed for a short testing window and a single presentation —
not to run continuously — the primary path uses **AWS App Runner** (backend, deployed from a
Docker image in ECR) + **AWS RDS PostgreSQL** (database) + **AWS Amplify Hosting** (frontend).
App Runner and RDS both support a genuine pause/resume and stop/start, so the whole paid stack
can be brought up for testing and torn back down in between, without losing data or having to
recreate anything — which keeps AWS charges to a few cents rather than several days of idle
compute.

This is fully scripted: see **`aws/README.md`** and the five PowerShell scripts in `aws/`
(`nexamart-bootstrap.ps1`, `nexamart-up.ps1`, `nexamart-down.ps1`, `nexamart-destroy.ps1`, and the
shared `nexamart-aws-config.ps1`). Run order:

```powershell
cd NexaMart\aws
.\nexamart-bootstrap.ps1   # once — creates everything, ~15-20 min
.\nexamart-up.ps1          # before testing / before the presentation
.\nexamart-down.ps1        # as soon as you're done, every time
.\nexamart-destroy.ps1     # once, after grading is fully over
```

Prerequisites (AWS CLI configured via `aws configure`, Docker Desktop running, secrets filled
into a gitignored `aws/.env.aws`) and full cost expectations are in `aws/README.md` — read that
file before running anything. The verification checklist in §8 below still applies regardless of
which path you deploy with.

**Status: live and verified.** App (Amplify): https://master.d29cdp99k1zxc4.amplifyapp.com ·
Backend (App Runner): https://nxkxiu5jng.us-east-1.awsapprunner.com · Database: RDS PostgreSQL,
connected. Login and the product catalog confirmed working end to end against the real deployed
stack — see `README.md`'s Cloud Deployment section for the two real bugs this surfaced (a
Postgres/Hibernate type-inference issue and a CORS misconfiguration) and how they were fixed.

---

## Alternative path: Elastic Beanstalk (no Docker, no CLI, console-only)

If you'd rather not use Docker/the AWS CLI at all, Elastic Beanstalk accepts a plain jar upload
through the console. **Trade-off:** Elastic Beanstalk has no first-class pause/resume for a
single-instance environment — manually stopping its EC2 instance risks EB's health checks
auto-replacing it — so this path is better suited to "deploy once, verify, then delete" than to
the repeated up/down cycle. Use this only if you don't want to install Docker.

Estimated time: 60–90 minutes end to end, most of it waiting on AWS provisioning, not active work.

### 0. Prerequisites

- An AWS account, with access to RDS, Elastic Beanstalk, and Amplify in one region (pick one —
  e.g. `us-east-1` — and use it consistently for every resource below).
- Locally: `mvn clean package -DskipTests` inside `nexamart-backend` once, to confirm it builds a
  runnable jar (`target/nexamart-backend-0.1.0.jar`). Fix any build error before touching AWS.
- Your real `OPENAI_API_KEY` and a fresh, long random string for `JWT_SECRET` (don't reuse the
  local dev value) ready to paste in — never commit either to the repo.

### 1. Create the RDS PostgreSQL database

AWS Console → RDS → **Create database**:

- Engine: PostgreSQL (latest 16.x)
- Templates: **Free tier**
- DB instance identifier: `nexamart-db`
- Master username: `nexamart` · Master password: generate a strong one and save it somewhere safe
- Instance class: `db.t3.micro` / `db.t4g.micro` (free-tier eligible)
- Storage: 20 GiB gp3 (free-tier default)
- Connectivity → Public access: **Yes** (simplest networking for a course-project demo window —
  see §2 for the tradeoff this makes, and note it as a deliberate, scoped-down-after-grading
  choice in your README, not a production security posture)
- VPC security group: create new, name it `nexamart-db-sg`
- Additional configuration → Initial database name: `nexamart`
- Create database, then wait (5–10 min) for status **Available**. Copy the **Endpoint** hostname
  and confirm the port (`5432`) from the RDS console — you'll need both in step 5.

### 2. Open the database to your backend

RDS → `nexamart-db-sg` → Inbound rules → **Add rule**: Type `PostgreSQL`, Port `5432`,
Source `Anywhere-IPv4 (0.0.0.0/0)`.

This is intentionally loose so Elastic Beanstalk (which doesn't have a fixed IP by default) can
always reach it without extra VPC networking work. Note explicitly in your README/known-limitations
that this is a scoped-down-for-grading choice — tighten it to a specific security group or VPC
peering rule if this project continues past the course.

### 3. Package the backend

```bash
cd nexamart-backend
mvn clean package -DskipTests
```

This produces `target/nexamart-backend-0.1.0.jar`.

### 4. Deploy the backend to Elastic Beanstalk

AWS Console → Elastic Beanstalk → **Create application**:

- Application name: `nexamart-backend`
- Platform: **Java**, Platform branch: **Corretto 21 running on 64bit Amazon Linux 2023**
- Application code: **Upload your code** → upload `nexamart-backend-0.1.0.jar` from step 3
- Presets: **Single instance (free tier eligible)** — no load balancer, keeps this both cheaper
  and simpler for a course-project demo
- Create environment, then wait (~5 min) for a URL like
  `nexamart-backend-env.eba-xxxxxxxx.us-east-1.elasticbeanstalk.com` and Health = **OK**

### 5. Configure environment variables

This is the "credentials managed securely through environment variables" half of the rubric
point. Elastic Beanstalk → your environment → **Configuration → Software → Environment
properties**, add:

| Key | Value |
|---|---|
| `SPRING_PROFILES_ACTIVE` | `docker` (reuses the existing Postgres profile already in `application.yml` — no code change needed for this part) |
| `DB_HOST` | the RDS endpoint from step 1 |
| `DB_PORT` | `5432` |
| `DB_NAME` | `nexamart` |
| `DB_USER` | `nexamart` |
| `DB_PASSWORD` | the RDS master password from step 1 |
| `JWT_SECRET` | a long random string — **not** the local dev default |
| `OPENAI_API_KEY` | your real OpenAI key |

`application.yml`'s `server.port` already reads `${PORT:8081}` (updated for this deployment) —
Elastic Beanstalk's Java platform sets `PORT` itself, so you don't need to add it manually.

Click **Apply** — the environment restarts and picks up the new configuration (~1–2 min).

### 6. Verify the backend is live

Visit `http://<your-eb-url>/api/products` in a browser. A real JSON array of products means the
backend is up, reading `SPRING_PROFILES_ACTIVE=docker`, and successfully talking to RDS — if you
see a connection error instead, double-check the security group rule from §2 and the `DB_*`
values from §5.

### 7. Deploy the frontend to AWS Amplify Hosting

AWS Console → **AWS Amplify → Host a web app** → connect your GitHub repo (`pracda/nexamart`) →
set the app root / monorepo path to `nexamart-frontend` → confirm build settings (Amplify
auto-detects Vite; build command `npm run build`, output directory `dist` — adjust if it guesses
wrong) → **Environment variables** → add `VITE_API_BASE_URL` = the backend's public URL (the App
Runner URL if you used the scripted path above, or the Elastic Beanstalk URL from step 4 here) →
Save and deploy.

Amplify gives you a public HTTPS URL like `https://main.dxxxxxxxxxxxxx.amplifyapp.com` once the
build finishes (~3–5 min). This is the URL you present and put in the README/deck. This step is
the same regardless of which backend path (App Runner or Elastic Beanstalk) you used.

### 9. Cost control — do this once grading is done (Elastic Beanstalk path)

- Elastic Beanstalk → Application → **Actions → Delete application** (this also terminates the
  underlying EC2 instance)
- RDS → `nexamart-db` → **Actions → Delete** (uncheck "create final snapshot" unless you want one)
- Amplify app: delete it too if you want a fully clean account, though static hosting at this
  scale is effectively free either way

---

## 8. Final verification checklist — maps directly to rubric criterion 13

Applies to whichever path you deployed with:

- [ ] App is reachable at the public Amplify URL with no VPN, no `localhost`, from a machine that
      isn't yours (try your phone on cellular data) — **rubric sub-point 1**
- [ ] Buyer, seller, and admin flows work end-to-end against the deployed backend + RDS, including
      at least one AI feature (confirms `OPENAI_API_KEY` made it through) — **rubric sub-point 2**
- [ ] No secret — DB password, JWT secret, OpenAI key — appears in any committed file; every one
      of them is an environment variable (App Runner `RuntimeEnvironmentVariables` or Elastic
      Beanstalk environment properties), not source — **rubric sub-point 2**
- [ ] Update `README.md`'s Cloud Deployment section, `docs/VISION.md`, and the presentation deck
      with the real live URLs once confirmed
