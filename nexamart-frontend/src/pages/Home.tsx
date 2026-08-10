import { useEffect, useState } from "react";
import { Link } from "react-router-dom";
import { api } from "../api/client";
import { useAuth } from "../context/AuthContext";
import { useCart } from "../context/CartContext";
import type { Product } from "../types";

export default function Home() {
  const { user } = useAuth();
  const { addToCart } = useCart();
  const [products, setProducts] = useState<Product[]>([]);
  const [q, setQ] = useState("");
  const [category, setCategory] = useState("");
  const [categories, setCategories] = useState<{ id: number; name: string }[]>([]);
  const [loading, setLoading] = useState(false);

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

  return (
    <div className="page">
      <div className="hero">
        <h1>Find anything, in plain English.</h1>
        <p>Use the search bar below, or open the chat assistant and just ask.</p>
      </div>

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
        {products.map((p) => (
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
          </div>
        ))}
        {!loading && products.length === 0 && <p>No products found. Try a different search.</p>}
      </div>
    </div>
  );
}
