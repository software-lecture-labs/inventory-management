package com.inventory.controller;

import com.inventory.dto.EquipmentDtos.EquipmentFilters;
import com.inventory.dto.EquipmentDtos.EquipmentOption;
import com.inventory.dto.EquipmentDtos.EquipmentRequest;
import com.inventory.dto.EquipmentDtos.EquipmentResponse;
import com.inventory.dto.EquipmentDtos.ImportResult;
import com.inventory.dto.EquipmentDtos.MaintenanceRequest;
import com.inventory.dto.EquipmentDtos.MaintenanceResponse;
import com.inventory.dto.EquipmentDtos.StatusUpdateRequest;
import com.inventory.dto.PageResponse;
import com.inventory.model.EquipmentStatus;
import com.inventory.security.UserPrincipal;
import com.inventory.service.EquipmentExcelService;
import com.inventory.service.EquipmentService;
import com.inventory.service.MaintenanceService;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.time.LocalDate;
import java.util.List;

@RestController
@RequestMapping("/api/equipment")
public class EquipmentController {

    private static final String XLSX = "application/vnd.openxmlformats-officedocument.spreadsheetml.sheet";

    private final EquipmentService equipmentService;
    private final EquipmentExcelService excelService;
    private final MaintenanceService maintenanceService;

    public EquipmentController(EquipmentService equipmentService,
                               EquipmentExcelService excelService,
                               MaintenanceService maintenanceService) {
        this.equipmentService = equipmentService;
        this.excelService = excelService;
        this.maintenanceService = maintenanceService;
    }

    /** Sayfalı ve filtreli ekipman listesi. */
    @GetMapping
    public PageResponse<EquipmentResponse> search(@RequestParam(required = false) String q,
                                                  @RequestParam(required = false) String category,
                                                  @RequestParam(required = false) String location,
                                                  @RequestParam(required = false) EquipmentStatus status,
                                                  @RequestParam(defaultValue = "0") int page,
                                                  @RequestParam(defaultValue = "20") int size) {
        return equipmentService.search(new EquipmentService.Filter(q, category, location, status),
                Paging.page(page), Paging.size(size));
    }

    /** Açılır listeler için tüm aktif ekipmanlar (sayfasız, hafif). */
    @GetMapping("/options")
    public List<EquipmentOption> options() {
        return equipmentService.getOptions();
    }

    /** Filtrelerde kullanılacak mevcut kategori ve konumlar. */
    @GetMapping("/filters")
    public EquipmentFilters filters() {
        return equipmentService.getFilters();
    }

    @GetMapping("/{id}")
    public EquipmentResponse getById(@PathVariable Long id) {
        return equipmentService.getById(id);
    }

    // POST / PUT / PATCH / DELETE sadece ADMIN (SecurityConfig)
    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    public EquipmentResponse create(@Valid @RequestBody EquipmentRequest request,
                                    @AuthenticationPrincipal UserPrincipal principal) {
        return equipmentService.create(request, principal.id());
    }

    @PutMapping("/{id}")
    public EquipmentResponse update(@PathVariable Long id,
                                    @Valid @RequestBody EquipmentRequest request,
                                    @AuthenticationPrincipal UserPrincipal principal) {
        return equipmentService.update(id, request, principal.id());
    }

    @PatchMapping("/{id}/status")
    public EquipmentResponse changeStatus(@PathVariable Long id,
                                          @Valid @RequestBody StatusUpdateRequest request,
                                          @AuthenticationPrincipal UserPrincipal principal) {
        return equipmentService.changeStatus(id, request, principal.id());
    }

    @DeleteMapping("/{id}")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void delete(@PathVariable Long id, @AuthenticationPrincipal UserPrincipal principal) {
        equipmentService.delete(id, principal.id());
    }

    // ---------- Bakım geçmişi ----------

    @GetMapping("/{id}/maintenance")
    public List<MaintenanceResponse> maintenanceHistory(@PathVariable Long id) {
        return maintenanceService.getHistory(id);
    }

    @PostMapping("/{id}/maintenance")
    @ResponseStatus(HttpStatus.CREATED)
    public MaintenanceResponse addMaintenance(@PathVariable Long id,
                                              @Valid @RequestBody MaintenanceRequest request,
                                              @AuthenticationPrincipal UserPrincipal principal) {
        return maintenanceService.add(id, request, principal.id());
    }

    // ---------- Excel (sadece ADMIN) ----------

    @GetMapping("/export")
    public ResponseEntity<byte[]> export(@RequestParam(required = false) String q,
                                         @RequestParam(required = false) String category,
                                         @RequestParam(required = false) String location,
                                         @RequestParam(required = false) EquipmentStatus status) {
        byte[] file = excelService.export(new EquipmentService.Filter(q, category, location, status));
        return Downloads.file(file, XLSX, "ekipmanlar-" + LocalDate.now() + ".xlsx");
    }

    @GetMapping("/import-template")
    public ResponseEntity<byte[]> importTemplate() {
        return Downloads.file(excelService.template(), XLSX, "ekipman-sablonu.xlsx");
    }

    @PostMapping(value = "/import", consumes = "multipart/form-data")
    public ImportResult importFile(@RequestParam("file") MultipartFile file,
                                   @AuthenticationPrincipal UserPrincipal principal) {
        return excelService.importFile(file, principal.id());
    }
}
