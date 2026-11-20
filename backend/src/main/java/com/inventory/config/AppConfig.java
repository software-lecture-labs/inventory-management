package com.inventory.config;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.scheduling.annotation.EnableScheduling;

import java.time.Clock;

@Configuration
@EnableScheduling // Hatırlatma ve kalibrasyon bildirimleri (ScheduledJobs)
public class AppConfig {

    /**
     * "Şu an" bilgisini tek yerden sağlar; testlerde sabit bir zamanla değiştirilebilir.
     */
    @Bean
    public Clock clock() {
        return Clock.systemDefaultZone();
    }
}
