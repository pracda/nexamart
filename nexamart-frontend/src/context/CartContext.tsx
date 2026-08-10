import { createContext, useContext, useEffect, useState, type ReactNode } from "react";
import type { CartLine, Product } from "../types";

interface CartContextValue {
  lines: CartLine[];
  addToCart: (product: Product, quantity?: number) => void;
  removeFromCart: (productId: number) => void;
  setQuantity: (productId: number, quantity: number) => void;
  clearCart: () => void;
  total: number;
}

const CartContext = createContext<CartContextValue | undefined>(undefined);
const STORAGE_KEY = "nexamart.cart";

export function CartProvider({ children }: { children: ReactNode }) {
  const [lines, setLines] = useState<CartLine[]>(() => {
    const raw = localStorage.getItem(STORAGE_KEY);
    return raw ? (JSON.parse(raw) as CartLine[]) : [];
  });

  useEffect(() => {
    localStorage.setItem(STORAGE_KEY, JSON.stringify(lines));
  }, [lines]);

  const addToCart = (product: Product, quantity = 1) => {
    setLines((prev) => {
      const existing = prev.find((l) => l.product.id === product.id);
      if (existing) {
        return prev.map((l) =>
          l.product.id === product.id ? { ...l, quantity: l.quantity + quantity } : l
        );
      }
      return [...prev, { product, quantity }];
    });
  };

  const removeFromCart = (productId: number) => {
    setLines((prev) => prev.filter((l) => l.product.id !== productId));
  };

  const setQuantity = (productId: number, quantity: number) => {
    setLines((prev) =>
      prev.map((l) => (l.product.id === productId ? { ...l, quantity } : l)).filter((l) => l.quantity > 0)
    );
  };

  const clearCart = () => setLines([]);

  const total = lines.reduce((sum, l) => sum + l.product.price * l.quantity, 0);

  return (
    <CartContext.Provider value={{ lines, addToCart, removeFromCart, setQuantity, clearCart, total }}>
      {children}
    </CartContext.Provider>
  );
}

export function useCart(): CartContextValue {
  const ctx = useContext(CartContext);
  if (!ctx) throw new Error("useCart must be used within CartProvider");
  return ctx;
}
