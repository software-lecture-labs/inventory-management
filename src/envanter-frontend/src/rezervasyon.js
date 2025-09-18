import React, { useEffect, useState, useRef } from "react";
import {
  Navbar,
  Container,
  Nav,
  Button,
  Modal,
  Form, 
  Row,
  Col,
} from "react-bootstrap";
import { useNavigate } from "react-router-dom";
import { FaSignOutAlt, FaTools } from "react-icons/fa";
import { BsFillCalendar2DateFill } from "react-icons/bs";
import { IoIosAddCircleOutline } from "react-icons/io";
import { BiSolidReport } from "react-icons/bi";
import axios from "axios";
import Select from "react-select";
import logo from "./assets/Assan_logo.png";
import "./calendarStyles.css";

import FullCalendar from "@fullcalendar/react";
import dayGridPlugin from "@fullcalendar/daygrid";
import timeGridPlugin from "@fullcalendar/timegrid";
import interactionPlugin from "@fullcalendar/interaction";
import trLocale from "@fullcalendar/core/locales/tr";

function Rezervasyon() {
  const [fullName, setFullName] = useState("");
  const [employeeId, setEmployeeId] = useState("");
  const [equipmentId, setEquipmentId] = useState("");
  const [equipmentOptions, setEquipmentOptions] = useState([]);
  const [reservations, setReservations] = useState([]);
  const [showModal, setShowModal] = useState(false);
  const [role, setRole] = useState(null);
  const colors = ["#28a745", "#007bff", "#ffc107", "#dc3545", "#6f42c1"];

  const [beginDate, setBeginDate] = useState("");
  const [endDate, setEndDate] = useState("");

  const navigate = useNavigate();
  const calendarRef = useRef(null);

  useEffect(() => {
    const name = localStorage.getItem("fullName");
    const id = localStorage.getItem("employeeId");
    const userData = JSON.parse(localStorage.getItem("user"));
    if (userData && userData.role) {
      
      setRole(userData.role.roleName); // "User" veya "Admin"
    }
    if (name) setFullName(name);
    if (id) setEmployeeId(id);

    fetch("http://localhost:8080/rezervasyon")
      .then((res) => res.json())
      .then((data) => setReservations(data))
      .catch((err) => console.error("Hata:", err));

    fetch("http://localhost:8080/equipment/active")
      .then((res) => res.json())
      .then((data) => {
        const options = data.map((eq) => ({
          value: eq.id,
          label: `${eq.serialNumber} - ${eq.equipmentName}`,
        }));
        setEquipmentOptions(options);
      })
      .catch((err) => console.error("Ekipman çekme hatası:", err));
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


  const handleSubmit = async (e) => {
    e.preventDefault();
    try {
      const response = await axios.post(
        "http://localhost:8080/rezervasyon",
        { beginDate, endDate },
        { params: { employeeId: Number(employeeId), equipmentId: Number(equipmentId) } }
      );
      alert("Rezervasyon eklendi!");
      setShowModal(false);
      setEquipmentId("");
      setBeginDate("");
      setEndDate("");
      setReservations((prev) => [...prev, response.data]);
    } catch (err) {
      if (err.response) {
        alert(`Hata: ${err.response.data.message}`);
      } else {
        alert("Sunucuya ulaşılamadı veya beklenmedik bir hata oluştu.");
      }
    }
  };

  const calendarEvents = reservations.map((res, index) => ({
    id: res.id,
    title: res.equipmentInfo || "Rezervasyon",
    start: res.beginDate,
    end: res.endDate,
    backgroundColor: colors[index % colors.length],
    borderColor: colors[index % colors.length],
    textColor: "#fff",
    extendedProps: {
      employeeName: res.employeeName,
      serialNumber: res.serialNumber,
      equipmentInfo: res.equipmentInfo,
      department: res.department,
      equipmentName: res.equipmentName,
    },
  }));

  const handleDateClick = (info) => {
    const calendarApi = calendarRef.current.getApi();
    const currentView = calendarApi.view.type;

    if (currentView === "dayGridMonth") {
      calendarApi.changeView("timeGridDay", info.date);
    }
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
            <Nav className="me-auto"></Nav>
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
        <div
          style={{
            width: "220px",
            height: "100vh",
            backgroundColor: "#f8f9faff",
            padding: "30px",
            fontSize: "1.1rem",
          }}
        >
          <Nav className="flex-column">
            <Nav.Link href="/ekipman">
              <FaTools className="me-2" /> Ekipmanlar
            </Nav.Link>
            <Nav.Link href="/rezervasyon">
              <BsFillCalendar2DateFill className="me-2" /> Rezervasyon
            </Nav.Link>
            {role?.toLowerCase() === "admin" && (
            <Nav.Link href="/rapor">
              <BiSolidReport className="me-2" /> Raporlar
            </Nav.Link>
            )}
          </Nav>
        </div>

        {/* İçerik */}
        <div style={{ flex: 1, padding: "20px" }}>
          <div className="d-flex justify-content-between align-items-center mb-3">
            <h2>Rezervasyonlar</h2>
            <Button variant="link" onClick={() => setShowModal(true)}>
              <IoIosAddCircleOutline 
                size={40} 
                color="green" 
                style={{ strokeWidth: 15 }} 
              />
            </Button>

          </div>

          {/* Takvim */}
          <div className="container mt-4">
            <FullCalendar
            ref={calendarRef}
            plugins={[dayGridPlugin, timeGridPlugin, interactionPlugin]}
            locale={trLocale}
            initialView="dayGridMonth"
            firstDay={1}
            headerToolbar={{
              left: "prev,next today",
              center: "title",
              right: "dayGridMonth,timeGridDay",
            }}
            events={calendarEvents}
            dateClick={handleDateClick}
            editable={false}
            selectable={false}
            height="80vh"
            slotMinTime="07:45:00"
            slotMaxTime="17:45:00"
            allDaySlot={false}
            slotEventOverlap={false} // Yan yana event için
            eventOverlap={false}     // Aynı saatte üst üste binmeyi engelle
            eventContent={(arg) => {
              let displayTime = arg.view.type === "dayGridMonth" ? "" : arg.timeText;
              return (
                <div style={{
                  display: "flex",
                  flexDirection: "column",
                  overflowY: "auto",
                  fontSize: "0.85rem",
                  padding: "2px 4px"
                }}>
                  <div><b>{displayTime} {arg.event.extendedProps.employeeName} </b></div>
                  <div>{arg.event.extendedProps.equipmentName} - {arg.event.extendedProps.serialNumber}</div>
                </div>
              );
            }}
          />
          </div>
        </div>
      </div>

      {/* Modal */}
      <Modal show={showModal} onHide={() => setShowModal(false)} centered>
        <Modal.Header closeButton>
          <Modal.Title>
            <IoIosAddCircleOutline /> Rezervasyon Ekleme
          </Modal.Title>
        </Modal.Header>
        <Modal.Body>
          <Form onSubmit={handleSubmit}>
            <Form.Group className="mb-3">
              <Form.Label>Ekipman Seç</Form.Label>
              <Select
                options={equipmentOptions}
                placeholder="Seri No - Ürün Adı seçiniz"
                isSearchable
                onChange={(selected) => setEquipmentId(selected.value)}
              />
            </Form.Group>

            <Row>
              <Col md={6}>
                <Form.Group className="mb-3">
                  <Form.Label>Başlangıç Tarihi</Form.Label>
                  <Form.Control
                    type="datetime-local"
                    value={beginDate}
                    onChange={(e) => setBeginDate(e.target.value)}
                  />
                </Form.Group>
              </Col>
              <Col md={6}>
                <Form.Group className="mb-3">
                  <Form.Label>Bitiş Tarihi</Form.Label>
                  <Form.Control
                    type="datetime-local"
                    value={endDate}
                    onChange={(e) => setEndDate(e.target.value)}
                  />
                </Form.Group>
              </Col>
            </Row>

            <div className="d-flex justify-content-center">
              <Button 
                variant="success" 
                type="submit" 
                className="w-25" 
                style={{ padding: "10px" }}
              >
                <IoIosAddCircleOutline size={30} />
              </Button>
            </div>
          </Form>
        </Modal.Body>
      </Modal>
    </div>
  );
}

export default Rezervasyon;
