package com.inventory.service;

import com.inventory.dto.EquipmentDtos.EquipmentRequest;
import com.inventory.dto.EquipmentDtos.ImportResult;
import com.inventory.dto.EquipmentDtos.ImportResult.RowError;
import com.inventory.exception.BusinessException;
import com.inventory.model.AuditAction;
import com.inventory.model.AuditEntityType;
import com.inventory.model.Equipment;
import com.inventory.model.EquipmentStatus;
import com.inventory.model.User;
import com.inventory.repository.EquipmentRepository;
import jakarta.validation.ConstraintViolation;
import jakarta.validation.Validator;
import org.apache.poi.ss.usermodel.*;
import org.apache.poi.xssf.usermodel.XSSFWorkbook;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.time.format.DateTimeParseException;
import java.util.*;
import java.util.stream.Collectors;

/**
 * Ekipman listesini Excel'e (.xlsx) aktarır ve Excel'den toplu ekipman ekler.
 * Dışa aktarılan dosya ile içe aktarma şablonu aynı sütun düzenini kullanır.
 */
@Service
public class EquipmentExcelService {

    public static final int MAX_IMPORT_ROWS = 2000;

    static final String[] HEADERS = {
            "Ekipman Adı *", "Marka *", "Model *", "Seri No *", "Kategori", "Konum",
            "Kalibrasyon Tarihi", "Durum", "Onay Gerekli", "Açıklama"
    };

    private static final Locale TR = Locale.forLanguageTag("tr-TR");

    private static final List<DateTimeFormatter> DATE_FORMATS = List.of(
            DateTimeFormatter.ofPattern("d.M.yyyy"),
            DateTimeFormatter.ofPattern("d/M/yyyy"),
            DateTimeFormatter.ISO_LOCAL_DATE);

    private final EquipmentService equipmentService;
    private final EquipmentRepository equipmentRepository;
    private final UserService userService;
    private final AuditLogService auditLogService;
    private final Validator validator;

    public EquipmentExcelService(EquipmentService equipmentService,
                                 EquipmentRepository equipmentRepository,
                                 UserService userService,
                                 AuditLogService auditLogService,
                                 Validator validator) {
        this.equipmentService = equipmentService;
        this.equipmentRepository = equipmentRepository;
        this.userService = userService;
        this.auditLogService = auditLogService;
        this.validator = validator;
    }

    // ================= Dışa aktarma =================

    public byte[] export(EquipmentService.Filter filter) {
        try (Workbook wb = new XSSFWorkbook()) {
            Sheet sheet = wb.createSheet("Ekipmanlar");
            writeHeader(wb, sheet);

            CellStyle dateStyle = wb.createCellStyle();
            dateStyle.setDataFormat(wb.getCreationHelper().createDataFormat().getFormat("dd.mm.yyyy"));

            int rowIndex = 1;
            for (Equipment e : equipmentService.findAll(filter)) {
                Row row = sheet.createRow(rowIndex++);
                row.createCell(0).setCellValue(e.getName());
                row.createCell(1).setCellValue(e.getBrand());
                row.createCell(2).setCellValue(e.getModel());
                row.createCell(3).setCellValue(e.getSerialNumber());
                setText(row, 4, e.getCategory());
                setText(row, 5, e.getLocation());
                if (e.getCalibrationDate() != null) {
                    Cell cell = row.createCell(6);
                    cell.setCellValue(e.getCalibrationDate());
                    cell.setCellStyle(dateStyle);
                }
                row.createCell(7).setCellValue(ReservationService.STATUS_LABELS.get(e.getStatus()));
                row.createCell(8).setCellValue(e.isRequiresApproval() ? "Evet" : "Hayır");
                setText(row, 9, e.getDescription());
            }
            autosize(sheet);
            return toBytes(wb);
        } catch (IOException e) {
            throw new IllegalStateException("Excel dosyası oluşturulamadı", e);
        }
    }

    /** Boş içe aktarma şablonu (başlık satırı + açıklama sayfası). */
    public byte[] template() {
        try (Workbook wb = new XSSFWorkbook()) {
            Sheet sheet = wb.createSheet("Ekipmanlar");
            writeHeader(wb, sheet);
            autosize(sheet);

            Sheet help = wb.createSheet("Açıklamalar");
            String[][] rows = {
                    {"Sütun", "Açıklama"},
                    {"* ile işaretli sütunlar", "Zorunludur"},
                    {"Seri No", "Sistemde zaten kayıtlı olan seri numaraları atlanır"},
                    {"Kalibrasyon Tarihi", "Tarih hücresi veya GG.AA.YYYY biçiminde metin (ör. 15.03.2026)"},
                    {"Durum", "Kullanılabilir, Arızalı, Bakımda veya Kalibrasyonda (boşsa Kullanılabilir)"},
                    {"Onay Gerekli", "Evet veya Hayır (boşsa Hayır)"},
                    {"Satır sınırı", "Tek seferde en fazla " + MAX_IMPORT_ROWS + " satır"},
            };
            for (int i = 0; i < rows.length; i++) {
                Row row = help.createRow(i);
                row.createCell(0).setCellValue(rows[i][0]);
                row.createCell(1).setCellValue(rows[i][1]);
            }
            help.autoSizeColumn(0);
            help.autoSizeColumn(1);
            return toBytes(wb);
        } catch (IOException e) {
            throw new IllegalStateException("Şablon oluşturulamadı", e);
        }
    }

    // ================= İçe aktarma =================

    /**
     * Excel'deki her satırı ayrı ayrı ekler. Hatalı satırlar atlanır ve satır numarasıyla raporlanır;
     * doğru satırlar yine de kaydedilir.
     * <p>
     * Bilerek @Transactional değildir: her ekipman kendi işleminde kaydedilir,
     * bir satırdaki hata diğerlerini geri almaz.
     */
    public ImportResult importFile(MultipartFile file, Long currentUserId) {
        if (file == null || file.isEmpty()) {
            throw new BusinessException("Dosya seçilmedi");
        }
        String filename = Objects.toString(file.getOriginalFilename(), "").toLowerCase(Locale.ROOT);
        if (!filename.endsWith(".xlsx")) {
            throw new BusinessException("Sadece .xlsx uzantılı Excel dosyaları desteklenir");
        }

        User currentUser = userService.getById(currentUserId);
        List<RowError> errors = new ArrayList<>();
        Set<String> serialsInFile = new HashSet<>();
        int created = 0;

        try (InputStream in = file.getInputStream(); Workbook wb = WorkbookFactory.create(in)) {
            Sheet sheet = wb.getSheetAt(0);
            DataFormatter formatter = new DataFormatter(TR);

            if (sheet.getLastRowNum() > MAX_IMPORT_ROWS) {
                throw new BusinessException("Dosyada en fazla " + MAX_IMPORT_ROWS + " satır olabilir");
            }

            for (int i = 1; i <= sheet.getLastRowNum(); i++) {
                Row row = sheet.getRow(i);
                if (isBlank(row, formatter)) {
                    continue;
                }
                int excelRow = i + 1;
                try {
                    EquipmentRequest request = parseRow(row, formatter);
                    validate(request);

                    String serialKey = request.serialNumber().trim().toLowerCase(Locale.ROOT);
                    if (!serialsInFile.add(serialKey)) {
                        throw new BusinessException("Seri numarası dosyada birden fazla kez geçiyor");
                    }
                    if (equipmentRepository.existsBySerialNumberIgnoreCaseAndActiveTrue(request.serialNumber().trim())) {
                        throw new BusinessException("Bu seri numarası sistemde zaten kayıtlı");
                    }

                    equipmentService.createEntity(request, currentUser);
                    created++;
                } catch (BusinessException e) {
                    errors.add(new RowError(excelRow, e.getMessage()));
                }
            }
        } catch (IOException | org.apache.poi.EmptyFileException | org.apache.poi.openxml4j.exceptions.NotOfficeXmlFileException e) {
            throw new BusinessException("Dosya okunamadı. Geçerli bir Excel (.xlsx) dosyası olduğundan emin olun");
        }

        if (created > 0) {
            auditLogService.record(AuditAction.IMPORT, AuditEntityType.EQUIPMENT, 0L, "Excel içe aktarma",
                    currentUser.getFullName(),
                    created + " ekipman eklendi" + (errors.isEmpty() ? "" : ", " + errors.size() + " satır atlandı"));
        }
        return new ImportResult(created, errors.size(), errors);
    }

    private EquipmentRequest parseRow(Row row, DataFormatter f) {
        return new EquipmentRequest(
                text(row, 0, f),
                text(row, 1, f),
                text(row, 2, f),
                text(row, 3, f),
                text(row, 4, f),
                text(row, 5, f),
                parseDate(row.getCell(6), f),
                text(row, 9, f),
                parseStatus(text(row, 7, f)),
                parseYesNo(text(row, 8, f)));
    }

    private void validate(EquipmentRequest request) {
        Set<ConstraintViolation<EquipmentRequest>> violations = validator.validate(request);
        if (!violations.isEmpty()) {
            String message = violations.stream()
                    .map(ConstraintViolation::getMessage)
                    .sorted()
                    .collect(Collectors.joining(", "));
            throw new BusinessException(message);
        }
    }

    static LocalDate parseDate(Cell cell, DataFormatter f) {
        if (cell == null || cell.getCellType() == CellType.BLANK) {
            return null;
        }
        if (cell.getCellType() == CellType.NUMERIC && DateUtil.isCellDateFormatted(cell)) {
            return cell.getLocalDateTimeCellValue().toLocalDate();
        }
        String value = f.formatCellValue(cell).trim();
        if (value.isEmpty()) {
            return null;
        }
        for (DateTimeFormatter format : DATE_FORMATS) {
            try {
                return LocalDate.parse(value, format);
            } catch (DateTimeParseException ignored) {
                // sıradaki biçimi dene
            }
        }
        throw new BusinessException("Kalibrasyon tarihi anlaşılamadı: \"" + value + "\" (GG.AA.YYYY biçiminde yazın)");
    }

    static EquipmentStatus parseStatus(String value) {
        if (value == null) {
            return EquipmentStatus.AVAILABLE;
        }
        String normalized = value.trim().toLowerCase(TR);
        for (var entry : ReservationService.STATUS_LABELS.entrySet()) {
            if (entry.getValue().toLowerCase(TR).equals(normalized)
                    || entry.getKey().name().equalsIgnoreCase(value.trim())) {
                return entry.getKey();
            }
        }
        throw new BusinessException("Geçersiz durum: \"" + value + "\" (Kullanılabilir, Arızalı, Bakımda veya Kalibrasyonda)");
    }

    static Boolean parseYesNo(String value) {
        if (value == null) {
            return false;
        }
        return switch (value.trim().toLowerCase(TR)) {
            case "evet", "e", "true", "1", "var" -> true;
            case "hayır", "hayir", "h", "false", "0", "yok" -> false;
            default -> throw new BusinessException("\"Onay Gerekli\" sütunu Evet veya Hayır olmalıdır");
        };
    }

    // ================= yardımcılar =================

    private static String text(Row row, int col, DataFormatter f) {
        Cell cell = row.getCell(col);
        if (cell == null) {
            return null;
        }
        String value = f.formatCellValue(cell).trim();
        return value.isEmpty() ? null : value;
    }

    private static boolean isBlank(Row row, DataFormatter f) {
        if (row == null) {
            return true;
        }
        for (int c = 0; c < HEADERS.length; c++) {
            if (text(row, c, f) != null) {
                return false;
            }
        }
        return true;
    }

    private static void setText(Row row, int col, String value) {
        if (value != null) {
            row.createCell(col).setCellValue(value);
        }
    }

    private static void writeHeader(Workbook wb, Sheet sheet) {
        Font bold = wb.createFont();
        bold.setBold(true);
        CellStyle style = wb.createCellStyle();
        style.setFont(bold);
        style.setFillForegroundColor(IndexedColors.GREY_25_PERCENT.getIndex());
        style.setFillPattern(FillPatternType.SOLID_FOREGROUND);

        Row header = sheet.createRow(0);
        for (int i = 0; i < HEADERS.length; i++) {
            Cell cell = header.createCell(i);
            cell.setCellValue(HEADERS[i]);
            cell.setCellStyle(style);
        }
        sheet.createFreezePane(0, 1);
    }

    private static void autosize(Sheet sheet) {
        for (int i = 0; i < HEADERS.length; i++) {
            sheet.autoSizeColumn(i);
            // Çok uzun açıklamalar sütunu aşırı genişletmesin
            sheet.setColumnWidth(i, Math.min(sheet.getColumnWidth(i) + 512, 60 * 256));
        }
    }

    private static byte[] toBytes(Workbook wb) throws IOException {
        ByteArrayOutputStream out = new ByteArrayOutputStream();
        wb.write(out);
        return out.toByteArray();
    }
}
