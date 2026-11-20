import { useCallback, useEffect, useMemo, useState } from "react";
import { Badge, Button, Form, Nav, Tab, Table } from "react-bootstrap";
import client, { getErrorMessage } from "../api/client";
import { useToast } from "../context/ToastContext";
import Pager from "../components/Pager";
import ReservationDetailModal from "../components/ReservationDetailModal";
import ReservationTable from "../components/ReservationTable";
import { usePagedList } from "../hooks/usePagedList";
import { RESERVATION_STATUS, formatDateTime } from "../utils/format";

/** Bekleme listesi kayıtları: aralık boşaldıysa vurgulanır. */
function WaitlistTab() {
  const { showToast } = useToast();
  const [entries, setEntries] = useState([]);

  const load = useCallback(() => {
    client
      .get("/waitlist")
      .then((res) => setEntries(res.data))
      .catch((err) => showToast(getErrorMessage(err, "Bekleme listesi yüklenemedi"), "danger"));
  }, [showToast]);

  useEffect(load, [load]);

  const leave = async (entry) => {
    if (!window.confirm("Bekleme listesinden çıkılsın mı?")) return;
    try {
      await client.delete(`/waitlist/${entry.id}`);
      setEntries((prev) => prev.filter((e) => e.id !== entry.id));
      showToast("Bekleme listesinden çıkıldı");
    } catch (err) {
      showToast(getErrorMessage(err, "İşlem yapılamadı"), "danger");
    }
  };

  return (
    <>
      <p className="text-muted small">
        Dolu bir aralık için bekleme listesine girdiğinizde, aralık boşalınca üst bardaki zile bildirim gelir ve
        listeden otomatik çıkarsınız. Bildirimdeki "Rezerve Et" düğmesiyle yer kapılmadan rezervasyon yapabilirsiniz.
        Beklediğiniz aralıkla çakışan bir rezervasyon yaparsanız da listeden çıkarsınız.
      </p>
      <Table bordered hover responsive className="bg-white">
        <thead>
          <tr>
            <th>Ekipman</th>
            <th>Başlangıç</th>
            <th>Bitiş</th>
            <th>Durum</th>
            <th />
          </tr>
        </thead>
        <tbody>
          {entries.length === 0 && (
            <tr>
              <td colSpan={5} className="text-center text-muted py-4">
                Bekleme listesinde kaydınız yok
              </td>
            </tr>
          )}
          {entries.map((e) => (
            <tr key={e.id}>
              <td>{e.equipmentName}</td>
              <td className="text-nowrap">{formatDateTime(e.startTime)}</td>
              <td className="text-nowrap">{formatDateTime(e.endTime)}</td>
              <td>
                {e.slotAvailable ? (
                  <Badge bg="success">Yer açıldı, rezervasyon yapabilirsiniz</Badge>
                ) : (
                  <Badge bg="secondary">Bekleniyor</Badge>
                )}
              </td>
              <td className="text-end">
                <Button size="sm" variant="outline-danger" onClick={() => leave(e)}>
                  Listeden çık
                </Button>
              </td>
            </tr>
          ))}
        </tbody>
      </Table>
    </>
  );
}

function MyReservationsPage() {
  const [status, setStatus] = useState("");
  const [selected, setSelected] = useState(null);
  const filters = useMemo(() => ({ mine: true, status }), [status]);
  const { data, loading, setPage, updateItem } = usePagedList("/reservations/search", filters);

  return (
    <>
      <div className="page-header">
        <h1 className="h3 mb-0">Rezervasyonlarım</h1>
      </div>

      <Tab.Container defaultActiveKey="reservations">
        <Nav variant="tabs" className="mb-3">
          <Nav.Item>
            <Nav.Link eventKey="reservations">Rezervasyonlar</Nav.Link>
          </Nav.Item>
          <Nav.Item>
            <Nav.Link eventKey="waitlist">Bekleme Listem</Nav.Link>
          </Nav.Item>
        </Nav>
        <Tab.Content>
          <Tab.Pane eventKey="reservations">
            <div className="filter-bar">
              <Form.Select value={status} onChange={(e) => setStatus(e.target.value)} aria-label="Durum">
                <option value="">Tüm durumlar</option>
                {Object.entries(RESERVATION_STATUS).map(([k, { label }]) => (
                  <option key={k} value={k}>
                    {label}
                  </option>
                ))}
              </Form.Select>
            </div>
            <ReservationTable
              items={data.content}
              loading={loading}
              showUser={false}
              onSelect={setSelected}
              emptyText="Rezervasyonunuz yok"
            />
            <Pager data={data} onChange={setPage} />
          </Tab.Pane>
          <Tab.Pane eventKey="waitlist" mountOnEnter unmountOnExit>
            <WaitlistTab />
          </Tab.Pane>
        </Tab.Content>
      </Tab.Container>

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

export default MyReservationsPage;
