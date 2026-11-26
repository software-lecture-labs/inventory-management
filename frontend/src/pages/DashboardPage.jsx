import { useCallback, useEffect, useState } from "react";
import { Link } from "react-router-dom";
import { Button, Card, Col, ListGroup, Row, Spinner, Table } from "react-bootstrap";
import { Bar, BarChart, CartesianGrid, ResponsiveContainer, Tooltip, XAxis, YAxis } from "recharts";
import client, { getErrorMessage } from "../api/client";
import { useAuth } from "../context/AuthContext";
import { useToast } from "../context/ToastContext";
import ReservationDetailModal from "../components/ReservationDetailModal";
import { ReservationStatusBadge } from "../components/StatusBadges";
import { EQUIPMENT_STATUS, formatDate, formatDateTime } from "../utils/format";

// Durum renkleri (iyi / uyarı / ciddi / kritik). Renk tek başına anlam taşımaz: her parça etiketlidir.
const STATUS_COLORS = {
  AVAILABLE: { color: "#0ca30c", text: "#fff" },
  MAINTENANCE: { color: "#fab219", text: "#1f1f1f" },
  CALIBRATION: { color: "#ec835a", text: "#1f1f1f" },
  OUT_OF_ORDER: { color: "#d03b3b", text: "#fff" },
};
const STATUS_ORDER = ["AVAILABLE", "MAINTENANCE", "CALIBRATION", "OUT_OF_ORDER"];
const SERIES_COLOR = "#2a78d6";

const shortDate = new Intl.DateTimeFormat("tr-TR", { day: "2-digit", month: "2-digit" });
const longDate = new Intl.DateTimeFormat("tr-TR", { weekday: "long", day: "numeric", month: "long" });

function Kpi({ label, value, to, tone }) {
  const body = (
    <Card.Body>
      <div className={`kpi-value ${tone === "alert" && value > 0 ? "text-danger" : ""}`}>{value}</div>
      <div className="kpi-label">{label}</div>
    </Card.Body>
  );
  return <Card className="dash-card">{to ? <Link to={to} className="kpi-link">{body}</Link> : body}</Card>;
}

/** Ekipmanların duruma göre dağılımı: tek bir bütünün parçaları. */
function StatusBreakdown({ counts, total }) {
  if (total === 0) return <p className="text-muted mb-0">Henüz ekipman yok</p>;
  const parts = STATUS_ORDER.map((s) => ({ status: s, count: counts[s] ?? 0 })).filter((p) => p.count > 0);
  return (
    <>
      <div
        className="segment-bar"
        role="img"
        aria-label={parts.map((p) => `${EQUIPMENT_STATUS[p.status].label}: ${p.count}`).join(", ")}
      >
        {parts.map((p) => {
          const share = p.count / total;
          return (
            <div
              key={p.status}
              className="segment"
              style={{ flexGrow: p.count, background: STATUS_COLORS[p.status].color, color: STATUS_COLORS[p.status].text }}
              title={`${EQUIPMENT_STATUS[p.status].label}: ${p.count} (%${Math.round(share * 100)})`}
            >
              {share >= 0.08 ? `%${Math.round(share * 100)}` : ""}
            </div>
          );
        })}
      </div>
      <div className="segment-legend">
        {STATUS_ORDER.map((s) => (
          <span key={s}>
            <span className="swatch" style={{ background: STATUS_COLORS[s].color }} />
            {EQUIPMENT_STATUS[s].label} <strong>{counts[s] ?? 0}</strong>
          </span>
        ))}
      </div>
    </>
  );
}

function DailyTooltip({ active, payload }) {
  if (!active || !payload?.length) return null;
  const { date, count } = payload[0].payload;
  return (
    <div className="chart-tooltip">
      <div className="text-muted">{longDate.format(new Date(`${date}T00:00:00`))}</div>
      <strong>{count}</strong> rezervasyon
    </div>
  );
}

/** Son 14 günde başlayan rezervasyon sayısı (tek seri, sütun grafik). */
function DailyChart({ data }) {
  const [asTable, setAsTable] = useState(false);
  const rows = data.map((d) => ({ ...d, label: shortDate.format(new Date(`${d.date}T00:00:00`)) }));
  return (
    <>
      <div className="d-flex justify-content-between align-items-center mb-2">
        <Card.Title className="mb-0">Son 14 günün rezervasyonları</Card.Title>
        <Button variant="link" size="sm" className="p-0" onClick={() => setAsTable((v) => !v)}>
          {asTable ? "Grafik olarak göster" : "Tablo olarak göster"}
        </Button>
      </div>
      {asTable ? (
        <Table size="sm" className="mb-0">
          <thead>
            <tr>
              <th>Gün</th>
              <th className="text-end">Rezervasyon</th>
            </tr>
          </thead>
          <tbody>
            {rows.map((r) => (
              <tr key={r.date}>
                <td>{formatDate(r.date)}</td>
                <td className="text-end">{r.count}</td>
              </tr>
            ))}
          </tbody>
        </Table>
      ) : (
        <ResponsiveContainer width="100%" height={220}>
          <BarChart data={rows} margin={{ top: 8, right: 8, bottom: 0, left: -16 }}>
            <CartesianGrid vertical={false} stroke="#eef0f3" />
            <XAxis dataKey="label" tickLine={false} axisLine={{ stroke: "#cbd5e1" }} tick={{ fontSize: 11, fill: "#64748b" }} />
            <YAxis allowDecimals={false} tickLine={false} axisLine={false} tick={{ fontSize: 11, fill: "#64748b" }} />
            <Tooltip content={<DailyTooltip />} cursor={{ fill: "rgba(42,120,214,0.08)" }} />
            <Bar dataKey="count" fill={SERIES_COLOR} radius={[4, 4, 0, 0]} maxBarSize={24} />
          </BarChart>
        </ResponsiveContainer>
      )}
    </>
  );
}

function TopEquipment({ items }) {
  if (items.length === 0) return <p className="text-muted mb-0">Son 30 günde rezervasyon yok</p>;
  const max = Math.max(...items.map((i) => i.count));
  return items.map((item) => (
    <div key={item.id} className="hbar-row" title={`${item.name}: ${item.count} rezervasyon`}>
      <Link to={`/equipment/${item.id}`} className="hbar-name">
        {item.name}
      </Link>
      <div className="hbar-track">
        <div className="hbar-fill" style={{ width: `${(item.count / max) * 100}%` }} />
      </div>
      <span className="hbar-value">{item.count}</span>
    </div>
  ));
}

function DashboardPage() {
  const { user, isAdmin } = useAuth();
  const { showToast } = useToast();
  const [data, setData] = useState(null);
  const [selected, setSelected] = useState(null);

  const load = useCallback(() => {
    client
      .get("/dashboard")
      .then((res) => setData(res.data))
      .catch((err) => showToast(getErrorMessage(err, "Özet yüklenemedi"), "danger"));
  }, [showToast]);

  useEffect(load, [load]);

  if (!data) {
    return (
      <div className="text-center py-5">
        <Spinner animation="border" variant="primary" />
      </div>
    );
  }

  const available = data.equipmentByStatus.AVAILABLE ?? 0;

  return (
    <>
      <div className="page-header">
        <h1 className="h3 mb-0">Merhaba, {user.firstName}</h1>
      </div>

      <Row className="g-3 mb-3">
        <Col xs={6} lg>
          <Kpi label="Toplam ekipman" value={data.totalEquipment} to="/equipment" />
        </Col>
        <Col xs={6} lg>
          <Kpi label="Kullanılabilir ekipman" value={available} to="/equipment?status=AVAILABLE" />
        </Col>
        <Col xs={6} lg>
          <Kpi label="Şu an süren rezervasyon" value={data.activeNow} to="/reservations" />
        </Col>
        <Col xs={6} lg>
          <Kpi label="Bugünkü rezervasyon" value={data.todayReservations} to="/reservations" />
        </Col>
        {isAdmin && (
          <>
            <Col xs={6} lg>
              <Kpi label="Onay bekleyen" value={data.pendingApprovals} to="/approvals" tone="alert" />
            </Col>
            <Col xs={6} lg>
              <Kpi label="Gecikmiş iade" value={data.overdueReturns} to="/approvals?tab=overdue" tone="alert" />
            </Col>
          </>
        )}
      </Row>

      <Row className="g-3 mb-3">
        <Col lg={7}>
          <Card className="dash-card">
            <Card.Body>
              <DailyChart data={data.dailyReservations} />
            </Card.Body>
          </Card>
        </Col>
        <Col lg={5}>
          <Card className="dash-card">
            <Card.Body>
              <Card.Title>Ekipman durumları</Card.Title>
              <StatusBreakdown counts={data.equipmentByStatus} total={data.totalEquipment} />
              <hr />
              <Card.Title>En çok rezerve edilenler (son 30 gün)</Card.Title>
              <TopEquipment items={data.topEquipment} />
            </Card.Body>
          </Card>
        </Col>
      </Row>

      <Row className="g-3">
        <Col lg={7}>
          <Card className="dash-card">
            <Card.Body>
              <div className="d-flex justify-content-between align-items-center mb-2">
                <Card.Title className="mb-0">Yaklaşan rezervasyonlarım</Card.Title>
                <Link to="/my-reservations" className="small">
                  Tümü
                </Link>
              </div>
              {data.myUpcoming.length === 0 ? (
                <p className="text-muted mb-0">Yaklaşan rezervasyonunuz yok</p>
              ) : (
                <ListGroup variant="flush">
                  {data.myUpcoming.map((r) => (
                    <ListGroup.Item key={r.id} action onClick={() => setSelected(r)} className="px-0">
                      <div className="d-flex justify-content-between gap-2">
                        <span>
                          <strong>{r.equipmentName}</strong>
                          <div className="small text-muted">
                            {formatDateTime(r.startTime)} – {formatDateTime(r.endTime)}
                          </div>
                        </span>
                        <span>
                          <ReservationStatusBadge reservation={r} />
                        </span>
                      </div>
                    </ListGroup.Item>
                  ))}
                </ListGroup>
              )}
            </Card.Body>
          </Card>
        </Col>
        <Col lg={5}>
          <Card className="dash-card">
            <Card.Body>
              <Card.Title>Kalibrasyonu yaklaşanlar (30 gün)</Card.Title>
              {data.calibrationDue.length === 0 ? (
                <p className="text-muted mb-0">Yaklaşan kalibrasyon yok</p>
              ) : (
                <ListGroup variant="flush">
                  {data.calibrationDue.map((c) => (
                    <ListGroup.Item key={c.id} className="px-0 d-flex justify-content-between">
                      <Link to={`/equipment/${c.id}`}>{c.name}</Link>
                      <span className={c.overdue ? "text-danger fw-semibold" : "text-muted"}>
                        {formatDate(c.calibrationDate)} {c.overdue && "· geçti"}
                      </span>
                    </ListGroup.Item>
                  ))}
                </ListGroup>
              )}
            </Card.Body>
          </Card>
        </Col>
      </Row>

      {selected && (
        <ReservationDetailModal
          reservation={selected}
          onHide={() => setSelected(null)}
          onChanged={() => {
            setSelected(null);
            load();
          }}
        />
      )}
    </>
  );
}

export default DashboardPage;
