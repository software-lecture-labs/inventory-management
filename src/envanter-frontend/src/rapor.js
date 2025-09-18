import React, { useEffect, useState } from "react";
import { Navbar, Container, Nav, Button, Tab, Table, Form, Row, Col } from "react-bootstrap";
import { useNavigate } from "react-router-dom";
import { FaSignOutAlt, FaTools, FaSearch } from "react-icons/fa";
import { BsFillCalendar2DateFill } from "react-icons/bs";
import { BiSolidReport } from "react-icons/bi";
import { PiBroomFill } from "react-icons/pi";
import logo from "./assets/Assan_logo.png";
import axios from "axios";

function Rapor() {
  const [fullName, setFullName] = useState("");
  const [id, setEmployeeId] = useState("");
  const [allEquipment, setAllEquipment] = useState([]);
  const [inactiveEquipment, setInactiveEquipment] = useState([]);
  const [logs, setLogs] = useState([]);
  const [deletes, setDeletes] = useState([]);
  const [reservations, setReservations] = useState([]);
  const [allReservations, setAllReservations] = useState([]);
  const [input1, setInput1] = useState("");
  const [input2, setInput2] = useState("");
  const [input3, setInput3] = useState("");

  const navigate = useNavigate();

  const fieldNames = {
    brand: "Marka",
    model: "Model",
    equipment_name: "Ekipman Adı",
    serial_number: "Seri Numarası",
    calibration_date: "Kalibrasyon Tarihi",
    description: "Açıklama",
  };

  useEffect(() => {
    const name = localStorage.getItem("fullName");
    const id = localStorage.getItem("employeeId");

    if (name) setFullName(name);
    if (id) setEmployeeId(id);

    // Ekipmanları çek
    fetch("http://localhost:8080/equipment/all", { headers: { "Content-Type": "application/json" } })
      .then((res) => res.json())
      .then((data) => {
        setAllEquipment(data);
        setInactiveEquipment(data.filter((e) => e.status === false));
      })
      .catch((err) => console.error("Ekipman hatası:", err));

    // Logları çek
    fetch("http://localhost:8080/equipment/logs", { headers: { "Content-Type": "application/json" } })
      .then((res) => res.json())
      .then((data) => setLogs(data))
      .catch((err) => console.error("Log çekme hatası:", err));

    fetch("http://localhost:8080/equipment/delete", { headers: { "Content-Type": "application/json" } })
      .then((res) => res.json())
      .then((data) => setDeletes(data))
      .catch((err) => console.error("Log çekme hatası:", err));

    // Rezervasyonları çek
    const fetchReservations = async () => {
      try {
        const res = await axios.get("http://localhost:8080/rezervasyon");
        setReservations(res.data);
        setAllReservations(res.data);
      } catch (err) {
        console.error("Rezervasyon çekme hatası:", err);
      }
    };
    fetchReservations();
  }, []);

  const handleLogout = () => {
    if (window.confirm("Emin misiniz?")) {
      // LocalStorage temizleme
      localStorage.removeItem("user");
      localStorage.removeItem("fullName");
      localStorage.removeItem("employeeId");

      // Login sayfasına yönlendir
      navigate("/giris");
    }
  };


  const araButonu = () => {
    // Filtreleme işlemi
    const filtered = allReservations.filter((r) => {
      const employeeMatch = r.employeeName.toLowerCase().includes(input1.toLowerCase());
      const equipmentMatch = r.equipmentName.toLowerCase().includes(input2.toLowerCase());
      const dateMatch = !input3 || r.beginDate.startsWith(input3);
      return employeeMatch && equipmentMatch && dateMatch;
    });
    setReservations(filtered);
  };

  const temizleButonu = () => {
    setInput1("");
    setInput2("");
    setInput3("");
  };


  return (
    
    <div>
      {/* Navbar */}
      <Navbar bg="light" expand="lg">
        <Container fluid>
          <div className="d-flex align-items-center ms-3">
            <img src={logo} width="150" height="45" className="me-2" alt="Logo" />
          </div>
          <Navbar.Toggle aria-controls="navbarScroll" />
          <Navbar.Collapse id="navbarScroll">
            <Nav className="me-auto my-2 my-lg-0" navbarScroll></Nav>
            <div style={{ display: "flex", alignItems: "center", gap: "15px" }}>
              <h5 style={{ margin: 0 }}>Hoşgeldin {fullName}</h5>
              <Button variant="secondary" onClick={handleLogout}>
                <FaSignOutAlt className="me-1" size={20}/> Çıkış Yap
              </Button>
            </div>
          </Navbar.Collapse>
        </Container>
      </Navbar>

      <div className="d-flex">
        {/* Sidebar */}
        <div style={{ width: "220px", height: "100vh", backgroundColor: "#f8f9faff", padding: "30px", fontSize: "1.1rem" }}>
          <Nav className="flex-column">
            <Nav.Link href="/ekipman"><FaTools className="me-2" />Ekipmanlar</Nav.Link>
            <Nav.Link href="/rezervasyon"><BsFillCalendar2DateFill className="me-2" /> Rezervasyon</Nav.Link>
            <Nav.Link href="/rapor"><BiSolidReport className="me-2" /> Raporlar</Nav.Link>
          </Nav>
        </div>

        {/* Ana içerik */}
        <div style={{ flex: 1, padding: "20px" }}>
          <div className="d-flex justify-content-between align-items-center mb-3">
            <h2>Raporlar</h2>
          </div>

          <Container className="mt-4">
            <Tab.Container defaultActiveKey="ekle">
              <Nav variant="pills" className="mb-3">
                <Nav.Item><Nav.Link eventKey="ekle">Kayıt İşlemleri</Nav.Link></Nav.Item>
                <Nav.Item><Nav.Link eventKey="guncelle">Güncelleme İşlemleri</Nav.Link></Nav.Item>
                <Nav.Item><Nav.Link eventKey="sil">Silme İşlemleri</Nav.Link></Nav.Item>
                <Nav.Item><Nav.Link eventKey="rezervasyon">Rezervasyon İşlemleri</Nav.Link></Nav.Item>
              </Nav>

              <Tab.Content>
                {/* Rezervasyon Tab */}
                <Tab.Pane eventKey="rezervasyon">
                  <div style={{ flex: 1, padding: "20px" }}>
                    <div className="d-flex justify-content-between align-items-center mb-4">
                      <h3>Rezervasyonlar</h3>
                    </div>

                    {/* Inputlar + Buton */}
                    <Container className="mb-3">
                      <Row className="align-items-center">
                        <Col md={8} className="d-flex gap-2">
                          <Form.Control
                            size="md"
                            type="text"
                            placeholder="Çalışan Adı"
                            value={input1}
                            onChange={(e) => {
                              const value = e.target.value;
                              // Sadece harf ve boşluk kabul et (Türkçe karakterler dahil)
                              if (/^[a-zA-ZğüşöçıİĞÜŞÖÇ\s]*$/.test(value)) {
                                setInput1(value);
                              }
                            }}
                          />
                          <Form.Control size="md" type="text" placeholder="Ekipman Adı" value={input2} onChange={(e) => setInput2(e.target.value)} />
                          <Form.Control size="md" type="date" placeholder="Tarih" value={input3} onChange={(e) => setInput3(e.target.value)} />
                        </Col>
                        <Col md={4} className="d-flex justify-content-end">

                          <Button size="md" variant="link" onClick={araButonu}><FaSearch size={25} style={{ strokeWidth: 15 }}/></Button>

                          <Button size="md" variant="link" className="ms-2" onClick={temizleButonu}><PiBroomFill size={25} color="gray" style={{ strokeWidth: 15 }}/></Button>

                        </Col>
                      </Row>
                    </Container>

                    {/* Rezervasyon Tablosu */}
                    <Table striped bordered hover responsive>
                      <thead>
                        <tr>
                          <th>Çalışan Adı</th>
                          <th>Ekipman Adı</th>
                          <th>Seri Numarası</th>
                          <th>Başlangıç Tarihi</th>
                          <th>Bitiş Tarihi</th>
                        </tr>
                      </thead>
                      <tbody>
                        {reservations.map((r) => (
                          <tr key={r.id}>
                            <td>{r.employeeName}</td>
                            <td>{r.equipmentName}</td>
                            <td>{r.serialNumber}</td>
                            <td>{new Date(r.beginDate).toLocaleString("tr-TR")}</td>
                            <td>{new Date(r.endDate).toLocaleString("tr-TR")}</td>
                          </tr>
                        ))}
                      </tbody>
                    </Table>
                  </div>
                </Tab.Pane>

                {/* Kayıt İşlemleri Tab */}
                <Tab.Pane eventKey="ekle">
                  <Table striped bordered hover>
                    <thead>
                      <tr>
                        <th style={{ textAlign: "center" }}>Ekipman Id</th>

                        <th style={{ textAlign: "center" }}>Ekipman Adı</th>
                        <th style={{ textAlign: "center" }}>Çalışan Adı</th>
                        <th style={{ textAlign: "center" }}>Tarih</th>
                        <th style={{ textAlign: "center" }}>Açıklama</th>
                      </tr>
                    </thead>
                    <tbody>
                      {allEquipment.map((data) => (
                        <tr key={data.id}>
                          <td style={{ textAlign: "center" }}>{data.id}</td>
                          <td style={{ textAlign: "center" }}>{data.equipmentName}</td>
                          <td style={{ textAlign: "center" }}>{data.employee.firstName + " " + data.employee.lastName}</td>
                          <td style={{ textAlign: "center" }}>{new Date(data.createdAt).toLocaleString("tr-TR")}</td>
                          <td style={{ textAlign: "center" }}>{data.description}</td>
                        </tr>
                      ))}
                    </tbody>
                  </Table>
                </Tab.Pane>

                {/* Güncelleme Tab */}
                <Tab.Pane eventKey="guncelle">
                  <Table striped bordered hover>
                    <thead>
                      <tr>
                        <th>Ekipman ID</th>
                        <th>Çalışan Adı</th>
                        <th>Değişen Sütunlar</th>
                        <th>Eski Değerler</th>
                        <th>Yeni Değerler</th>
                        <th>Güncelleme Tarihi</th>
                      </tr>
                    </thead>
                    <tbody>
                      {logs.map((log) => {
                        const changedColumns = log.changedColumns.split(",");
                        const oldValues = log.oldValues.split(",");
                        const newValues = log.newValues.split(",");
                        return (
                          <tr key={log.id}>
                            <td>{log.equipmentId}</td>
                            <td>{log.employeeName}</td>
                            <td style={{ whiteSpace: "pre-line" }}>{changedColumns.map((col) => fieldNames[col] || col).join("\n")}</td>
                            <td style={{ whiteSpace: "pre-line" }}>{oldValues.join("\n")}</td>
                            <td style={{ whiteSpace: "pre-line" }}>{newValues.join("\n")}</td>
                            <td>{new Date(log.updatedAt).toLocaleString("tr-TR")}</td>
                          </tr>
                        );
                      })}
                    </tbody>
                  </Table>
                </Tab.Pane>

                {/* Silme Tab */}
                <Tab.Pane eventKey="sil">
                  <Table striped bordered hover>
                    <thead>
                      <tr>
                        <th style={{ textAlign: "center" }}>Ekipman Id</th>
                        <th style={{ textAlign: "center" }}>Ekipman Adı</th>
                        <th style={{ textAlign: "center" }}>Çalışan Adı</th>
                        <th style={{ textAlign: "center" }}>Silinme Tarihi</th>
                        <th style={{ textAlign: "center" }}>Açıklama</th>
                      </tr>
                    </thead>
                    <tbody>
                      {deletes.map((data) => (
                        <tr key={data.id}>
                          <td style={{ textAlign: "center" }}>{data.equipmentId}</td>
                          <td style={{ textAlign: "center" }}>{data.equipmentName}</td>
                          <td style={{ textAlign: "center" }}>{data.employeeName}</td>
                          <td style={{ textAlign: "center" }}>{new Date(data.deletedDate).toLocaleString("tr-TR")}</td>
                          <td style={{ textAlign: "center" }}>{data.description}</td>
                        </tr>
                      ))}
                    </tbody>
                  </Table>
                </Tab.Pane>
              </Tab.Content>
            </Tab.Container>
          </Container>
        </div>
      </div>
    </div>
  );
}

export default Rapor;
