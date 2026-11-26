package com.inventory;

import com.jayway.jsonpath.JsonPath;
import org.apache.poi.ss.usermodel.Row;
import org.apache.poi.ss.usermodel.Sheet;
import org.apache.poi.ss.usermodel.Workbook;
import org.apache.poi.xssf.usermodel.XSSFWorkbook;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.mock.web.MockMultipartFile;
import org.springframework.test.web.servlet.MockMvc;

import java.io.ByteArrayOutputStream;
import java.time.LocalDateTime;
import java.time.temporal.ChronoUnit;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

/**
 * Uygulamanın tamamını (güvenlik + controller + servis + H2 veritabanı) birlikte test eder.
 * Testler aynı veritabanını paylaştığı için her test kendine özgü email / seri no kullanır.
 */
@SpringBootTest
@AutoConfigureMockMvc
class ApiIntegrationTest {

    @Autowired
    private MockMvc mockMvc;


    // ---------- kimlik doğrulama ----------

    @Test
    void protectedEndpointsRequireToken() throws Exception {
        mockMvc.perform(get("/api/equipment"))
                .andExpect(status().isUnauthorized())
                .andExpect(jsonPath("$.message").value("Oturum açmanız gerekiyor"));
    }

    @Test
    void registeredUserGetsUserRoleAndCannotCreateEquipment() throws Exception {
        String token = register("ayse@test.com", "5551112233");

        mockMvc.perform(get("/api/equipment").header("Authorization", "Bearer " + token))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.content").isArray());

        mockMvc.perform(post("/api/equipment")
                        .header("Authorization", "Bearer " + token)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(equipmentJson("SN-USER", "Osiloskop", null, false)))
                .andExpect(status().isForbidden());
    }

    @Test
    void duplicateEmailIsRejected() throws Exception {
        register("mehmet@test.com", "5552223344");

        mockMvc.perform(post("/api/auth/register")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(registerJson("mehmet@test.com", "5559998877")))
                .andExpect(status().isConflict());
    }

    @Test
    void invalidRegistrationReturnsFieldErrors() throws Exception {
        mockMvc.perform(post("/api/auth/register")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"firstName":"","lastName":"Kaya","email":"gecersiz","phone":"12","password":"zayif"}
                                """))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.fieldErrors.email").exists())
                .andExpect(jsonPath("$.fieldErrors.password").exists());
    }

    @Test
    void wrongPasswordReturnsUnauthorized() throws Exception {
        mockMvc.perform(post("/api/auth/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"email":"nobody@test.com","password":"YanlisSifre1!"}
                                """))
                .andExpect(status().isUnauthorized());
    }

    @Test
    void accountIsLockedAfterRepeatedFailedLogins() throws Exception {
        register("kilit@test.com", "5550000001");

        for (int i = 1; i <= 4; i++) {
            loginRequest("kilit@test.com", "YanlisSifre1!").andExpect(status().isUnauthorized());
        }
        // 5. hatalı deneme hesabı kilitler
        loginRequest("kilit@test.com", "YanlisSifre1!")
                .andExpect(status().isTooManyRequests())
                .andExpect(jsonPath("$.message").value(org.hamcrest.Matchers.containsString("dakika")));
        // Kilitliyken doğru şifre de kabul edilmez
        loginRequest("kilit@test.com", "Sifre123!").andExpect(status().isTooManyRequests());
    }

    @Test
    void disabledUserCannotLoginAndOldTokenStopsWorking() throws Exception {
        String userToken = register("engel@test.com", "5550000002");
        Long userId = userId(userToken);
        String adminToken = adminToken();

        mockMvc.perform(patch("/api/users/" + userId + "/enabled")
                        .header("Authorization", "Bearer " + adminToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"enabled\":false}"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.enabled").value(false));

        mockMvc.perform(get("/api/auth/me").header("Authorization", "Bearer " + userToken))
                .andExpect(status().isUnauthorized());
        loginRequest("engel@test.com", "Sifre123!").andExpect(status().isForbidden());

        // Engeli kaldırınca tekrar giriş yapabilir
        mockMvc.perform(patch("/api/users/" + userId + "/enabled")
                        .header("Authorization", "Bearer " + adminToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"enabled\":true}"))
                .andExpect(status().isOk());
        loginRequest("engel@test.com", "Sifre123!").andExpect(status().isOk());
    }

    @Test
    void forgotPasswordGoesToAdminWhoSetsOneTimePassword() throws Exception {
        String oldToken = register("unuttum@test.com", "5550000003");
        Long userId = userId(oldToken);
        String adminToken = adminToken();

        // Kayıtlı olmayan email için de aynı cevap döner
        mockMvc.perform(post("/api/auth/forgot-password")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"email\":\"yok@test.com\"}"))
                .andExpect(status().isOk());

        // Kullanıcı talep gönderir (iki kez gönderse de tek talep oluşur)
        for (int i = 0; i < 2; i++) {
            mockMvc.perform(post("/api/auth/forgot-password")
                            .contentType(MediaType.APPLICATION_JSON)
                            .content("{\"email\":\"unuttum@test.com\"}"))
                    .andExpect(status().isOk());
        }

        // Talep yönetici paneline ve zile düşer
        mockMvc.perform(get("/api/users/password-requests").header("Authorization", "Bearer " + adminToken))
                .andExpect(jsonPath("$[?(@.email == 'unuttum@test.com')]").exists())
                .andExpect(jsonPath("$[?(@.email == 'unuttum@test.com')].userId").value(userId.intValue()));
        mockMvc.perform(get("/api/notifications").header("Authorization", "Bearer " + adminToken))
                .andExpect(jsonPath("$.content[?(@.type == 'PASSWORD_RESET_REQUEST')]").exists());

        // Normal kullanıcı talepleri göremez / şifre belirleyemez
        mockMvc.perform(get("/api/users/password-requests").header("Authorization", "Bearer " + oldToken))
                .andExpect(status().isForbidden());

        // Yönetici tek kullanımlık şifre belirler -> talep kapanır
        mockMvc.perform(post("/api/users/" + userId + "/temporary-password")
                        .header("Authorization", "Bearer " + adminToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"password\":\"Gecici-1234\"}"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.mustChangePassword").value(true));
        mockMvc.perform(get("/api/users/password-requests").header("Authorization", "Bearer " + adminToken))
                .andExpect(jsonPath("$[?(@.email == 'unuttum@test.com')]").doesNotExist());

        // Eski şifre artık çalışmaz; tek kullanımlık şifreyle giriş -> yeni şifre belirlemesi gerekir
        loginRequest("unuttum@test.com", "Sifre123!").andExpect(status().isUnauthorized());
        String body = loginRequest("unuttum@test.com", "Gecici-1234")
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.user.mustChangePassword").value(true))
                .andReturn().getResponse().getContentAsString();
        String tempToken = JsonPath.read(body, "$.token");

        // Yeni şifre belirlenmeden başka bir işlem yapılamaz (API seviyesinde)
        mockMvc.perform(get("/api/equipment").header("Authorization", "Bearer " + tempToken))
                .andExpect(status().isForbidden());
        mockMvc.perform(get("/api/auth/me").header("Authorization", "Bearer " + tempToken))
                .andExpect(status().isOk());

        // Yeni şifre tek kullanımlık şifreyle aynı olamaz
        mockMvc.perform(post("/api/auth/change-password")
                        .header("Authorization", "Bearer " + tempToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"newPassword\":\"Gecici-1234\"}"))
                .andExpect(status().isBadRequest());

        mockMvc.perform(post("/api/auth/change-password")
                        .header("Authorization", "Bearer " + tempToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"newPassword\":\"YeniSifre1!\"}"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.mustChangePassword").value(false));

        // Artık her şey açık; tek kullanımlık şifre bir daha kullanılamaz
        mockMvc.perform(get("/api/equipment").header("Authorization", "Bearer " + tempToken))
                .andExpect(status().isOk());
        loginRequest("unuttum@test.com", "Gecici-1234").andExpect(status().isUnauthorized());
        loginRequest("unuttum@test.com", "YeniSifre1!")
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.user.mustChangePassword").value(false));
    }

    // ---------- ekipman ----------

    @Test
    void adminCanCreateEquipmentAndItIsAudited() throws Exception {
        String adminToken = adminToken();
        createEquipment(adminToken, "SN-ADMIN", "Osiloskop", "Ölçüm", false);

        mockMvc.perform(get("/api/audit-logs").param("action", "CREATE").param("entityType", "EQUIPMENT")
                        .header("Authorization", "Bearer " + adminToken))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.content[?(@.entityName == 'Osiloskop')]").exists());
    }

    @Test
    void equipmentListIsPaginatedAndFiltered() throws Exception {
        String adminToken = adminToken();
        for (int i = 1; i <= 3; i++) {
            createEquipment(adminToken, "SN-PAGE-" + i, "Sayfa Ekipmanı " + i, "Sayfalama", false);
        }

        mockMvc.perform(get("/api/equipment")
                        .param("category", "Sayfalama").param("size", "2").param("page", "0")
                        .header("Authorization", "Bearer " + adminToken))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.totalElements").value(3))
                .andExpect(jsonPath("$.totalPages").value(2))
                .andExpect(jsonPath("$.content.length()").value(2));

        mockMvc.perform(get("/api/equipment").param("q", "sn-page-3")
                        .header("Authorization", "Bearer " + adminToken))
                .andExpect(jsonPath("$.totalElements").value(1));
    }

    @Test
    void excelImportAddsValidRowsAndReportsInvalidOnes() throws Exception {
        String adminToken = adminToken();

        mockMvc.perform(get("/api/equipment/import-template").header("Authorization", "Bearer " + adminToken))
                .andExpect(status().isOk())
                .andExpect(header().string("Content-Disposition", org.hamcrest.Matchers.containsString(".xlsx")));

        byte[] xlsx;
        try (Workbook wb = new XSSFWorkbook(); ByteArrayOutputStream out = new ByteArrayOutputStream()) {
            Sheet sheet = wb.createSheet();
            sheet.createRow(0).createCell(0).setCellValue("başlık");
            row(sheet, 1, "Terazi", "Mettler", "XS205", "SN-XLS-1", "Tartım", "Lab 1", "15.03.2031", "Kullanılabilir", "Evet");
            row(sheet, 2, "Pipet", "Eppendorf", "Research", "SN-XLS-2", "", "", "", "", "");
            row(sheet, 3, "", "Marka", "Model", "SN-XLS-3", "", "", "", "", "");          // ad eksik
            row(sheet, 4, "Pipet 2", "Eppendorf", "Research", "SN-XLS-2", "", "", "", "", ""); // dosyada tekrar
            wb.write(out);
            xlsx = out.toByteArray();
        }

        mockMvc.perform(multipart("/api/equipment/import")
                        .file(new MockMultipartFile("file", "ekipman.xlsx",
                                "application/vnd.openxmlformats-officedocument.spreadsheetml.sheet", xlsx))
                        .header("Authorization", "Bearer " + adminToken))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.created").value(2))
                .andExpect(jsonPath("$.failed").value(2))
                .andExpect(jsonPath("$.errors[0].row").value(4))
                .andExpect(jsonPath("$.errors[1].row").value(5));

        mockMvc.perform(get("/api/equipment").param("q", "SN-XLS-1").header("Authorization", "Bearer " + adminToken))
                .andExpect(jsonPath("$.content[0].requiresApproval").value(true))
                .andExpect(jsonPath("$.content[0].calibrationDate").value("2031-03-15"));

        mockMvc.perform(get("/api/equipment/export").header("Authorization", "Bearer " + adminToken))
                .andExpect(status().isOk());
    }

    @Test
    void maintenanceRecordUpdatesCalibrationAndStatus() throws Exception {
        String adminToken = adminToken();
        Long id = createEquipment(adminToken, "SN-BAKIM", "Spektrometre", null, false);

        mockMvc.perform(patch("/api/equipment/" + id + "/status")
                        .header("Authorization", "Bearer " + adminToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"status\":\"CALIBRATION\",\"note\":\"Yıllık kalibrasyon\"}"))
                .andExpect(jsonPath("$.status").value("CALIBRATION"));

        mockMvc.perform(post("/api/equipment/" + id + "/maintenance")
                        .header("Authorization", "Bearer " + adminToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"type":"CALIBRATION","performedDate":"2026-01-10","nextDueDate":"2032-01-10",
                                 "performedBy":"Kalibrasyon A.Ş.","markAvailable":true}
                                """))
                .andExpect(status().isCreated());

        mockMvc.perform(get("/api/equipment/" + id).header("Authorization", "Bearer " + adminToken))
                .andExpect(jsonPath("$.status").value("AVAILABLE"))
                .andExpect(jsonPath("$.calibrationDate").value("2032-01-10"));

        mockMvc.perform(get("/api/equipment/" + id + "/maintenance").header("Authorization", "Bearer " + adminToken))
                .andExpect(jsonPath("$.length()").value(1))
                .andExpect(jsonPath("$[0].performedBy").value("Kalibrasyon A.Ş."));
    }

    // ---------- rezervasyon ----------

    @Test
    void reservationApprovalFlowAndWaitlist() throws Exception {
        String adminToken = adminToken();
        String userToken = register("onay@test.com", "5550000004");
        String otherToken = register("bekleyen@test.com", "5550000005");
        Long equipmentId = createEquipment(adminToken, "SN-ONAY", "Mikroskop", null, true);

        LocalDateTime start = LocalDateTime.now().plusDays(20).truncatedTo(ChronoUnit.HOURS);
        String body = reservationJson(equipmentId, start, start.plusHours(2));

        String created = mockMvc.perform(post("/api/reservations")
                        .header("Authorization", "Bearer " + userToken)
                        .contentType(MediaType.APPLICATION_JSON).content(body))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.status").value("PENDING"))
                .andReturn().getResponse().getContentAsString();
        Integer reservationId = JsonPath.read(created, "$.id");

        // Aynı aralık dolu: diğer kullanıcı rezervasyon yapamaz ama bekleme listesine girebilir
        mockMvc.perform(post("/api/reservations")
                        .header("Authorization", "Bearer " + otherToken)
                        .contentType(MediaType.APPLICATION_JSON).content(body))
                .andExpect(status().isConflict());
        mockMvc.perform(post("/api/waitlist")
                        .header("Authorization", "Bearer " + otherToken)
                        .contentType(MediaType.APPLICATION_JSON).content(body))
                .andExpect(status().isCreated());

        // Normal kullanıcı onaylayamaz
        mockMvc.perform(post("/api/reservations/" + reservationId + "/approve")
                        .header("Authorization", "Bearer " + userToken))
                .andExpect(status().isForbidden());

        // Yönetici reddeder -> aralık boşalır, bekleyen kişiye haber verilir
        mockMvc.perform(post("/api/reservations/" + reservationId + "/reject")
                        .header("Authorization", "Bearer " + adminToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"note\":\"O gün laboratuvar kapalı\"}"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value("REJECTED"));

        mockMvc.perform(get("/api/waitlist").header("Authorization", "Bearer " + otherToken))
                .andExpect(jsonPath("$.length()").value(0)); // haber verildi ve listeden çıkarıldı
        mockMvc.perform(get("/api/notifications").header("Authorization", "Bearer " + otherToken))
                .andExpect(jsonPath("$.content[0].type").value("WAITLIST_SLOT_AVAILABLE"));

        // Kullanıcı kendi rezervasyonlarını sayfalı listede görür
        mockMvc.perform(get("/api/reservations/search").header("Authorization", "Bearer " + userToken))
                .andExpect(jsonPath("$.totalElements").value(1))
                .andExpect(jsonPath("$.content[0].note").value("O gün laboratuvar kapalı"));
    }

    @Test
    void ownerCancellingNotifiesWaitlistedUserByEmail() throws Exception {
        String adminToken = adminToken();
        String ownerToken = register("sahip@test.com", "5550000008");
        String waitingToken = register("sirada@test.com", "5550000009");
        Long equipmentId = createEquipment(adminToken, "SN-IPTAL", "Santrifüj", null, false);

        LocalDateTime start = LocalDateTime.now().plusDays(25).truncatedTo(ChronoUnit.HOURS);
        String body = reservationJson(equipmentId, start, start.plusHours(2));

        String created = mockMvc.perform(post("/api/reservations")
                        .header("Authorization", "Bearer " + ownerToken)
                        .contentType(MediaType.APPLICATION_JSON).content(body))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.status").value("APPROVED"))
                .andReturn().getResponse().getContentAsString();
        Integer reservationId = JsonPath.read(created, "$.id");

        mockMvc.perform(post("/api/waitlist")
                        .header("Authorization", "Bearer " + waitingToken)
                        .contentType(MediaType.APPLICATION_JSON).content(body))
                .andExpect(status().isCreated());

        // Rezervasyon sahibi kendisi iptal eder (arayüzdeki "İptal Et" düğmesi)
        mockMvc.perform(post("/api/reservations/" + reservationId + "/cancel")
                        .header("Authorization", "Bearer " + ownerToken)
                        .contentType(MediaType.APPLICATION_JSON).content("{}"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value("CANCELLED"));

        mockMvc.perform(get("/api/waitlist").header("Authorization", "Bearer " + waitingToken))
                .andExpect(jsonPath("$.length()").value(0)); // haber verildi ve listeden çıkarıldı

        // Uygulama içi bildirim: zil sayısı 1, bildirimde rezervasyon formunu dolduracak bilgiler var
        mockMvc.perform(get("/api/notifications/unread-count").header("Authorization", "Bearer " + waitingToken))
                .andExpect(jsonPath("$.count").value(1));
        String list = mockMvc.perform(get("/api/notifications").header("Authorization", "Bearer " + waitingToken))
                .andExpect(jsonPath("$.content[0].type").value("WAITLIST_SLOT_AVAILABLE"))
                .andExpect(jsonPath("$.content[0].equipmentId").value(equipmentId))
                .andExpect(jsonPath("$.content[0].startTime").isNotEmpty())
                .andExpect(jsonPath("$.content[0].read").value(false))
                .andReturn().getResponse().getContentAsString();
        Integer notificationId = JsonPath.read(list, "$.content[0].id");

        // Başkası bu bildirimi okundu yapamaz
        mockMvc.perform(post("/api/notifications/" + notificationId + "/read")
                        .header("Authorization", "Bearer " + ownerToken))
                .andExpect(status().isNotFound());

        mockMvc.perform(post("/api/notifications/" + notificationId + "/read")
                        .header("Authorization", "Bearer " + waitingToken))
                .andExpect(jsonPath("$.read").value(true));
        mockMvc.perform(get("/api/notifications/unread-count").header("Authorization", "Bearer " + waitingToken))
                .andExpect(jsonPath("$.count").value(0));

        // Bildirimdeki "Rezerve Et" ile aynı aralık rezerve edilebilir
        mockMvc.perform(post("/api/reservations")
                        .header("Authorization", "Bearer " + waitingToken)
                        .contentType(MediaType.APPLICATION_JSON).content(body))
                .andExpect(status().isCreated());
    }

    @Test
    void reservingRemovesUserFromOverlappingWaitlist() throws Exception {
        String adminToken = adminToken();
        String ownerToken = register("dolu@test.com", "5550000010");
        String waitingToken = register("kismi@test.com", "5550000011");
        Long equipmentId = createEquipment(adminToken, "SN-KISMI", "Termal Kamera", null, false);

        LocalDateTime start = LocalDateTime.now().plusDays(30).truncatedTo(ChronoUnit.HOURS);
        // 10:00-12:00 dolu; bekleyen kişi 10:00-14:00 için listeye girer
        mockMvc.perform(post("/api/reservations")
                        .header("Authorization", "Bearer " + ownerToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(reservationJson(equipmentId, start, start.plusHours(2))))
                .andExpect(status().isCreated());
        mockMvc.perform(post("/api/waitlist")
                        .header("Authorization", "Bearer " + waitingToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(reservationJson(equipmentId, start, start.plusHours(4))))
                .andExpect(status().isCreated());

        // Boş olan 12:00-14:00'ı rezerve edince bekleme kaydı silinir
        mockMvc.perform(post("/api/reservations")
                        .header("Authorization", "Bearer " + waitingToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(reservationJson(equipmentId, start.plusHours(2), start.plusHours(4))))
                .andExpect(status().isCreated());
        mockMvc.perform(get("/api/waitlist").header("Authorization", "Bearer " + waitingToken))
                .andExpect(jsonPath("$.length()").value(0));
    }

    // ---------- raporlar ----------

    @Test
    void pdfReportsAreAdminOnly() throws Exception {
        String adminToken = adminToken();
        String userToken = register("pdf@test.com", "5550000006");

        for (String report : new String[]{"reservations", "audit-logs", "equipment"}) {
            byte[] pdf = mockMvc.perform(get("/api/reports/" + report + ".pdf")
                            .header("Authorization", "Bearer " + adminToken))
                    .andExpect(status().isOk())
                    .andExpect(content().contentType("application/pdf"))
                    .andReturn().getResponse().getContentAsByteArray();
            assertThat(new String(pdf, 0, 4)).isEqualTo("%PDF");
        }

        mockMvc.perform(get("/api/reports/reservations.pdf").header("Authorization", "Bearer " + userToken))
                .andExpect(status().isForbidden());
    }

    @Test
    void dashboardIsAvailableToEveryUser() throws Exception {
        String userToken = register("panel@test.com", "5550000007");

        mockMvc.perform(get("/api/dashboard").header("Authorization", "Bearer " + userToken))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.equipmentByStatus.AVAILABLE").exists())
                .andExpect(jsonPath("$.dailyReservations.length()").value(14))
                .andExpect(jsonPath("$.pendingApprovals").value(0));
    }

    // --- yardımcı metotlar ---


    private String adminToken() throws Exception {
        return login("admin@test.com", "Admin123!");
    }

    private String register(String email, String phone) throws Exception {
        String body = mockMvc.perform(post("/api/auth/register")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(registerJson(email, phone)))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.user.role").value("USER"))
                .andReturn().getResponse().getContentAsString();
        return JsonPath.read(body, "$.token");
    }

    private Long userId(String token) throws Exception {
        String body = mockMvc.perform(get("/api/auth/me").header("Authorization", "Bearer " + token))
                .andReturn().getResponse().getContentAsString();
        return ((Number) JsonPath.read(body, "$.id")).longValue();
    }

    private org.springframework.test.web.servlet.ResultActions loginRequest(String email, String password)
            throws Exception {
        return mockMvc.perform(post("/api/auth/login")
                .contentType(MediaType.APPLICATION_JSON)
                .content("{\"email\":\"" + email + "\",\"password\":\"" + password + "\"}"));
    }

    private String login(String email, String password) throws Exception {
        String body = loginRequest(email, password)
                .andExpect(status().isOk())
                .andReturn().getResponse().getContentAsString();
        return JsonPath.read(body, "$.token");
    }

    private Long createEquipment(String adminToken, String serial, String name, String category,
                                 boolean requiresApproval) throws Exception {
        String body = mockMvc.perform(post("/api/equipment")
                        .header("Authorization", "Bearer " + adminToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(equipmentJson(serial, name, category, requiresApproval)))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.serialNumber").value(serial))
                .andExpect(jsonPath("$.createdBy").isNotEmpty())
                .andReturn().getResponse().getContentAsString();
        return ((Number) JsonPath.read(body, "$.id")).longValue();
    }

    private static void row(Sheet sheet, int index, String... values) {
        Row row = sheet.createRow(index);
        for (int i = 0; i < values.length; i++) {
            if (!values[i].isEmpty()) {
                row.createCell(i).setCellValue(values[i]);
            }
        }
    }

    private static String registerJson(String email, String phone) {
        return """
                {"firstName":"Test","lastName":"Kullanıcı","email":"%s","phone":"%s","password":"Sifre123!"}
                """.formatted(email, phone);
    }

    private static String equipmentJson(String serialNumber, String name, String category, boolean requiresApproval) {
        return """
                {"name":"%s","brand":"Rigol","model":"DS1054Z","serialNumber":"%s","calibrationDate":"2030-05-01",
                 "category":%s,"requiresApproval":%s}
                """.formatted(name, serialNumber, category == null ? "null" : "\"" + category + "\"", requiresApproval);
    }

    private static String reservationJson(Long equipmentId, LocalDateTime start, LocalDateTime end) {
        return """
                {"equipmentId":%d,"startTime":"%s","endTime":"%s"}
                """.formatted(equipmentId, start, end);
    }
}
