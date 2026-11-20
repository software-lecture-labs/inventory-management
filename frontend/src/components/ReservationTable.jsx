import { Table } from "react-bootstrap";
import { ReservationStatusBadge } from "./StatusBadges";
import { formatDateTime } from "../utils/format";

/**
 * Rezervasyon listesi tablosu. Satıra tıklanınca onSelect(rezervasyon) çağrılır.
 * showUser=false ise "Rezerve Eden" sütunu gizlenir (kullanıcının kendi listesi).
 */
function ReservationTable({ items, loading, onSelect, showUser = true, emptyText = "Kayıt bulunamadı" }) {
  const colSpan = showUser ? 6 : 5;
  return (
    <Table striped bordered hover responsive className={`bg-white ${loading ? "table-loading" : ""}`}>
      <thead>
        <tr>
          {showUser && <th>Rezerve Eden</th>}
          <th>Ekipman</th>
          <th>Başlangıç</th>
          <th>Bitiş</th>
          <th>Durum</th>
          <th>Not</th>
        </tr>
      </thead>
      <tbody>
        {!loading && items.length === 0 && (
          <tr>
            <td colSpan={colSpan} className="text-center text-muted py-4">
              {emptyText}
            </td>
          </tr>
        )}
        {items.map((r) => (
          <tr key={r.id} className={onSelect ? "clickable-row" : undefined} onClick={() => onSelect?.(r)}>
            {showUser && <td>{r.userFullName}</td>}
            <td>
              {r.equipmentName} <code className="small">{r.serialNumber}</code>
            </td>
            <td className="text-nowrap">{formatDateTime(r.startTime)}</td>
            <td className="text-nowrap">{formatDateTime(r.endTime)}</td>
            <td className="text-nowrap">
              <ReservationStatusBadge reservation={r} />
            </td>
            <td className="small">{r.note || <span className="text-muted">-</span>}</td>
          </tr>
        ))}
      </tbody>
    </Table>
  );
}

export default ReservationTable;
