import React, { useEffect, useState } from "react";
import {Navbar,Container,Nav,Button,Badge,Modal,Form,Row,Col,} from "react-bootstrap";
import { useNavigate } from "react-router-dom";
import { FaSignOutAlt, FaTools } from "react-icons/fa";
import { BsFillCalendar2DateFill } from "react-icons/bs";
import { CiEdit } from "react-icons/ci";
import { MdDelete } from "react-icons/md";
import { BiSolidReport, BiSolidSave } from "react-icons/bi";
import { IoIosAddCircleOutline } from "react-icons/io";
import logo from "./assets/Assan_logo.png";
import Table from "react-bootstrap/Table";

function Ekipman() {
  const [fullName, setFullName] = useState("");
  const [id, setEmployeeId] = useState("");
  const [equipments, setEquipments] = useState([]);
  const navigate = useNavigate();
  const [role, setRole] = useState(null);

  useEffect(() => {
    const name = localStorage.getItem("fullName");
    const id = localStorage.getItem("employeeId");

    const userData = JSON.parse(localStorage.getItem("user"));
    if (userData && userData.role) {
      
      setRole(userData.role.roleName); // "User" veya "Admin"
    }
    if (name) setFullName(name);
    if (id) setEmployeeId(id);

    // ekipmanları çek
    fetch("http://localhost:8080/equipment/active", {
      method: "GET",
      headers: { "Content-Type": "application/json" },
    })
      .then((res) => res.json())
      .then((data) => setEquipments(data))
      .catch((err) => console.error("Hata:", err));
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


  // -----------------------------
  // EKLEME MODALI STATE’LERİ
  // -----------------------------
  const [show, setShow] = useState(false);
  const handleClose = () => setShow(false);
  const handleShow = () => setShow(true);

  const [brand, setBrand] = useState("");
  const [model, setModel] = useState("");
  const [name, setName] = useState("");
  const [serialNumber, setSerialNumber] = useState("");
  const [description, setDescription] = useState("");
  const [calibrationDate, setCalibrationDate] = useState("");

  const handleSubmit = async (e) => {
  e.preventDefault();

  const newEquipment = {
    brand,
    model,
    equipmentName: name,
    serialNumber,
    description,
    calibrationDate,
  };

  const employeeId = localStorage.getItem("employeeId"); // giriş yapan kullanıcı

  try {
    const response = await fetch(`http://localhost:8080/equipment?employeeId=${employeeId}`, {
      method: "POST",
      headers: { "Content-Type": "application/json" },
      body: JSON.stringify(newEquipment),
    });

    if (response.ok) {
      const addedEquipment = await response.json();
      alert("Ekipman başarıyla eklendi!");
      setEquipments([...equipments, addedEquipment]);
      handleClose();
      setBrand("");
      setModel("");
      setName("");
      setSerialNumber("");
      setDescription("");
      setCalibrationDate("");
    } else {
      const errMsg = await response.text();
      alert("Hata: " + errMsg);
    }
  } catch (err) {
    console.error(err);
    alert("Backend’e ulaşılamıyor. Lütfen server açık mı kontrol et.");
  }
};


  // -----------------------------
  // SİLME
  // -----------------------------
  const handleDelete = async (id) => {
    if (!window.confirm("Bu ekipmanı silmek istediğinize emin misiniz?")) return;

    try {
      const response = await fetch(`http://localhost:8080/equipment/${id}`, {
        method: "DELETE",
      });

      if (response.ok) {
        setEquipments(equipments.filter((eq) => eq.id !== id));
        alert("Ekipman silindi!");
      } else {
        alert("Silme işlemi başarısız!");
      }
    } catch (err) {
      console.error(err);
      alert("Silme sırasında hata oluştu!");
    }
  };

  // -----------------------------
  // DÜZENLEME MODALI
  // -----------------------------
  const [editShow, setEditShow] = useState(false);
  const [editEquipment, setEditEquipment] = useState(null);

  const handleEdit = (equipment) => {
    setEditEquipment({ ...equipment });
    setEditShow(true);
  };

  const handleEditClose = () => {
    setEditShow(false);
    setEditEquipment(null);
  };

  const handleEditSave = async () => {
    try {
      const response = await fetch(
        `http://localhost:8080/equipment/${editEquipment.id}`,
        {
          method: "PUT",
          headers: { "Content-Type": "application/json" },
          body: JSON.stringify(editEquipment),
        }
      );

      if (response.ok) {
        const updated = await response.json();
        setEquipments((prev) =>
          prev.map((eq) => (eq.id === updated.id ? updated : eq))
        );
        alert("Ekipman başarıyla güncellendi!");
        handleEditClose();
      } else {
        alert("Güncelleme başarısız!");
      }
    } catch (err) {
      console.error(err);
      alert("Backend’e ulaşılamıyor.");
    }
  };

  return (
    <div>
      {/* Üst Navbar */}
      <Navbar bg="light" expand="lg">
        <Container fluid>
          <div className="d-flex align-items-center ms-3">
            <img
              src={logo}
              width="150"
              height="45"
              className="me-2"
              alt="Logo"
            />
          </div>
          <Navbar.Toggle aria-controls="navbarScroll" />
          <Navbar.Collapse id="navbarScroll">
            <Nav
              className="me-auto my-2 my-lg-0"
              style={{ maxHeight: "100px" }}
              navbarScroll
            ></Nav>
            <div
              style={{ display: "flex", alignItems: "center", gap: "15px" }}
            >
              <h5 style={{ margin: 0 }}>Hoşgeldin {fullName}</h5>
              <Button variant="secondary" onClick={handleLogout}>
                <FaSignOutAlt className="me-1" size={20}/> Çıkış Yap
              </Button>
            </div>
          </Navbar.Collapse>
        </Container>
      </Navbar>

      {/* Sidebar ve İçerik */}
      <div className="d-flex">
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

        <div style={{ flex: 1, padding: "20px" }}>
          <div className="d-flex justify-content-between align-items-center mb-3">
            <h2>Ekipmanlar</h2>
            {role?.toLowerCase() === "admin" && (
              <Button 
                variant="success" 
                onClick={handleShow} 
              >
                <IoIosAddCircleOutline size={25} style={{ strokeWidth: 30 }}/>
              </Button>
            )}

            

            {/* EKLEME MODALI */}
            <Modal show={show} onHide={handleClose} centered>
              <Modal.Header closeButton>
                <Modal.Title>
                  <IoIosAddCircleOutline /> Ekipman Ekleme
                </Modal.Title>
              </Modal.Header>
              <Modal.Body>
                <Form onSubmit={handleSubmit}>
                  <Row>
                    <Col>
                      <Form.Group className="mb-3">
                        <Form.Label>Marka</Form.Label>
                        <Form.Control
                          type="text"
                          placeholder="Markayı giriniz"
                          value={brand}
                          onChange={(e) => setBrand(e.target.value)}
                          required
                        />
                      </Form.Group>
                    </Col>
                    <Col>
                      <Form.Group className="mb-3">
                        <Form.Label>Model</Form.Label>
                        <Form.Control
                          type="text"
                          placeholder="Modeli giriniz"
                          value={model}
                          onChange={(e) => setModel(e.target.value)}
                          required
                        />
                      </Form.Group>
                    </Col>
                  </Row>

                  <Row>
                    <Col md={15}>
                      <Form.Group className="mb-3">
                        <Form.Label>Ekipman Adı</Form.Label>
                        <Form.Control
                          type="text"
                          placeholder="Ekipman adını giriniz"
                          value={name}
                          onChange={(e) => setName(e.target.value)}
                          required
                        />
                      </Form.Group>
                    </Col>
                    <Col>
                      <Form.Group className="mb-3">
                        <Form.Label>Seri No</Form.Label>
                        <Form.Control
                          type="text"
                          placeholder="Seri numarasını giriniz"
                          value={serialNumber}
                          onChange={(e) => setSerialNumber(e.target.value)}
                          required
                        />
                      </Form.Group>
                    </Col>
                  </Row>

                  <Form.Group className="mb-3">
                    <Form.Label>Kalibrasyon Tarihi</Form.Label>
                    <Form.Control
                      type="date"
                      value={calibrationDate}
                      onChange={(e) => setCalibrationDate(e.target.value)}
                      required
                    />
                  </Form.Group>

                  <Form.Group className="mb-3">
                    <Form.Label>Açıklama</Form.Label>
                    <Form.Control
                      as="textarea"
                      rows={3}
                      placeholder="Açıklamanızı giriniz..."
                      value={description}
                      onChange={(e) => setDescription(e.target.value)}
                    />
                  </Form.Group>

                  <div className="d-flex justify-content-center">
                    <Button 
                      variant="success" 
                      type="submit" 
                      className="w-50" 
                      style={{ padding: "10px" }}
                    >
                      <IoIosAddCircleOutline size={30} />
                    </Button>
                  </div>
                </Form>
              </Modal.Body>
              <Modal.Footer>
              </Modal.Footer>
            </Modal>
          </div>

          {/* EKİPMAN TABLOSU */}
          <Table striped bordered hover>
            <thead>
              <tr>
                <th>ID</th>
                <th>Marka</th>
                <th>Model</th>
                <th>Ekipman Adı</th>
                <th>Seri No</th>
                <th>Açıklama</th>
                <th>Kalibrasyon Tarihi</th>
                {role?.toLowerCase() === "admin" && <th>İşlemler</th>}
              </tr>
            </thead>

            <tbody>
  {equipments.map((data) => (
    <tr key={data.id}>
      <td>{data.id}</td>
      <td>{data.brand}</td>
      <td>{data.model}</td>
      <td>{data.equipmentName}</td>
      <td>{data.serialNumber}</td>
      <td>{data.description}</td>
      <td>{data.calibrationDate}</td>
      
      {role?.toLowerCase() === "admin" && (
        <td>
          <div style={{ display: "flex", alignItems: "center", gap: "5px" }}>
            <Button 
              variant="link" 
              style={{ padding: "5px" }} 
              onClick={() => handleEdit(data)}
            >
              <CiEdit size={25} color="blue"/>
            </Button>
            <Button 
              variant="link" 
              style={{ padding: "5px" }} 
              onClick={() => handleDelete(data.id)}
            >
              <MdDelete size={25} color="red"/>
            </Button>
          </div>
        </td>
      )}
    </tr>
  ))}
</tbody>

          </Table>

          {/* DÜZENLEME MODALI */}
          <Modal show={editShow} onHide={handleEditClose} centered>
            <Modal.Header closeButton>
              <Modal.Title>
                <CiEdit /> Ekipman Düzenle
              </Modal.Title>
            </Modal.Header>
            <Modal.Body>
              {editEquipment && (
                <Form>
                  <Row>
                    <Col>
                      <Form.Group className="mb-3">
                        <Form.Label>Marka</Form.Label>
                        <Form.Control
                          type="text"
                          value={editEquipment.brand}
                          onChange={(e) =>
                            setEditEquipment({
                              ...editEquipment,
                              brand: e.target.value,
                            })
                          }
                        />
                      </Form.Group>
                    </Col>
                    <Col>
                      <Form.Group className="mb-3">
                        <Form.Label>Model</Form.Label>
                        <Form.Control
                          type="text"
                          value={editEquipment.model}
                          onChange={(e) =>
                            setEditEquipment({
                              ...editEquipment,
                              model: e.target.value,
                            })
                          }
                        />
                      </Form.Group>
                    </Col>
                  </Row>

                  <Row>
                    <Col>
                      <Form.Group className="mb-3">
                        <Form.Label>Ekipman Adı</Form.Label>
                        <Form.Control
                          type="text"
                          value={editEquipment.equipmentName}
                          onChange={(e) =>
                            setEditEquipment({
                              ...editEquipment,
                              equipmentName: e.target.value,
                            })
                          }
                        />
                      </Form.Group>
                    </Col>
                    <Col>
                      <Form.Group className="mb-3">
                        <Form.Label>Seri No</Form.Label>
                        <Form.Control
                          type="text"
                          value={editEquipment.serialNumber}
                          onChange={(e) =>
                            setEditEquipment({
                              ...editEquipment,
                              serialNumber: e.target.value,
                            })
                          }
                        />
                      </Form.Group>
                    </Col>
                  </Row>

                  <Form.Group className="mb-3">
                    <Form.Label>Kalibrasyon Tarihi</Form.Label>
                    <Form.Control
                      type="date"
                      value={editEquipment.calibrationDate}
                      onChange={(e) =>
                        setEditEquipment({
                          ...editEquipment,
                          calibrationDate: e.target.value,
                        })
                      }
                    />
                  </Form.Group>

                  <Form.Group className="mb-3">
                    <Form.Label>Açıklama</Form.Label>
                    <Form.Control
                      as="textarea"
                      rows={3}
                      value={editEquipment.description}
                      onChange={(e) =>
                        setEditEquipment({
                          ...editEquipment,
                          description: e.target.value,
                        })
                      }
                    />
                  </Form.Group>
                </Form>
              )}
            </Modal.Body>
            <Modal.Footer>
              <Button 
                variant="link" 
                style={{ padding: "5px" }}
                onClick={handleEditSave}
              >
                <BiSolidSave size={40}/>
              </Button>
            </Modal.Footer>
          </Modal>
        </div>
      </div>
    </div>
  );
}

export default Ekipman;