import { useEffect, useState } from "react";
import { Link } from "react-router-dom";
import { api } from "../api/client";
import { useAuth } from "../context/AuthContext";
import type { Order } from "../types";

export default function Orders() {
  const { user } = useAuth();
  const [orders, setOrders] = useState<Order[]>([]);

  useEffect(() => {
    if (user) {
      api.get<Order[]>("/api/orders", user.token).then(setOrders);
    }
  }, [user]);

  if (!user) return <div className="page">Please log in to see your orders.</div>;

  return (
    <div className="page">
      <h1>My orders</h1>
      {orders.length === 0 && <p>You haven't placed any orders yet.</p>}
      <div className="order-list">
        {orders.map((o) => (
          <Link to={`/orders/${o.id}`} className="order-row" key={o.id}>
            <span>Order #{o.id}</span>
            <span className={`status-badge status-${o.status.toLowerCase()}`}>{o.status}</span>
            <span>${o.totalAmount.toFixed(2)}</span>
            <span>{new Date(o.createdAt).toLocaleDateString()}</span>
          </Link>
        ))}
      </div>
    </div>
  );
}
