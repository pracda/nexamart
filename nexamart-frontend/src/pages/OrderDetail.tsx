import { useEffect, useState } from "react";
import { useParams, Link } from "react-router-dom";
import { api } from "../api/client";
import { useAuth } from "../context/AuthContext";
import type { Order } from "../types";

export default function OrderDetail() {
  const { id } = useParams();
  const { user } = useAuth();
  const [order, setOrder] = useState<Order | null>(null);

  useEffect(() => {
    if (user && id) {
      api.get<Order>(`/api/orders/${id}`, user.token).then(setOrder);
    }
  }, [user, id]);

  if (!user) return <div className="page">Please log in to view this order.</div>;
  if (!order) return <div className="page">Loading…</div>;

  return (
    <div className="page">
      <Link to="/orders">&larr; Back to orders</Link>
      <h1>Order #{order.id}</h1>
      <p>
        Status: <span className={`status-badge status-${order.status.toLowerCase()}`}>{order.status}</span>
      </p>
      <p>Estimated delivery: {order.estimatedDelivery}</p>
      <table className="cart-table">
        <thead>
          <tr>
            <th>Product</th>
            <th>Qty</th>
            <th>Unit price</th>
            <th>Subtotal</th>
          </tr>
        </thead>
        <tbody>
          {order.items.map((it) => (
            <tr key={it.productId}>
              <td>{it.productTitle}</td>
              <td>{it.quantity}</td>
              <td>${it.unitPrice.toFixed(2)}</td>
              <td>${(it.unitPrice * it.quantity).toFixed(2)}</td>
            </tr>
          ))}
        </tbody>
      </table>
      <div className="cart-total">Total: ${order.totalAmount.toFixed(2)}</div>
      <p className="chat-hint">Tip: ask the assistant "where is order #{order.id}?" any time.</p>
    </div>
  );
}
