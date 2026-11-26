import { useCallback, useEffect, useMemo, useState } from "react";
import { useSearchParams } from "react-router-dom";
import { Badge, Button, Form, InputGroup, Nav, Table } from "react-bootstrap";
import { FaKey, FaSearch } from "react-icons/fa";
import client, { getErrorMessage } from "../api/client";
import { useAuth } from "../context/AuthContext";
import { useToast } from "../context/ToastContext";
import Pager from "../components/Pager";
import TemporaryPasswordModal from "../components/TemporaryPasswordModal";
import { useDebounced, usePagedList } from "../hooks/usePagedList";
import { formatDateTime } from "../utils/format";

/** "Şifremi unuttum" talepleri: yönetici tek kullanımlık şifre belirler veya talebi yok sayar. */
function PasswordRequestsTab({ requests, onSetPassword, onDismiss }) {
  return (
    <>
      <p className="text-muted small">
        Kullanıcı "Şifremi unuttum" sayfasından talep gönderdiğinde burada görünür. Tek kullanımlık şifre belirleyip
        kullanıcıya iletin (telefon, yüz yüze vb.). Kullanıcı bu şifreyle giriş yapınca kendi şifresini belirler.
      </p>
      <Table bordered hover responsive className="bg-white">
        <thead>
          <tr>
            <th>Ad Soyad</th>
            <th>Email</th>
            <th>Talep Zamanı</th>
            <th style={{ width: 280 }} />
          </tr>
        </thead>
        <tbody>
          {requests.length === 0 && (
            <tr>
              <td colSpan={4} className="text-center text-muted py-4">
                Bekleyen şifre talebi yok
              </td>
            </tr>
          )}
          {requests.map((r) => (
            <tr key={r.id}>
              <td>{r.fullName}</td>
              <td>{r.email}</td>
              <td className="text-nowrap">{formatDateTime(r.createdAt)}</td>
              <td className="text-end text-nowrap">
                <Button size="sm" onClick={() => onSetPassword(r)}>
                  <FaKey className="me-1" /> Tek kullanımlık şifre ver
                </Button>
                <Button size="sm" variant="outline-secondary" className="ms-2" onClick={() => onDismiss(r)}>
                  Yoksay
                </Button>
              </td>
            </tr>
          ))}
        </tbody>
      </Table>
    </>
  );
}

function UsersPage() {
  const { user: currentUser } = useAuth();
  const { showToast } = useToast();
  const [searchParams, setSearchParams] = useSearchParams();
  const tab = searchParams.get("tab") === "password" ? "password" : "users";

  // Şifre talepleri ve tek kullanımlık şifre penceresi ({ userId, fullName, email } | null)
  const [requests, setRequests] = useState([]);
  const [passwordTarget, setPasswordTarget] = useState(null);
  const loadRequests = useCallback(() => {
    client
      .get("/users/password-requests")
      .then((res) => setRequests(res.data))
      .catch((err) => showToast(getErrorMessage(err, "Şifre talepleri yüklenemedi"), "danger"));
  }, [showToast]);
  useEffect(loadRequests, [loadRequests]);

  const dismissRequest = async (r) => {
    if (!window.confirm(`${r.fullName} adlı kişinin şifre talebi yok sayılsın mı?`)) return;
    try {
      await client.post(`/users/password-requests/${r.id}/dismiss`);
      setRequests((prev) => prev.filter((x) => x.id !== r.id));
      showToast("Talep yok sayıldı");
    } catch (err) {
      showToast(getErrorMessage(err, "İşlem yapılamadı"), "danger");
    }
  };
  const [search, setSearch] = useState("");
  const [role, setRole] = useState("");
  const [enabled, setEnabled] = useState("");
  const q = useDebounced(search);

  const filters = useMemo(() => ({ q: q.trim(), role, enabled }), [q, role, enabled]);
  const { data, loading, setPage, updateItem, reload } = usePagedList("/users", filters, {
    errorMessage: "Kullanıcılar yüklenemedi",
  });

  const changeRole = async (target, newRole) => {
    const label = newRole === "ADMIN" ? "yönetici" : "kullanıcı";
    if (!window.confirm(`${target.fullName} adlı kişinin rolü "${label}" olarak değiştirilsin mi?`)) {
      return;
    }
    try {
      const res = await client.patch(`/users/${target.id}/role`, { role: newRole });
      updateItem(res.data);
      showToast("Rol güncellendi");
    } catch (err) {
      showToast(getErrorMessage(err, "Rol değiştirilemedi"), "danger");
    }
  };

  const toggleEnabled = async (target) => {
    const disabling = target.enabled;
    const message = disabling
      ? `${target.fullName} engellensin mi?\n\nGiriş yapamayacak, açık oturumu kapanacak ve ileri tarihli rezervasyonları iptal edilecek.`
      : `${target.fullName} adlı kişinin engeli kaldırılsın mı?`;
    if (!window.confirm(message)) return;
    try {
      const res = await client.patch(`/users/${target.id}/enabled`, { enabled: !disabling });
      updateItem(res.data);
      showToast(disabling ? "Kullanıcı engellendi" : "Engel kaldırıldı");
    } catch (err) {
      showToast(getErrorMessage(err, "İşlem yapılamadı"), "danger");
    }
  };

  const unlock = async (target) => {
    try {
      const res = await client.patch(`/users/${target.id}/enabled`, { enabled: true });
      updateItem(res.data);
      showToast("Hesap kilidi açıldı");
    } catch (err) {
      showToast(getErrorMessage(err, "İşlem yapılamadı"), "danger");
    }
  };

  return (
    <>
      <div className="page-header">
        <h1 className="h3 mb-0">Kullanıcılar</h1>
      </div>

      <Nav variant="tabs" className="mb-3" activeKey={tab} onSelect={(key) => setSearchParams(key === "password" ? { tab: key } : {})}>
        <Nav.Item>
          <Nav.Link eventKey="users">Kullanıcılar</Nav.Link>
        </Nav.Item>
        <Nav.Item>
          <Nav.Link eventKey="password">
            Şifre Talepleri{" "}
            {requests.length > 0 && (
              <Badge bg="warning" text="dark" pill>
                {requests.length}
              </Badge>
            )}
          </Nav.Link>
        </Nav.Item>
      </Nav>

      {tab === "password" && (
        <PasswordRequestsTab requests={requests} onSetPassword={setPasswordTarget} onDismiss={dismissRequest} />
      )}

      {tab === "users" && (
      <>
      <div className="filter-bar">
        <InputGroup className="filter-search">
          <InputGroup.Text>
            <FaSearch />
          </InputGroup.Text>
          <Form.Control placeholder="Ad, email veya telefon" value={search} onChange={(e) => setSearch(e.target.value)} />
        </InputGroup>
        <Form.Select value={role} onChange={(e) => setRole(e.target.value)} aria-label="Rol">
          <option value="">Tüm roller</option>
          <option value="USER">Kullanıcı</option>
          <option value="ADMIN">Yönetici</option>
        </Form.Select>
        <Form.Select value={enabled} onChange={(e) => setEnabled(e.target.value)} aria-label="Hesap durumu">
          <option value="">Tüm hesaplar</option>
          <option value="true">Aktif</option>
          <option value="false">Engelli</option>
        </Form.Select>
      </div>

      <Table striped bordered hover responsive className={`bg-white ${loading ? "table-loading" : ""}`}>
        <thead>
          <tr>
            <th>Ad Soyad</th>
            <th>Email</th>
            <th>Telefon</th>
            <th>Kayıt</th>
            <th style={{ width: 150 }}>Rol</th>
            <th style={{ width: 260 }}>Hesap</th>
          </tr>
        </thead>
        <tbody>
          {!loading && data.content.length === 0 && (
            <tr>
              <td colSpan={6} className="text-center text-muted py-4">
                Kullanıcı bulunamadı
              </td>
            </tr>
          )}
          {data.content.map((u) => {
            const isSelf = u.id === currentUser.id;
            return (
              <tr key={u.id} className={u.enabled ? undefined : "table-secondary"}>
                <td>
                  {u.fullName}{" "}
                  {isSelf && (
                    <Badge bg="info" className="ms-1">
                      Siz
                    </Badge>
                  )}
                </td>
                <td>{u.email}</td>
                <td>{u.phone}</td>
                <td className="text-nowrap small">{formatDateTime(u.createdAt)}</td>
                <td>
                  <Form.Select
                    size="sm"
                    value={u.role}
                    disabled={isSelf}
                    onChange={(e) => changeRole(u, e.target.value)}
                    aria-label={`${u.fullName} rolü`}
                  >
                    <option value="USER">Kullanıcı</option>
                    <option value="ADMIN">Yönetici</option>
                  </Form.Select>
                </td>
                <td>
                  <div className="d-flex align-items-center gap-2 flex-wrap">
                    {u.enabled ? <Badge bg="success">Aktif</Badge> : <Badge bg="danger">Engelli</Badge>}
                    {u.lockedUntil && (
                      <Badge bg="warning" text="dark" title={`${formatDateTime(u.lockedUntil)} tarihine kadar`}>
                        Kilitli
                      </Badge>
                    )}
                    {!isSelf && (
                      <Button
                        size="sm"
                        variant={u.enabled ? "outline-danger" : "outline-success"}
                        onClick={() => toggleEnabled(u)}
                      >
                        {u.enabled ? "Engelle" : "Engeli kaldır"}
                      </Button>
                    )}
                    {!isSelf && u.enabled && u.lockedUntil && (
                      <Button size="sm" variant="outline-secondary" onClick={() => unlock(u)}>
                        Kilidi aç
                      </Button>
                    )}
                    {!isSelf && u.enabled && (
                      <Button
                        size="sm"
                        variant="outline-primary"
                        title="Tek kullanımlık şifre ver"
                        onClick={() => setPasswordTarget({ userId: u.id, fullName: u.fullName, email: u.email })}
                      >
                        <FaKey />
                      </Button>
                    )}
                    {u.mustChangePassword && (
                      <Badge bg="info" text="dark" title="Tek kullanımlık şifre verildi, kullanıcı henüz yeni şifre belirlemedi">
                        Şifre bekleniyor
                      </Badge>
                    )}
                  </div>
                </td>
              </tr>
            );
          })}
        </tbody>
      </Table>
      <Pager data={data} onChange={setPage} />
      </>
      )}

      <TemporaryPasswordModal
        target={passwordTarget}
        onHide={() => setPasswordTarget(null)}
        onDone={() => {
          loadRequests();
          reload();
        }}
      />
    </>
  );
}

export default UsersPage;
