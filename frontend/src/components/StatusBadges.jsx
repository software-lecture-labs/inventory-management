import { Badge } from "react-bootstrap";
import { EQUIPMENT_STATUS, RESERVATION_STATUS, formatDate } from "../utils/format";

const textFor = (variant) => (variant === "warning" || variant === "info" || variant === "light" ? "dark" : undefined);

export function EquipmentStatusBadge({ status }) {
  const s = EQUIPMENT_STATUS[status] ?? { label: status, variant: "secondary" };
  return (
    <Badge bg={s.variant} text={textFor(s.variant)}>
      {s.label}
    </Badge>
  );
}

/** Rezervasyon durumu; gecikmiş / geç iade bilgisi de rozete eklenir. */
export function ReservationStatusBadge({ reservation }) {
  const s = RESERVATION_STATUS[reservation.status] ?? { label: reservation.status, variant: "secondary" };
  return (
    <>
      <Badge bg={s.variant} text={textFor(s.variant)}>
        {s.label}
      </Badge>
      {reservation.overdue && (
        <Badge bg="danger" className="ms-1">
          Gecikmiş
        </Badge>
      )}
      {reservation.returnedLate && (
        <Badge bg="warning" text="dark" className="ms-1">
          Geç iade
        </Badge>
      )}
    </>
  );
}

/** Kalibrasyon tarihi; geçmişse veya 30 gün içindeyse uyarı rozeti gösterir. */
export function CalibrationCell({ date }) {
  if (!date) return <span className="text-muted">-</span>;
  const due = new Date(`${date}T23:59:59`);
  const now = new Date();
  const overdue = due < now;
  const soon = !overdue && due - now < 30 * 24 * 60 * 60 * 1000;
  return (
    <span className="text-nowrap">
      {formatDate(date)}{" "}
      {overdue && (
        <Badge bg="danger" title="Kalibrasyon tarihi geçmiş">
          Süresi geçti
        </Badge>
      )}
      {soon && (
        <Badge bg="warning" text="dark" title="30 gün içinde kalibrasyon gerekiyor">
          Yaklaşıyor
        </Badge>
      )}
    </span>
  );
}
