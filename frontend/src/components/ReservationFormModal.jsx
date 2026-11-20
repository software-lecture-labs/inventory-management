import { useEffect, useState } from "react";
import { Alert, Button, Col, Form, Modal, Row, Spinner } from "react-bootstrap";
import client, { getErrorMessage } from "../api/client";
import { useToast } from "../context/ToastContext";
import { toDateTimeLocal } from "../utils/format";
import EquipmentPicker from "./EquipmentPicker";

// Varsayılan aralık: bir sonraki tam saatten başlayan 1 saat
function defaultRange() {
  const start = new Date();
  start.setHours(start.getHours() + 1, 0, 0, 0);
  const end = new Date(start);
  end.setHours(end.getHours() + 1);
  return { startTime: toDateTimeLocal(start), endTime: toDateTimeLocal(end) };
}

/**
 * Yeni rezervasyon modalı.
 * initialValues ile ekipman veya zaman aralığı önceden seçilebilir.
 * Seçilen aralık doluysa kullanıcıya bekleme listesine girme seçeneği sunulur.
 */
function ReservationFormModal({ show, equipmentOptions, initialValues, onHide, onSaved }) {
  const { showToast } = useToast();
  const [form, setForm] = useState({ equipmentId: "", startTime: "", endTime: "" });
  const [saving, setSaving] = useState(false);
  const [conflict, setConflict] = useState(false);
  const [equipmentMissing, setEquipmentMissing] = useState(false);

  useEffect(() => {
    if (show) {
      setForm({ equipmentId: "", ...defaultRange(), ...initialValues });
      setConflict(false);
      setEquipmentMissing(false);
    }
  }, [show, initialValues]);

  const selected = equipmentOptions.find((eq) => String(eq.id) === String(form.equipmentId));

  const handleChange = (e) => {
    setForm({ ...form, [e.target.name]: e.target.value });
    setConflict(false);
  };

  const payload = () => ({
    equipmentId: Number(form.equipmentId),
    startTime: form.startTime,
    endTime: form.endTime,
  });

  const handleSubmit = async (e) => {
    e.preventDefault();
    if (!form.equipmentId) {
      setEquipmentMissing(true);
      return;
    }
    if (form.endTime <= form.startTime) {
      showToast("Bitiş zamanı başlangıçtan sonra olmalıdır", "danger");
      return;
    }

    setSaving(true);
    try {
      const res = await client.post("/reservations", payload());
      showToast(
        res.data.status === "PENDING"
          ? "Rezervasyon talebiniz alındı, yönetici onayı bekleniyor"
          : "Rezervasyon oluşturuldu"
      );
      onSaved(res.data);
    } catch (err) {
      if (err.response?.status === 409) {
        setConflict(true);
      } else {
        showToast(getErrorMessage(err, "Rezervasyon oluşturulamadı"), "danger");
      }
    } finally {
      setSaving(false);
    }
  };

  const joinWaitlist = async () => {
    setSaving(true);
    try {
      await client.post("/waitlist", payload());
      showToast("Bekleme listesine eklendiniz. Aralık boşalırsa bildirim gelecek.");
      onHide();
    } catch (err) {
      showToast(getErrorMessage(err, "Bekleme listesine eklenemedi"), "danger");
    } finally {
      setSaving(false);
    }
  };

  return (
    <Modal show={show} onHide={onHide} centered>
      <Form onSubmit={handleSubmit}>
        <Modal.Header closeButton>
          <Modal.Title>Yeni Rezervasyon</Modal.Title>
        </Modal.Header>
        <Modal.Body>
          <Form.Group className="mb-3">
            <Form.Label>Ekipman</Form.Label>
            <EquipmentPicker
              options={equipmentOptions}
              value={form.equipmentId}
              isInvalid={equipmentMissing}
              onChange={(id) => {
                setForm((prev) => ({ ...prev, equipmentId: id }));
                setConflict(false);
                if (id) setEquipmentMissing(false);
              }}
            />
            {equipmentMissing && <div className="invalid-feedback d-block">Listeden bir ekipman seçin</div>}
            {selected?.requiresApproval && (
              <Form.Text className="text-warning-emphasis">
                Bu ekipmanın rezervasyonları yönetici onayından sonra kesinleşir.
              </Form.Text>
            )}
          </Form.Group>
          <Row>
            <Col sm={6}>
              <Form.Group className="mb-3">
                <Form.Label>Başlangıç</Form.Label>
                <Form.Control
                  type="datetime-local"
                  name="startTime"
                  value={form.startTime}
                  onChange={handleChange}
                  required
                />
              </Form.Group>
            </Col>
            <Col sm={6}>
              <Form.Group className="mb-3">
                <Form.Label>Bitiş</Form.Label>
                <Form.Control
                  type="datetime-local"
                  name="endTime"
                  value={form.endTime}
                  min={form.startTime}
                  onChange={handleChange}
                  required
                />
              </Form.Group>
            </Col>
          </Row>

          {conflict && (
            <Alert variant="warning" className="mb-0">
              <div className="mb-2">Ekipman bu zaman aralığında dolu.</div>
              <Button size="sm" variant="warning" onClick={joinWaitlist} disabled={saving}>
                Bekleme listesine ekle
              </Button>
              <div className="small mt-2 text-muted">
                Aralık boşalırsa (iptal, red veya erken iade) üst bardaki zile bildirim gelir.
              </div>
            </Alert>
          )}
        </Modal.Body>
        <Modal.Footer>
          <Button variant="secondary" onClick={onHide}>
            Vazgeç
          </Button>
          <Button type="submit" variant="success" disabled={saving}>
            {saving ? <Spinner size="sm" animation="border" /> : "Rezerve Et"}
          </Button>
        </Modal.Footer>
      </Form>
    </Modal>
  );
}

export default ReservationFormModal;
