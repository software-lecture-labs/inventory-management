import React, { useState, useEffect } from 'react';
import { Row, Form, Col, Button, Toast, ToastContainer } from 'react-bootstrap';
import Select from "react-select";

function Register() {
  const containerStyle = {
    display: 'flex',
    flexDirection: 'column',
    justifyContent: 'center',
    alignItems: 'center',
    height: '100vh',
    textAlign: 'center',
  };

  const [firstName, setFirstName] = useState('');
  const [lastName, setLastName] = useState('');
  const [email, setEmail] = useState('');
  const [password, setPassword] = useState('');
  const [confirmPassword, setConfirmPassword] = useState('');
  const [phoneNumber, setPhoneNumber] = useState('');
  const [roleOptions, setRoleOptions] = useState([]);
  const [roleId, setRoleId] = useState(null);

  // Toast için state
  const [showToast, setShowToast] = useState(false);
  const [toastMessage, setToastMessage] = useState('');
  const [toastVariant, setToastVariant] = useState('danger'); // hata = red, success = green

  // Roller backend’den çekilsin
  useEffect(() => {
    fetch("http://localhost:8080/role")
      .then(res => res.json())
      .then(data => {
        setRoleOptions(
          data.map(r => ({ value: r.roleId, label: r.roleName }))
        );
      })
      .catch(err => {
        setToastMessage("Rol verileri alınamadı!");
        setToastVariant("danger");
        setShowToast(true);
      });
  }, []);

  const handleSubmit = async (e) => {
    e.preventDefault();

    if (password !== confirmPassword) {
      setToastMessage("Şifreler eşleşmiyor!");
      setToastVariant("danger");
      setShowToast(true);
      return;
    }

    if (!roleId) {
      setToastMessage("Lütfen bir rol seçiniz!");
      setToastVariant("danger");
      setShowToast(true);
      return;
    }

    const userData = {
      firstName,
      lastName,
      email,
      phoneNumber,
      password,
      role: { roleId: roleId }
    };

    try {
      const response = await fetch('http://localhost:8080/employee/register', {
        method: 'POST',
        headers: { 'Content-Type': 'application/json' },
        body: JSON.stringify(userData),
      });

      if (response.ok) {
        setToastMessage('Kayıt başarılı!');
        setToastVariant("success");
        setShowToast(true);

        setFirstName('');
        setLastName('');
        setEmail('');
        setPassword('');
        setConfirmPassword('');
        setPhoneNumber('');
        setRoleId(null);

        setTimeout(() => { window.location.href = "/"; }, 1500);
      } else {
        const errData = await response.json();
        setToastMessage(errData.message || 'Kayıt sırasında bir hata oluştu.');
        setToastVariant("danger");
        setShowToast(true);
      }
    } catch (error) {
      console.error('Hata:', error);
      setToastMessage('Sunucuya ulaşılamıyor.');
      setToastVariant("danger");
      setShowToast(true);
    }
  };

  return (
    <div style={containerStyle}>
      <h1>Kayıt Ol</h1>
      <Form onSubmit={handleSubmit} style={{ textAlign: "left" }}>
        <Row>
          <Col>
            <Form.Group className="mb-3">
              <Form.Label>Ad</Form.Label>
              <Form.Control
                type="text"
                value={firstName}
                onChange={(e) => {
                  const value = e.target.value;
                  if (/^[a-zA-ZğüşöçıİĞÜŞÖÇ\s]*$/.test(value)) {
                    setFirstName(value);
                  }
                }}
                placeholder="Adınızı giriniz"
                required
              />
            </Form.Group>
          </Col>
          <Col>
            <Form.Group className="mb-3">
              <Form.Label>Soyad</Form.Label>
              <Form.Control
                type="text"
                value={lastName}
                onChange={(e) => {
                  const value = e.target.value;
                  if (/^[a-zA-ZğüşöçıİĞÜŞÖÇ\s]*$/.test(value)) {
                    setLastName(value);
                  }
                }}
                placeholder="Soyadınızı giriniz"
                required
              />
            </Form.Group>
          </Col>
        </Row>

        <Row>
          <Col>
            <Form.Group className="mb-3">
              <Form.Label>Rol Seç</Form.Label>
              <Select
                options={roleOptions}
                placeholder="Rol seçiniz"
                isSearchable
                value={roleOptions.find(option => option.value === roleId) || null}
                onChange={(selected) => setRoleId(selected.value)}
              />
            </Form.Group>
          </Col>
        </Row>

        <Form.Group className="mb-3">
          <Form.Label>Telefon Numarası</Form.Label>
          <Form.Control
            type="text"
            value={phoneNumber}
            onChange={(e) => {
              const value = e.target.value;
              if (/^[0-9]*$/.test(value)) {
                setPhoneNumber(value);
              }
            }}
            placeholder="Telefon numaranızı giriniz"
            required
          />
        </Form.Group>

        <Form.Group className="mb-3">
          <Form.Label>Email</Form.Label>
          <Form.Control
            type="email"
            value={email}
            onChange={(e) => setEmail(e.target.value)}
            placeholder="Emailinizi giriniz"
            required
            pattern="^[a-zA-Z0-9._%+-]+@[a-zA-Z0-9.-]+\.[a-zA-Z]{2,}$"
          />
        </Form.Group>

        <Form.Group className="mb-3">
          <Form.Label>Şifre</Form.Label>
          <Form.Control
            type="password"
            value={password}
            onChange={(e) => setPassword(e.target.value)}
            placeholder="Şifrenizi giriniz"
            required
            pattern="^(?=.*[a-z])(?=.*[A-Z])(?=.*[^a-zA-Z0-9]).{8,}$"
            title="Şifre en az 8 karakterli, bir büyük harf, bir küçük harf ve bir özel karakter içermelidir."
          />
        </Form.Group>

        <Form.Group className="mb-3">
          <Form.Label>Şifre Tekrarı</Form.Label>
          <Form.Control
            type="password"
            value={confirmPassword}
            onChange={(e) => setConfirmPassword(e.target.value)}
            placeholder="Şifrenizi tekrar giriniz"
            required
            pattern="^(?=.*[a-z])(?=.*[A-Z])(?=.*[^a-zA-Z0-9]).{8,}$"
            title="Şifre en az 8 karakterli, bir büyük harf, bir küçük harf ve bir özel karakter içermelidir."
          />
        </Form.Group>

        <Button variant="success" type="submit" className="w-100">
          Kayıt Ol
        </Button>
      </Form>

      {/* Toast Container */}
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

export default Register;
