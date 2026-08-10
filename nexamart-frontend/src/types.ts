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
