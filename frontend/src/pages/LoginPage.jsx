import { useState } from "react";
import { Link, Navigate, useLocation, useNavigate } from "react-router-dom";
import { Button, Card, Form, Spinner } from "react-bootstrap";
import { FaCube } from "react-icons/fa";
import { useAuth } from "../context/AuthContext";
import { useToast } from "../context/ToastContext";
import { getErrorMessage } from "../api/client";

function LoginPage() {
  const { user, login } = useAuth();
  const { showToast } = useToast();
  const navigate = useNavigate();
  const location = useLocation();
  const [form, setForm] = useState({ email: "", password: "" });
  const [submitting, setSubmitting] = useState(false);

  if (user) {
    return <Navigate to="/dashboard" replace />;
  }

  const handleChange = (e) => setForm({ ...form, [e.target.name]: e.target.value });

  const handleSubmit = async (e) => {
    e.preventDefault();
    setSubmitting(true);
    try {
      const loggedIn = await login(form.email, form.password);
      showToast(`Hoş geldin, ${loggedIn.firstName}!`);
      navigate(location.state?.from || "/dashboard", { replace: true });
    } catch (err) {
      showToast(getErrorMessage(err, "Giriş yapılamadı"), "danger");
    } finally {
      setSubmitting(false);
    }
  };

  return (
    <div className="auth-page">
      <Card className="auth-card">
        <Card.Body>
          <div className="text-center mb-4">
            <FaCube size={40} className="text-primary mb-2" />
            <h2 className="h4">Envanter Yönetimi</h2>
            <p className="text-muted mb-0">Devam etmek için giriş yapın</p>
          </div>

          <Form onSubmit={handleSubmit}>
            <Form.Group className="mb-3">
              <Form.Label>Email</Form.Label>
              <Form.Control
                type="email"
                name="email"
                value={form.email}
                onChange={handleChange}
                autoComplete="username"
                required
              />
            </Form.Group>
            <Form.Group className="mb-4">
              <div className="d-flex justify-content-between">
                <Form.Label>Şifre</Form.Label>
                <Link to="/forgot-password" className="small">
                  Şifremi unuttum
                </Link>
              </div>
              <Form.Control
                type="password"
                name="password"
                value={form.password}
                onChange={handleChange}
                autoComplete="current-password"
                required
              />
            </Form.Group>
            <Button type="submit" className="w-100" disabled={submitting}>
              {submitting ? <Spinner size="sm" animation="border" /> : "Giriş Yap"}
            </Button>
          </Form>

          <p className="text-center mt-3 mb-0">
            Hesabınız yok mu? <Link to="/register">Kayıt olun</Link>
          </p>
        </Card.Body>
      </Card>
    </div>
  );
}

export default LoginPage;
