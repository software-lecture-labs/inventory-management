import { useMemo, useState } from "react";
import { Badge, Button, Form, Nav, Spinner, Tab, Table } from "react-bootstrap";
import { FaFilePdf } from "react-icons/fa";
import { downloadFile, getErrorMessage } from "../api/client";
import { useToast } from "../context/ToastContext";
import Pager from "../components/Pager";
import ReservationDetailModal from "../components/ReservationDetailModal";
import ReservationTable from "../components/ReservationTable";
import { useDebounced, usePagedList } from "../hooks/usePagedList";
import {
  AUDIT_ACTIONS,
  ENTITY_TYPES,
  FIELD_LABELS,
  RESERVATION_STATUS,
  formatDateTime,
  formatFieldValue,
} from "../utils/format";

/** Filtreleri kullanarak PDF indiren düğme. */
function PdfButton({ url, params, name }) {
  const { showToast } = useToast();
  const [busy, setBusy] = useState(false);
  const download = async () => {
    setBusy(true);
    try {
      await downloadFile(url, params, name);
    } catch (err) {
      showToast(getErrorMessage(err, "PDF oluşturulamadı"), "danger");
    } finally {
      setBusy(false);
    }
  };
  return (
    <Button variant="outline-danger" onClick={download} disabled={busy} className="ms-auto">
      {busy ? <Spinner size="sm" animation="border" /> : <FaFilePdf className="me-1" />} PDF indir
    </Button>
  );
}

const emptyAuditFilters = { entityType: "", action: "", q: "", from: "", to: "" };

function AuditLogTab() {
  const [filters, setFilters] = useState(emptyAuditFilters);
  const q = useDebounced(filters.q);
  const query = useMemo(() => ({ ...filters, q: q.trim() }), [filters, q]);
  const { data, loading, setPage } = usePagedList("/audit-logs", query, { errorMessage: "Kayıtlar yüklenemedi" });

  const set = (e) => setFilters({ ...filters, [e.target.name]: e.target.value });

  return (
    <>
      <div className="filter-bar">
        <Form.Select name="entityType" value={filters.entityType} onChange={set} aria-label="Kayıt türü">
          <option value="">Tüm kayıt türleri</option>
          {Object.entries(ENTITY_TYPES).map(([k, label]) => (
            <option key={k} value={k}>
              {label}
            </option>
          ))}
        </Form.Select>
        <Form.Select name="action" value={filters.action} onChange={set} aria-label="İşlem">
          <option value="">Tüm işlemler</option>
          {Object.entries(AUDIT_ACTIONS).map(([k, { label }]) => (
            <option key={k} value={k}>
              {label}
            </option>
          ))}
        </Form.Select>
        <Form.Control name="q" placeholder="Kayıt adı veya kişi" value={filters.q} onChange={set} />
        <Form.Control type="date" name="from" value={filters.from} onChange={set} aria-label="Başlangıç tarihi" />
        <Form.Control type="date" name="to" value={filters.to} min={filters.from} onChange={set} aria-label="Bitiş tarihi" />
        <Button variant="link" onClick={() => setFilters(emptyAuditFilters)}>
          Temizle
        </Button>
        <PdfButton url="/reports/audit-logs.pdf" params={query} name="islem-gecmisi.pdf" />
      </div>

      <Table bordered hover responsive className={`bg-white ${loading ? "table-loading" : ""}`}>
        <thead>
          <tr>
            <th>Tarih</th>
            <th>Tür</th>
            <th>İşlem</th>
            <th>Kayıt</th>
            <th>İşlemi Yapan</th>
            <th>Ayrıntı</th>
          </tr>
        </thead>
        <tbody>
          {!loading && data.content.length === 0 && (
            <tr>
              <td colSpan={6} className="text-center text-muted py-4">
                Kayıt bulunamadı
              </td>
            </tr>
          )}
          {data.content.map((log) => {
            const action = AUDIT_ACTIONS[log.action] ?? { label: log.action, variant: "secondary" };
            return (
              <tr key={log.id}>
                <td className="text-nowrap">{formatDateTime(log.createdAt)}</td>
                <td>{ENTITY_TYPES[log.entityType]}</td>
                <td>
                  <Badge bg={action.variant} text={action.variant === "warning" || action.variant === "info" ? "dark" : undefined}>
                    {action.label}
                  </Badge>
                </td>
                <td>{log.entityName}</td>
                <td>{log.performedBy}</td>
                <td className="small">
                  {log.changes.map((c) => (
                    <div key={c.field} className="change-row">
                      <Badge bg="light" text="dark" className="me-2">
                        {FIELD_LABELS[c.field] || c.field}
                      </Badge>
                      <span className="text-danger text-decoration-line-through">
                        {formatFieldValue(c.field, c.oldValue) ?? "boş"}
                      </span>
                      {" → "}
                      <span className="text-success">{formatFieldValue(c.field, c.newValue) ?? "boş"}</span>
                    </div>
                  ))}
                  {log.note && <div className="text-muted">{log.note}</div>}
                </td>
              </tr>
            );
          })}
        </tbody>
      </Table>
      <Pager data={data} onChange={setPage} />
    </>
  );
}

const emptyReservationFilters = { user: "", equipment: "", status: "", from: "", to: "", overdue: false };

function ReservationReportTab() {
  const [filters, setFilters] = useState(emptyReservationFilters);
  const [selected, setSelected] = useState(null);
  const user = useDebounced(filters.user);
  const equipment = useDebounced(filters.equipment);
  const query = useMemo(
    () => ({ ...filters, user: user.trim(), equipment: equipment.trim() }),
    [filters, user, equipment]
  );
  const { data, loading, setPage, updateItem } = usePagedList("/reservations/search", query);

  const set = (e) => {
    const { name, value, type, checked } = e.target;
    setFilters({ ...filters, [name]: type === "checkbox" ? checked : value });
  };

  return (
    <>
      <div className="filter-bar">
        <Form.Control name="user" placeholder="Kişi adı" value={filters.user} onChange={set} />
        <Form.Control name="equipment" placeholder="Ekipman adı veya seri no" value={filters.equipment} onChange={set} />
        <Form.Select name="status" value={filters.status} onChange={set} aria-label="Durum">
          <option value="">Tüm durumlar</option>
          {Object.entries(RESERVATION_STATUS).map(([k, { label }]) => (
            <option key={k} value={k}>
              {label}
            </option>
          ))}
        </Form.Select>
        <Form.Control type="date" name="from" value={filters.from} onChange={set} aria-label="Başlangıç tarihi" />
        <Form.Control type="date" name="to" value={filters.to} min={filters.from} onChange={set} aria-label="Bitiş tarihi" />
        <Form.Check
          type="switch"
          id="overdue-only"
          name="overdue"
          label="Sadece gecikmiş"
          checked={filters.overdue}
          onChange={set}
        />
        <Button variant="link" onClick={() => setFilters(emptyReservationFilters)}>
          Temizle
        </Button>
        <PdfButton url="/reports/reservations.pdf" params={query} name="rezervasyon-raporu.pdf" />
      </div>

      <ReservationTable items={data.content} loading={loading} onSelect={setSelected} />
      <Pager data={data} onChange={setPage} />

      {selected && (
        <ReservationDetailModal
          reservation={selected}
          onHide={() => setSelected(null)}
          onChanged={(updated) => {
            updateItem(updated);
            setSelected(null);
          }}
        />
      )}
    </>
  );
}

function ReportsPage() {
  return (
    <>
      <div className="page-header">
        <h1 className="h3 mb-0">Raporlar</h1>
      </div>

      <Tab.Container defaultActiveKey="audit">
        <Nav variant="tabs" className="mb-3">
          <Nav.Item>
            <Nav.Link eventKey="audit">İşlem Geçmişi</Nav.Link>
          </Nav.Item>
          <Nav.Item>
            <Nav.Link eventKey="reservations">Rezervasyonlar</Nav.Link>
          </Nav.Item>
        </Nav>
        <Tab.Content>
          <Tab.Pane eventKey="audit">
            <AuditLogTab />
          </Tab.Pane>
          <Tab.Pane eventKey="reservations" mountOnEnter>
            <ReservationReportTab />
          </Tab.Pane>
        </Tab.Content>
      </Tab.Container>
      <p className="text-muted small mt-2">Ekipman envanteri PDF'i Ekipmanlar sayfasındaki "PDF" düğmesinden alınabilir.</p>
    </>
  );
}

export default ReportsPage;
