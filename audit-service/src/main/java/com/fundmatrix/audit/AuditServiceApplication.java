package com.fundmatrix.audit;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.data.jpa.repository.config.EnableJpaAuditing;

/**
 * audit-service is a pure producer: every other business service calls POST /audit/logs via
 * its own Feign client to record an audit trail entry. This service issues no outbound Feign
 * calls of its own, so no @EnableFeignClients is needed here.
 */
@SpringBootApplication
@EnableJpaAuditing
public class AuditServiceApplication {

    public static void main(String[] args) {
        SpringApplication.run(AuditServiceApplication.class, args);
        System.out.print("audit service running");
    }
}
