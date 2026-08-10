import { Link, useNavigate } from "react-router-dom";
import { useAuth } from "../context/AuthContext";
import { useCart } from "../context/CartContext";

export default function Navbar() {
  const { user, logout } = useAuth();
  const { lines } = useCart();
  const navigate = useNavigate();
  const itemCount = lines.reduce((n, l) => n + l.quantity, 0);

  const handleLogout = () => {
    logout();
    navigate("/");
  };

  return (
    <header className="navbar">
      <Link to="/" className="brand">
        NexaMart
      </Link>
      <nav className="nav-links">
        <Link to="/">Shop</Link>
        {user?.role === "BUYER" && <Link to="/orders">My Orders</Link>}
        {(user?.role === "SELLER" || user?.role === "ADMIN") && <Link to="/seller">Seller Dashboard</Link>}
        {user?.role === "BUYER" && (
          <Link to="/cart" className="cart-link">
            Cart{itemCount > 0 ? ` (${itemCount})` : ""}
          </Link>
        )}
      </nav>
      <div className="nav-auth">
        {user ? (
          <>
            <span className="user-chip">
              {user.fullName} · {user.role}
            </span>
            <button onClick={handleLogout}>Log out</button>
          </>
        ) : (
          <>
            <Link to="/login">Log in</Link>
            <Link to="/register" className="cta">
              Sign up
            </Link>
          </>
        )}
      </div>
    </header>
  );
}
