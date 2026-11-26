import { useState } from "react";
import { Link } from "react-router-dom";
import { Alert, Button, Card, Form, Spinner } from "react-bootstrap";
import { FaCube } from "react-icons/fa";
import client, { getErrorMessage } from "../api/client";
import { useToast } from "../context/ToastContext";

function ForgotPasswordPage() {
  const { showToast } = useToast();
  const [email, setEmail] = useState("");
  const [submitting, setSubmitting] = useState(false);
  const [sentMessage, setSentMessage] = useState(null);

  const handleSubmit = async (e) => {
    e.preventDefault();
    setSubmitting(true);
    try {
      const res = await client.post("/auth/forgot-password", { email });
      setSentMessage(res.data.message);
    } catch (err) {
      showToast(getErrorMessage(err, "İstek gönderilemedi"), "danger");
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
            <h2 className="h4">Şifremi Unuttum</h2>
            <p className="text-muted mb-0">
              Kayıtlı email adresinizi yazın; talebiniz yöneticiye iletilir. Yönetici size tek kullanımlık bir şifre
              verir, bu şifreyle giriş yapınca yeni şifrenizi belirlersiniz.
            </p>
          </div>

          {sentMessage ? (
            <Alert variant="success">
              {sentMessage}
              <div className="mt-2 small">Tek kullanımlık şifreyi aldığınızda giriş sayfasından giriş yapabilirsiniz.</div>
            </Alert>
          ) : (
            <Form onSubmit={handleSubmit}>
              <Form.Group className="mb-4">
                <Form.Label>Email</Form.Label>
                <Form.Control
                  type="email"
                  value={email}
                  onChange={(e) => setEmail(e.target.value)}
                  autoComplete="username"
                  required
                  autoFocus
                />
              </Form.Group>
              <Button type="submit" className="w-100" disabled={submitting}>
                {submitting ? <Spinner size="sm" animation="border" /> : "Yöneticiye Talep Gönder"}
              </Button>
            </Form>
          )}

          <p className="text-center mt-3 mb-0">
            <Link to="/login">Giriş sayfasına dön</Link>
          </p>
        </Card.Body>
      </Card>
    </div>
  );
}

export default ForgotPasswordPage;
