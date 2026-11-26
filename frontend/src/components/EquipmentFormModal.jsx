import { useEffect, useState } from "react";
import { Button, Col, Form, Modal, Row, Spinner } from "react-bootstrap";
import client, { getErrorMessage } from "../api/client";
import { useToast } from "../context/ToastContext";
import { EQUIPMENT_STATUS } from "../utils/format";

const emptyForm = {
  name: "",
  brand: "",
  model: "",
  serialNumber: "",
  category: "",
  location: "",
  calibrationDate: "",
  description: "",
  status: "AVAILABLE",
  requiresApproval: false,
};

/**
 * Ekipman ekleme ve düzenleme için ortak modal.
 * equipment verilirse düzenleme, verilmezse ekleme modunda açılır.
 * filterOptions: mevcut kategori ve konumlar (yazarken öneri olarak gösterilir).
 */
function EquipmentFormModal({ show, equipment, filterOptions, onHide, onSaved }) {
  const { showToast } = useToast();
  const [form, setForm] = useState(emptyForm);
  const [fieldErrors, setFieldErrors] = useState({});
  const [saving, setSaving] = useState(false);
  const isEdit = Boolean(equipment);

  // Modal her açıldığında formu doldur / temizle
  useEffect(() => {
    if (show) {
      setForm(
        equipment
          ? Object.fromEntries(Object.keys(emptyForm).map((k) => [k, equipment[k] ?? emptyForm[k]]))
          : emptyForm
      );
      setFieldErrors({});
    }
  }, [show, equipment]);

  const handleChange = (e) => {
    const { name, value, type, checked } = e.target;
    setForm({ ...form, [name]: type === "checkbox" ? checked : value });
  };

  const handleSubmit = async (e) => {
    e.preventDefault();
    setSaving(true);
    const payload = { ...form, calibrationDate: form.calibrationDate || null };

    try {
      const res = isEdit
        ? await client.put(`/equipment/${equipment.id}`, payload)
        : await client.post("/equipment", payload);
      showToast(isEdit ? "Ekipman güncellendi" : "Ekipman eklendi");
      onSaved(res.data);
    } catch (err) {
      setFieldErrors(err.response?.data?.fieldErrors || {});
      showToast(getErrorMessage(err, "Kaydedilemedi"), "danger");
    } finally {
      setSaving(false);
    }
  };

  const field = (name, label, props = {}) => (
    <Form.Group className="mb-3">
      <Form.Label>{label}</Form.Label>
      <Form.Control
        name={name}
        value={form[name]}
        onChange={handleChange}
        isInvalid={!!fieldErrors[name]}
        {...props}
      />
      <Form.Control.Feedback type="invalid">{fieldErrors[name]}</Form.Control.Feedback>
    </Form.Group>
  );

  return (
    <Modal show={show} onHide={onHide} centered size="lg">
      <Form onSubmit={handleSubmit}>
        <Modal.Header closeButton>
          <Modal.Title>{isEdit ? "Ekipmanı Düzenle" : "Yeni Ekipman"}</Modal.Title>
        </Modal.Header>
        <Modal.Body>
          <Row>
            <Col sm={6}>{field("name", "Ekipman Adı", { required: true, maxLength: 100 })}</Col>
            <Col sm={6}>{field("serialNumber", "Seri No", { required: true, maxLength: 100 })}</Col>
          </Row>
          <Row>
            <Col sm={6}>{field("brand", "Marka", { required: true, maxLength: 100 })}</Col>
            <Col sm={6}>{field("model", "Model", { required: true, maxLength: 100 })}</Col>
          </Row>
          <Row>
            <Col sm={6}>
              {field("category", "Kategori", { maxLength: 50, list: "category-options", placeholder: "ör. Ölçüm" })}
            </Col>
            <Col sm={6}>
              {field("location", "Konum", { maxLength: 100, list: "location-options", placeholder: "ör. Lab 2 / Raf A" })}
            </Col>
          </Row>
          <datalist id="category-options">
            {filterOptions?.categories.map((c) => (
              <option key={c} value={c} />
            ))}
          </datalist>
          <datalist id="location-options">
            {filterOptions?.locations.map((l) => (
              <option key={l} value={l} />
            ))}
          </datalist>
          <Row>
            <Col sm={6}>{field("calibrationDate", "Sonraki Kalibrasyon Tarihi", { type: "date" })}</Col>
            <Col sm={6}>
              <Form.Group className="mb-3">
                <Form.Label>Durum</Form.Label>
                <Form.Select name="status" value={form.status} onChange={handleChange}>
                  {Object.entries(EQUIPMENT_STATUS).map(([key, { label }]) => (
                    <option key={key} value={key}>
                      {label}
                    </option>
                  ))}
                </Form.Select>
              </Form.Group>
            </Col>
          </Row>
          <Form.Check
            type="switch"
            id="requires-approval"
            name="requiresApproval"
            className="mb-3"
            label="Rezervasyonlar yönetici onayı gerektirsin"
            checked={form.requiresApproval}
            onChange={handleChange}
          />
          {field("description", "Açıklama", { as: "textarea", rows: 3, maxLength: 500 })}
        </Modal.Body>
        <Modal.Footer>
          <Button variant="secondary" onClick={onHide}>
            Vazgeç
          </Button>
          <Button type="submit" disabled={saving}>
            {saving ? <Spinner size="sm" animation="border" /> : "Kaydet"}
          </Button>
        </Modal.Footer>
      </Form>
    </Modal>
  );
}

export default EquipmentFormModal;
