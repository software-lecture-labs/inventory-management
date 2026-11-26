import { useCallback, useEffect, useMemo, useState } from "react";
import { Link, useParams } from "react-router-dom";
import { Badge, Button, Card, Col, Form, Modal, Row, Spinner, Table } from "react-bootstrap";
import { FaArrowLeft, FaCalendarPlus, FaEdit, FaPlus } from "react-icons/fa";
import client, { getErrorMessage } from "../api/client";
import { useAuth } from "../context/AuthContext";
import { useToast } from "../context/ToastContext";
import EquipmentFormModal from "../components/EquipmentFormModal";
import ReservationFormModal from "../components/ReservationFormModal";
import { CalibrationCell, EquipmentStatusBadge } from "../components/StatusBadges";
import { EQUIPMENT_STATUS, MAINTENANCE_TYPES, formatDate, formatDateTime, todayIso } from "../utils/format";

const emptyMaintenance = {
  type: "MAINTENANCE",
  performedDate: "",
  nextDueDate: "",
  performedBy: "",
  description: "",
  markAvailable: true,
};

/** Bakım / kalibrasyon / onarım kaydı ekleme formu. */
function MaintenanceModal({ show, equipment, onHide, onSaved }) {
  const { showToast } = useToast();
  const [form, setForm] = useState(emptyMaintenance);
  const [fieldErrors, setFieldErrors] = useState({});
  const [saving, setSaving] = useState(false);

  useEffect(() => {
    if (show) {
      setForm({ ...emptyMaintenance, performedDate: todayIso(), markAvailable: equipment.status !== "AVAILABLE" });
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
    try {
      await client.post(`/equipment/${equipment.id}/maintenance`, {
        ...form,
        nextDueDate: form.nextDueDate || null,
      });
      showToast("Kayıt eklendi");
      onSaved();
    } catch (err) {
      setFieldErrors(err.response?.data?.fieldErrors || {});
      showToast(getErrorMessage(err, "Kayıt eklenemedi"), "danger");
    } finally {
      setSaving(false);
    }
  };

  return (
    <Modal show={show} onHide={onHide} centered>
      <Form onSubmit={handleSubmit}>
        <Modal.Header closeButton>
          <Modal.Title>Bakım / Kalibrasyon Kaydı</Modal.Title>
        </Modal.Header>
        <Modal.Body>
          <Row>
            <Col sm={6}>
              <Form.Group className="mb-3">
                <Form.Label>İşlem türü</Form.Label>
                <Form.Select name="type" value={form.type} onChange={handleChange}>
                  {Object.entries(MAINTENANCE_TYPES).map(([k, label]) => (
                    <option key={k} value={k}>
                      {label}
                    </option>
                  ))}
                </Form.Select>
              </Form.Group>
            </Col>
            <Col sm={6}>
              <Form.Group className="mb-3">
                <Form.Label>Yapıldığı tarih</Form.Label>
                <Form.Control
                  type="date"
                  name="performedDate"
                  max={todayIso()}
                  value={form.performedDate}
                  onChange={handleChange}
                  isInvalid={!!fieldErrors.performedDate}
                  required
                />
                <Form.Control.Feedback type="invalid">{fieldErrors.performedDate}</Form.Control.Feedback>
              </Form.Group>
            </Col>
          </Row>
          <Row>
            <Col sm={6}>
              <Form.Group className="mb-3">
                <Form.Label>Sonraki tarih</Form.Label>
                <Form.Control
                  type="date"
                  name="nextDueDate"
                  min={form.performedDate}
                  value={form.nextDueDate}
                  onChange={handleChange}
                />
                {form.type === "CALIBRATION" && (
                  <Form.Text>Ekipmanın kalibrasyon tarihi de bu tarihle güncellenir.</Form.Text>
                )}
              </Form.Group>
            </Col>
            <Col sm={6}>
              <Form.Group className="mb-3">
                <Form.Label>Yapan kişi / firma</Form.Label>
                <Form.Control name="performedBy" maxLength={100} value={form.performedBy} onChange={handleChange} />
              </Form.Group>
            </Col>
          </Row>
          <Form.Group className="mb-3">
            <Form.Label>Açıklama</Form.Label>
            <Form.Control
              as="textarea"
              rows={3}
              name="description"
              maxLength={500}
              value={form.description}
              onChange={handleChange}
            />
          </Form.Group>
          {equipment.status !== "AVAILABLE" && (
            <Form.Check
              type="checkbox"
              id="mark-available"
              name="markAvailable"
              label={`Ekipmanı tekrar "Kullanılabilir" yap (şu an: ${EQUIPMENT_STATUS[equipment.status].label})`}
              checked={form.markAvailable}
              onChange={handleChange}
            />
          )}
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

/** Ekipman durumunu not ile değiştirme. */
function StatusModal({ show, equipment, onHide, onSaved }) {
  const { showToast } = useToast();
  const [status, setStatus] = useState(equipment.status);
  const [note, setNote] = useState("");
  const [saving, setSaving] = useState(false);

  useEffect(() => {
    if (show) {
      setStatus(equipment.status);
      setNote("");
    }
  }, [show, equipment]);

  const handleSubmit = async (e) => {
    e.preventDefault();
    setSaving(true);
    try {
      const res = await client.patch(`/equipment/${equipment.id}/status`, { status, note });
      showToast("Durum güncellendi");
      onSaved(res.data);
    } catch (err) {
      showToast(getErrorMessage(err, "Durum güncellenemedi"), "danger");
    } finally {
      setSaving(false);
    }
  };

  return (
    <Modal show={show} onHide={onHide} centered>
      <Form onSubmit={handleSubmit}>
        <Modal.Header closeButton>
          <Modal.Title>Durumu Değiştir</Modal.Title>
        </Modal.Header>
        <Modal.Body>
          <Form.Group className="mb-3">
            <Form.Label>Yeni durum</Form.Label>
            <Form.Select value={status} onChange={(e) => setStatus(e.target.value)}>
              {Object.entries(EQUIPMENT_STATUS).map(([k, { label }]) => (
                <option key={k} value={k}>
                  {label}
                </option>
              ))}
            </Form.Select>
            {status !== "AVAILABLE" && (
              <Form.Text>
                Kullanılabilir olmayan ekipman için yeni rezervasyon yapılamaz ve teslim alınamaz. Mevcut rezervasyonlar
                iptal edilmez.
              </Form.Text>
            )}
          </Form.Group>
          <Form.Group>
            <Form.Label>Not (isteğe bağlı)</Form.Label>
            <Form.Control
              as="textarea"
              rows={2}
              maxLength={500}
              value={note}
              onChange={(e) => setNote(e.target.value)}
              placeholder="ör. Ekran arızası, servise gönderildi"
            />
          </Form.Group>
        </Modal.Body>
        <Modal.Footer>
          <Button variant="secondary" onClick={onHide}>
            Vazgeç
          </Button>
          <Button type="submit" disabled={saving || status === equipment.status}>
            {saving ? <Spinner size="sm" animation="border" /> : "Kaydet"}
          </Button>
        </Modal.Footer>
      </Form>
    </Modal>
  );
}

function EquipmentDetailPage() {
  const { id } = useParams();
  const { isAdmin } = useAuth();
  const { showToast } = useToast();
  const [equipment, setEquipment] = useState(null);
  const [history, setHistory] = useState([]);
  const [notFound, setNotFound] = useState(false);
  const [modal, setModal] = useState(null); // "edit" | "status" | "maintenance" | "reserve"
  const [filterOptions, setFilterOptions] = useState({ categories: [], locations: [] });

  const load = useCallback(() => {
    Promise.all([client.get(`/equipment/${id}`), client.get(`/equipment/${id}/maintenance`)])
      .then(([eqRes, histRes]) => {
        setEquipment(eqRes.data);
        setHistory(histRes.data);
      })
      .catch((err) => {
        if (err.response?.status === 404) setNotFound(true);
        else showToast(getErrorMessage(err, "Ekipman yüklenemedi"), "danger");
      });
  }, [id, showToast]);

  useEffect(load, [load]);

  useEffect(() => {
    if (isAdmin) client.get("/equipment/filters").then((res) => setFilterOptions(res.data)).catch(() => {});
  }, [isAdmin]);

  const reserveOptions = useMemo(() => (equipment ? [equipment] : []), [equipment]);
  const reserveInitial = useMemo(() => (equipment ? { equipmentId: String(equipment.id) } : undefined), [equipment]);

  if (notFound) {
    return (
      <div className="text-center py-5">
        <p className="text-muted">Ekipman bulunamadı veya silinmiş.</p>
        <Link to="/equipment">Ekipman listesine dön</Link>
      </div>
    );
  }
  if (!equipment) {
    return (
      <div className="text-center py-5">
        <Spinner animation="border" variant="primary" />
      </div>
    );
  }

  const closeAndReload = () => {
    setModal(null);
    load();
  };

  return (
    <>
      <Link to="/equipment" className="small d-inline-block mb-2">
        <FaArrowLeft className="me-1" /> Ekipmanlar
      </Link>
      <div className="page-header flex-wrap">
        <h1 className="h3 mb-0">
          {equipment.name} <EquipmentStatusBadge status={equipment.status} />
        </h1>
        <div className="d-flex flex-wrap gap-2">
          <Button
            variant="success"
            disabled={equipment.status !== "AVAILABLE"}
            onClick={() => setModal("reserve")}
          >
            <FaCalendarPlus className="me-1" /> Rezerve Et
          </Button>
          {isAdmin && (
            <>
              <Button variant="outline-secondary" onClick={() => setModal("status")}>
                Durumu Değiştir
              </Button>
              <Button variant="outline-primary" onClick={() => setModal("edit")}>
                <FaEdit className="me-1" /> Düzenle
              </Button>
            </>
          )}
        </div>
      </div>

      <Row className="g-3">
        <Col lg={5}>
          <Card className="dash-card">
            <Card.Body>
              <dl className="row mb-0">
                <dt className="col-5">Marka / Model</dt>
                <dd className="col-7">
                  {equipment.brand} {equipment.model}
                </dd>
                <dt className="col-5">Seri No</dt>
                <dd className="col-7">
                  <code>{equipment.serialNumber}</code>
                </dd>
                <dt className="col-5">Kategori</dt>
                <dd className="col-7">{equipment.category || "-"}</dd>
                <dt className="col-5">Konum</dt>
                <dd className="col-7">{equipment.location || "-"}</dd>
                <dt className="col-5">Kalibrasyon</dt>
                <dd className="col-7">
                  <CalibrationCell date={equipment.calibrationDate} />
                </dd>
                <dt className="col-5">Rezervasyon</dt>
                <dd className="col-7">
                  {equipment.requiresApproval ? (
                    <Badge bg="warning" text="dark">
                      Yönetici onayı gerekir
                    </Badge>
                  ) : (
                    "Doğrudan onaylanır"
                  )}
                </dd>
                <dt className="col-5">Açıklama</dt>
                <dd className="col-7">{equipment.description || "-"}</dd>
                <dt className="col-5">Ekleyen</dt>
                <dd className="col-7">
                  {equipment.createdBy}
                  <div className="small text-muted">{formatDateTime(equipment.createdAt)}</div>
                </dd>
              </dl>
            </Card.Body>
          </Card>
        </Col>

        <Col lg={7}>
          <Card className="dash-card">
            <Card.Body>
              <div className="d-flex justify-content-between align-items-center mb-2">
                <Card.Title className="mb-0">Bakım ve kalibrasyon geçmişi</Card.Title>
                {isAdmin && (
                  <Button size="sm" onClick={() => setModal("maintenance")}>
                    <FaPlus className="me-1" /> Kayıt Ekle
                  </Button>
                )}
              </div>
              {history.length === 0 ? (
                <p className="text-muted mb-0">Henüz kayıt yok</p>
              ) : (
                <Table size="sm" responsive className="mb-0">
                  <thead>
                    <tr>
                      <th>Tarih</th>
                      <th>Tür</th>
                      <th>Yapan</th>
                      <th>Sonraki</th>
                      <th>Açıklama</th>
                    </tr>
                  </thead>
                  <tbody>
                    {history.map((h) => (
                      <tr key={h.id}>
                        <td className="text-nowrap">{formatDate(h.performedDate)}</td>
                        <td>{MAINTENANCE_TYPES[h.type]}</td>
                        <td>{h.performedBy || "-"}</td>
                        <td className="text-nowrap">{formatDate(h.nextDueDate)}</td>
                        <td>
                          {h.description || "-"}
                          <div className="small text-muted">Kaydeden: {h.createdBy}</div>
                        </td>
                      </tr>
                    ))}
                  </tbody>
                </Table>
              )}
            </Card.Body>
          </Card>
        </Col>
      </Row>

      {isAdmin && (
        <>
          <EquipmentFormModal
            show={modal === "edit"}
            equipment={equipment}
            filterOptions={filterOptions}
            onHide={() => setModal(null)}
            onSaved={closeAndReload}
          />
          <StatusModal show={modal === "status"} equipment={equipment} onHide={() => setModal(null)} onSaved={closeAndReload} />
          <MaintenanceModal
            show={modal === "maintenance"}
            equipment={equipment}
            onHide={() => setModal(null)}
            onSaved={closeAndReload}
          />
        </>
      )}
      <ReservationFormModal
        show={modal === "reserve"}
        equipmentOptions={reserveOptions}
        initialValues={reserveInitial}
        onHide={() => setModal(null)}
        onSaved={() => setModal(null)}
      />
    </>
  );
}

export default EquipmentDetailPage;
