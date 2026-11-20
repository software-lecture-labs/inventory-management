package com.inventory.service;

import com.inventory.dto.EquipmentDtos.EquipmentRequest;
import com.inventory.model.Equipment;
import com.inventory.model.EquipmentStatus;
import com.inventory.model.FieldChange;
import org.junit.jupiter.api.Test;

import java.time.LocalDate;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

class EquipmentServiceDiffTest {

    private Equipment existing() {
        Equipment equipment = new Equipment();
        equipment.setName("Multimetre");
        equipment.setBrand("Fluke");
        equipment.setModel("87V");
        equipment.setSerialNumber("SN-100");
        equipment.setCategory("Ölçüm");
        equipment.setLocation(null);
        equipment.setCalibrationDate(LocalDate.of(2025, 1, 15));
        equipment.setDescription(null);
        return equipment;
    }

    @Test
    void noChangesWhenValuesAreSame() {
        EquipmentRequest request = new EquipmentRequest(
                "Multimetre", "Fluke", "87V", "SN-100", "Ölçüm", " ", LocalDate.of(2025, 1, 15), "  ",
                EquipmentStatus.AVAILABLE, false);

        assertThat(EquipmentService.diff(existing(), request)).isEmpty();
    }

    @Test
    void nullStatusAndApprovalMeanUnchanged() {
        EquipmentRequest request = new EquipmentRequest(
                "Multimetre", "Fluke", "87V", "SN-100", "Ölçüm", null, LocalDate.of(2025, 1, 15), null,
                null, null);

        assertThat(EquipmentService.diff(existing(), request)).isEmpty();
    }

    @Test
    void detectsChangedFieldsWithOldAndNewValues() {
        EquipmentRequest request = new EquipmentRequest(
                "Multimetre", "Fluke", "179", "SN-100", "Ölçüm", "Lab 2", LocalDate.of(2026, 1, 15),
                "Laboratuvar 2", EquipmentStatus.MAINTENANCE, true);

        List<FieldChange> changes = EquipmentService.diff(existing(), request);

        assertThat(changes).extracting(FieldChange::getField)
                .containsExactly("model", "location", "calibrationDate", "description", "status", "requiresApproval");
        assertThat(changes.get(0).getOldValue()).isEqualTo("87V");
        assertThat(changes.get(0).getNewValue()).isEqualTo("179");
        assertThat(changes.get(2).getNewValue()).isEqualTo("2026-01-15");
        assertThat(changes.get(3).getOldValue()).isNull();
        assertThat(changes.get(4).getNewValue()).isEqualTo("MAINTENANCE");
        assertThat(changes.get(5).getNewValue()).isEqualTo("true");
    }
}
