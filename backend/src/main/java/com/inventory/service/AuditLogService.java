package com.inventory.service;

import com.inventory.dto.AuditLogResponse;
import com.inventory.dto.PageResponse;
import com.inventory.model.AuditAction;
import com.inventory.model.AuditEntityType;
import com.inventory.model.AuditLog;
import com.inventory.model.FieldChange;
import com.inventory.repository.AuditLogRepository;
import jakarta.persistence.criteria.Predicate;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;

@Service
public class AuditLogService {

    /** PDF raporuna en fazla bu kadar kayıt yazılır. */
    public static final int REPORT_LIMIT = 5000;

    private static final Sort NEWEST_FIRST = Sort.by(Sort.Direction.DESC, "createdAt", "id");

    private final AuditLogRepository auditLogRepository;

    public AuditLogService(AuditLogRepository auditLogRepository) {
        this.auditLogRepository = auditLogRepository;
    }

    /** Arama kriterleri; boş (null) alanlar filtrelemeye katılmaz. */
    public record Filter(AuditAction action, AuditEntityType entityType, String q, LocalDate from, LocalDate to) {
    }

    @Transactional
    public void record(AuditAction action, AuditEntityType entityType, Long entityId, String entityName,
                       String performedBy, List<FieldChange> changes, String note) {
        auditLogRepository.save(new AuditLog(action, entityType, entityId, entityName, performedBy, changes, note));
    }

    @Transactional
    public void record(AuditAction action, AuditEntityType entityType, Long entityId, String entityName,
                       String performedBy, String note) {
        record(action, entityType, entityId, entityName, performedBy, List.of(), note);
    }

    @Transactional(readOnly = true)
    public PageResponse<AuditLogResponse> search(Filter filter, int page, int size) {
        Pageable pageable = PageRequest.of(page, size, NEWEST_FIRST);
        return PageResponse.from(auditLogRepository.findAll(toSpec(filter), pageable), AuditLogResponse::from);
    }

    @Transactional(readOnly = true)
    public List<AuditLogResponse> findForReport(Filter filter) {
        return auditLogRepository.findAll(toSpec(filter), PageRequest.of(0, REPORT_LIMIT, NEWEST_FIRST))
                .map(AuditLogResponse::from)
                .getContent();
    }

    private static Specification<AuditLog> toSpec(Filter f) {
        return (root, query, cb) -> {
            List<Predicate> predicates = new ArrayList<>();
            if (f.action() != null) {
                predicates.add(cb.equal(root.get("action"), f.action()));
            }
            if (f.entityType() != null) {
                predicates.add(cb.equal(root.get("entityType"), f.entityType()));
            }
            if (f.q() != null && !f.q().isBlank()) {
                String like = "%" + f.q().trim().toLowerCase(Locale.ROOT) + "%";
                predicates.add(cb.or(
                        cb.like(cb.lower(root.get("entityName")), like),
                        cb.like(cb.lower(root.get("performedBy")), like)));
            }
            if (f.from() != null) {
                predicates.add(cb.greaterThanOrEqualTo(root.get("createdAt"), f.from().atStartOfDay()));
            }
            if (f.to() != null) {
                predicates.add(cb.lessThan(root.get("createdAt"), f.to().plusDays(1).atStartOfDay()));
            }
            return cb.and(predicates.toArray(Predicate[]::new));
        };
    }
}
