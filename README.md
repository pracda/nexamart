# NexaMart

AI-powered multi-vendor e-commerce marketplace — CS425 SWE course project (Prasiddha Paudel, 618076).

This repo implements **13 of the 15 AI features** from the [NexaMart Vision Document](../NexaMart_Vision_Document_v3.docx),
end-to-end against a real OpenAI backend, across a working Buyer/Seller/Admin/Finance app:

- **Buyer** — NL Product Finder Chatbot, Order Tracking Chatbot, AI Recommendation Engine, AI Product Comparison
- **Seller** — AI Listing Assistant, NL Inventory Query, NL Sales Analytics Chat, AI Pricing Advisor
- **Admin** — AI Dispute Summarizer, NL Report Generator, AI Fraud Detector
- **Finance** — NL Financial Query, AI Anomaly Alerts

Only the two **DevOps** features (NL System Health Query, AI Log Anomaly Explainer) are not implemented —
they need real metrics/log infrastructure this monolith doesn't have, and are flagged as likely out of
scope for a course project rather than faked.

## Scope decisions vs. the vision document

The vision document describes an 11-microservice platform (Kafka, Kubernetes, Pinecone, ClickHouse,
15 AI features). That's the right shape for a *vision* document, but not realistic to stand up from
scratch in a course project. This milestone instead builds a **modular monolith** with the same
domain boundaries (auth / catalog / order / dispute / finance / admin / ai as separate packages), so
it can be split into real microservices later without a rewrite — same pattern used in the Lab8/Lab9
coursework.

| Vision doc | This milestone |
|---|---|
| 11 microservices + API Gateway + Kafka | Single Spring Boot app, packages per domain |
| Postgres + MongoDB + Redis + ClickHouse | H2 (dev, zero setup) or Postgres via Docker Compose |
| Elasticsearch + Pinecone semantic search | Simple SQL `LIKE` search (swap-in point for later) |
| Python/PyTorch recommendation engine | Java heuristic: category affinity + best-sellers fallback |
| Separate payment/payout ledger | Commission/payout figures derived on the fly from real order data (fixed 10% rate) |
| AI reviews review patterns for fraud | No review feature yet — fraud heuristics use price outliers + repeat-order patterns instead |
| 15 AI features | 13 implemented for real; DevOps (2) intentionally out of scope — see below |
| React/Next.js SSR | React + Vite (no SSR — not needed for this milestone) |
| Persistent server-side cart | Client-side cart (localStorage), submitted as one order on checkout |

## Architecture

```
nexamart-backend/   Spring Boot 3 (Java 21), Maven
  com.nexamart.auth            User/Role, JWT issuing + validation, Spring Security config
  com.nexamart.catalog         Category/Product, search, seller CRUD
  com.nexamart.order           Order/OrderItem lifecycle (place, list, status update)
  com.nexamart.dispute         Dispute/DisputeMessage domain, buyer/seller/admin-scoped access
  com.nexamart.recommendation  Buyer recommendation heuristic (category affinity + best-sellers)
  com.nexamart.analytics       Seller + platform-wide sales/dispute aggregation off real order data
  com.nexamart.finance         Commission/payout math + revenue-anomaly detection off real order data
  com.nexamart.admin           Fraud/anomaly heuristics (price outliers, repeat-order patterns)
  com.nexamart.ai              OpenAI function-calling loop, role-scoped tool functions, one-shot generators
  com.nexamart.common          Shared exception handling

nexamart-frontend/  React 18 + TypeScript + Vite
  src/pages          Home (search/recommend/compare), product detail, cart, orders, seller/admin/finance dashboards
  src/components      Navbar, floating AI chat widget
  src/context          Auth (JWT in localStorage) and cart state
```

### How the AI features work

**Chat-based features** (Product Finder, Order Tracking, Inventory Query, Sales Analytics, NL Report
Generator, NL Financial Query) all go through one endpoint: `POST /api/ai/chat { message }` →
`ChatController` → `AiChatService`:

1. Sends the user's message + a system prompt (built for the caller's role) + a **role-scoped tool
   list** to the OpenAI Chat Completions API. Buyers get `search_products`/`get_order_status`; sellers
   get inventory/sales tools; admins get platform revenue + dispute-stats tools; finance gets
   commission/payout tools. Admins get everything. See `ToolFunctions.toolDefinitions(Role)`.
2. If the model requests a tool call, `ToolFunctions` executes it against the real service layer —
   every tool is scoped to the caller server-side (a buyer only sees their own orders, a seller only
   sees their own products/sales; there's no "which seller" parameter for the model to get wrong).
3. The tool result is fed back to the model, which produces the final natural-language reply.
4. Loop is capped at 4 rounds to avoid runaway tool-calling.

**One-shot generators** (AI Listing Assistant, AI Product Comparison, AI Pricing Advisor, AI Dispute
Summarizer) skip tool calling entirely — the caller already has all the data, so the service builds one
prompt, calls OpenAI with `response_format: json_object`, and parses the structured JSON straight into
a response DTO. Nothing is auto-saved; sellers/admins review before acting.

**Heuristic + AI explanation** (AI Recommendation Engine, AI Fraud Detector, AI Anomaly Alerts) compute
real signals in plain Java first (category affinity, price-outlier detection, period-over-period revenue
comparison), then — for Fraud Detector and Anomaly Alerts — hand those signals to GPT to write the
plain-language explanation an admin/finance user actually reads. The Recommendation Engine doesn't need
an LLM call at all; it's a defensible collaborative/content-filtering substitute for the vision doc's
PyTorch service, scoped to what a course timeline allows.

This is the same request → context injection → intent recognition → API execution → response
generation flow described in the vision doc's NL/AI layer section, just implemented as one service
instead of a separate microservice for now.

## Running it

### Backend

Needs Java 21 and Maven (already on this machine).

```bash
cd nexamart-backend
mvn spring-boot:run
```

Runs on `http://localhost:8081` with an embedded H2 database (file-based, in `nexamart-backend/data/`).
On first run it seeds four demo accounts (password `password123` for all):

- `admin@nexamart.dev`
- `seller@nexamart.dev`
- `buyer@nexamart.dev`
- `finance@nexamart.dev`

8 sample products across Electronics / Home & Kitchen / Fashion, one seed order, and one seed dispute
(so the Admin dashboard has something to demo immediately).

To enable the AI features, set `OPENAI_API_KEY` before starting (see `.env.example`):

```bash
export OPENAI_API_KEY=sk-...
mvn spring-boot:run
```

Without a key, everything else works — AI endpoints return a clear error instead of a reply.

To run against Postgres instead of H2: `docker compose up -d` (from the repo root), then
`mvn spring-boot:run -Dspring-boot.run.profiles=docker`.

**Note:** restarting the backend re-signs the JWT secret's session state — any browser tab still holding
an old login token will start getting 403s. Log out and back in after a restart.

### Frontend

Needs Node.js (already on this machine).

```bash
cd nexamart-frontend
npm install
npm run dev
```

Runs on `http://localhost:5173` and talks to the backend at `http://localhost:8081`
(override with `VITE_API_BASE_URL`, see `.env.example`).

### Tests

```bash
cd nexamart-backend
mvn test
```

13 JUnit 5 + Mockito unit tests across Auth, Catalog, and Order services (normal/boundary/error cases).

## Known simplifications (call out in the SRS / next iteration)

- No persistent server-side cart — the cart lives in the browser and becomes one order at checkout.
- Search is SQL `LIKE`, not Elasticsearch/semantic search.
- Order status transitions aren't restricted to seller/admin at the API layer yet (RBAC is enforced
  for product management and admin routes, not yet per-order-status-change).
- NL Inventory Query, NL Sales Analytics, NL Report Generator, and NL Financial Query all use the same
  floating chat widget rather than dedicated tab UIs as the vision doc's feature locations describe —
  the backend/LLM behavior is real, only the UI placement is simplified.
- AI Listing Assistant doesn't use image input (no GPT-4o Vision) — text name + notes only.
- SEO tags from the AI Listing Assistant are shown to the seller but not persisted (`Product` has no
  tags column yet).
- AI Recommendation Engine is a Java heuristic (purchase-history category affinity → best-sellers →
  newest, as fallbacks), not a trained model — no Python/PyTorch service.
- AI Fraud Detector has no review/rating data to analyze (the app doesn't have reviews yet), so it uses
  price-outlier and repeat-order heuristics instead — clearly documented as a substitute signal.
- Commission/payout figures (NL Financial Query, AI Anomaly Alerts) are computed on the fly from real
  order data using a fixed 10% platform commission rate — there is no separate payment/payout ledger.
- DevOps features (NL System Health Query, AI Log Anomaly Explainer) are **not implemented** — they'd
  need real metrics/log infrastructure (Prometheus/ELK) that doesn't exist in this monolith and was
  judged out of scope for a course project rather than faked with placeholder data.

## Next milestones

1. DevOps features, if in scope: would need Spring Boot Actuator + a real log pipeline before an NL
   layer makes sense on top.
2. Persist AI Listing Assistant's SEO tags (`Product.tags`) and surface them in search.
3. Real payment/payout ledger to replace the derived-from-orders commission math.
4. Review/rating feature, which would let AI Fraud Detector use the review-pattern signals the vision
   doc originally describes.
5. Split catalog/order/dispute/finance/admin/ai into separately deployable services once the domain
   boundaries are proven out.
