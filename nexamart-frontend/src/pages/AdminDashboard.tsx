import { useEffect, useState } from "react";
import { api, ApiError } from "../api/client";
import { useAuth } from "../context/AuthContext";
import type { Dispute, DisputeSummary, FraudScanResult } from "../types";

export default function AdminDashboard() {
  const { user } = useAuth();
  const [disputes, setDisputes] = useState<Dispute[]>([]);
  const [expandedId, setExpandedId] = useState<number | null>(null);

  const [summaries, setSummaries] = useState<Record<number, DisputeSummary>>({});
  const [summarizing, setSummarizing] = useState<number | null>(null);
  const [summaryError, setSummaryError] = useState<string | null>(null);

  const [resolveNote, setResolveNote] = useState("");
  const [resolving, setResolving] = useState(false);

  const [fraudResult, setFraudResult] = useState<FraudScanResult | null>(null);
  const [fraudLoading, setFraudLoading] = useState(false);
  const [fraudError, setFraudError] = useState<string | null>(null);

  const loadDisputes = () => {
    if (user) api.get<Dispute[]>("/api/disputes", user.token).then(setDisputes);
  };

  useEffect(loadDisputes, [user]);

  if (!user || user.role !== "ADMIN") {
    return <div className="page">This page is for admins only.</div>;
  }

  const summarize = async (disputeId: number) => {
    setSummaryError(null);
    setSummarizing(disputeId);
    try {
      const result = await api.post<DisputeSummary>(`/api/admin/disputes/${disputeId}/ai-summary`, {}, user.token);
      setSummaries((prev) => ({ ...prev, [disputeId]: result }));
    } catch (err) {
      setSummaryError(err instanceof ApiError ? err.message : "Could not summarize this dispute");
    } finally {
      setSummarizing(null);
    }
  };

  const resolve = async (disputeId: number, status: "RESOLVED" | "REJECTED") => {
    setResolving(true);
    try {
      await api.patch(`/api/admin/disputes/${disputeId}/resolve`, { status, resolutionNote: resolveNote }, user.token);
      setResolveNote("");
      loadDisputes();
    } finally {
      setResolving(false);
    }
  };

  const runFraudScan = async () => {
    setFraudError(null);
    setFraudLoading(true);
    try {
      const result = await api.get<FraudScanResult>("/api/admin/fraud-scan", user.token);
      setFraudResult(result);
    } catch (err) {
      setFraudError(err instanceof ApiError ? err.message : "Could not run the fraud scan");
    } finally {
      setFraudLoading(false);
    }
  };

  return (
    <div className="page">
      <h1>Admin dashboard</h1>
      <p className="chat-hint">
        Tip: open the chat assistant and ask "what's total revenue by category?" or "which sellers had the most
        disputes?" — the NL Report Generator runs on the same assistant.
      </p>

      <h2>Disputes</h2>
      <div className="dispute-list">
        {disputes.length === 0 && <p>No disputes.</p>}
        {disputes.map((d) => (
          <div className="admin-dispute-card" key={d.id}>
            <div className="admin-dispute-header" onClick={() => setExpandedId(expandedId === d.id ? null : d.id)}>
              <span>
                Dispute #{d.id} — Order #{d.orderId} — {d.openedByName}
              </span>
              <span className={`status-badge status-${d.status.toLowerCase()}`}>{d.status}</span>
            </div>
            <p className="dispute-reason">{d.reason}</p>

            {expandedId === d.id && (
              <div className="admin-dispute-body">
                <div className="dispute-thread">
                  {d.messages.map((m) => (
                    <div key={m.id} className="dispute-message">
                      <strong>
                        {m.authorName} ({m.authorRole})
                      </strong>
                      <p>{m.body}</p>
                    </div>
                  ))}
                </div>

                {summaryError && <div className="form-error">{summaryError}</div>}
                {summaries[d.id] ? (
                  <div className="ai-summary-box">
                    <strong>🤖 AI Summary</strong>
                    <p>{summaries[d.id].summary}</p>
                    <p>
                      <em>Recommended: {summaries[d.id].recommendedResolution}</em>
                    </p>
                  </div>
                ) : (
                  <button onClick={() => summarize(d.id)} disabled={summarizing === d.id}>
                    {summarizing === d.id ? "Summarizing…" : "🤖 Summarize with AI"}
                  </button>
                )}

                {d.status === "OPEN" && (
                  <div className="resolve-row">
                    <input
                      placeholder="Resolution note"
                      value={resolveNote}
                      onChange={(e) => setResolveNote(e.target.value)}
                    />
                    <button onClick={() => resolve(d.id, "RESOLVED")} disabled={resolving}>
                      Mark resolved
                    </button>
                    <button onClick={() => resolve(d.id, "REJECTED")} disabled={resolving}>
                      Reject
                    </button>
                  </div>
                )}
                {d.resolutionNote && (
                  <p className="resolution-note">
                    <strong>Resolution:</strong> {d.resolutionNote}
                  </p>
                )}
              </div>
            )}
          </div>
        ))}
      </div>

      <h2>🕵️ AI Fraud Detector</h2>
      {fraudError && <div className="form-error">{fraudError}</div>}
      <button onClick={runFraudScan} disabled={fraudLoading}>
        {fraudLoading ? "Scanning…" : "Run fraud scan"}
      </button>
      {fraudResult && (
        <div className="fraud-results">
          <p className="chat-hint">{fraudResult.scanNote}</p>
          {fraudResult.flags.map((f, i) => (
            <div className="fraud-flag" key={i}>
              <div className="fraud-flag-header">
                <span className="tag-chip">{f.type}</span>
                <strong>{f.subject}</strong>
              </div>
              <p className="fraud-evidence">{f.evidence}</p>
              <p className="fraud-explanation">🤖 {f.explanation}</p>
            </div>
          ))}
        </div>
      )}
    </div>
  );
}
