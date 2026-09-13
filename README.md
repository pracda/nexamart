# NexaMart — AI-Powered Multi-Vendor E-Commerce Marketplace

**A full-featured e-commerce marketplace** with **13 AI-powered features** running against OpenAI in production. Built with Spring Boot, React, and PostgreSQL, featuring roles for buyers, sellers, admins, and finance teams.

## 🎯 Overview

NexaMart is a complete B2B/B2C marketplace platform showcasing:
- **Multi-vendor architecture** — Sellers manage inventory and pricing
- **Smart commerce AI** — Product recommendations, pricing advice, dispute resolution
- **Platform analytics** — Real-time insights, fraud detection, anomaly alerts
- **Buyer & seller tools** — Chat-based natural language interfaces
- **Payment & commission management** — Automated payouts and analytics
- **Scalable design** — Modular monolith ready to split into microservices

## ✨ AI Features Implemented

### Buyer Features
- 🤖 **AI Product Finder Chatbot** — Natural language product search
- 📦 **Order Tracking Chatbot** — "Where's my order?" with AI context
- 🎯 **AI Recommendation Engine** — Smart suggestions based on purchase history
- 🔄 **AI Product Comparison** — Side-by-side analysis of similar products

### Seller Features
- 📝 **AI Listing Assistant** — Draft product listings with SEO tags
- 📊 **NL Inventory Query** — "How many units do I have in fashion?" 
- 📈 **NL Sales Analytics Chat** — Trend analysis and performance queries
- 💰 **AI Pricing Advisor** — Dynamic pricing suggestions based on demand

### Admin Features
- ⚖️ **AI Dispute Summarizer** — Auto-summarize buyer/seller conflicts
- 📋 **NL Report Generator** — "Revenue by category this month?"
- 🚨 **AI Fraud Detector** — Identify suspicious orders and patterns

### Finance Features
- 💳 **NL Financial Query** — Commission and payout queries
- 📊 **AI Anomaly Alerts** — Detect unusual revenue patterns

**Not Implemented (intentionally):**
- DevOps features (NL System Health, Log Anomaly Explainer) — would need real Prometheus/ELK

## 🛠️ Tech Stack

| Layer | Technology |
|---|---|
| **Backend** | Java 21, Spring Boot 3, Spring Security, Spring Data JPA, Maven |
| **Frontend** | React 18, TypeScript, Vite, Bootstrap 5 |
| **Database** | PostgreSQL 16 (prod), H2 (dev) |
| **AI** | OpenAI Chat Completions API (GPT-4o-mini, function calling) |
| **Authentication** | JWT (self-issued), BCrypt password hashing |
| **Testing** | JUnit 5, Mockito |
| **Cloud** | AWS App Runner (backend), RDS (database), Amplify (frontend) |
| **Build & CI** | Maven, GitHub Actions, Docker |

## 📁 Project Structure

```
nexamart-backend/
├── src/main/java/com/nexamart/
│   ├── auth/               # JWT, Spring Security, authentication
│   ├── catalog/            # Products, categories, search
│   ├── order/              # Order lifecycle, status tracking
│   ├── dispute/            # Buyer-seller disputes, messaging
│   ├── recommendation/     # Heuristic recommendation engine
│   ├── analytics/          # Seller & platform sales aggregation
│   ├── finance/            # Commission math, payout tracking, anomalies
│   ├── admin/              # Fraud detection heuristics
│   ├── ai/                 # OpenAI integration, tool functions, chat
│   └── common/             # Exception handling, DTOs
├── src/main/resources/
│   ├── application.yml     # Spring config
│   ├── db/migration/       # Flyway schema migrations
│   └── data.sql           # Seed data (demo accounts, products)
└── pom.xml

nexamart-frontend/
├── src/
│   ├── app/
│   │   ├── modules/
│   │   │   ├── student/   # Student dashboard (enrolled courses)
│   │   │   ├── seller/    # Seller dashboard (inventory, pricing)
│   │   │   ├── admin/     # Admin (disputes, fraud, revenue)
│   │   │   └── finance/   # Finance (payouts, commission)
│   │   ├── shared/        # Common components, pipes
│   │   └── services/      # API client, auth, state management
│   ├── assets/            # Images, styles
│   └── main.ts
├── vite.config.ts
└── package.json

aws/                        # Cost-controlled AWS deployment
├── nexamart-up.ps1        # Resume backend + database
├── nexamart-down.ps1      # Pause backend + database
├── nexamart-bootstrap.ps1 # One-time setup
└── nexamart-destroy.ps1   # Delete everything
```

## 🚀 Quick Start

### Option 1: Docker Compose (Recommended)

```bash
# From repo root
docker compose up -d

# Backend: http://localhost:8081
# Frontend: http://localhost:5173
# Database: postgres://localhost:5432 (postgres/postgres)
```

Demo credentials (password: `password123`):
- `buyer@nexamart.dev` — Buyer role
- `seller@nexamart.dev` — Seller role
- `admin@nexamart.dev` — Admin role
- `finance@nexamart.dev` — Finance role

### Option 2: Manual (Backend + Frontend Separate)

**Backend:**
```bash
cd nexamart-backend
export OPENAI_API_KEY=sk-...  # For AI features
mvn spring-boot:run
# Runs on http://localhost:8081
```

**Frontend:**
```bash
cd nexamart-frontend
npm install
npm run dev
# Runs on http://localhost:5173
```

## 🎓 Core Features by Role

### 👤 Buyer
- ✅ Browse products by category
- ✅ Use **AI Product Finder** to search naturally ("show me wireless headphones under $50")
- ✅ View **AI Product Comparison** between selected items
- ✅ Receive **AI Recommendations** based on purchase history
- ✅ Check order status with **Order Tracking Chatbot**
- ✅ Rate products, track shipments

### 🏪 Seller
- ✅ List and manage products
- ✅ Use **AI Listing Assistant** to draft new product pages
- ✅ Query inventory with **NL Inventory Chat** ("Low stock items?")
- ✅ Analyze sales with **Sales Analytics Chat**
- ✅ Get **AI Pricing Advice** based on demand and competition
- ✅ Manage promotions and discounts

### 🛡️ Admin
- ✅ Resolve **buyer-seller disputes** (with **AI Dispute Summarizer**)
- ✅ Detect **fraud** with AI heuristics (price outliers, repeat abuse patterns)
- ✅ Generate **business reports** ("Revenue by category?")
- ✅ Manage users, categories, platform settings
- ✅ Monitor platform health and trends

### 💰 Finance
- ✅ View commission and payout figures
- ✅ Query financials with **NL Financial Chat**
- ✅ Get **AI Anomaly Alerts** on unusual revenue patterns
- ✅ Export reports for accounting

## 🤖 How AI Features Work

### Chat-Based Features
**Product Finder, Order Tracking, Inventory Query, Sales Analytics, NL Report Generator, NL Financial Query** all use the same pattern:

```
User message
    ↓
AiChatService.chat(message, userRole)
    ↓
Build system prompt (role-specific context)
    ↓
Send to OpenAI Chat Completions with tool definitions
    ↓
If model requests a tool:
  → Execute it with role-scoped data access
  → Feed result back to model
    ↓
Model generates final natural-language response
    ↓
Return to user
```

**Security:** Every tool call is scoped server-side to the authenticated user (e.g., sellers only see their own products, buyers only their own orders).

### One-Shot Generators
**AI Listing Assistant, AI Product Comparison, AI Pricing Advisor, AI Dispute Summarizer** send one request to OpenAI with `response_format: json_object`:

```
User provides data (product info, dispute messages, etc.)
    ↓
Build prompt with full context
    ↓
Call OpenAI with JSON schema
    ↓
Parse JSON response
    ↓
Return structured data to user (not auto-saved — user reviews first)
```

### Heuristic + AI Explanation
**AI Recommendation Engine, AI Fraud Detector, AI Anomaly Alerts** compute signals in Java first, then ask GPT for explanations:

```
Compute Java signals:
  - Category affinity (user purchase history)
  - Price outliers (for fraud)
  - Revenue changes (for anomalies)
    ↓
Ask GPT: "Explain why this order is suspicious" (signals provided)
    ↓
Return heuristic result + AI-generated explanation to user
```

## 🔐 Security

### Authentication & Authorization
- Passwords hashed with BCrypt (never stored plain)
- JWT tokens (self-issued, validated server-side)
- Role-based access control (RBAC) enforced on API layer (not just UI)
- Authorization is server-side — hiding a button doesn't protect data

### Input Validation
- 14 `@Valid`-annotated request DTOs reject malformed input
- Fields: `@NotBlank`, `@Email`, `@Min`, `@Max`, etc.
- Prevents XSS, injection, oversized payloads

### Secrets Management
- `.env` file (gitignored) for development
- Environment variables for production (no secrets committed)
- OpenAI API key never logged or exposed

### AI Tool Scoping
- Every tool function receives the authenticated user's ID from JWT
- Sellers can't request another seller's data
- Buyers can't access admin features
- Scoping is server-side (model can't bypass it)

## 📊 Database Schema

**Key Tables:**
- `users` — Accounts with roles and encrypted passwords
- `products` — Seller inventory
- `categories` — Product taxonomy
- `orders` — Buyer orders + order items
- `disputes` — Buyer/seller conflicts + messages
- `analytics_snapshot` — Cached sales data for queries
- `commission_log` — Payout history

**Indexes:** On frequently queried columns (user_id, seller_id, created_at) for performance.

## 🧪 Testing

```bash
cd nexamart-backend
mvn test
```

Results: **13/13 tests passing**
- Auth (JWT issuance, validation, expiration)
- Catalog (ownership checks, search)
- Order (stock decrement, insufficient-stock handling)

## 🚢 Cloud Deployment

### AWS (Production)

**Live URLs:**
- **Frontend** — https://master.d29cdp99k1zxc4.amplifyapp.com
- **Backend** — https://nxkxiu5jng.us-east-1.awsapprunner.com/api/products

**Architecture:**
- **App Runner** — Backend (pause/resume for cost control)
- **RDS PostgreSQL** — Database (stop/start between testing)
- **Amplify** — Frontend (always on, negligible cost)

**Scripts (PowerShell):**
```powershell
cd aws

# First time setup
.\nexamart-bootstrap.ps1    # ~15-20 min (provisions everything)

# Before testing
.\nexamart-up.ps1           # Resume backend + start database

# After testing
.\nexamart-down.ps1         # Pause backend + stop database

# After grading
.\nexamart-destroy.ps1      # Delete everything
```

**Cost:** ~$0.03–0.06/hour when running. Paused: ~$0.50/month (storage only).

## 📚 Documentation

- `docs/VISION.md` — Problem statement, scope, stakeholders, feature list
- `docs/SRS.md` — System Requirements Specification with 17 use cases
- `docs/diagrams.md` — Architecture, sequence, collaboration diagrams
- `docs/DEPLOYMENT.md` — Full AWS deployment runbook
- `aws/README.md` — Cost control scripts

## 🤝 Contributing

1. Fork the repository
2. Create a feature branch: `git checkout -b feature/my-feature`
3. Write tests first (JUnit 5 + Mockito)
4. Commit: `git commit -am 'Add feature'`
5. Push: `git push origin feature/my-feature`
6. Open a Pull Request

## 📝 License

[MIT License](LICENSE)

---

**Questions?** Open an issue or check `docs/` for detailed guides.

**Ready to build?** This is a complete, production-grade foundation for your marketplace. Happy selling! 🚀
