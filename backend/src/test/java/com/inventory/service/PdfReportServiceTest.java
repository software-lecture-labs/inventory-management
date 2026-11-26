package com.inventory.service;

import com.inventory.model.Equipment;
import com.inventory.model.EquipmentStatus;
import com.inventory.model.User;
import com.lowagie.text.pdf.PdfReader;
import com.lowagie.text.pdf.parser.PdfTextExtractor;
import org.junit.jupiter.api.Test;

import java.time.Clock;
import java.time.LocalDate;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

class PdfReportServiceTest {

    @Test
    void turkishCharactersSurviveInPdf() throws Exception {
        User user = new User();
        user.setFirstName("Şükrü");
        user.setLastName("Işık");

        Equipment e = new Equipment();
        e.setName("Ğıda Ölçüm Cihazı İç");
        e.setBrand("Çağ");
        e.setModel("ŞÖÜ-1");
        e.setSerialNumber("SN-ğüşiöç");
        e.setCategory("Ölçüm");
        e.setLocation("Laboratuvar Üç");
        e.setCalibrationDate(LocalDate.of(2030, 1, 1));
        e.setStatus(EquipmentStatus.OUT_OF_ORDER);
        e.setCreatedBy(user);

        byte[] pdf = new PdfReportService(Clock.systemDefaultZone())
                .equipment(List.of(e), List.of("Kategori: Ölçüm"));

        PdfReader reader = new PdfReader(pdf);
        String text = new PdfTextExtractor(reader).getTextFromPage(1);
        reader.close();

        assertThat(text)
                .contains("Ekipman Envanter Raporu")
                .contains("Ğıda Ölçüm Cihazı İç")
                .contains("SN-ğüşiöç")
                .contains("Arızalı");
    }
}
