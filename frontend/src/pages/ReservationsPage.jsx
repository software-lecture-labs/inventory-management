import { useCallback, useEffect, useMemo, useRef, useState } from "react";
import { Button, Form } from "react-bootstrap";
import { FaPlus } from "react-icons/fa";
import FullCalendar from "@fullcalendar/react";
import dayGridPlugin from "@fullcalendar/daygrid";
import timeGridPlugin from "@fullcalendar/timegrid";
import interactionPlugin from "@fullcalendar/interaction";
import trLocale from "@fullcalendar/core/locales/tr";
import client, { getErrorMessage } from "../api/client";
import { useAuth } from "../context/AuthContext";
import { useToast } from "../context/ToastContext";
import ReservationFormModal from "../components/ReservationFormModal";
import ReservationDetailModal from "../components/ReservationDetailModal";
import { RESERVATION_STATUS, toDateTimeLocal, toLocalIso } from "../utils/format";
import "../styles/calendar.css";

// Her ekipman takvimde hep aynı renkte görünsün
const COLORS = ["#2563eb", "#16a34a", "#d97706", "#dc2626", "#7c3aed", "#0891b2", "#db2777", "#4b5563"];
const colorFor = (equipmentId) => COLORS[equipmentId % COLORS.length];

// Takvimden kaldırılması gereken durumlar
const HIDDEN_STATUSES = ["CANCELLED", "REJECTED"];

function ReservationsPage() {
  const { user } = useAuth();
  const { showToast } = useToast();
  const calendarRef = useRef(null);

  const [reservations, setReservations] = useState([]);
  const [equipment, setEquipment] = useState([]);
  const [range, setRange] = useState(null);
  const [viewType, setViewType] = useState("dayGridMonth");
  const [equipmentFilter, setEquipmentFilter] = useState("");
  const [onlyMine, setOnlyMine] = useState(false);

  // newReservation = { initialValues } | null, selected = rezervasyon | null
  const [newReservation, setNewReservation] = useState(null);
  const [selected, setSelected] = useState(null);

  useEffect(() => {
    client
      .get("/equipment/options")
      .then((res) => setEquipment(res.data))
      .catch((err) => showToast(getErrorMessage(err, "Ekipmanlar yüklenemedi"), "danger"));
  }, [showToast]);

  // Takvimde görünen aralık değiştikçe (ay / hafta geçişi) sadece o aralığı yükle
  const loadRange = useCallback(() => {
    if (!range) return;
    client
      .get("/reservations", { params: { from: toLocalIso(range.start), to: toLocalIso(range.end) } })
      .then((res) => setReservations(res.data))
      .catch((err) => showToast(getErrorMessage(err, "Rezervasyonlar yüklenemedi"), "danger"));
  }, [range, showToast]);

  useEffect(loadRange, [loadRange]);

  const events = useMemo(
    () =>
      reservations
        .filter((r) => !equipmentFilter || r.equipmentId === Number(equipmentFilter))
        .filter((r) => !onlyMine || r.userId === user.id)
        .map((r) => ({
          id: String(r.id),
          title: `${r.equipmentName} · ${r.userFullName}${r.status === "PENDING" ? " (onay bekliyor)" : ""}`,
          start: r.startTime,
          end: r.endTime,
          backgroundColor: colorFor(r.equipmentId),
          borderColor: r.overdue ? "#dc2626" : colorFor(r.equipmentId),
          classNames: [`event-${r.status.toLowerCase()}`, r.overdue ? "event-overdue" : ""],
          extendedProps: r,
        })),
    [reservations, equipmentFilter, onlyMine, user.id]
  );

  // Ay görünümünde güne tıklayınca o günün saatlik görünümüne geç
  const handleDateClick = (info) => {
    const api = calendarRef.current.getApi();
    if (api.view.type === "dayGridMonth") {
      api.changeView("timeGridDay", info.date);
    }
  };

  // Saatlik görünümde bir aralığı sürükleyerek seçince rezervasyon formunu doldur
  const handleSelect = (info) => {
    if (info.view.type === "dayGridMonth") return;
    calendarRef.current.getApi().unselect();
    if (info.start < new Date()) {
      showToast("Geçmiş bir zamana rezervasyon yapılamaz", "warning");
      return;
    }
    setNewReservation({
      initialValues: {
        equipmentId: equipmentFilter,
        startTime: toDateTimeLocal(info.start),
        endTime: toDateTimeLocal(info.end),
      },
    });
  };

  const handleChanged = (updated) => {
    setReservations((prev) =>
      HIDDEN_STATUSES.includes(updated.status)
        ? prev.filter((r) => r.id !== updated.id)
        : prev.map((r) => (r.id === updated.id ? updated : r))
    );
    setSelected(null);
  };

  return (
    <>
      <div className="page-header">
        <h1 className="h3 mb-0">Rezervasyon Takvimi</h1>
        <Button variant="success" onClick={() => setNewReservation({ initialValues: undefined })}>
          <FaPlus className="me-1" /> Yeni Rezervasyon
        </Button>
      </div>

      <div className="d-flex flex-wrap align-items-center gap-3 mb-3">
        <Form.Select
          className="w-auto"
          value={equipmentFilter}
          onChange={(e) => setEquipmentFilter(e.target.value)}
        >
          <option value="">Tüm ekipmanlar</option>
          {equipment.map((eq) => (
            <option key={eq.id} value={eq.id}>
              {eq.name} — {eq.serialNumber}
            </option>
          ))}
        </Form.Select>
        <Form.Check
          type="switch"
          id="only-mine"
          label="Sadece benim rezervasyonlarım"
          checked={onlyMine}
          onChange={(e) => setOnlyMine(e.target.checked)}
        />
        <div className="calendar-legend small text-muted ms-auto">
          <span className="legend-item legend-pending">{RESERVATION_STATUS.PENDING.label}</span>
          <span className="legend-item legend-returned">{RESERVATION_STATUS.RETURNED.label}</span>
          <span className="legend-item legend-overdue">Gecikmiş iade</span>
        </div>
      </div>

      <div className="calendar-card">
        <FullCalendar
          ref={calendarRef}
          plugins={[dayGridPlugin, timeGridPlugin, interactionPlugin]}
          locale={trLocale}
          initialView="dayGridMonth"
          firstDay={1}
          headerToolbar={{
            left: "prev,next today",
            center: "title",
            right: "dayGridMonth,timeGridWeek,timeGridDay",
          }}
          events={events}
          datesSet={(info) => {
            setRange({ start: info.start, end: info.end });
            setViewType(info.view.type);
          }}
          dateClick={handleDateClick}
          selectable
          selectMirror
          selectAllow={(info) => info.start.toDateString() === new Date(info.end - 1).toDateString()}
          select={handleSelect}
          eventClick={(info) => setSelected(info.event.extendedProps)}
          slotMinTime="00:00:00"
          slotMaxTime="24:00:00"
          scrollTime="07:00:00"
          nowIndicator
          allDaySlot={false}
          dayMaxEvents={3}
          // Ay görünümü içeriğe göre uzar; saatlik görünümlerde 24 saat kaydırılabilir alanda gösterilir
          height={viewType === "dayGridMonth" ? "auto" : 720}
        />
      </div>
      <p className="text-muted small mt-2">
        İpucu: Hafta veya gün görünümünde boş bir saat aralığını sürükleyerek hızlıca rezervasyon oluşturabilirsiniz.
        Bir rezervasyona tıklayarak teslim alma, iade ve iptal işlemlerini yapabilirsiniz.
      </p>

      <ReservationFormModal
        show={newReservation !== null}
        equipmentOptions={equipment}
        initialValues={newReservation?.initialValues}
        onHide={() => setNewReservation(null)}
        onSaved={(saved) => {
          setReservations((prev) => [...prev, saved]);
          setNewReservation(null);
        }}
      />

      {selected && (
        <ReservationDetailModal reservation={selected} onHide={() => setSelected(null)} onChanged={handleChanged} />
      )}
    </>
  );
}

export default ReservationsPage;
