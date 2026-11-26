import { useEffect, useId, useMemo, useRef, useState } from "react";
import { Form } from "react-bootstrap";
import { EQUIPMENT_STATUS } from "../utils/format";

const normalize = (text) => (text || "").toLocaleLowerCase("tr");

const labelFor = (eq) => (eq ? `${eq.name} — ${eq.serialNumber}` : "");

/**
 * Yazarak aranabilen ekipman seçici.
 * Ad veya seri numarasıyla arar; birden fazla kelime yazılırsa hepsi eşleşmelidir.
 * Kullanılabilir olmayan ekipmanlar listede görünür ama seçilemez.
 * Klavye: ↑ / ↓ ile gezin, Enter ile seç, Esc ile kapat.
 *
 *   <EquipmentPicker options={[...]} value="12" onChange={(id) => ...} />
 */
function EquipmentPicker({ options, value, onChange, isInvalid, placeholder = "Ekipman adı veya seri no yazın" }) {
  const listId = useId();
  const wrapperRef = useRef(null);
  const listRef = useRef(null);
  const selected = options.find((eq) => String(eq.id) === String(value));

  const [text, setText] = useState(labelFor(selected));
  const [open, setOpen] = useState(false);
  const [active, setActive] = useState(-1);

  // Seçim dışarıdan değişirse (ör. takvimden ekipman ön seçimi) kutudaki yazıyı güncelle
  useEffect(() => {
    if (!open) setText(labelFor(selected));
  }, [selected, open]);

  // Kutudaki yazı seçili ekipmanın adıysa tüm listeyi göster, değilse filtrele
  const query = text === labelFor(selected) ? "" : text;
  const matches = useMemo(() => {
    const words = normalize(query).split(/\s+/).filter(Boolean);
    if (words.length === 0) return options;
    return options.filter((eq) => {
      const haystack = normalize(`${eq.name} ${eq.serialNumber}`);
      return words.every((w) => haystack.includes(w));
    });
  }, [options, query]);

  // Dışarı tıklanınca kapat ve yazıyı seçili ekipmana geri döndür
  useEffect(() => {
    if (!open) return undefined;
    const onClick = (e) => {
      if (!wrapperRef.current?.contains(e.target)) close();
    };
    document.addEventListener("mousedown", onClick);
    return () => document.removeEventListener("mousedown", onClick);
  });

  // Klavyeyle gezinirken aktif satırı görünür tut
  useEffect(() => {
    if (active >= 0) listRef.current?.children[active]?.scrollIntoView({ block: "nearest" });
  }, [active]);

  const close = () => {
    setOpen(false);
    setActive(-1);
    setText(labelFor(selected));
  };

  const choose = (eq) => {
    if (!eq || eq.status !== "AVAILABLE") return;
    onChange(String(eq.id));
    setText(labelFor(eq));
    setOpen(false);
    setActive(-1);
  };

  const move = (step) => {
    if (matches.length === 0) return;
    let next = active;
    // Seçilemeyen (kullanılamaz) satırları atla
    for (let i = 0; i < matches.length; i++) {
      next = (next + step + matches.length) % matches.length;
      if (matches[next].status === "AVAILABLE") break;
    }
    setActive(next);
  };

  const handleKeyDown = (e) => {
    if (e.key === "ArrowDown") {
      e.preventDefault();
      if (!open) setOpen(true);
      move(1);
    } else if (e.key === "ArrowUp") {
      e.preventDefault();
      if (!open) setOpen(true);
      move(-1);
    } else if (e.key === "Enter") {
      if (open) {
        // Form gönderilmesin, önce seçim yapılsın
        e.preventDefault();
        const available = matches.filter((m) => m.status === "AVAILABLE");
        choose(active >= 0 ? matches[active] : available.length === 1 ? available[0] : null);
      }
    } else if (e.key === "Escape" && open) {
      e.preventDefault();
      e.stopPropagation(); // Modalın kapanmasını engelle
      close();
    } else if (e.key === "Tab" && open) {
      close();
    }
  };

  return (
    <div className="picker" ref={wrapperRef}>
      <Form.Control
        role="combobox"
        aria-expanded={open}
        aria-controls={listId}
        aria-autocomplete="list"
        aria-activedescendant={open && active >= 0 ? `${listId}-${active}` : undefined}
        value={text}
        placeholder={placeholder}
        isInvalid={isInvalid}
        autoComplete="off"
        onFocus={(e) => {
          e.target.select();
          setOpen(true);
        }}
        onClick={() => setOpen(true)}
        onChange={(e) => {
          setText(e.target.value);
          setOpen(true);
          setActive(-1);
          // Yazı değişince önceki seçim geçersiz olur
          if (value) onChange("");
        }}
        onKeyDown={handleKeyDown}
      />
      {open && (
        <ul className="picker-list" id={listId} role="listbox" ref={listRef}>
          {matches.length === 0 && <li className="picker-empty">Eşleşen ekipman yok</li>}
          {matches.map((eq, i) => {
            const disabled = eq.status !== "AVAILABLE";
            return (
              <li
                key={eq.id}
                id={`${listId}-${i}`}
                role="option"
                aria-selected={String(eq.id) === String(value)}
                aria-disabled={disabled}
                className={[
                  "picker-option",
                  i === active ? "active" : "",
                  disabled ? "disabled" : "",
                  String(eq.id) === String(value) ? "selected" : "",
                ].join(" ")}
                // mousedown: input odağını kaybetmeden önce seçimi yap
                onMouseDown={(e) => {
                  e.preventDefault();
                  choose(eq);
                }}
                onMouseEnter={() => !disabled && setActive(i)}
              >
                <span>
                  {eq.name} <span className="text-muted">— {eq.serialNumber}</span>
                </span>
                <span className="picker-meta">
                  {disabled && EQUIPMENT_STATUS[eq.status]?.label}
                  {!disabled && eq.requiresApproval && "onay gerekir"}
                </span>
              </li>
            );
          })}
        </ul>
      )}
    </div>
  );
}

export default EquipmentPicker;
