import { useEffect, useState } from "react";
import { useParams, Link } from "react-router-dom";
import { api } from "../api/client";
import { useAuth } from "../context/AuthContext";
import { useCart } from "../context/CartContext";
import type { Product } from "../types";

export default function ProductDetail() {
  const { id } = useParams();
  const { user } = useAuth();
  const { addToCart } = useCart();
  const [product, setProduct] = useState<Product | null>(null);
  const [qty, setQty] = useState(1);
  const [added, setAdded] = useState(false);

  useEffect(() => {
    api.get<Product>(`/api/products/${id}`).then(setProduct);
  }, [id]);

  if (!product) return <div className="page">Loading…</div>;

  return (
    <div className="page">
      <Link to="/">&larr; Back to shop</Link>
      <div className="product-detail">
        <h1>{product.title}</h1>
        <div className="product-category">{product.category}</div>
        <p className="product-detail-price">${product.price.toFixed(2)}</p>
        <p>{product.description}</p>
        <p className="product-stock">
          {product.stockQuantity > 0 ? `${product.stockQuantity} in stock` : "Out of stock"}
        </p>
        <p className="seller-line">Sold by {product.sellerName}</p>
        {user?.role === "BUYER" && product.stockQuantity > 0 && (
          <div className="add-to-cart-row">
            <input
              type="number"
              min={1}
              max={product.stockQuantity}
              value={qty}
              onChange={(e) => setQty(Number(e.target.value))}
            />
            <button
              onClick={() => {
                addToCart(product, qty);
                setAdded(true);
                setTimeout(() => setAdded(false), 1500);
              }}
            >
              Add to cart
            </button>
            {added && <span className="added-confirm">Added!</span>}
          </div>
        )}
      </div>
    </div>
  );
}
