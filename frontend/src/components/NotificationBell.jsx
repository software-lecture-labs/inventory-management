import { useCallback, useEffect, useRef, useState } from "react";
import { useLocation, useNavigate } from "react-router-dom";
import { Badge, Button, Modal, Spinner } from "react-bootstrap";
import { FaBell } from "react-icons/fa";
import client, { getErrorMessage } from "../api/client";
import { useToast } from "../context/ToastContext";
import ReservationFormModal from "./ReservationFormModal";
import { formatDateTime } from "../utils/format";

// Okunmamış bildirim sayısı bu aralıkla yenilenir
const POLL_MS = 30000;

const relative = new Intl.RelativeTimeFormat("tr", { numeric: "auto" });

function timeAgo(value) {
  const seconds = Math.round((new Date(value) - new Date()) / 1000);
  const abs = Math.abs(seconds);
  if (abs < 60) return "az önce";
  if (abs < 3600) return relative.format(Math.round(seconds / 60), "minute");
  if (abs < 86400) return relative.format(Math.round(seconds / 3600), "hour");
  if (abs < 7 * 86400) return relative.format(Math.round(seconds / 86400), "day");
  return formatDateTime(value);
}

/** Backend'in "2026-11-17T22:00:00" değerini datetime-local alanının beklediği "2026-11-17T22:00" biçimine çevirir. */
const toInputValue = (iso) => (iso ? iso.slice(0, 16) : "");

/**
 * Üst bardaki bildirim zili: okunmamış sayısı, son bildirimler listesi ve bildirim detayı.
 * "Yer açıldı" bildiriminden doğrudan, ekipmanı ve saati dolu bir rezervasyon formu açılır.
 */
function NotificationBell() {
  const { showToast } = useToast();
  const navigate = useNavigate();
  const location = useLocation();
  const wrapperRef = useRef(null);
  const lastCount = useRef(null);

  const [count, setCount] = useState(0);
  const [open, setOpen] = useState(false);
  const [items, setItems] = useState([]);
  const [loading, setLoading] = useState(false);
  const [selected, setSelected] = useState(null);
  const [reserve, setReserve] = useState(null); // { options, initialValues }

  const refreshCount = useCallback(() => {
    client
      .get("/notifications/unread-count")
      .then((res) => {
        const next = res.data.count;
        // İlk yüklemede değil, sonradan yeni bildirim geldiğinde haber ver
        if (lastCount.current !== null && next > lastCount.current) {
          showToast("Yeni bildiriminiz var", "info");
        }
        lastCount.current = next;
        setCount(next);
      })
      .catch(() => {});
  }, [showToast]);

  // Açılışta, sayfa değiştikçe ve düzenli aralıklarla sayıyı yenile
  useEffect(refreshCount, [refreshCount, location.pathname]);
  useEffect(() => {
    const id = setInterval(refreshCount, POLL_MS);
    return () => clearInterval(id);
  }, [refreshCount]);

  const loadList = useCallback(() => {
    setLoading(true);
    client
      .get("/notifications", { params: { size: 20 } })
      .then((res) => setItems(res.data.content))
      .catch((err) => showToast(getErrorMessage(err, "Bildirimler yüklenemedi"), "danger"))
      .finally(() => setLoading(false));
  }, [showToast]);

  const toggle = () => {
    if (!open) loadList();
    setOpen((v) => !v);
  };

  // Dışarı tıklanınca veya Esc ile kapat
  useEffect(() => {
    if (!open) return undefined;
    const onClick = (e) => !wrapperRef.current?.contains(e.target) && setOpen(false);
    const onKey = (e) => e.key === "Escape" && setOpen(false);
    document.addEventListener("mousedown", onClick);
    document.addEventListener("keydown", onKey);
    return () => {
      document.removeEventListener("mousedown", onClick);
      document.removeEventListener("keydown", onKey);
    };
  }, [open]);

  const openNotification = async (n) => {
    setOpen(false);
    setSelected(n);
    if (!n.read) {
      try {
        const res = await client.post(`/notifications/${n.id}/read`);
        setItems((prev) => prev.map((x) => (x.id === n.id ? res.data : x)));
        setSelected(res.data);
        setCount((c) => Math.max(c - 1, 0));
        lastCount.current = Math.max((lastCount.current ?? 1) - 1, 0);
      } catch {
        /* okundu işaretlenemese de detay gösterilir */
      }
    }
  };

  const markAllRead = async () => {
    try {
      await client.post("/notifications/read-all");
      setItems((prev) => prev.map((x) => ({ ...x, read: true })));
      setCount(0);
      lastCount.current = 0;
    } catch (err) {
      showToast(getErrorMessage(err, "İşlem yapılamadı"), "danger");
    }
  };

  // "Rezerve Et": ekipman listesini al, formu bildirimdeki ekipman ve saatle doldur
  const startReservation = async (n) => {
    try {
      const res = await client.get("/equipment/options");
      setSelected(null);
      setReserve({
        options: res.data,
        initialValues: {
          equipmentId: String(n.equipmentId),
          startTime: toInputValue(n.startTime),
          endTime: toInputValue(n.endTime),
        },
      });
    } catch (err) {
      showToast(getErrorMessage(err, "Ekipmanlar yüklenemedi"), "danger");
    }
  };

  const goTo = (path) => {
    setSelected(null);
    navigate(path);
  };

  const slotStillAhead = selected?.startTime && new Date(selected.startTime) > new Date();

  return (
    <div className="notif" ref={wrapperRef}>
      <button
        type="button"
        className="notif-button"
        onClick={toggle}
        aria-label={count > 0 ? `Bildirimler, ${count} okunmamış` : "Bildirimler"}
        aria-expanded={open}
      >
        <FaBell size={18} />
        {count > 0 && (
          <Badge bg="danger" pill className="notif-count">
            {count > 99 ? "99+" : count}
          </Badge>
        )}
      </button>

      {open && (
        <div className="notif-panel" role="dialog" aria-label="Bildirimler">
          <div className="notif-head">
            <strong>Bildirimler</strong>
            {items.some((n) => !n.read) && (
              <Button variant="link" size="sm" className="p-0" onClick={markAllRead}>
                Tümünü okundu işaretle
              </Button>
            )}
          </div>
          <div className="notif-list">
            {loading && items.length === 0 && (
              <div className="text-center py-3">
                <Spinner size="sm" animation="border" />
              </div>
            )}
            {!loading && items.length === 0 && <div className="notif-empty">Henüz bildiriminiz yok</div>}
            {items.map((n) => (
              <button
                type="button"
                key={n.id}
                className={`notif-item ${n.read ? "" : "unread"}`}
                onClick={() => openNotification(n)}
              >
                <span className="notif-dot" aria-hidden="true" />
                <span className="notif-body">
                  <span className="notif-title">{n.title}</span>
                  <span className="notif-message">{n.message}</span>
                  <span className="notif-time">{timeAgo(n.createdAt)}</span>
                </span>
              </button>
            ))}
          </div>
        </div>
      )}

      {/* Bildirim detayı */}
      <Modal show={selected !== null} onHide={() => setSelected(null)} centered>
        {selected && (
          <>
            <Modal.Header closeButton>
              <Modal.Title className="h5">{selected.title}</Modal.Title>
            </Modal.Header>
            <Modal.Body>
              <p>{selected.message}</p>
              {selected.equipmentName && (
                <dl className="row mb-0 small">
                  <dt className="col-4">Ekipman</dt>
                  <dd className="col-8">{selected.equipmentName}</dd>
                  {selected.startTime && (
                    <>
                      <dt className="col-4">Zaman</dt>
                      <dd className="col-8">
                        {formatDateTime(selected.startTime)} – {formatDateTime(selected.endTime)}
                      </dd>
                    </>
                  )}
                </dl>
              )}
              {selected.type === "WAITLIST_SLOT_AVAILABLE" && !slotStillAhead && (
                <p className="text-muted small mt-3 mb-0">Bu aralığın zamanı geçtiği için artık rezerve edilemez.</p>
              )}
              <div className="text-muted small mt-3">{formatDateTime(selected.createdAt)}</div>
            </Modal.Body>
            <Modal.Footer>
              {selected.type === "WAITLIST_SLOT_AVAILABLE" && slotStillAhead && (
                <Button variant="success" onClick={() => startReservation(selected)}>
                  Rezerve Et
                </Button>
              )}
              {selected.type === "APPROVAL_REQUEST" && (
                <Button onClick={() => goTo("/approvals")}>Onay sayfasına git</Button>
              )}
              {["RESERVATION_APPROVED", "RESERVATION_REJECTED", "RESERVATION_CANCELLED", "RESERVATION_REMINDER", "RESERVATION_OVERDUE"].includes(
                selected.type
              ) && <Button onClick={() => goTo("/my-reservations")}>Rezervasyonlarıma git</Button>}
              {selected.type === "CALIBRATION_DUE" && <Button onClick={() => goTo("/equipment")}>Ekipmanlara git</Button>}
              {selected.type === "PASSWORD_RESET_REQUEST" && (
                <Button onClick={() => goTo("/users?tab=password")}>Şifre taleplerine git</Button>
              )}
              <Button variant="secondary" onClick={() => setSelected(null)}>
                Kapat
              </Button>
            </Modal.Footer>
          </>
        )}
      </Modal>

      <ReservationFormModal
        show={reserve !== null}
        equipmentOptions={reserve?.options ?? []}
        initialValues={reserve?.initialValues}
        onHide={() => setReserve(null)}
        onSaved={() => {
          setReserve(null);
          navigate("/my-reservations");
        }}
      />
    </div>
  );
}

export default NotificationBell;
