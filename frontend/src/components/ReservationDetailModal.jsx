import { useEffect, useState } from "react";
import { Alert, Button, Form, Modal, Spinner } from "react-bootstrap";
import client, { getErrorMessage } from "../api/client";
import { useAuth } from "../context/AuthContext";
import { useToast } from "../context/ToastContext";
import { ReservationStatusBadge } from "./StatusBadges";
import { formatDateTime } from "../utils/format";

// Ekipman başlangıçtan en erken bu kadar dakika önce teslim alınabilir (backend ile aynı)
const CHECK_OUT_EARLY_MINUTES = 30;

/**
 * Rezervasyon detayı ve duruma göre yapılabilecek işlemler:
 * onay / red (yönetici), teslim alma, iade, iptal.
 * İşlem başarılı olunca onChanged(güncel rezervasyon) çağrılır.
 */
function ReservationDetailModal({ reservation, onHide, onChanged }) {
  const { user, isAdmin } = useAuth();
  const { showToast } = useToast();
  // mode: null | "reject" | "cancel" | "return" — not yazılması gereken işlemler için
  const [mode, setMode] = useState(null);
  const [note, setNote] = useState("");
  const [reportFault, setReportFault] = useState(false);
  const [busy, setBusy] = useState(false);

  useEffect(() => {
    setMode(null);
    setNote("");
    setReportFault(false);
  }, [reservation]);

  if (!reservation) return null;
  const r = reservation;

  const isOwner = r.userId === user.id;
  const canManage = isOwner || isAdmin;
  const now = new Date();
  const start = new Date(r.startTime);
  const end = new Date(r.endTime);
  const checkOutOpen = now >= new Date(start.getTime() - CHECK_OUT_EARLY_MINUTES * 60000) && now < end;

  const run = async (action, body, successMessage) => {
    setBusy(true);
    try {
      const res = await client.post(`/reservations/${r.id}/${action}`, body);
      showToast(successMessage);
      onChanged(res.data);
    } catch (err) {
      showToast(getErrorMessage(err, "İşlem yapılamadı"), "danger");
    } finally {
      setBusy(false);
    }
  };

  const submitNoteForm = (e) => {
    e.preventDefault();
    if (mode === "reject") run("reject", { note }, "Rezervasyon reddedildi");
    if (mode === "cancel") run("cancel", { note }, "Rezervasyon iptal edildi");
    if (mode === "return") run("return", { note, reportFault }, "Ekipman iade edildi");
  };

  const modeTitles = {
    reject: "Red gerekçesi (kullanıcıya bildirimle iletilir)",
    cancel: isOwner ? "İptal nedeni (isteğe bağlı)" : "İptal nedeni (kullanıcıya bildirimle iletilir)",
    return: "İade notu (isteğe bağlı)",
  };

  return (
    <Modal show onHide={onHide} centered>
      <Modal.Header closeButton>
        <Modal.Title>Rezervasyon Detayı</Modal.Title>
      </Modal.Header>
      <Modal.Body>
        <dl className="row mb-0">
          <dt className="col-4">Durum</dt>
          <dd className="col-8">
            <ReservationStatusBadge reservation={r} />
          </dd>
          <dt className="col-4">Ekipman</dt>
          <dd className="col-8">
            {r.equipmentName} <code className="ms-1">{r.serialNumber}</code>
          </dd>
          <dt className="col-4">Rezerve Eden</dt>
          <dd className="col-8">{r.userFullName}</dd>
          <dt className="col-4">Başlangıç</dt>
          <dd className="col-8">{formatDateTime(r.startTime)}</dd>
          <dt className="col-4">Bitiş</dt>
          <dd className="col-8">{formatDateTime(r.endTime)}</dd>
          {r.checkedOutAt && (
            <>
              <dt className="col-4">Teslim Alındı</dt>
              <dd className="col-8">{formatDateTime(r.checkedOutAt)}</dd>
            </>
          )}
          {r.returnedAt && (
            <>
              <dt className="col-4">İade Edildi</dt>
              <dd className="col-8">{formatDateTime(r.returnedAt)}</dd>
            </>
          )}
          {r.note && (
            <>
              <dt className="col-4">Not</dt>
              <dd className="col-8">{r.note}</dd>
            </>
          )}
        </dl>

        {r.status === "APPROVED" && canManage && !checkOutOpen && now < end && (
          <Alert variant="light" className="small mt-3 mb-0">
            Ekipmanı başlangıçtan en erken {CHECK_OUT_EARLY_MINUTES} dakika önce teslim alabilirsiniz.
          </Alert>
        )}

        {mode && (
          <Form onSubmit={submitNoteForm} className="mt-3 border-top pt-3">
            <Form.Group className="mb-2">
              <Form.Label className="small">{modeTitles[mode]}</Form.Label>
              <Form.Control
                as="textarea"
                rows={2}
                maxLength={500}
                value={note}
                onChange={(e) => setNote(e.target.value)}
                autoFocus
              />
            </Form.Group>
            {mode === "return" && (
              <Form.Check
                id="report-fault"
                type="checkbox"
                className="mb-2"
                label="Ekipmanda arıza var (ekipman 'Arızalı' durumuna alınır)"
                checked={reportFault}
                onChange={(e) => setReportFault(e.target.checked)}
              />
            )}
            <div className="d-flex gap-2 justify-content-end">
              <Button variant="secondary" size="sm" onClick={() => setMode(null)} disabled={busy}>
                Vazgeç
              </Button>
              <Button type="submit" size="sm" variant={mode === "return" ? "success" : "danger"} disabled={busy}>
                {busy ? <Spinner size="sm" animation="border" /> : "Onayla"}
              </Button>
            </div>
          </Form>
        )}
      </Modal.Body>

      {!mode && (
        <Modal.Footer className="flex-wrap">
          {isAdmin && r.status === "PENDING" && (
            <>
              <Button variant="success" disabled={busy} onClick={() => run("approve", undefined, "Rezervasyon onaylandı")}>
                Onayla
              </Button>
              <Button variant="outline-danger" disabled={busy} onClick={() => setMode("reject")}>
                Reddet
              </Button>
            </>
          )}
          {canManage && r.status === "APPROVED" && checkOutOpen && (
            <Button variant="primary" disabled={busy} onClick={() => run("check-out", undefined, "Ekipman teslim alındı")}>
              Teslim Al
            </Button>
          )}
          {canManage && r.status === "CHECKED_OUT" && (
            <Button variant="success" disabled={busy} onClick={() => setMode("return")}>
              İade Et
            </Button>
          )}
          {canManage && (r.status === "PENDING" || r.status === "APPROVED") && (
            <Button variant="outline-danger" disabled={busy} onClick={() => setMode("cancel")}>
              İptal Et
            </Button>
          )}
          <Button variant="secondary" onClick={onHide}>
            Kapat
          </Button>
        </Modal.Footer>
      )}
    </Modal>
  );
}

export default ReservationDetailModal;
