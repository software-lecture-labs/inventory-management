import { useState } from "react";
import { useNavigate } from "react-router-dom";
import { Alert, Button, Card, Form, Spinner } from "react-bootstrap";
import { FaKey } from "react-icons/fa";
import client, { getErrorMessage } from "../api/client";
import { useAuth } from "../context/AuthContext";
import { useToast } from "../context/ToastContext";

// Kayıt formuyla aynı kural (backend de kontrol eder)
const PASSWORD_PATTERN = "(?=.*[a-z])(?=.*[A-Z])(?=.*[^a-zA-Z0-9]).{8,}";

/**
 * Yöneticinin verdiği tek kullanımlık şifreyle giriş yapan kullanıcının yeni şifre belirlediği ekran.
 * Yeni şifre kaydedilene kadar uygulamanın başka bir sayfasına geçilemez.
 */
function ChangePasswordPage() {
  const { user, updateUser, logout } = useAuth();
  const { showToast } = useToast();
  const navigate = useNavigate();
  const [form, setForm] = useState({ newPassword: "", confirmPassword: "" });
  const [fieldError, setFieldError] = useState(null);
  const [submitting, setSubmitting] = useState(false);

  const handleChange = (e) => setForm({ ...form, [e.target.name]: e.target.value });

  const handleSubmit = async (e) => {
    e.preventDefault();
    if (form.newPassword !== form.confirmPassword) {
      showToast("Şifreler eşleşmiyor", "danger");
      return;
    }
    setSubmitting(true);
    setFieldError(null);
    try {
      const res = await client.post("/auth/change-password", { newPassword: form.newPassword });
      updateUser(res.data);
      showToast("Yeni şifreniz kaydedildi");
      navigate("/dashboard", { replace: true });
    } catch (err) {
      setFieldError(err.response?.data?.fieldErrors?.newPassword || null);
      showToast(getErrorMessage(err, "Şifre değiştirilemedi"), "danger");
    } finally {
      setSubmitting(false);
    }
  };

  const handleLogout = () => {
    logout();
    navigate("/login", { replace: true });
  };

  return (
    <div className="auth-page">
      <Card className="auth-card">
        <Card.Body>
          <div className="text-center mb-3">
            <FaKey size={36} className="text-primary mb-2" />
            <h2 className="h4">Yeni Şifre Belirleyin</h2>
          </div>

          <Alert variant="info" className="small">
            Merhaba {user.firstName}, yöneticinin verdiği tek kullanımlık şifreyle giriş yaptınız. Devam etmek için
            kendi şifrenizi belirleyin. Tek kullanımlık şifre bundan sonra geçersiz olacak.
          </Alert>

          <Form onSubmit={handleSubmit}>
            <Form.Group className="mb-3">
              <Form.Label>Yeni şifre</Form.Label>
              <Form.Control
                type="password"
                name="newPassword"
                value={form.newPassword}
                onChange={handleChange}
                autoComplete="new-password"
                pattern={PASSWORD_PATTERN}
                title="En az 8 karakter; büyük harf, küçük harf ve özel karakter içermelidir"
                isInvalid={!!fieldError}
                required
                autoFocus
              />
              <Form.Control.Feedback type="invalid">{fieldError}</Form.Control.Feedback>
              <Form.Text>En az 8 karakter; büyük harf, küçük harf ve özel karakter içermelidir.</Form.Text>
            </Form.Group>
            <Form.Group className="mb-4">
              <Form.Label>Yeni şifre (tekrar)</Form.Label>
              <Form.Control
                type="password"
                name="confirmPassword"
                value={form.confirmPassword}
                onChange={handleChange}
                autoComplete="new-password"
                isInvalid={form.confirmPassword !== "" && form.confirmPassword !== form.newPassword}
                required
              />
              <Form.Control.Feedback type="invalid">Şifreler eşleşmiyor</Form.Control.Feedback>
            </Form.Group>
            <Button type="submit" className="w-100" disabled={submitting}>
              {submitting ? <Spinner size="sm" animation="border" /> : "Şifremi Kaydet"}
            </Button>
          </Form>

          <p className="text-center mt-3 mb-0">
            <Button variant="link" className="p-0" onClick={handleLogout}>
              Çıkış yap
            </Button>
          </p>
        </Card.Body>
      </Card>
    </div>
  );
}

export default ChangePasswordPage;
