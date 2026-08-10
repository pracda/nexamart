import { useState } from "react";
import { useNavigate } from "react-router-dom";
import { api, ApiError } from "../api/client";
import { useAuth } from "../context/AuthContext";
import { useCart } from "../context/CartContext";
import type { Order } from "../types";

export default function Cart() {
  const { user } = useAuth();
  const { lines, removeFromCart, setQuantity, clearCart, total } = useCart();
  const navigate = useNavigate();
  const [error, setError] = useState<string | null>(null);
  const [placing, setPlacing] = useState(false);

  const checkout = async () => {
    if (!user) {
      navigate("/login");
      return;
    }
    setError(null);
    setPlacing(true);
    try {
      const order = await api.post<Order>(
        "/api/orders",
        { items: lines.map((l) => ({ productId: l.product.id, quantity: l.quantity })) },
        user.token
      );
      clearCart();
      navigate(`/orders/${order.id}`);
    } catch (err) {
      setError(err instanceof ApiError ? err.message : "Could not place order");
    } finally {
      setPlacing(false);
    }
  };

  if (lines.length === 0) {
    return (
      <div className="page">
        <h1>Your cart</h1>
        <p>Your cart is empty.</p>
      </div>
    );
  }

  return (
    <div className="page">
      <h1>Your cart</h1>
      {error && <div className="form-error">{error}</div>}
      <table className="cart-table">
        <thead>
          <tr>
            <th>Product</th>
            <th>Price</th>
            <th>Qty</th>
            <th>Subtotal</th>
            <th></th>
          </tr>
        </thead>
        <tbody>
          {lines.map((l) => (
            <tr key={l.product.id}>
              <td>{l.product.title}</td>
              <td>${l.product.price.toFixed(2)}</td>
              <td>
                <input
                  type="number"
                  min={1}
                  max={l.product.stockQuantity}
                  value={l.quantity}
                  onChange={(e) => setQuantity(l.product.id, Number(e.target.value))}
                />
              </td>
              <td>${(l.product.price * l.quantity).toFixed(2)}</td>
              <td>
                <button onClick={() => removeFromCart(l.product.id)}>Remove</button>
              </td>
            </tr>
          ))}
        </tbody>
      </table>
      <div className="cart-total">Total: ${total.toFixed(2)}</div>
      <button onClick={checkout} disabled={placing}>
        {placing ? "Placing order…" : "Checkout"}
      </button>
    </div>
  );
}
