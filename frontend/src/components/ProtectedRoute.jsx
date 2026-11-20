import { Navigate, Outlet, useLocation } from "react-router-dom";
import { Spinner } from "react-bootstrap";
import { useAuth } from "../context/AuthContext";

/**
 * Giriş yapmamış kullanıcıyı giriş sayfasına yönlendirir.
 * adminOnly verilirse yönetici olmayanları ana sayfaya gönderir.
 * Tek kullanımlık şifreyle giren kullanıcı, yeni şifre belirleyene kadar sadece şifre değiştirme
 * sayfasını (passwordChange) görebilir.
 */
function ProtectedRoute({ adminOnly = false, passwordChange = false }) {
  const { user, loading, isAdmin } = useAuth();
  const location = useLocation();

  if (loading) {
    return (
      <div className="d-flex justify-content-center align-items-center vh-100">
        <Spinner animation="border" variant="primary" />
      </div>
    );
  }

  if (!user) {
    return <Navigate to="/login" replace state={{ from: location.pathname }} />;
  }

  if (user.mustChangePassword && !passwordChange) {
    return <Navigate to="/change-password" replace />;
  }

  if (!user.mustChangePassword && passwordChange) {
    return <Navigate to="/dashboard" replace />;
  }

  if (adminOnly && !isAdmin) {
    return <Navigate to="/dashboard" replace />;
  }

  return <Outlet />;
}

export default ProtectedRoute;
