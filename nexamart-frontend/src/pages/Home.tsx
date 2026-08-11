import { useEffect, useState } from "react";
import { Link } from "react-router-dom";
import { api, ApiError } from "../api/client";
import { useAuth } from "../context/AuthContext";
import { useCart } from "../context/CartContext";
import type { ComparisonResult, Product } from "../types";

export default function Home() {
  const { user } = useAuth();
  const { addToCart } = useCart();
  const [products, setProducts] = useState<Product[]>([]);
  const [q, setQ] = useState("");
  const [category, setCategory] = useState("");
  const [categories, setCategories] = useState<{ id: number; name: string }[]>([]);
  const [loading, setLoading] = useState(false);

  const [recommended, setRecommended] = useState<Product[]>([]);

  const [compareIds, setCompareIds] = useState<number[]>([]);
  const [comparing, setComparing] = useState(false);
  const [comparison, setComparison] = useState<ComparisonResult | null>(null);
  const [compareError, setCompareError] = useState<string | null>(null);

  const load = async (query = q, cat = category) => {
    setLoading(true);
    try {
      const params = new URLSearchParams();
      if (query) params.set("q", query);
      if (cat) params.set("category", cat);
      const res = await api.get<Product[]>(`/api/products?${params.toString()}`);
      setProducts(res);
    } finally {
      setLoading(false);
    }
  };

  useEffect(() => {
    load("", "");
    api.get<{ id: number; name: string }[]>("/api/categories").then(setCategories);
    // eslint-disable-next-line react-hooks/exhaustive-deps
  }, []);

  useEffect(() => {
    if (user?.role === "BUYER") {
      api.get<Product[]>("/api/recommendations?limit=4", user.token).then(setRecommended);
    } else {
      setRecommended([]);
    }
  }, [user]);

  const toggleCompare = (id: number) => {
    setComparison(null);
    setCompareError(null);
    setCompareIds((prev) => {
      if (prev.includes(id)) return prev.filter((x) => x !== id);
      if (prev.length >= 3) return prev;
      return [...prev, id];
    });
  };

  const runComparison = async () => {
    if (!user) return;
    setComparing(true);
    setCompareError(null);
    try {
      const result = await api.post<ComparisonResult>("/api/ai/compare-products", { productIds: compareIds }, user.token);
      setComparison(result);
    } catch (err) {
      setCompareError(err instanceof ApiError ? err.message : "Could not compare these products");
    } finally {
      setComparing(false);
    }
  };

  const renderCard = (p: Product, showCompare: boolean) => (
    <div className="product-card" key={p.id}>
      <Link to={`/products/${p.id}`} className="product-title">
        {p.title}
      </Link>
      <div className="product-category">{p.category}</div>
      <div className="product-price">${p.price.toFixed(2)}</div>
      <div className="product-stock">{p.stockQuantity > 0 ? `${p.stockQuantity} in stock` : "Out of stock"}</div>
      {user?.role === "BUYER" && (
        <button disabled={p.stockQuantity === 0} onClick={() => addToCart(p)}>
          Add to cart
        </button>
      )}
      {showCompare && user?.role === "BUYER" && (
        <label className="compare-check">
          <input
            type="checkbox"
            checked={compareIds.includes(p.id)}
            disabled={!compareIds.includes(p.id) && compareIds.length >= 3}
            onChange={() => toggleCompare(p.id)}
          />
          Compare
        </label>
      )}
    </div>
  );

  return (
    <div className="page">
      <div className="hero">
        <h1>Find anything, in plain English.</h1>
        <p>Use the search bar below, or open the chat assistant and just ask.</p>
      </div>

      {user?.role === "BUYER" && recommended.length > 0 && (
        <div className="recommend-section">
          <h2>✨ Picked for you</h2>
          <div className="product-grid">{recommended.map((p) => renderCard(p, false))}</div>
        </div>
      )}

      <form
        className="search-bar"
        onSubmit={(e) => {
          e.preventDefault();
          load();
        }}
      >
        <input placeholder="Search products…" value={q} onChange={(e) => setQ(e.target.value)} />
        <select value={category} onChange={(e) => setCategory(e.target.value)}>
          <option value="">All categories</option>
          {categories.map((c) => (
            <option key={c.id} value={c.name}>
              {c.name}
            </option>
          ))}
        </select>
        <button type="submit">Search</button>
      </form>

      {loading && <p>Loading…</p>}

      <div className="product-grid">
        {products.map((p) => renderCard(p, true))}
        {!loading && products.length === 0 && <p>No products found. Try a different search.</p>}
      </div>

      {user?.role === "BUYER" && compareIds.length >= 2 && (
        <div className="compare-bar">
          <span>{compareIds.length} selected</span>
          <button onClick={runComparison} disabled={comparing}>
            {comparing ? "Comparing…" : `Compare with AI (${compareIds.length})`}
          </button>
          <button className="compare-clear" onClick={() => { setCompareIds([]); setComparison(null); }}>
            Clear
          </button>
        </div>
      )}

      {compareError && <div className="form-error">{compareError}</div>}

      {comparison && (
        <div className="comparison-panel">
          <h2>🤖 AI Comparison</h2>
          <p>{comparison.summary}</p>
          <p className="comparison-recommendation">
            <strong>Recommendation: </strong>
            {comparison.recommendation}
          </p>
          <div className="comparison-grid">
            {comparison.highlights.map((h) => (
              <div className="comparison-card" key={h.productId}>
                <h3>{h.title}</h3>
                <div className="comparison-pros">
                  {h.pros.map((pro, i) => (
                    <div key={i}>✓ {pro}</div>
                  ))}
                </div>
                <div className="comparison-cons">
                  {h.cons.map((con, i) => (
                    <div key={i}>✗ {con}</div>
                  ))}
                </div>
              </div>
            ))}
          </div>
        </div>
      )}
    </div>
  );
}
