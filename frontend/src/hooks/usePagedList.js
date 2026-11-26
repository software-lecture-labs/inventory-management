import { useCallback, useEffect, useRef, useState } from "react";
import client, { cleanParams, getErrorMessage } from "../api/client";
import { useToast } from "../context/ToastContext";

const EMPTY_PAGE = { content: [], page: 0, size: 20, totalElements: 0, totalPages: 0 };

/** Değer değiştikten [delay] ms sonra güncellenen kopyasını döner (arama kutuları için). */
export function useDebounced(value, delay = 350) {
  const [debounced, setDebounced] = useState(value);
  useEffect(() => {
    const id = setTimeout(() => setDebounced(value), delay);
    return () => clearTimeout(id);
  }, [value, delay]);
  return debounced;
}

/**
 * Backend'in sayfalı uç noktalarından ({ content, page, totalPages... }) veri çeker.
 * Filtreler değişince otomatik olarak ilk sayfaya döner.
 *
 *   const { data, loading, page, setPage, reload } = usePagedList("/equipment", filters);
 */
export function usePagedList(url, filters, { size = 20, errorMessage = "Liste yüklenemedi" } = {}) {
  const { showToast } = useToast();
  const [data, setData] = useState(EMPTY_PAGE);
  const [loading, setLoading] = useState(true);
  const [page, setPage] = useState(0);
  const [reloadKey, setReloadKey] = useState(0);

  // Filtre nesnesi her render'da yeniden oluşabilir; içeriğine göre karşılaştır
  const filterKey = JSON.stringify(cleanParams(filters));
  const previousFilterKey = useRef(filterKey);

  useEffect(() => {
    // Filtre değiştiyse önce ilk sayfaya dön; bu effect page=0 ile tekrar çalışır
    if (previousFilterKey.current !== filterKey) {
      previousFilterKey.current = filterKey;
      if (page !== 0) {
        setPage(0);
        return;
      }
    }

    let cancelled = false;
    setLoading(true);
    client
      .get(url, { params: { ...JSON.parse(filterKey), page, size } })
      .then((res) => {
        if (cancelled) return;
        // Son sayfadaki tek kayıt silindiyse bir önceki sayfaya geç
        if (res.data.content.length === 0 && page > 0 && res.data.totalPages <= page) {
          setPage(Math.max(res.data.totalPages - 1, 0));
          return;
        }
        setData(res.data);
      })
      .catch((err) => !cancelled && showToast(getErrorMessage(err, errorMessage), "danger"))
      .finally(() => !cancelled && setLoading(false));

    return () => {
      cancelled = true;
    };
  }, [url, filterKey, page, size, reloadKey, showToast, errorMessage]);

  const reload = useCallback(() => setReloadKey((k) => k + 1), []);

  /** Listedeki tek bir kaydı yerinde günceller (tüm listeyi yeniden çekmeden). */
  const updateItem = useCallback((updated) => {
    setData((prev) => ({
      ...prev,
      content: prev.content.map((item) => (item.id === updated.id ? updated : item)),
    }));
  }, []);

  return { data, loading, page, setPage, reload, updateItem };
}
