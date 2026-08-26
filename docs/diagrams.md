# NexaMart — Architecture & UML Diagrams

This document satisfies the "System architecture, sequence, collaboration, and VOPC diagrams"
submission requirement. All diagrams describe the **actual implemented system** (Spring Boot 3
modular monolith + React 18 frontend), not the aspirational 11-microservice design from the
Vision Document — see [`../README.md#scope-decisions-vs-the-vision-document`](../README.md) for
why that scope was cut for this milestone. Diagrams render natively on GitHub.

## 1. System Architecture

```mermaid
flowchart TB
    subgraph Client["Client"]
        UI["React 18 + TypeScript + Vite\n(pages, ChatWidget, Navbar, contexts)"]
    end

    subgraph Backend["nexamart-backend — Spring Boot 3 (Java 21)"]
        SEC["Spring Security\nJwtAuthFilter · SecurityConfig"]
        CTRL["Controllers\nAuth · Catalog · Order · Dispute · Recommendation\nAdmin · Finance · Chat"]
        SVC["Services\nProductService · OrderService · DisputeService\nSellerAnalyticsService · PlatformAnalyticsService\nFinanceAnalyticsService · FraudDetectionService\nAnomalyAlertService · RecommendationService"]
        AI["AI Layer\nAiChatService · ToolFunctions · OpenAiClient\nListingAssistantService · PricingAdvisorService\nProductComparisonService · DisputeSummaryService"]
        REPO["Repositories — Spring Data JPA\nUserRepository · ProductRepository · CategoryRepository\nOrderRepository · OrderItemRepository\nDisputeRepository · DisputeMessageRepository"]
        ERR["GlobalExceptionHandler / ApiException"]
    end

    subgraph Data["Data"]
        DB[("H2 (dev, file-based)\nor PostgreSQL via Docker Compose")]
    end

    subgraph External["External"]
        OPENAI["OpenAI Chat Completions API\n(GPT-4o-mini, function calling +\njson_object structured output)"]
    end

    UI -- "HTTPS / JSON, JWT bearer token" --> SEC
    SEC --> CTRL
    CTRL --> SVC
    CTRL --> AI
    AI -- "role-scoped tool calls" --> SVC
    SVC --> REPO
    REPO --> DB
    AI -- "HTTPS" --> OPENAI
    CTRL -.-> ERR
```

Controllers stay HTTP-only, services hold business logic, repositories are the sole DB access
path (rubric criteria 4–7). Package boundaries (`com.nexamart.auth`, `catalog`, `order`,
`dispute`, `recommendation`, `analytics`, `finance`, `admin`, `ai`, `common`) mirror the domain
boundaries the Vision Document assigns to separate microservices, so the system can be split
apart later without a rewrite.

## 2. Sequence Diagrams

### 2.1 Buyer places an order (core CRUD flow — UC-04 Shopping Cart & Checkout)

```mermaid
sequenceDiagram
    actor Buyer
    participant UI as React UI (Cart)
    participant Sec as JwtAuthFilter
    participant OC as OrderController
    participant OS as OrderService
    participant PR as ProductRepository
    participant OR as OrderRepository
    participant DB as Database

    Buyer->>UI: Click "Checkout"
    UI->>Sec: POST /api/orders (JWT bearer, cart items)
    Sec->>Sec: Validate signature + expiration
    Sec->>OC: forward request with authenticated User
    OC->>OS: placeOrder(user, items)
    loop each cart item
        OS->>PR: findById(productId)
        PR->>DB: SELECT product
        DB-->>PR: product row
        PR-->>OS: Product (stock, price)
        OS->>OS: verify stock, decrement quantity
    end
    OS->>OR: save(new Order + OrderItems)
    OR->>DB: INSERT order, order_items
    OS->>PR: save(updated stock)
    PR->>DB: UPDATE product
    OS-->>OC: OrderDto (order number, status)
    OC-->>UI: 201 Created + OrderDto
    UI-->>Buyer: Show order confirmation
```

### 2.2 Buyer — NL Product Finder Chatbot (function calling — UC-02)

```mermaid
sequenceDiagram
    actor Buyer
    participant UI as ChatWidget
    participant CC as ChatController
    participant ACS as AiChatService
    participant AI as OpenAI API
    participant TF as ToolFunctions
    participant PS as ProductService
    participant PR as ProductRepository

    Buyer->>UI: "find me headphones under $50"
    UI->>CC: POST /api/ai/chat { message }
    CC->>ACS: chat(user, message)
    ACS->>AI: role-scoped tool list + system prompt + message
    AI-->>ACS: tool_call: search_products(keyword, maxPrice)
    ACS->>TF: execute(search_products, args, user)
    TF->>PS: search(keyword, maxPrice)
    PS->>PR: findByNameContainingAndPriceLessThan(...)
    PR-->>PS: matching Products
    PS-->>TF: List<ProductDto>
    TF-->>ACS: tool result (JSON)
    ACS->>AI: tool result appended to conversation
    AI-->>ACS: natural-language reply grounded in real data
    ACS-->>CC: ChatResponseDto
    CC-->>UI: 200 OK + reply
    UI-->>Buyer: "Wireless Headphones - Red — $39.99..."
```

### 2.3 Seller — AI Listing Assistant (one-shot structured generation — UC-03 support)

```mermaid
sequenceDiagram
    actor Seller
    participant UI as Seller Dashboard
    participant CC as ChatController / ListingController
    participant LAS as ListingAssistantService
    participant AI as OpenAI API
    participant PC as ProductController
    participant PS as ProductService
    participant PR as ProductRepository

    Seller->>UI: name + notes, click "Generate with AI"
    UI->>CC: POST /api/ai/listing-assistant
    CC->>LAS: generate(name, notes)
    LAS->>AI: one prompt, response_format=json_object
    AI-->>LAS: { title, description, bullets, tags, category }
    LAS-->>CC: ListingDraftDto
    CC-->>UI: 200 OK + draft (seller reviews/edits)
    Seller->>UI: set price/stock, click "Publish listing"
    UI->>PC: POST /api/products
    PC->>PS: createProduct(seller, dto)
    PS->>PR: save(Product)
    PR-->>PS: saved Product
    PS-->>PC: ProductDto
    PC-->>UI: 201 Created
```

### 2.4 Admin — AI Fraud Detector (heuristic signal + AI explanation)

```mermaid
sequenceDiagram
    actor Admin
    participant UI as Admin Dashboard
    participant FC as FraudDetectionController
    participant FS as FraudDetectionService
    participant OR as OrderRepository
    participant PR as ProductRepository
    participant AI as OpenAI API

    Admin->>UI: Click "Run fraud scan"
    UI->>FC: POST /api/admin/fraud/scan
    FC->>FS: scan()
    FS->>PR: findAll() (per-category price stats)
    FS->>OR: findAll() (repeat-order patterns)
    FS->>FS: compute price-outlier + repeat-order candidates (plain Java)
    FS->>AI: candidates + signals → request plain-language explanation
    AI-->>FS: Explanations (per candidate)
    FS-->>FC: List<FraudCandidateDto>
    FC-->>UI: 200 OK + flagged candidates
    UI-->>Admin: "USB-C Charging Cable — priced well under category avg..."
```

## 3. Collaboration Diagram — NL Product Finder Chatbot

A collaboration (communication) view of the same UC-02 interaction as §2.2, showing participating
objects and the order messages travel between them.

```mermaid
flowchart LR
    Buyer((Buyer))
    UI["ChatWidget"]
    CC["ChatController"]
    ACS["AiChatService"]
    TF["ToolFunctions"]
    PS["ProductService"]
    PR["ProductRepository"]
    AI["OpenAI API"]

    Buyer -- "1: types message" --> UI
    UI -- "2: POST /api/ai/chat" --> CC
    CC -- "3: chat(user, message)" --> ACS
    ACS -- "4: send message + tools" --> AI
    AI -- "5: tool_call: search_products" --> ACS
    ACS -- "6: execute(tool call)" --> TF
    TF -- "7: search(keyword, maxPrice)" --> PS
    PS -- "8: findByNameContaining...()" --> PR
    PR -- "9: matching products" --> PS
    PS -- "10: ProductDto list" --> TF
    TF -- "11: tool result" --> ACS
    ACS -- "12: tool result" --> AI
    AI -- "13: natural-language reply" --> ACS
    ACS -- "14: ChatResponseDto" --> CC
    CC -- "15: 200 OK" --> UI
    UI -- "16: render reply" --> Buyer
```

## 4. VOPC (View of Participating Classes) Diagrams

### 4.1 UC-02 — NL Product Finder Chatbot

```mermaid
classDiagram
    class ChatController {
        +chat(ChatRequestDto) ChatResponseDto
    }
    class AiChatService {
        -MAX_TOOL_ROUNDS int
        +chat(User, String) ChatResponseDto
    }
    class ToolFunctions {
        +toolDefinitions(Role) List~Tool~
        +execute(String, Map, User) Object
    }
    class OpenAiClient {
        +chatCompletion(...) OpenAiResponse
    }
    class ProductService {
        +search(keyword, maxPrice) List~ProductDto~
    }
    class ProductRepository {
        <<interface>>
        +findByNameContainingAndPriceLessThan(...)
    }
    class Product {
        -id Long
        -name String
        -price BigDecimal
        -stock int
        -category Category
    }

    ChatController --> AiChatService
    AiChatService --> OpenAiClient
    AiChatService --> ToolFunctions
    ToolFunctions --> ProductService
    ProductService --> ProductRepository
    ProductRepository --> Product
```

### 4.2 UC-04 — Shopping Cart & Checkout

```mermaid
classDiagram
    class OrderController {
        +placeOrder(OrderRequestDto) OrderDto
        +listOrders(User) List~OrderDto~
    }
    class OrderService {
        +placeOrder(User, List~CartItem~) OrderDto
        +updateStatus(Long, OrderStatus) OrderDto
    }
    class OrderRepository {
        <<interface>>
    }
    class OrderItemRepository {
        <<interface>>
    }
    class ProductRepository {
        <<interface>>
    }
    class Order {
        -id Long
        -buyer User
        -status OrderStatus
        -items List~OrderItem~
    }
    class OrderItem {
        -product Product
        -quantity int
        -priceAtPurchase BigDecimal
    }
    class OrderStatus {
        <<enumeration>>
        PLACED
        SHIPPED
        DELIVERED
        CANCELLED
    }
    class Product {
        -stock int
        -price BigDecimal
    }

    OrderController --> OrderService
    OrderService --> OrderRepository
    OrderService --> OrderItemRepository
    OrderService --> ProductRepository
    OrderRepository --> Order
    Order "1" o-- "many" OrderItem
    Order --> OrderStatus
    OrderItem --> Product
```

## 5. Diagram-to-code cross-reference

| Diagram | Backend classes it reflects |
|---|---|
| System Architecture | `com.nexamart.*` package layout — see `nexamart-backend/src/main/java/com/nexamart` |
| 2.1 Checkout sequence | `OrderController`, `OrderService`, `OrderRepository`, `Product`, `ProductRepository` |
| 2.2 Product Finder sequence + Collaboration | `ChatController`, `AiChatService`, `ToolFunctions`, `OpenAiClient`, `ProductService` |
| 2.3 AI Listing Assistant sequence | `ListingAssistantService`, `ProductController`, `ProductService` |
| 2.4 AI Fraud Detector sequence | `FraudDetectionController`, `FraudDetectionService` |
| VOPC 4.1 / 4.2 | as labeled above |

Keep this file in sync if class/package names change before the presentation date.
