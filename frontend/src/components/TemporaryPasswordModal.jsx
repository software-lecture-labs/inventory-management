import { useEffect, useState } from "react";
import { Alert, Button, Form, InputGroup, Modal, Spinner } from "react-bootstrap";
import { FaCopy, FaSyncAlt } from "react-icons/fa";
import client, { getErrorMessage } from "../api/client";
import { useToast } from "../context/ToastContext";

// Karışabilecek karakterler (0/O, 1/l/I) bilerek çıkarıldı; kullanıcıya sözlü/yazılı iletilecek
const LOWER = "abcdefghjkmnpqrstuvwxyz";
const UPPER = "ABCDEFGHJKLMNPQRSTUVWXYZ";
const DIGITS = "23456789";
const SYMBOLS = "!@#$%*?";

function randomPassword(length = 10) {
  const pick = (set) => set[crypto.getRandomValues(new Uint32Array(1))[0] % set.length];
  const all = LOWER + UPPER + DIGITS + SYMBOLS;
  const chars = [pick(LOWER), pick(UPPER), pick(DIGITS), pick(SYMBOLS)];
  while (chars.length < length) chars.push(pick(all));
  // Karıştır
  for (let i = chars.length - 1; i > 0; i--) {
    const j = crypto.getRandomValues(new Uint32Array(1))[0] % (i + 1);
    [chars[i], chars[j]] = [chars[j], chars[i]];
  }
  return chars.join("");
}

/**
 * Yöneticinin bir kullanıcıya tek kullanımlık şifre belirlediği pencere.
 * Kaydedilince şifre bir kez daha gösterilir ki yönetici kopyalayıp kullanıcıya iletebilsin.
 */
function TemporaryPasswordModal({ target, onHide, onDone }) {
  const { showToast } = useToast();
  const [password, setPassword] = useState("");
  const [saving, setSaving] = useState(false);
  const [done, setDone] = useState(false);

  useEffect(() => {
    if (target) {
      setPassword(randomPassword());
      setDone(false);
    }
  }, [target]);

  if (!target) return null;

  const handleSubmit = async (e) => {
    e.preventDefault();
    setSaving(true);
    try {
      await client.post(`/users/${target.userId}/temporary-password`, { password });
      setDone(true);
      onDone();
    } catch (err) {
      showToast(getErrorMessage(err, "Şifre belirlenemedi"), "danger");
    } finally {
      setSaving(false);
    }
  };

  const copy = async () => {
    try {
      await navigator.clipboard.writeText(password);
      showToast("Şifre kopyalandı");
    } catch {
      showToast("Kopyalanamadı, şifreyi elle seçip kopyalayın", "warning");
    }
  };

  return (
    <Modal show onHide={onHide} centered>
      <Form onSubmit={handleSubmit}>
        <Modal.Header closeButton>
          <Modal.Title className="h5">Tek Kullanımlık Şifre</Modal.Title>
        </Modal.Header>
        <Modal.Body>
          <p className="mb-3">
            <strong>{target.fullName}</strong> <span className="text-muted">({target.email})</span>
          </p>

          {done ? (
            <>
              <Alert variant="success">
                Şifre belirlendi. Aşağıdaki şifreyi kullanıcıya iletin; 24 saat geçerlidir. Kullanıcı bu şifreyle
                giriş yapınca kendi şifresini belirlemesi istenecek.
              </Alert>
              <InputGroup>
                <Form.Control value={password} readOnly className="font-monospace fs-5" onFocus={(e) => e.target.select()} />
                <Button variant="outline-secondary" onClick={copy}>
                  <FaCopy className="me-1" /> Kopyala
                </Button>
              </InputGroup>
              <Form.Text>Bu pencere kapandıktan sonra şifre tekrar gösterilmez.</Form.Text>
            </>
          ) : (
            <>
              <Form.Label>Şifre</Form.Label>
              <InputGroup>
                <Form.Control
                  value={password}
                  onChange={(e) => setPassword(e.target.value)}
                  className="font-monospace"
                  minLength={8}
                  maxLength={64}
                  required
                  autoComplete="off"
                />
                <Button variant="outline-secondary" onClick={() => setPassword(randomPassword())} title="Yeni şifre oluştur">
                  <FaSyncAlt className="me-1" /> Oluştur
                </Button>
              </InputGroup>
              <Form.Text>
                Otomatik oluşturulan şifreyi kullanabilir veya kendiniz yazabilirsiniz (en az 8 karakter). Kullanıcının
                eski şifresi geçersiz olur.
              </Form.Text>
            </>
          )}
        </Modal.Body>
        <Modal.Footer>
          {done ? (
            <Button onClick={onHide}>Tamam</Button>
          ) : (
            <>
              <Button variant="secondary" onClick={onHide}>
                Vazgeç
              </Button>
              <Button type="submit" disabled={saving}>
                {saving ? <Spinner size="sm" animation="border" /> : "Şifreyi Belirle"}
              </Button>
            </>
          )}
        </Modal.Footer>
      </Form>
    </Modal>
  );
}

export default TemporaryPasswordModal;
