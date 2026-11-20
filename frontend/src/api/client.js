import axios from "axios";

export const TOKEN_KEY = "inventory_token";

// Geliştirmede Vite proxy'si /api isteklerini backend'e iletir.
// Farklı bir adres için .env dosyasına VITE_API_URL yazılabilir.
const client = axios.create({
  baseURL: import.meta.env.VITE_API_URL || "/api",
});

// Her isteğe token'ı ekle
client.interceptors.request.use((config) => {
  const token = localStorage.getItem(TOKEN_KEY);
  if (token) {
    config.headers.Authorization = `Bearer ${token}`;
  }
  return config;
});

// Token süresi dolmuşsa (401) oturumu kapat
client.interceptors.response.use(
  (response) => response,
  (error) => {
    if (error.response?.status === 401 && localStorage.getItem(TOKEN_KEY)) {
      localStorage.removeItem(TOKEN_KEY);
      window.dispatchEvent(new Event("auth:expired"));
    }
    return Promise.reject(error);
  }
);

/** Backend'in döndürdüğü hata mesajını (veya genel bir mesajı) verir. */
export function getErrorMessage(error, fallback = "Bir hata oluştu") {
  if (!error.response) {
    return "Sunucuya ulaşılamıyor. Backend çalışıyor mu?";
  }
  return error.response.data?.message || fallback;
}

/** Boş filtre değerlerini istek parametrelerinden çıkarır. */
export function cleanParams(params) {
  return Object.fromEntries(
    Object.entries(params).filter(([, v]) => v !== "" && v !== null && v !== undefined && v !== false)
  );
}

/**
 * Dosya indirir (Excel / PDF). Token gerektiği için normal bir link yerine
 * axios ile indirilip tarayıcıya kaydettirilir.
 */
export async function downloadFile(url, params = {}, fallbackName = "dosya") {
  let res;
  try {
    res = await client.get(url, { params: cleanParams(params), responseType: "blob" });
  } catch (err) {
    // Hata cevabı da blob olarak gelir; mesajı okuyabilmek için JSON'a çevir
    if (err.response?.data instanceof Blob) {
      try {
        err.response.data = JSON.parse(await err.response.data.text());
      } catch {
        /* JSON değilse olduğu gibi bırak */
      }
    }
    throw err;
  }

  const disposition = res.headers["content-disposition"] || "";
  const match = /filename\*=UTF-8''([^;]+)/i.exec(disposition) || /filename="?([^";]+)"?/i.exec(disposition);
  const filename = match ? decodeURIComponent(match[1]) : fallbackName;

  const href = URL.createObjectURL(res.data);
  const link = document.createElement("a");
  link.href = href;
  link.download = filename;
  document.body.appendChild(link);
  link.click();
  link.remove();
  setTimeout(() => URL.revokeObjectURL(href), 1000);
}

export default client;
