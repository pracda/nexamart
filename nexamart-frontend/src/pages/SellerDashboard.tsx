import { useEffect, useState } from "react";
import { api, ApiError } from "../api/client";
import { useAuth } from "../context/AuthContext";
import type { GeneratedListing, PricingAdvice, Product } from "../types";

const emptyForm = { title: "", description: "", price: "", stockQuantity: "", category: "", imageUrl: "" };

export default function SellerDashboard() {
  const { user } = useAuth();
  const [products, setProducts] = useState<Product[]>([]);
  const [form, setForm] = useState(emptyForm);
  const [error, setError] = useState<string | null>(null);
  const [saving, setSaving] = useState(false);

  const [quickName, setQuickName] = useState("");
  const [aiNotes, setAiNotes] = useState("");
  const [aiLoading, setAiLoading] = useState(false);
  const [aiError, setAiError] = useState<string | null>(null);
  const [seoTags, setSeoTags] = useState<string[]>([]);

  const [priceAdvice, setPriceAdvice] = useState<PricingAdvice | null>(null);
  const [priceAdviceLoading, setPriceAdviceLoading] = useState(false);
  const [priceAdviceError, setPriceAdviceError] = useState<string | null>(null);

  const load = () => {
    if (user) api.get<Product[]>("/api/seller/products", user.token).then(setProducts);
  };

  useEffect(load, [user]);

  if (!user || (user.role !== "SELLER" && user.role !== "ADMIN")) {
    return <div className="page">This page is for sellers and admins only.</div>;
  }

  const generateWithAi = async () => {
    if (!quickName.trim()) {
      setAiError("Enter a rough product name first.");
      return;
    }
    setAiError(null);
    setAiLoading(true);
    try {
      const result = await api.post<GeneratedListing>(
        "/api/seller/ai/generate-listing",
        { productName: quickName, notes: aiNotes || null },
        user.token
      );
      const bullets = result.bulletFeatures.map((b) => `• ${b}`).join("\n");
      setForm({
        ...form,
        title: result.title,
        description: `${result.description}\n\n${bullets}`,
        category: result.suggestedCategory,
      });
      setSeoTags(result.seoTags);
    } catch (err) {
      setAiError(err instanceof ApiError ? err.message : "Could not generate a listing");
    } finally {
      setAiLoading(false);
    }
  };

  const suggestPrice = async () => {
    if (!form.title.trim() || !form.category.trim()) {
      setPriceAdviceError("Fill in a title and category first.");
      return;
    }
    setPriceAdviceError(null);
    setPriceAdviceLoading(true);
    try {
      const advice = await api.post<PricingAdvice>(
        "/api/seller/ai/pricing-advice",
        { title: form.title, category: form.category, draftPrice: form.price ? Number(form.price) : null },
        user.token
      );
      setPriceAdvice(advice);
    } catch (err) {
      setPriceAdviceError(err instanceof ApiError ? err.message : "Could not get pricing advice");
    } finally {
      setPriceAdviceLoading(false);
    }
  };

  const submit = async (e: React.FormEvent) => {
    e.preventDefault();
    setError(null);
    setSaving(true);
    try {
      await api.post(
        "/api/seller/products",
        {
          title: form.title,
          description: form.description,
          price: Number(form.price),
          stockQuantity: Number(form.stockQuantity),
          category: form.category,
          imageUrl: form.imageUrl || null,
        },
        user.token
      );
      setForm(emptyForm);
      setSeoTags([]);
      setQuickName("");
      setAiNotes("");
      setPriceAdvice(null);
      load();
    } catch (err) {
      setError(err instanceof ApiError ? err.message : "Could not save product");
    } finally {
      setSaving(false);
    }
  };

  const remove = async (id: number) => {
    await api.del(`/api/seller/products/${id}`, user.token);
    load();
  };

  return (
    <div className="page">
      <h1>Seller dashboard</h1>
      <p className="chat-hint">
        Tip: open the chat assistant (bottom right) and ask things like "which products are running low" or "what
        were my top selling products this month".
      </p>

      <div className="seller-grid">
        <div>
          <div className="ai-assistant-box">
            <h2>✨ AI Listing Assistant</h2>
            {aiError && <div className="form-error">{aiError}</div>}
            <label>
              Rough product name
              <input
                value={quickName}
                onChange={(e) => setQuickName(e.target.value)}
                placeholder="e.g. bamboo cutting board"
              />
            </label>
            <label>
              Notes (optional)
              <input
                value={aiNotes}
                onChange={(e) => setAiNotes(e.target.value)}
                placeholder="e.g. eco-friendly, dishwasher safe"
              />
            </label>
            <button type="button" onClick={generateWithAi} disabled={aiLoading}>
              {aiLoading ? "Generating…" : "Generate with AI"}
            </button>
            {seoTags.length > 0 && (
              <div className="tag-row">
                {seoTags.map((t) => (
                  <span className="tag-chip" key={t}>
                    {t}
                  </span>
                ))}
              </div>
            )}
          </div>

          <form className="product-form" onSubmit={submit}>
            <h2>New listing</h2>
            {error && <div className="form-error">{error}</div>}
            <label>
              Title
              <input value={form.title} onChange={(e) => setForm({ ...form, title: e.target.value })} required />
            </label>
            <label>
              Description
              <textarea
                rows={6}
                value={form.description}
                onChange={(e) => setForm({ ...form, description: e.target.value })}
              />
            </label>
            <label>
              Category
              <input
                value={form.category}
                onChange={(e) => setForm({ ...form, category: e.target.value })}
                required
              />
            </label>
            <label>
              Price (USD)
              <div className="price-row">
                <input
                  type="number"
                  step="0.01"
                  min="0.01"
                  value={form.price}
                  onChange={(e) => setForm({ ...form, price: e.target.value })}
                  required
                />
                <button type="button" onClick={suggestPrice} disabled={priceAdviceLoading}>
                  {priceAdviceLoading ? "…" : "💡 Suggest price"}
                </button>
              </div>
            </label>
            {priceAdviceError && <div className="form-error">{priceAdviceError}</div>}
            {priceAdvice && (
              <div className="price-advice">
                <strong>
                  ${priceAdvice.recommendedMin.toFixed(2)} – ${priceAdvice.recommendedMax.toFixed(2)}
                </strong>
                <p>{priceAdvice.justification}</p>
              </div>
            )}
            <label>
              Stock quantity
              <input
                type="number"
                min="0"
                value={form.stockQuantity}
                onChange={(e) => setForm({ ...form, stockQuantity: e.target.value })}
                required
              />
            </label>
            <label>
              Image URL (optional)
              <input value={form.imageUrl} onChange={(e) => setForm({ ...form, imageUrl: e.target.value })} />
            </label>
            <button type="submit" disabled={saving}>
              {saving ? "Saving…" : "Publish listing"}
            </button>
          </form>
        </div>

        <div>
          <h2>Your products</h2>
          <table className="cart-table">
            <thead>
              <tr>
                <th>Title</th>
                <th>Category</th>
                <th>Price</th>
                <th>Stock</th>
                <th></th>
              </tr>
            </thead>
            <tbody>
              {products.map((p) => (
                <tr key={p.id}>
                  <td>{p.title}</td>
                  <td>{p.category}</td>
                  <td>${p.price.toFixed(2)}</td>
                  <td>{p.stockQuantity}</td>
                  <td>
                    <button onClick={() => remove(p.id)}>Delete</button>
                  </td>
                </tr>
              ))}
              {products.length === 0 && (
                <tr>
                  <td colSpan={5}>No listings yet.</td>
                </tr>
              )}
            </tbody>
          </table>
        </div>
      </div>
    </div>
  );
}
