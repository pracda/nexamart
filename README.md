# NexaMart

AI-powered multi-vendor e-commerce marketplace — CS425 SWE course project (Prasiddha Paudel, 618076).

This repo is the first coded milestone toward the [NexaMart Vision Document](../NexaMart_Vision_Document_v3.docx):
a working backend + frontend with real Buyer/Seller/Admin flows, plus 5 of the vision doc's 15 AI
features implemented end-to-end via OpenAI (function calling for the chat-based ones, a structured
one-shot completion for the listing generator):

- **Buyer** — NL Product Finder Chatbot, Order Tracking Chatbot
- **Seller** — AI Listing Assistant, NL Inventory Query, NL Sales Analytics Chat

## Scope decisions vs. the vision document

The vision document describes an 11-microservice platform (Kafka, Kubernetes, Pinecone, ClickHouse,
15 AI features). That's the right shape for a *vision* document, but not realistic to stand up from
scratch in a course project. This milestone instead builds a **modular monolith** with the same
domain boundaries (auth / catalog / order / ai as separate packages), so it can be split into real
microservices later without a rewrite — same pattern used in the Lab8/Lab9 coursework.

| Vision doc | This milestone |
|---|---|
| 11 microservices + API Gateway + Kafka | Single Spring Boot app, packages per domain |
| Postgres + MongoDB + Redis + ClickHouse | H2 (dev, zero setup) or Postgres via Docker Compose |
| Elasticsearch + Pinecone semantic search | Simple SQL `LIKE` search (swap-in point for later) |
| 15 AI features | 5 implemented for real (see above); Admin/Finance/DevOps features need modules that don't exist yet |
| React/Next.js SSR | React + Vite (no SSR — not needed for this milestone) |
| Persistent server-side cart | Client-side cart (localStorage), submitted as one order on checkout |

## Architecture

```
nexamart-backend/   Spring Boot 3 (Java 21), Maven
  com.nexamart.auth        User/Role, JWT issuing + validation, Spring Security config
  com.nexamart.catalog     Category/Product, search, seller CRUD
  com.nexamart.order       Order/OrderItem lifecycle (place, list, status update)
  com.nexamart.analytics   Seller sales aggregation (revenue, units, top products) off order data
  com.nexamart.ai          OpenAI function-calling loop, role-scoped tool functions, listing generator
  com.nexamart.common      Shared exception handling

nexamart-frontend/  React 18 + TypeScript + Vite
  src/pages          Home/search, product detail, cart, orders, login/register, seller dashboard
  src/components      Navbar, floating AI chat widget
  src/context          Auth (JWT in localStorage) and cart state
```

### How the AI features work

**Chat-based features** (Product Finder, Order Tracking, Inventory Query, Sales Analytics) all go
through one endpoint: `POST /api/ai/chat { message }` → `ChatController` → `AiChatService`:

1. Sends the user's message + a system prompt (built for the caller's role) + a **role-scoped tool
   list** to the OpenAI Chat Completions API. Buyers get `search_products`/`get_order_status`;
   sellers get `list_my_products`/`get_low_stock_products`/`get_seller_sales_summary`; admins get both
   sets. See `ToolFunctions.toolDefinitions(Role)`.
2. If the model requests a tool call, `ToolFunctions` executes it against the real `ProductService` /
   `OrderService` / `SellerAnalyticsService` — every tool is scoped to the caller (a buyer only sees
   their own orders, a seller only sees their own products/sales; there's no "which seller" parameter
   for the model to get wrong).
3. The tool result is fed back to the model, which produces the final natural-language reply.
4. Loop is capped at 4 rounds to avoid runaway tool-calling.

**AI Listing Assistant** is a separate, simpler flow — no tool calling needed since it doesn't need to
read app data, just transform a rough product name into a listing: `POST /api/seller/ai/generate-listing`
→ `ListingAssistantService` sends one system+user message pair to OpenAI with `response_format:
json_object`, parses the JSON into title/description/bullets/SEO tags/category, and returns it for the
seller to review and edit before publishing (nothing is auto-saved).

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
On first run it seeds three demo accounts (password `password123` for all):

- `admin@nexamart.dev`
- `seller@nexamart.dev`
- `buyer@nexamart.dev`

and 8 sample products across Electronics / Home & Kitchen / Fashion.

To enable the AI chat, set `OPENAI_API_KEY` before starting (see `.env.example`):

```bash
export OPENAI_API_KEY=sk-...
mvn spring-boot:run
```

Without a key, everything else works — the chat endpoint just returns a clear error instead of a reply.

To run against Postgres instead of H2: `docker compose up -d` (from the repo root), then
`mvn spring-boot:run -Dspring-boot.run.profiles=docker`.

### Frontend

Needs Node.js (already on this machine).

```bash
cd nexamart-frontend
npm install
npm run dev
```

Runs on `http://localhost:5173` and talks to the backend at `http://localhost:8081`
(override with `VITE_API_BASE_URL`, see `.env.example`).

## Known simplifications (call out in the SRS / next iteration)

- No persistent server-side cart — the cart lives in the browser and becomes one order at checkout.
- Search is SQL `LIKE`, not Elasticsearch/semantic search.
- Order status transitions aren't restricted to seller/admin at the API layer yet (RBAC is enforced
  for product management and admin routes, not yet per-order-status-change).
- Only 5 of the vision doc's 15 AI features are wired to a real LLM; the rest are future milestones.
- NL Inventory Query and NL Sales Analytics Chat use the same floating chat widget as the buyer
  features, rather than dedicated "Inventory" / "Analytics" tab UIs as the vision doc's feature
  locations describe — the backend/LLM behavior is real, only the UI placement is simplified.
- AI Listing Assistant doesn't use image input (no GPT-4o Vision) — text name + notes only.
- SEO tags from the AI Listing Assistant are shown to the seller but not persisted (`Product` has no
  tags column yet).

## Next milestones

1. Admin module (dispute data model) + AI Dispute Summarizer + NL Report Generator — biggest lift
   since no admin-facing data exists yet.
2. Finance module (payouts/commissions data model) + NL Financial Query + AI Anomaly Alerts.
3. AI Recommendation Engine and AI Product Comparison for buyers.
4. AI Pricing Advisor for sellers (compare against similar listings' prices).
5. DevOps features (NL System Health Query, AI Log Anomaly Explainer) need real metrics/log
   infrastructure first — flag scope with the instructor before starting, likely out of bounds for
   a course-project monolith.
6. Split catalog/order/ai into separately deployable services once the domain boundaries are proven out.
