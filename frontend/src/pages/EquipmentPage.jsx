import { useCallback, useEffect, useMemo, useState } from "react";
import { useNavigate, useSearchParams } from "react-router-dom";
import { Badge, Button, Dropdown, Form, InputGroup, Spinner, Table } from "react-bootstrap";
import { FaCalendarPlus, FaEdit, FaFileExcel, FaFilePdf, FaPlus, FaSearch, FaTrash } from "react-icons/fa";
import client, { downloadFile, getErrorMessage } from "../api/client";
import { useAuth } from "../context/AuthContext";
import { useToast } from "../context/ToastContext";
import EquipmentFormModal from "../components/EquipmentFormModal";
import ImportModal from "../components/ImportModal";
import Pager from "../components/Pager";
import ReservationFormModal from "../components/ReservationFormModal";
import { CalibrationCell, EquipmentStatusBadge } from "../components/StatusBadges";
import { useDebounced, usePagedList } from "../hooks/usePagedList";
import { EQUIPMENT_STATUS } from "../utils/format";

function EquipmentPage() {
  const { isAdmin } = useAuth();
  const { showToast } = useToast();
  const navigate = useNavigate();
  const [searchParams] = useSearchParams();

  const [search, setSearch] = useState("");
  const [category, setCategory] = useState("");
  const [location, setLocation] = useState("");
  const [status, setStatus] = useState(searchParams.get("status") || "");
  const debouncedSearch = useDebounced(search);

  const filters = useMemo(
    () => ({ q: debouncedSearch.trim(), category, location, status }),
    [debouncedSearch, category, location, status]
  );
  const { data, loading, setPage, reload } = usePagedList("/equipment", filters, {
    errorMessage: "Ekipmanlar yüklenemedi",
  });

  const [filterOptions, setFilterOptions] = useState({ categories: [], locations: [] });
  const [options, setOptions] = useState([]);
  const loadOptions = useCallback(() => {
    client.get("/equipment/filters").then((res) => setFilterOptions(res.data)).catch(() => {});
    client.get("/equipment/options").then((res) => setOptions(res.data)).catch(() => {});
  }, []);
  useEffect(loadOptions, [loadOptions]);

  // Modallar: formModal = { equipment?: {...} } | null
  const [formModal, setFormModal] = useState(null);
  const [reserveFor, setReserveFor] = useState(null);
  const [showImport, setShowImport] = useState(false);
  const [downloading, setDownloading] = useState(false);

  const reserveInitialValues = useMemo(
    () => (reserveFor ? { equipmentId: String(reserveFor.id) } : undefined),
    [reserveFor]
  );

  const refresh = () => {
    reload();
    loadOptions();
  };

  const handleDelete = async (item) => {
    if (!window.confirm(`"${item.name}" silinsin mi? İleri tarihli rezervasyonları da iptal edilecek.`)) {
      return;
    }
    try {
      await client.delete(`/equipment/${item.id}`);
      showToast("Ekipman silindi");
      refresh();
    } catch (err) {
      showToast(getErrorMessage(err, "Silinemedi"), "danger");
    }
  };

  const download = async (url, name) => {
    setDownloading(true);
    try {
      await downloadFile(url, filters, name);
    } catch (err) {
      showToast(getErrorMessage(err, "Dosya indirilemedi"), "danger");
    } finally {
      setDownloading(false);
    }
  };

  const hasFilter = search || category || location || status;

  return (
    <>
      <div className="page-header flex-wrap">
        <h1 className="h3 mb-0">Ekipmanlar</h1>
        {isAdmin && (
          <div className="d-flex flex-wrap gap-2">
            <Dropdown>
              <Dropdown.Toggle variant="outline-success" disabled={downloading}>
                <FaFileExcel className="me-1" /> Excel
              </Dropdown.Toggle>
              <Dropdown.Menu>
                <Dropdown.Item onClick={() => download("/equipment/export", "ekipmanlar.xlsx")}>
                  Listeyi Excel'e aktar
                </Dropdown.Item>
                <Dropdown.Item onClick={() => setShowImport(true)}>Excel'den içe aktar</Dropdown.Item>
              </Dropdown.Menu>
            </Dropdown>
            <Button
              variant="outline-danger"
              disabled={downloading}
              onClick={() => download("/reports/equipment.pdf", "ekipman-envanteri.pdf")}
            >
              <FaFilePdf className="me-1" /> PDF
            </Button>
            <Button onClick={() => setFormModal({})}>
              <FaPlus className="me-1" /> Yeni Ekipman
            </Button>
          </div>
        )}
      </div>

      <div className="filter-bar">
        <InputGroup className="filter-search">
          <InputGroup.Text>
            <FaSearch />
          </InputGroup.Text>
          <Form.Control
            placeholder="Ad, marka, model veya seri no ile ara"
            value={search}
            onChange={(e) => setSearch(e.target.value)}
          />
        </InputGroup>
        <Form.Select value={category} onChange={(e) => setCategory(e.target.value)} aria-label="Kategori">
          <option value="">Tüm kategoriler</option>
          {filterOptions.categories.map((c) => (
            <option key={c}>{c}</option>
          ))}
        </Form.Select>
        <Form.Select value={location} onChange={(e) => setLocation(e.target.value)} aria-label="Konum">
          <option value="">Tüm konumlar</option>
          {filterOptions.locations.map((l) => (
            <option key={l}>{l}</option>
          ))}
        </Form.Select>
        <Form.Select value={status} onChange={(e) => setStatus(e.target.value)} aria-label="Durum">
          <option value="">Tüm durumlar</option>
          {Object.entries(EQUIPMENT_STATUS).map(([key, { label }]) => (
            <option key={key} value={key}>
              {label}
            </option>
          ))}
        </Form.Select>
        {hasFilter && (
          <Button
            variant="link"
            onClick={() => {
              setSearch("");
              setCategory("");
              setLocation("");
              setStatus("");
            }}
          >
            Filtreleri temizle
          </Button>
        )}
        {loading && <Spinner size="sm" animation="border" variant="primary" />}
      </div>

      <Table striped bordered hover responsive className={`bg-white ${loading ? "table-loading" : ""}`}>
        <thead>
          <tr>
            <th>Ekipman Adı</th>
            <th>Marka / Model</th>
            <th>Seri No</th>
            <th>Kategori</th>
            <th>Konum</th>
            <th>Durum</th>
            <th>Kalibrasyon</th>
            <th className="text-center">İşlemler</th>
          </tr>
        </thead>
        <tbody>
          {!loading && data.content.length === 0 && (
            <tr>
              <td colSpan={8} className="text-center text-muted py-4">
                {hasFilter ? "Filtrelere uygun ekipman bulunamadı" : "Henüz ekipman eklenmemiş"}
              </td>
            </tr>
          )}
          {data.content.map((item) => (
            <tr key={item.id} className="clickable-row" onClick={() => navigate(`/equipment/${item.id}`)}>
              <td>
                {item.name}
                {item.requiresApproval && (
                  <Badge bg="light" text="dark" className="ms-2 border" title="Rezervasyonlar yönetici onayı gerektirir">
                    Onaylı
                  </Badge>
                )}
              </td>
              <td>
                {item.brand} {item.model}
              </td>
              <td>
                <code>{item.serialNumber}</code>
              </td>
              <td>{item.category || <span className="text-muted">-</span>}</td>
              <td>{item.location || <span className="text-muted">-</span>}</td>
              <td>
                <EquipmentStatusBadge status={item.status} />
              </td>
              <td>
                <CalibrationCell date={item.calibrationDate} />
              </td>
              <td className="text-center text-nowrap" onClick={(e) => e.stopPropagation()}>
                <Button
                  size="sm"
                  variant="outline-success"
                  title={item.status === "AVAILABLE" ? "Rezerve et" : "Ekipman şu anda kullanılamaz"}
                  disabled={item.status !== "AVAILABLE"}
                  onClick={() => setReserveFor(item)}
                >
                  <FaCalendarPlus />
                </Button>
                {isAdmin && (
                  <>
                    <Button
                      size="sm"
                      variant="outline-primary"
                      className="ms-1"
                      title="Düzenle"
                      onClick={() => setFormModal({ equipment: item })}
                    >
                      <FaEdit />
                    </Button>
                    <Button
                      size="sm"
                      variant="outline-danger"
                      className="ms-1"
                      title="Sil"
                      onClick={() => handleDelete(item)}
                    >
                      <FaTrash />
                    </Button>
                  </>
                )}
              </td>
            </tr>
          ))}
        </tbody>
      </Table>
      <Pager data={data} onChange={setPage} />

      <EquipmentFormModal
        show={formModal !== null}
        equipment={formModal?.equipment}
        filterOptions={filterOptions}
        onHide={() => setFormModal(null)}
        onSaved={() => {
          setFormModal(null);
          refresh();
        }}
      />

      <ReservationFormModal
        show={reserveFor !== null}
        equipmentOptions={options}
        initialValues={reserveInitialValues}
        onHide={() => setReserveFor(null)}
        onSaved={() => setReserveFor(null)}
      />

      <ImportModal
        show={showImport}
        onHide={() => setShowImport(false)}
        onImported={refresh}
      />
    </>
  );
}

export default EquipmentPage;
