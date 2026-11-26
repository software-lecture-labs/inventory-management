import { Navigate, Route, Routes } from "react-router-dom";
import AppLayout from "./components/AppLayout";
import ProtectedRoute from "./components/ProtectedRoute";
import LoginPage from "./pages/LoginPage";
import RegisterPage from "./pages/RegisterPage";
import ForgotPasswordPage from "./pages/ForgotPasswordPage";
import ChangePasswordPage from "./pages/ChangePasswordPage";
import DashboardPage from "./pages/DashboardPage";
import EquipmentPage from "./pages/EquipmentPage";
import EquipmentDetailPage from "./pages/EquipmentDetailPage";
import ReservationsPage from "./pages/ReservationsPage";
import MyReservationsPage from "./pages/MyReservationsPage";
import ApprovalsPage from "./pages/ApprovalsPage";
import ReportsPage from "./pages/ReportsPage";
import UsersPage from "./pages/UsersPage";

function App() {
  return (
    <Routes>
      {/* Herkese açık */}
      <Route path="/login" element={<LoginPage />} />
      <Route path="/register" element={<RegisterPage />} />
      <Route path="/forgot-password" element={<ForgotPasswordPage />} />

      {/* Tek kullanımlık şifreyle girildiyse: önce yeni şifre */}
      <Route element={<ProtectedRoute passwordChange />}>
        <Route path="/change-password" element={<ChangePasswordPage />} />
      </Route>

      {/* Giriş gerektirir */}
      <Route element={<ProtectedRoute />}>
        <Route element={<AppLayout />}>
          <Route path="/dashboard" element={<DashboardPage />} />
          <Route path="/equipment" element={<EquipmentPage />} />
          <Route path="/equipment/:id" element={<EquipmentDetailPage />} />
          <Route path="/reservations" element={<ReservationsPage />} />
          <Route path="/my-reservations" element={<MyReservationsPage />} />

          {/* Sadece yönetici */}
          <Route element={<ProtectedRoute adminOnly />}>
            <Route path="/approvals" element={<ApprovalsPage />} />
            <Route path="/reports" element={<ReportsPage />} />
            <Route path="/users" element={<UsersPage />} />
          </Route>
        </Route>
      </Route>

      <Route path="*" element={<Navigate to="/dashboard" replace />} />
    </Routes>
  );
}

export default App;
