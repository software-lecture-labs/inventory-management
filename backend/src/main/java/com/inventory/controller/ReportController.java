package com.inventory.controller;

import com.inventory.model.AuditAction;
import com.inventory.model.AuditEntityType;
import com.inventory.model.EquipmentStatus;
import com.inventory.model.ReservationStatus;
import com.inventory.service.AuditLogService;
import com.inventory.service.EquipmentService;
import com.inventory.service.PdfReportService;
import com.inventory.service.ReservationService;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.List;

/**
 * PDF raporları (sadece yönetici - SecurityConfig). Filtreler liste ekranlarıyla aynıdır.
 */
@RestController
@RequestMapping("/api/reports")
public class ReportController {

    private static final String PDF = "application/pdf";
    private static final DateTimeFormatter DATE = DateTimeFormatter.ofPattern("dd.MM.yyyy");

    private final PdfReportService pdfReportService;
    private final ReservationService reservationService;
    private final AuditLogService auditLogService;
    private final EquipmentService equipmentService;

    public ReportController(PdfReportService pdfReportService,
                            ReservationService reservationService,
                            AuditLogService auditLogService,
                            EquipmentService equipmentService) {
        this.pdfReportService = pdfReportService;
        this.reservationService = reservationService;
        this.auditLogService = auditLogService;
        this.equipmentService = equipmentService;
    }

    @GetMapping("/reservations.pdf")
    public ResponseEntity<byte[]> reservations(
            @RequestParam(required = false) String user,
            @RequestParam(required = false) String equipment,
            @RequestParam(required = false) ReservationStatus status,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate from,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate to,
            @RequestParam(defaultValue = "false") boolean overdue) {
        var filter = new ReservationService.Filter(null, user, equipment, status, from, to, overdue);

        List<String> summary = new ArrayList<>();
        addIf(summary, "Kişi", user);
        addIf(summary, "Ekipman", equipment);
        if (status != null) summary.add("Durum: " + PdfReportService.RESERVATION_STATUS_LABELS.get(status));
        addDateRange(summary, from, to);
        if (overdue) summary.add("Sadece gecikmiş iadeler");

        byte[] pdf = pdfReportService.reservations(reservationService.findForReport(filter), summary);
        return Downloads.file(pdf, PDF, "rezervasyon-raporu-" + LocalDate.now() + ".pdf");
    }

    @GetMapping("/audit-logs.pdf")
    public ResponseEntity<byte[]> auditLogs(
            @RequestParam(required = false) AuditAction action,
            @RequestParam(required = false) AuditEntityType entityType,
            @RequestParam(required = false) String q,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate from,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate to) {
        var filter = new AuditLogService.Filter(action, entityType, q, from, to);

        List<String> summary = new ArrayList<>();
        if (entityType != null) summary.add("Tür: " + PdfReportService.ENTITY_LABELS.get(entityType));
        if (action != null) summary.add("İşlem: " + PdfReportService.ACTION_LABELS.get(action));
        addIf(summary, "Arama", q);
        addDateRange(summary, from, to);

        byte[] pdf = pdfReportService.auditLogs(auditLogService.findForReport(filter), summary);
        return Downloads.file(pdf, PDF, "islem-gecmisi-" + LocalDate.now() + ".pdf");
    }

    @GetMapping("/equipment.pdf")
    public ResponseEntity<byte[]> equipment(@RequestParam(required = false) String q,
                                            @RequestParam(required = false) String category,
                                            @RequestParam(required = false) String location,
                                            @RequestParam(required = false) EquipmentStatus status) {
        var filter = new EquipmentService.Filter(q, category, location, status);

        List<String> summary = new ArrayList<>();
        addIf(summary, "Arama", q);
        addIf(summary, "Kategori", category);
        addIf(summary, "Konum", location);
        if (status != null) summary.add("Durum: " + ReservationService.STATUS_LABELS.get(status));

        byte[] pdf = pdfReportService.equipment(equipmentService.findAll(filter), summary);
        return Downloads.file(pdf, PDF, "ekipman-envanteri-" + LocalDate.now() + ".pdf");
    }

    private static void addIf(List<String> summary, String label, String value) {
        if (value != null && !value.isBlank()) {
            summary.add(label + ": " + value.trim());
        }
    }

    private static void addDateRange(List<String> summary, LocalDate from, LocalDate to) {
        if (from != null || to != null) {
            summary.add("Tarih: " + (from != null ? from.format(DATE) : "…") + " - " + (to != null ? to.format(DATE) : "…"));
        }
    }
}
