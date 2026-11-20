package com.inventory.model;

import jakarta.persistence.Column;
import jakarta.persistence.Embeddable;

/**
 * Bir güncelleme işleminde değişen tek bir alan (eski ve yeni değeriyle).
 */
@Embeddable
public class FieldChange {

    @Column(nullable = false, length = 50)
    private String field;

    @Column(length = 500)
    private String oldValue;

    @Column(length = 500)
    private String newValue;

    protected FieldChange() {
    }

    public FieldChange(String field, String oldValue, String newValue) {
        this.field = field;
        this.oldValue = oldValue;
        this.newValue = newValue;
    }

    public String getField() { return field; }
    public String getOldValue() { return oldValue; }
    public String getNewValue() { return newValue; }
}
