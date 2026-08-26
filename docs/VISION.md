# NexaMart — Vision Document (Delivered-Scope Revision)

CS425 Software Engineering · Prasiddha Paudel · Student ID 618076
Original: `NexaMart_Vision_Document_v3.docx` (Assignment 1, June 2026).
This revision keeps the original problem/positioning/stakeholder framing intact and marks,
feature by feature, what was actually delivered in the final submission vs. what remains
aspirational. It is the authoritative "features, assumptions, and constraints" reference
required for the final submission; the original `.docx` is kept for the record of how the idea
started.

## 1. Problem

Online commerce is fragmented and unintelligent by default. Shoppers get keyword search, not
conversation — poor discovery, high cart abandonment. SMB sellers manage listings by hand with
no AI writing help and no natural-language insight into their own inventory or sales. Internal
teams — admins, finance — have no natural-language access to platform data at all. The result is
lost sales, manual overhead, and no business intelligence layer that scales with the platform.

## 2. Purpose & Product Position

NexaMart is an AI-powered, multi-vendor e-commerce marketplace where every stakeholder — Buyer,
Seller, Admin, and Finance — gets at least one real natural-language AI feature backed by a
shared AI layer, instead of a chatbot bolted on as an afterthought. The differentiator is
breadth (every stakeholder, not just the shopper) and honesty (features are either real against
live data, or explicitly marked deferred — never faked against placeholder data).

## 3. Scope

**Delivered in this submission:** a modular-monolith Spring Boot 3 backend and a React 18
frontend implementing 13 of the vision's 15 AI features end-to-end against a real OpenAI
backend, across a working Buyer / Seller / Admin / Finance application. See §5 for the full
feature-by-feature status.

**Not in scope for this submission:** the 11-microservice deployment topology, Kafka event
streaming, Kubernetes autoscaling, Elasticsearch/Pinecone search, a Python/PyTorch
recommendation service, a separate payment/payout ledger, and the two DevOps AI features. These
remain the target for a future milestone — see README "Next milestones."

## 4. Stakeholders

| Stakeholder | Interest |
|---|---|
| **Buyer** | Find and buy products quickly; get help via conversation instead of manual filters. |
| **Seller (SMB)** | Reduce listing effort; understand their own sales/inventory without a BI tool. |
| **Admin** | Resolve disputes and understand platform health without querying a database directly. |
| **Finance** | Get revenue/payout figures and anomaly alerts in plain language. |
| **DevOps** *(not served in this milestone)* | Would want NL system-health/log queries — deferred, no infrastructure to query yet. |
| **Course instructor / grader** | Needs a working, honestly-scoped, well-tested, documented system to evaluate against the CS425 rubric. |

## 5. Features — Delivered vs. Deferred

| # | Stakeholder | Feature | Status |
|---|---|---|---|
| 1 | Buyer | NL Product Finder Chatbot | ✅ Delivered — function calling against live catalog |
| 2 | Buyer | AI Order Tracking Chatbot | ✅ Delivered — scoped to authenticated buyer |
| 3 | Buyer | AI Recommendation Engine | ✅ Delivered — Java heuristic (category affinity → best-sellers → newest) |
| 4 | Buyer | AI Product Comparison | ✅ Delivered — structured JSON generation |
| 5 | Seller | AI Listing Assistant | ✅ Delivered — structured JSON generation, no image input |
| 6 | Seller | NL Inventory Query | ✅ Delivered — function calling, seller-scoped |
| 7 | Seller | NL Sales Analytics Chat | ✅ Delivered — function calling + real aggregation |
| 8 | Seller | AI Pricing Advisor | ✅ Delivered — structured JSON generation |
| 9 | Admin | AI Dispute Summarizer | ✅ Delivered — structured JSON generation over real thread |
| 10 | Admin | NL Report Generator | ✅ Delivered — function calling over platform analytics |
| 11 | Admin | AI Fraud Detector | ✅ Delivered — heuristic signal + AI explanation (price outliers / repeat orders, not review data — no review feature yet) |
| 12 | Finance | NL Financial Query | ✅ Delivered — function calling, commission math derived from order data |
| 13 | Finance | AI Anomaly Alerts | ✅ Delivered — heuristic signal + AI explanation |
| 14 | DevOps | NL System Health Query | ⛔ Deferred — no metrics infrastructure (Prometheus-equivalent) exists to query |
| 15 | DevOps | AI Log Anomaly Explainer | ⛔ Deferred — no centralized log pipeline (ELK-equivalent) exists to reason over |

13 / 15 delivered. The two deferred features are the only ones that would require standing up
infrastructure this monolith doesn't have; every other feature reuses data that already exists
in the system (orders, products, disputes) rather than inventing placeholder data.

## 6. Assumptions

- The OpenAI Chat Completions API is reachable and `OPENAI_API_KEY` is valid at demo time; every
  non-AI feature still functions without it.
- A single running instance of the backend is sufficient for grading/demo purposes — no
  assumption of horizontal scaling or multi-region deployment.
- Demo data (4 seed accounts, 8 seed products, 1 seed order, 1 seed dispute) is sufficient to
  exercise every use case live.

## 7. Constraints

- Course timeline: an 11-microservice, Kafka/Kubernetes-backed system is not realistic to stand
  up correctly in the available time, so this milestone builds a modular monolith with the same
  domain boundaries instead (splittable later without a rewrite).
- No payment gateway, SMS, push, or email integration — checkout, order status, and disputes are
  data-only flows.
- No dedicated cloud infrastructure budget — H2 is the zero-setup default; PostgreSQL is
  available via local Docker Compose, not a managed cloud instance (unless the optional cloud-
  deployment extra credit is pursued — see README).

## 8. Alternatives Considered

Building the DevOps features against synthetic/fabricated metrics and logs was considered and
rejected — it would satisfy the letter of "15/15 features" while misrepresenting what the system
actually observes about itself, which conflicts with the honesty-over-completeness principle
this vision document commits to in §1.
