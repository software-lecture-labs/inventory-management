package com.inventory.service;

import com.inventory.dto.AuditLogResponse;
import com.inventory.dto.ReservationDtos.ReservationResponse;
import com.inventory.model.AuditAction;
import com.inventory.model.AuditEntityType;
import com.inventory.model.Equipment;
import com.inventory.model.ReservationStatus;
import com.lowagie.text.*;
import com.lowagie.text.pdf.BaseFont;
import com.lowagie.text.pdf.ColumnText;
import com.lowagie.text.pdf.PdfPCell;
import com.lowagie.text.pdf.PdfPTable;
import com.lowagie.text.pdf.PdfPageEventHelper;
import com.lowagie.text.pdf.PdfWriter;
import org.springframework.stereotype.Service;

import java.awt.Color;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.time.Clock;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

/**
 * Raporların PDF çıktısı.
 * Türkçe karakterler (ğ, ş, ı, İ...) için yerleşik Helvetica yazı tipi Windows-1254 kodlamasıyla kullanılır;
 * böylece sunucuda ayrıca yazı tipi dosyası gerekmez.
 */
@Service
public class PdfReportService {

    private static final DateTimeFormatter DATE_TIME = DateTimeFormatter.ofPattern("dd.MM.yyyy HH:mm");
    private static final DateTimeFormatter DATE = DateTimeFormatter.ofPattern("dd.MM.yyyy");
    private static final Color HEADER_BG = new Color(226, 232, 240);
    private static final Color STRIPE_BG = new Color(248, 250, 252);

    public static final Map<ReservationStatus, String> RESERVATION_STATUS_LABELS = Map.of(
            ReservationStatus.PENDING, "Onay bekliyor",
            ReservationStatus.APPROVED, "Onaylandı",
            ReservationStatus.CHECKED_OUT, "Teslim alındı",
            ReservationStatus.RETURNED, "İade edildi",
            ReservationStatus.REJECTED, "Reddedildi",
            ReservationStatus.CANCELLED, "İptal edildi");

    public static final Map<AuditAction, String> ACTION_LABELS = Map.ofEntries(
            Map.entry(AuditAction.CREATE, "Ekleme"),
            Map.entry(AuditAction.UPDATE, "Güncelleme"),
            Map.entry(AuditAction.DELETE, "Silme"),
            Map.entry(AuditAction.STATUS_CHANGE, "Durum değişikliği"),
            Map.entry(AuditAction.APPROVE, "Onay"),
            Map.entry(AuditAction.REJECT, "Red"),
            Map.entry(AuditAction.CANCEL, "İptal"),
            Map.entry(AuditAction.CHECK_OUT, "Teslim alma"),
            Map.entry(AuditAction.RETURN, "İade"),
            Map.entry(AuditAction.ROLE_CHANGE, "Rol değişikliği"),
            Map.entry(AuditAction.ENABLE, "Engel kaldırma"),
            Map.entry(AuditAction.DISABLE, "Engelleme"),
            Map.entry(AuditAction.IMPORT, "İçe aktarma"));

    public static final Map<AuditEntityType, String> ENTITY_LABELS = Map.of(
            AuditEntityType.EQUIPMENT, "Ekipman",
            AuditEntityType.RESERVATION, "Rezervasyon",
            AuditEntityType.USER, "Kullanıcı",
            AuditEntityType.MAINTENANCE, "Bakım");

    private final Clock clock;
    private final Font titleFont;
    private final Font normalFont;
    private final Font boldFont;
    private final Font smallFont;

    public PdfReportService(Clock clock) {
        this.clock = clock;
        try {
            BaseFont regular = BaseFont.createFont(BaseFont.HELVETICA, "Cp1254", BaseFont.NOT_EMBEDDED);
            BaseFont bold = BaseFont.createFont(BaseFont.HELVETICA_BOLD, "Cp1254", BaseFont.NOT_EMBEDDED);
            this.titleFont = new Font(bold, 16);
            this.normalFont = new Font(regular, 8.5f);
            this.boldFont = new Font(bold, 8.5f);
            this.smallFont = new Font(regular, 8, Font.NORMAL, Color.GRAY);
        } catch (DocumentException | IOException e) {
            throw new IllegalStateException("PDF yazı tipi yüklenemedi", e);
        }
    }

    // ================= Raporlar =================

    public byte[] reservations(List<ReservationResponse> rows, List<String> filterSummary) {
        String[] headers = {"Rezerve Eden", "Ekipman", "Seri No", "Başlangıç", "Bitiş", "Durum", "Teslim", "İade"};
        float[] widths = {14, 16, 10, 11, 11, 11, 11, 11};
        List<String[]> data = new ArrayList<>();
        for (ReservationResponse r : rows) {
            String status = RESERVATION_STATUS_LABELS.get(r.status());
            if (r.overdue()) {
                status += " (gecikmiş)";
            } else if (r.returnedLate()) {
                status += " (geç iade)";
            }
            data.add(new String[]{
                    r.userFullName(), r.equipmentName(), r.serialNumber(),
                    format(r.startTime()), format(r.endTime()), status,
                    format(r.checkedOutAt()), format(r.returnedAt())});
        }
        return build("Rezervasyon Raporu", filterSummary, headers, widths, data,
                summarize(rows.stream().collect(Collectors.groupingBy(
                        r -> RESERVATION_STATUS_LABELS.get(r.status()), Collectors.counting()))));
    }

    public byte[] auditLogs(List<AuditLogResponse> rows, List<String> filterSummary) {
        String[] headers = {"Tarih", "Tür", "İşlem", "Kayıt", "İşlemi Yapan", "Ayrıntı"};
        float[] widths = {11, 9, 11, 16, 13, 40};
        List<String[]> data = new ArrayList<>();
        for (AuditLogResponse log : rows) {
            List<String> details = new ArrayList<>();
            log.changes().forEach(c -> details.add(FIELD_LABELS.getOrDefault(c.field(), c.field()) + ": "
                    + displayValue(c.field(), c.oldValue()) + " -> " + displayValue(c.field(), c.newValue())));
            if (log.note() != null) {
                details.add(log.note());
            }
            data.add(new String[]{
                    format(log.createdAt()),
                    ENTITY_LABELS.get(log.entityType()),
                    ACTION_LABELS.get(log.action()),
                    log.entityName(),
                    log.performedBy(),
                    String.join("\n", details)});
        }
        return build("İşlem Geçmişi Raporu", filterSummary, headers, widths, data,
                summarize(rows.stream().collect(Collectors.groupingBy(
                        l -> ACTION_LABELS.get(l.action()), Collectors.counting()))));
    }

    public byte[] equipment(List<Equipment> rows, List<String> filterSummary) {
        LocalDate today = LocalDate.now(clock);
        String[] headers = {"Ekipman Adı", "Marka / Model", "Seri No", "Kategori", "Konum", "Durum", "Kalibrasyon", "Onay"};
        float[] widths = {17, 17, 11, 11, 13, 10, 11, 6};
        List<String[]> data = new ArrayList<>();
        for (Equipment e : rows) {
            String calibration = e.getCalibrationDate() == null ? "-" : e.getCalibrationDate().format(DATE)
                    + (e.getCalibrationDate().isBefore(today) ? " (geçti)" : "");
            data.add(new String[]{
                    e.getName(), e.getBrand() + " " + e.getModel(), e.getSerialNumber(),
                    orDash(e.getCategory()), orDash(e.getLocation()),
                    ReservationService.STATUS_LABELS.get(e.getStatus()),
                    calibration, e.isRequiresApproval() ? "Evet" : "Hayır"});
        }
        return build("Ekipman Envanter Raporu", filterSummary, headers, widths, data,
                summarize(rows.stream().collect(Collectors.groupingBy(
                        e -> ReservationService.STATUS_LABELS.get(e.getStatus()), Collectors.counting()))));
    }

    // ================= PDF oluşturma =================

    private byte[] build(String title, List<String> filterSummary, String[] headers, float[] widths,
                         List<String[]> rows, String summary) {
        ByteArrayOutputStream out = new ByteArrayOutputStream();
        Document document = new Document(PageSize.A4.rotate(), 28, 28, 28, 36);
        try {
            PdfWriter writer = PdfWriter.getInstance(document, out);
            writer.setPageEvent(new Footer(smallFont, "Oluşturulma: " + format(LocalDateTime.now(clock))));
            document.addTitle(title);
            document.open();

            document.add(new Paragraph(title, titleFont));
            Paragraph meta = new Paragraph(8, "", smallFont);
            meta.add("Toplam " + rows.size() + " kayıt");
            if (!filterSummary.isEmpty()) {
                meta.add("  ·  Filtre: " + String.join(", ", filterSummary));
            }
            meta.setSpacingAfter(4);
            document.add(meta);
            if (!summary.isEmpty()) {
                Paragraph sum = new Paragraph(summary, smallFont);
                sum.setSpacingAfter(8);
                document.add(sum);
            }

            PdfPTable table = new PdfPTable(widths);
            table.setWidthPercentage(100);
            table.setHeaderRows(1);
            for (String header : headers) {
                PdfPCell cell = new PdfPCell(new Phrase(header, boldFont));
                cell.setBackgroundColor(HEADER_BG);
                cell.setPadding(4);
                table.addCell(cell);
            }
            if (rows.isEmpty()) {
                PdfPCell empty = new PdfPCell(new Phrase("Kayıt bulunamadı", normalFont));
                empty.setColspan(headers.length);
                empty.setHorizontalAlignment(Element.ALIGN_CENTER);
                empty.setPadding(8);
                table.addCell(empty);
            }
            for (int i = 0; i < rows.size(); i++) {
                for (String value : rows.get(i)) {
                    PdfPCell cell = new PdfPCell(new Phrase(orDash(value), normalFont));
                    cell.setPadding(3.5f);
                    if (i % 2 == 1) {
                        cell.setBackgroundColor(STRIPE_BG);
                    }
                    table.addCell(cell);
                }
            }
            document.add(table);
        } catch (DocumentException e) {
            throw new IllegalStateException("PDF oluşturulamadı", e);
        } finally {
            document.close();
        }
        return out.toByteArray();
    }

    private static String summarize(Map<String, Long> counts) {
        return counts.entrySet().stream()
                .sorted(Map.Entry.<String, Long>comparingByValue().reversed())
                .map(e -> e.getKey() + ": " + e.getValue())
                .collect(Collectors.joining("   "));
    }

    static final Map<String, String> FIELD_LABELS = Map.ofEntries(
            Map.entry("name", "Ad"),
            Map.entry("brand", "Marka"),
            Map.entry("model", "Model"),
            Map.entry("serialNumber", "Seri No"),
            Map.entry("category", "Kategori"),
            Map.entry("location", "Konum"),
            Map.entry("calibrationDate", "Kalibrasyon"),
            Map.entry("description", "Açıklama"),
            Map.entry("status", "Durum"),
            Map.entry("requiresApproval", "Onay gerekli"),
            Map.entry("role", "Rol"));

    private static String displayValue(String field, String value) {
        if (value == null) {
            return "-";
        }
        return switch (field) {
            case "status" -> {
                try {
                    yield ReservationService.STATUS_LABELS.get(com.inventory.model.EquipmentStatus.valueOf(value));
                } catch (IllegalArgumentException e) {
                    yield value;
                }
            }
            case "requiresApproval" -> "true".equals(value) ? "Evet" : "Hayır";
            case "role" -> "ADMIN".equals(value) ? "Yönetici" : "Kullanıcı";
            default -> value;
        };
    }

    private static String format(LocalDateTime time) {
        return time == null ? "-" : time.format(DATE_TIME);
    }

    private static String orDash(String value) {
        return (value == null || value.isBlank()) ? "-" : value;
    }

    /** Her sayfanın altına oluşturulma zamanı ve sayfa numarası yazar. */
    private static final class Footer extends PdfPageEventHelper {
        private final Font font;
        private final String left;

        Footer(Font font, String left) {
            this.font = font;
            this.left = left;
        }

        @Override
        public void onEndPage(PdfWriter writer, Document document) {
            float y = document.bottom() - 18;
            ColumnText.showTextAligned(writer.getDirectContent(), Element.ALIGN_LEFT,
                    new Phrase("Envanter Yönetim Sistemi  ·  " + left, font), document.left(), y, 0);
            ColumnText.showTextAligned(writer.getDirectContent(), Element.ALIGN_RIGHT,
                    new Phrase("Sayfa " + writer.getPageNumber(), font), document.right(), y, 0);
        }
    }
}
