import { useMemo, useState } from "react";
import { useSearchParams } from "react-router-dom";
import { Nav } from "react-bootstrap";
import Pager from "../components/Pager";
import ReservationDetailModal from "../components/ReservationDetailModal";
import ReservationTable from "../components/ReservationTable";
import { usePagedList } from "../hooks/usePagedList";

const TABS = {
  pending: { label: "Onay Bekleyenler", filters: { status: "PENDING" }, empty: "Onay bekleyen talep yok" },
  overdue: { label: "Gecikmiş İadeler", filters: { overdue: true }, empty: "Gecikmiş iade yok" },
  out: { label: "Teslim Alınmış", filters: { status: "CHECKED_OUT" }, empty: "Şu an teslim alınmış ekipman yok" },
};

/** Yönetici: onay bekleyen talepler ve iade takibi. */
function ApprovalsPage() {
  const [searchParams, setSearchParams] = useSearchParams();
  const tab = TABS[searchParams.get("tab")] ? searchParams.get("tab") : "pending";
  const [selected, setSelected] = useState(null);

  const filters = useMemo(() => TABS[tab].filters, [tab]);
  const { data, loading, setPage, reload } = usePagedList("/reservations/search", filters);

  return (
    <>
      <div className="page-header">
        <h1 className="h3 mb-0">Onay ve İade Takibi</h1>
      </div>

      <Nav variant="tabs" className="mb-3" activeKey={tab} onSelect={(key) => setSearchParams({ tab: key })}>
        {Object.entries(TABS).map(([key, { label }]) => (
          <Nav.Item key={key}>
            <Nav.Link eventKey={key}>{label}</Nav.Link>
          </Nav.Item>
        ))}
      </Nav>

      <ReservationTable items={data.content} loading={loading} onSelect={setSelected} emptyText={TABS[tab].empty} />
      <Pager data={data} onChange={setPage} />

      {selected && (
        <ReservationDetailModal
          reservation={selected}
          onHide={() => setSelected(null)}
          onChanged={() => {
            setSelected(null);
            reload();
          }}
        />
      )}
    </>
  );
}

export default ApprovalsPage;
