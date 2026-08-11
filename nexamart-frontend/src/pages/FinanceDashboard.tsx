import { useState } from "react";
import { api, ApiError } from "../api/client";
import { useAuth } from "../context/AuthContext";
import type { AnomalyAlert } from "../types";

export default function FinanceDashboard() {
  const { user } = useAuth();
  const [alert, setAlert] = useState<AnomalyAlert | null>(null);
  const [loading, setLoading] = useState(false);
  const [error, setError] = useState<string | null>(null);

  if (!user || (user.role !== "FINANCE" && user.role !== "ADMIN")) {
    return <div className="page">This page is for finance and admins only.</div>;
  }

  const runScan = async () => {
    setError(null);
    setLoading(true);
    try {
      const result = await api.get<AnomalyAlert>("/api/finance/anomaly-scan", user.token);
      setAlert(result);
    } catch (err) {
      setError(err instanceof ApiError ? err.message : "Could not run the anomaly scan");
    } finally {
      setLoading(false);
    }
  };

  return (
    <div className="page">
      <h1>Finance dashboard</h1>
      <p className="chat-hint">
        Tip: open the chat assistant and ask "what were total seller payouts last month?" or "show commissions
        earned from electronics this quarter" — the NL Financial Query assistant runs on the same widget. Figures
        are derived from real order data using a fixed 10% platform commission rate (there's no separate payout
        ledger in this milestone).
      </p>

      <h2>🚨 AI Anomaly Alerts</h2>
      {error && <div className="form-error">{error}</div>}
      <button onClick={runScan} disabled={loading}>
        {loading ? "Analyzing…" : "Check revenue trend"}
      </button>

      {alert && (
        <div className={`anomaly-box ${alert.significant ? "anomaly-significant" : ""}`}>
          <div className="anomaly-periods">
            <div>
              <span className="chat-hint">{alert.currentPeriodDescription}</span>
              <div className="anomaly-figure">${alert.currentPeriodRevenue.toFixed(2)}</div>
            </div>
            <div>
              <span className="chat-hint">{alert.previousPeriodDescription}</span>
              <div className="anomaly-figure">${alert.previousPeriodRevenue.toFixed(2)}</div>
            </div>
          </div>
          <p>
            {alert.significant ? "⚠️ " : "✓ "}
            {alert.message}
          </p>
        </div>
      )}
    </div>
  );
}
