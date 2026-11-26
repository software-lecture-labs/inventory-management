import { useState } from "react";
import { Link, Navigate, useNavigate } from "react-router-dom";
import { Button, Card, Col, Form, Row, Spinner } from "react-bootstrap";
import { useAuth } from "../context/AuthContext";
import { useToast } from "../context/ToastContext";
import { getErrorMessage } from "../api/client";

const LETTERS_ONLY = /^[\p{L} ]*$/u;
const DIGITS_ONLY = /^[0-9]*$/;
const PASSWORD_PATTERN = "(?=.*[a-z])(?=.*[A-Z])(?=.*[^a-zA-Z0-9]).{8,}";

const emptyForm = {
  firstName: "",
  lastName: "",
  email: "",
  phone: "",
  password: "",
  confirmPassword: "",
};

function RegisterPage() {
  const { user, register } = useAuth();
  const { showToast } = useToast();
  const navigate = useNavigate();
  const [form, setForm] = useState(emptyForm);
  const [fieldErrors, setFieldErrors] = useState({});
  const [submitting, setSubmitting] = useState(false);

  if (user) {
    return <Navigate to="/dashboard" replace />;
  }

  // pattern verilirse sadece uyan karakterleri kabul eder (ör. ad alanına rakam yazılamaz)
  const handleChange = (pattern) => (e) => {
    const { name, value } = e.target;
    if (!pattern || pattern.test(value)) {
      setForm((prev) => ({ ...prev, [name]: value }));
    }
  };

  const handleSubmit = async (e) => {
    e.preventDefault();
    if (form.password !== form.confirmPassword) {
      setFieldErrors({ confirmPassword: "Şifreler eşleşmiyor" });
      return;
    }

    setSubmitting(true);
    setFieldErrors({});
    try {
      const { confirmPassword, ...payload } = form;
      await register(payload);
      showToast("Kayıt başarılı, hoş geldiniz!");
      navigate("/dashboard", { replace: true });
    } catch (err) {
      setFieldErrors(err.response?.data?.fieldErrors || {});
      showToast(getErrorMessage(err, "Kayıt sırasında bir hata oluştu"), "danger");
    } finally {
      setSubmitting(false);
    }
  };

  const field = (name, label, props = {}, pattern) => (
    <Form.Group className="mb-3">
      <Form.Label>{label}</Form.Label>
      <Form.Control
        name={name}
        value={form[name]}
        onChange={handleChange(pattern)}
        isInvalid={!!fieldErrors[name]}
        required
        {...props}
      />
      <Form.Control.Feedback type="invalid">{fieldErrors[name]}</Form.Control.Feedback>
    </Form.Group>
  );

  return (
    <div className="auth-page">
      <Card className="auth-card auth-card-wide">
        <Card.Body>
          <h2 className="h4 text-center mb-4">Kayıt Ol</h2>
          <Form onSubmit={handleSubmit}>
            <Row>
              <Col sm={6}>{field("firstName", "Ad", { maxLength: 50 }, LETTERS_ONLY)}</Col>
              <Col sm={6}>{field("lastName", "Soyad", { maxLength: 50 }, LETTERS_ONLY)}</Col>
            </Row>
            {field("email", "Email", { type: "email", autoComplete: "email" })}
            {field("phone", "Telefon", { placeholder: "5xxxxxxxxx", minLength: 10, maxLength: 15 }, DIGITS_ONLY)}
            <Row>
              <Col sm={6}>
                {field("password", "Şifre", {
                  type: "password",
                  pattern: PASSWORD_PATTERN,
                  title: "En az 8 karakter; büyük harf, küçük harf ve özel karakter içermelidir",
                  autoComplete: "new-password",
                })}
              </Col>
              <Col sm={6}>
                {field("confirmPassword", "Şifre Tekrarı", { type: "password", autoComplete: "new-password" })}
              </Col>
            </Row>
            <Form.Text className="d-block mb-3">
              Şifre en az 8 karakter olmalı; büyük harf, küçük harf ve özel karakter içermelidir.
            </Form.Text>

            <Button type="submit" variant="success" className="w-100" disabled={submitting}>
              {submitting ? <Spinner size="sm" animation="border" /> : "Kayıt Ol"}
            </Button>
          </Form>

          <p className="text-center mt-3 mb-0">
            Zaten hesabınız var mı? <Link to="/login">Giriş yapın</Link>
          </p>
        </Card.Body>
      </Card>
    </div>
  );
}

export default RegisterPage;
