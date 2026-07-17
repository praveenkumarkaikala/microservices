package com.fundmatrix.foliokyc;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.cloud.openfeign.EnableFeignClients;
import org.springframework.data.jpa.repository.config.EnableJpaAuditing;
import org.springframework.scheduling.annotation.EnableScheduling;

@SpringBootApplication
@EnableFeignClients
@EnableJpaAuditing
@EnableScheduling
public class FolioKycServiceApplication {

    public static void main(String[] args) {
        SpringApplication.run(FolioKycServiceApplication.class, args);
        System.out.print("kyc holding service running on 8085");
    }
}
