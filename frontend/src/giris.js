import React, { useState, useEffect } from "react";
import { useNavigate } from "react-router-dom";
import { Toast, ToastContainer } from "react-bootstrap";

function Giris() {
  const [email, setEmail] = useState('');
  const [password, setPassword] = useState('');
  const [showToast, setShowToast] = useState(false);
  const [toastMessage, setToastMessage] = useState('');
  const [toastVariant, setToastVariant] = useState('danger'); // hata = kırmızı, başarılı = yeşil
  const navigate = useNavigate();

  useEffect(() => {
    // PrivateRoute'tan gelen mesaj varsa göster
    const authMsg = localStorage.getItem("authMessage");
    if (authMsg) {
      setToastMessage(authMsg);
      setToastVariant("danger");
      setShowToast(true);
      localStorage.removeItem("authMessage"); // bir kez göster
    }
  }, []);

  const containerStyle = {
    display: "flex",
    justifyContent: "center",
    alignItems: "center",
    height: "100vh",
    background: "linear-gradient(135deg, #ffffff, #e0f2fe, #bae6fd)",
    fontFamily: "Arial, sans-serif",
  };

  const cardStyle = {
    backgroundColor: "white",
    padding: "40px",
    borderRadius: "12px",
    boxShadow: "0 8px 30px rgba(0,0,0,0.2)",
    width: "100%",
    maxWidth: "400px",
    textAlign: "center",
  };

  const inputStyle = {
    width: "100%",
    padding: "12px",
    margin: "10px 0",
    borderRadius: "8px",
    border: "1px solid #ccc",
    fontSize: "16px",
  };

  const buttonStyle = {
    width: "100%",
    padding: "12px",
    margin: "15px 0",
    borderRadius: "8px",
    border: "none",
    fontSize: "16px",
    cursor: "pointer",
    backgroundColor: "#2563eb",
    color: "white",
    fontWeight: "bold",
    transition: "0.3s",
  };

  const text = {
    color: "black",
    cursor: "pointer",
  };

  const handleLogin = async (e) => {
    e.preventDefault();
    
    if (!email || !password) {
      setToastMessage("Lütfen tüm alanları doldurun");
      setToastVariant("danger");
      setShowToast(true);
      return;
    }

    try {
      const response = await fetch("http://localhost:8080/employee/login", {
        method: "POST",
        headers: { "Content-Type": "application/json" },
        body: JSON.stringify({ email, password }),
      });

      const data = await response.json();

      if (data.success) {
        setToastMessage("Giriş başarılı");
        setToastVariant("success");
        setShowToast(true);

        setEmail('');
        setPassword('');

        localStorage.setItem("employeeId", data.id);
        localStorage.setItem("fullName", data.fullName);
        localStorage.setItem("user", JSON.stringify(data));

        setTimeout(() => navigate("/ekipman"), 1500);
      } else {
        setToastMessage("Email veya şifre hatalı");
        setToastVariant("danger");
        setShowToast(true);
      }
    } catch (error) {
      console.error(error);
      setToastMessage("Sunucuya bağlanırken hata oluştu");
      setToastVariant("danger");
      setShowToast(true);
    }
  };

  return (
    <div style={containerStyle}>
      <div style={cardStyle}>
        <h2>Giriş Yap</h2>
        <form onSubmit={handleLogin}>
          <input
            type="email"
            placeholder="Email"
            style={inputStyle}
            value={email}
            onChange={(e) => setEmail(e.target.value)}
            required
          />
          <input
            type="password"
            placeholder="Şifre"
            style={inputStyle}
            value={password}
            onChange={(e) => setPassword(e.target.value)}
            required
          />
          <button type="submit" style={buttonStyle}>
            Giriş Yap
          </button>
          <h6
            style={text}
            onClick={() => (window.location.href = "/register")}
          >
            Kayıt Ol
          </h6>
        </form>
      </div>

      {/* Toast */}
      <ToastContainer position="top-end" className="p-3">
        <Toast
          bg={toastVariant}
          show={showToast}
          onClose={() => setShowToast(false)}
          delay={2000}
          autohide
        >
          <Toast.Body style={{ color: "white" }}>{toastMessage}</Toast.Body>
        </Toast>
      </ToastContainer>
    </div>
  );
}

export default Giris;