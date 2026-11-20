import { useEffect, useState } from "react";
import { NavLink, Outlet, useLocation, useNavigate } from "react-router-dom";
import { Button, Badge } from "react-bootstrap";
import {
  FaBoxes,
  FaCalendarAlt,
  FaChartBar,
  FaClipboardCheck,
  FaCube,
  FaHome,
  FaListAlt,
  FaSignOutAlt,
  FaUsers,
} from "react-icons/fa";
import client from "../api/client";
import NotificationBell from "./NotificationBell";
import { useAuth } from "../context/AuthContext";

const menu = [
  { to: "/dashboard", label: "Özet", icon: FaHome },
  { to: "/equipment", label: "Ekipmanlar", icon: FaBoxes },
  { to: "/reservations", label: "Takvim", icon: FaCalendarAlt },
  { to: "/my-reservations", label: "Rezervasyonlarım", icon: FaListAlt },
  { to: "/approvals", label: "Onay / İade", icon: FaClipboardCheck, adminOnly: true, badge: "pending" },
  { to: "/reports", label: "Raporlar", icon: FaChartBar, adminOnly: true },
  { to: "/users", label: "Kullanıcılar", icon: FaUsers, adminOnly: true },
];

/** Giriş sonrası tüm sayfaların ortak iskeleti: üst bar + yan menü + içerik. */
function AppLayout() {
  const { user, isAdmin, logout } = useAuth();
  const navigate = useNavigate();
  const location = useLocation();
  const [pendingCount, setPendingCount] = useState(0);

  // Yönetici için onay bekleyen talep sayısı (sayfa değiştikçe tazelenir)
  useEffect(() => {
    if (!isAdmin) return;
    client
      .get("/reservations/search", { params: { status: "PENDING", size: 1 } })
      .then((res) => setPendingCount(res.data.totalElements))
      .catch(() => {});
  }, [isAdmin, location.pathname]);

  const handleLogout = () => {
    if (window.confirm("Çıkış yapmak istediğinize emin misiniz?")) {
      logout();
      navigate("/login");
    }
  };

  return (
    <div className="app-shell">
      <header className="app-header">
        <div className="brand">
          <FaCube size={22} />
          <span>Envanter Yönetimi</span>
        </div>
        <div className="d-flex align-items-center gap-3">
          <NotificationBell />
          <span className="d-none d-sm-inline">
            {user.fullName}{" "}
            <Badge bg={isAdmin ? "primary" : "secondary"}>{isAdmin ? "Yönetici" : "Kullanıcı"}</Badge>
          </span>
          <Button variant="outline-light" size="sm" onClick={handleLogout}>
            <FaSignOutAlt className="me-1" /> Çıkış
          </Button>
        </div>
      </header>

      <div className="app-body">
        <nav className="app-sidebar">
          {menu
            .filter((item) => !item.adminOnly || isAdmin)
            .map(({ to, label, icon: Icon, badge }) => (
              <NavLink key={to} to={to} className="sidebar-link">
                <Icon className="me-2" /> {label}
                {badge === "pending" && pendingCount > 0 && (
                  <Badge bg="warning" text="dark" pill title="Onay bekleyen talep">
                    {pendingCount}
                  </Badge>
                )}
              </NavLink>
            ))}
        </nav>

        <main className="app-content">
          <Outlet />
        </main>
      </div>
    </div>
  );
}

export default AppLayout;
