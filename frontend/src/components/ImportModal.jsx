import { useEffect, useState } from "react";
import { Alert, Button, Form, Modal, Spinner, Table } from "react-bootstrap";
import { FaDownload } from "react-icons/fa";
import client, { downloadFile, getErrorMessage } from "../api/client";
import { useToast } from "../context/ToastContext";

/** Excel'den toplu ekipman ekleme. Hatalı satırlar satır numarasıyla listelenir. */
function ImportModal({ show, onHide, onImported }) {
  const { showToast } = useToast();
  const [file, setFile] = useState(null);
  const [uploading, setUploading] = useState(false);
  const [result, setResult] = useState(null);

  useEffect(() => {
    if (show) {
      setFile(null);
      setResult(null);
    }
  }, [show]);

  const handleSubmit = async (e) => {
    e.preventDefault();
    if (!file) return;
    setUploading(true);
    const formData = new FormData();
    formData.append("file", file);
    try {
      const res = await client.post("/equipment/import", formData);
      setResult(res.data);
      if (res.data.created > 0) {
        showToast(`${res.data.created} ekipman eklendi`);
        onImported();
      }
    } catch (err) {
      showToast(getErrorMessage(err, "Dosya içe aktarılamadı"), "danger");
    } finally {
      setUploading(false);
    }
  };

  const downloadTemplate = async () => {
    try {
      await downloadFile("/equipment/import-template", {}, "ekipman-sablonu.xlsx");
    } catch (err) {
      showToast(getErrorMessage(err, "Şablon indirilemedi"), "danger");
    }
  };

  return (
    <Modal show={show} onHide={onHide} centered>
      <Form onSubmit={handleSubmit}>
        <Modal.Header closeButton>
          <Modal.Title>Excel'den Ekipman Ekle</Modal.Title>
        </Modal.Header>
        <Modal.Body>
          <p className="small text-muted">
            Şablondaki sütun düzenini kullanın. Dışa aktarılan Excel dosyası da aynı düzendedir. Sistemde zaten kayıtlı
            seri numaraları atlanır; hatalı satırlar eklenmez, diğerleri eklenir.
          </p>
          <Button variant="link" className="p-0 mb-3" onClick={downloadTemplate}>
            <FaDownload className="me-1" /> Boş şablonu indir
          </Button>

          <Form.Group>
            <Form.Control
              type="file"
              accept=".xlsx"
              onChange={(e) => {
                setFile(e.target.files[0] || null);
                setResult(null);
              }}
              required
            />
          </Form.Group>

          {result && (
            <div className="mt-3">
              <Alert variant={result.failed === 0 ? "success" : "warning"} className="py-2">
                {result.created} ekipman eklendi
                {result.failed > 0 && `, ${result.failed} satır atlandı`}.
              </Alert>
              {result.errors.length > 0 && (
                <div style={{ maxHeight: 220, overflowY: "auto" }}>
                  <Table size="sm" bordered className="mb-0">
                    <thead>
                      <tr>
                        <th style={{ width: 70 }}>Satır</th>
                        <th>Hata</th>
                      </tr>
                    </thead>
                    <tbody>
                      {result.errors.map((e) => (
                        <tr key={e.row}>
                          <td>{e.row}</td>
                          <td>{e.message}</td>
                        </tr>
                      ))}
                    </tbody>
                  </Table>
                </div>
              )}
            </div>
          )}
        </Modal.Body>
        <Modal.Footer>
          <Button variant="secondary" onClick={onHide}>
            Kapat
          </Button>
          <Button type="submit" variant="success" disabled={!file || uploading}>
            {uploading ? <Spinner size="sm" animation="border" /> : "İçe Aktar"}
          </Button>
        </Modal.Footer>
      </Form>
    </Modal>
  );
}

export default ImportModal;
