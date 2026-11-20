const dateTimeFormatter = new Intl.DateTimeFormat("tr-TR", {
  dateStyle: "short",
  timeStyle: "short",
});

const dateFormatter = new Intl.DateTimeFormat("tr-TR", { dateStyle: "short" });

export function formatDateTime(value) {
  return value ? dateTimeFormatter.format(new Date(value)) : "-";
}

export function formatDate(value) {
  // "2026-05-01" gibi tarihleri saat dilimi kaymasından korumak için yerel tarih olarak oku
  return value ? dateFormatter.format(new Date(`${value}T00:00:00`)) : "-";
}

/** Date nesnesini <input type="datetime-local"> formatına çevirir: "2026-05-01T14:30" */
export function toDateTimeLocal(date) {
  const pad = (n) => String(n).padStart(2, "0");
  return (
    `${date.getFullYear()}-${pad(date.getMonth() + 1)}-${pad(date.getDate())}` +
    `T${pad(date.getHours())}:${pad(date.getMinutes())}`
  );
}

/** Date nesnesini backend'in beklediği yerel ISO formatına çevirir: "2026-05-01T14:30:00" */
export function toLocalIso(date) {
  return `${toDateTimeLocal(date)}:00`;
}

/** Bugünün tarihi "2026-05-01" biçiminde (yerel saat). */
export function todayIso() {
  return toDateTimeLocal(new Date()).slice(0, 10);
}

// ---------- Ekipman ----------

export const EQUIPMENT_STATUS = {
  AVAILABLE: { label: "Kullanılabilir", variant: "success" },
  OUT_OF_ORDER: { label: "Arızalı", variant: "danger" },
  MAINTENANCE: { label: "Bakımda", variant: "warning" },
  CALIBRATION: { label: "Kalibrasyonda", variant: "info" },
};

export const MAINTENANCE_TYPES = {
  MAINTENANCE: "Bakım",
  CALIBRATION: "Kalibrasyon",
  REPAIR: "Onarım",
};

// ---------- Rezervasyon ----------

export const RESERVATION_STATUS = {
  PENDING: { label: "Onay bekliyor", variant: "warning" },
  APPROVED: { label: "Onaylandı", variant: "primary" },
  CHECKED_OUT: { label: "Teslim alındı", variant: "info" },
  RETURNED: { label: "İade edildi", variant: "success" },
  REJECTED: { label: "Reddedildi", variant: "danger" },
  CANCELLED: { label: "İptal edildi", variant: "secondary" },
};

// ---------- Audit log ----------

export const AUDIT_ACTIONS = {
  CREATE: { label: "Ekleme", variant: "success" },
  UPDATE: { label: "Güncelleme", variant: "primary" },
  DELETE: { label: "Silme", variant: "danger" },
  STATUS_CHANGE: { label: "Durum değişikliği", variant: "warning" },
  APPROVE: { label: "Onay", variant: "success" },
  REJECT: { label: "Red", variant: "danger" },
  CANCEL: { label: "İptal", variant: "secondary" },
  CHECK_OUT: { label: "Teslim alma", variant: "info" },
  RETURN: { label: "İade", variant: "info" },
  ROLE_CHANGE: { label: "Rol değişikliği", variant: "primary" },
  ENABLE: { label: "Engel kaldırma", variant: "success" },
  DISABLE: { label: "Engelleme", variant: "danger" },
  IMPORT: { label: "İçe aktarma", variant: "dark" },
  PASSWORD_RESET: { label: "Şifre sıfırlama", variant: "warning" },
};

export const ENTITY_TYPES = {
  EQUIPMENT: "Ekipman",
  RESERVATION: "Rezervasyon",
  USER: "Kullanıcı",
  MAINTENANCE: "Bakım",
};

// Değişiklik kayıtlarındaki alanların ekranda gösterilecek Türkçe adları
export const FIELD_LABELS = {
  name: "Ekipman Adı",
  brand: "Marka",
  model: "Model",
  serialNumber: "Seri No",
  category: "Kategori",
  location: "Konum",
  calibrationDate: "Kalibrasyon Tarihi",
  description: "Açıklama",
  status: "Durum",
  requiresApproval: "Onay Gerekli",
  role: "Rol",
};

/** Değişiklik kaydındaki ham değeri okunur hale getirir. */
export function formatFieldValue(field, value) {
  if (value === null || value === undefined) return null;
  switch (field) {
    case "status":
      return EQUIPMENT_STATUS[value]?.label ?? value;
    case "requiresApproval":
      return value === "true" ? "Evet" : "Hayır";
    case "role":
      return value === "ADMIN" ? "Yönetici" : "Kullanıcı";
    case "calibrationDate":
      return formatDate(value);
    default:
      return value;
  }
}
