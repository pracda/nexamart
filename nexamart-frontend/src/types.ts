export type Role = "BUYER" | "SELLER" | "ADMIN" | "FINANCE" | "DEVOPS";

export interface AuthUser {
  token: string;
  userId: number;
  email: string;
  fullName: string;
  role: Role;
}

export interface Product {
  id: number;
  title: string;
  description: string;
  price: number;
  stockQuantity: number;
  imageUrl: string | null;
  category: string;
  sellerId: number;
  sellerName: string;
}

export interface OrderItem {
  productId: number;
  productTitle: string;
  quantity: number;
  unitPrice: number;
}

export interface Order {
  id: number;
  status: "PLACED" | "PROCESSING" | "SHIPPED" | "DELIVERED" | "CANCELLED" | "RETURNED";
  totalAmount: number;
  estimatedDelivery: string;
  createdAt: string;
  updatedAt: string;
  items: OrderItem[];
}

export interface CartLine {
  product: Product;
  quantity: number;
}

export interface ChatMessage {
  role: "user" | "assistant";
  content: string;
}

export interface GeneratedListing {
  title: string;
  description: string;
  bulletFeatures: string[];
  seoTags: string[];
  suggestedCategory: string;
}

export interface ProductHighlight {
  productId: number;
  title: string;
  pros: string[];
  cons: string[];
}

export interface ComparisonResult {
  summary: string;
  recommendation: string;
  highlights: ProductHighlight[];
}

export interface PricingAdvice {
  recommendedMin: number;
  recommendedMax: number;
  justification: string;
}

export interface DisputeMessage {
  id: number;
  authorName: string;
  authorRole: string;
  body: string;
  createdAt: string;
}

export interface Dispute {
  id: number;
  orderId: number;
  status: "OPEN" | "RESOLVED" | "REJECTED";
  reason: string;
  openedByName: string;
  resolutionNote: string | null;
  createdAt: string;
  updatedAt: string;
  messages: DisputeMessage[];
}

export interface DisputeSummary {
  summary: string;
  recommendedResolution: string;
}

export interface FraudFlag {
  type: string;
  subject: string;
  evidence: string;
  explanation: string;
}

export interface FraudScanResult {
  flags: FraudFlag[];
  scanNote: string;
}

export interface AnomalyAlert {
  currentPeriodDescription: string;
  currentPeriodRevenue: number;
  previousPeriodDescription: string;
  previousPeriodRevenue: number;
  significant: boolean;
  message: string;
}
