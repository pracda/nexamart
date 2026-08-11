import { useEffect, useState } from "react";
import { useParams, Link } from "react-router-dom";
import { api, ApiError } from "../api/client";
import { useAuth } from "../context/AuthContext";
import type { Order } from "../types";

export default function OrderDetail() {
  const { id } = useParams();
  const { user } = useAuth();
  const [order, setOrder] = useState<Order | null>(null);

  const [showDisputeForm, setShowDisputeForm] = useState(false);
  const [reason, setReason] = useState("");
  const [message, setMessage] = useState("");
  const [submitting, setSubmitting] = useState(false);
  const [disputeError, setDisputeError] = useState<string | null>(null);
  const [disputeSubmitted, setDisputeSubmitted] = useState(false);

  useEffect(() => {
    if (user && id) {
      api.get<Order>(`/api/orders/${id}`, user.token).then(setOrder);
    }
  }, [user, id]);

  if (!user) return <div className="page">Please log in to view this order.</div>;
  if (!order) return <div className="page">Loading…</div>;

  const submitDispute = async (e: React.FormEvent) => {
    e.preventDefault();
    setDisputeError(null);
    setSubmitting(true);
    try {
      await api.post("/api/disputes", { orderId: order.id, reason, message }, user.token);
      setDisputeSubmitted(true);
      setShowDisputeForm(false);
    } catch (err) {
      setDisputeError(err instanceof ApiError ? err.message : "Could not open a dispute");
    } finally {
      setSubmitting(false);
    }
  };

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

      {user.role === "BUYER" && (
        <div className="dispute-section">
          {disputeSubmitted ? (
            <p className="dispute-confirm">✓ Dispute opened — our team will review it shortly.</p>
          ) : showDisputeForm ? (
            <form className="dispute-form" onSubmit={submitDispute}>
              {disputeError && <div className="form-error">{disputeError}</div>}
              <label>
                Reason
                <input value={reason} onChange={(e) => setReason(e.target.value)} required placeholder="e.g. Item arrived damaged" />
              </label>
              <label>
                Details
                <textarea rows={3} value={message} onChange={(e) => setMessage(e.target.value)} required />
              </label>
              <div className="dispute-form-actions">
                <button type="submit" disabled={submitting}>
                  {submitting ? "Submitting…" : "Submit dispute"}
                </button>
                <button type="button" className="compare-clear" onClick={() => setShowDisputeForm(false)}>
                  Cancel
                </button>
              </div>
            </form>
          ) : (
            <button className="report-problem-btn" onClick={() => setShowDisputeForm(true)}>
              Report a problem with this order
            </button>
          )}
        </div>
      )}
    </div>
  );
}
