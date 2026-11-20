package com.inventory.model;

/** Audit log kaydının hangi tür kayıtla ilgili olduğu. */
public enum AuditEntityType {
    EQUIPMENT,
    RESERVATION,
    USER,
    MAINTENANCE
}
