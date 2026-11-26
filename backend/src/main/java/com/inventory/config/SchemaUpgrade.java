package com.inventory.config;

import jakarta.persistence.EntityManagerFactory;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.InitializingBean;
import org.springframework.context.annotation.DependsOn;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Component;

import javax.sql.DataSource;
import java.sql.Connection;
import java.sql.SQLException;

/**
 * Önceki sürümle oluşturulmuş PostgreSQL veritabanlarını yeni sürüme uyumlu hale getirir.
 * <p>
 * Hibernate (ddl-auto=update) yeni tablo ve kolonları kendisi ekler, fakat enum kolonlarındaki
 * mevcut CHECK kısıtlarını güncellemez. Örneğin audit_logs.action kolonu eskiden sadece
 * CREATE/UPDATE/DELETE kabul ediyordu. Yeni değer eklenen enum kolonlarının eski kısıtları kaldırılır;
 * değer kontrolünü uygulamadaki enum zaten yapar.
 */
@Component
@DependsOn("entityManagerFactory")
public class SchemaUpgrade implements InitializingBean {

    private static final Logger log = LoggerFactory.getLogger(SchemaUpgrade.class);

    private final DataSource dataSource;

    public SchemaUpgrade(DataSource dataSource, EntityManagerFactory ignored) {
        this.dataSource = dataSource;
    }

    @Override
    public void afterPropertiesSet() {
        try (Connection connection = dataSource.getConnection()) {
            String product = connection.getMetaData().getDatabaseProductName();
            if (!"PostgreSQL".equalsIgnoreCase(product)) {
                return;
            }
        } catch (SQLException e) {
            log.warn("Veritabanı türü belirlenemedi, şema yükseltmesi atlandı: {}", e.getMessage());
            return;
        }

        JdbcTemplate jdbc = new JdbcTemplate(dataSource);
        // Değer listesi zamanla büyüyen enum kolonları
        dropCheck(jdbc, "audit_logs", "audit_logs_action_check");
        dropCheck(jdbc, "notifications", "notifications_type_check");
    }

    private static void dropCheck(JdbcTemplate jdbc, String table, String constraint) {
        try {
            jdbc.execute("ALTER TABLE IF EXISTS " + table + " DROP CONSTRAINT IF EXISTS " + constraint);
        } catch (RuntimeException e) {
            log.warn("{} kısıtı kaldırılamadı: {}", constraint, e.getMessage());
        }
    }
}
