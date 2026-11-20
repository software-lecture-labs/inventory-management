import { Pagination } from "react-bootstrap";

/**
 * Sayfalı listelerin altındaki gezinme çubuğu.
 * page 0'dan başlar (backend ile aynı).
 */
function Pager({ data, onChange }) {
  const { page, size, totalElements, totalPages } = data;
  if (totalElements === 0) return null;

  const first = page * size + 1;
  const last = Math.min((page + 1) * size, totalElements);

  // Mevcut sayfanın çevresindeki en fazla 5 sayfa numarası
  const start = Math.max(0, Math.min(page - 2, totalPages - 5));
  const pages = [];
  for (let i = start; i < Math.min(start + 5, totalPages); i++) pages.push(i);

  return (
    <div className="d-flex flex-wrap justify-content-between align-items-center gap-2 mt-2">
      <small className="text-muted">
        {totalElements} kayıttan {first}–{last} arası
      </small>
      {totalPages > 1 && (
        <Pagination size="sm" className="mb-0">
          <Pagination.First disabled={page === 0} onClick={() => onChange(0)} />
          <Pagination.Prev disabled={page === 0} onClick={() => onChange(page - 1)} />
          {pages.map((p) => (
            <Pagination.Item key={p} active={p === page} onClick={() => onChange(p)}>
              {p + 1}
            </Pagination.Item>
          ))}
          <Pagination.Next disabled={page >= totalPages - 1} onClick={() => onChange(page + 1)} />
          <Pagination.Last disabled={page >= totalPages - 1} onClick={() => onChange(totalPages - 1)} />
        </Pagination>
      )}
    </div>
  );
}

export default Pager;
