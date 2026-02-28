package com.smartmobility.billingservice;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.cloud.client.discovery.EnableDiscoveryClient;
import org.springframework.cloud.openfeign.EnableFeignClients;

@SpringBootApplication
@EnableDiscoveryClient
@EnableFeignClients
public class BillingServiceApplication {

    public static void main(String[] args) {
        SpringApplication.run(BillingServiceApplication.class, args);
        System.out.println("========================================");
        System.out.println("  BILLING SERVICE démarré ✅");
        System.out.println("  Port: 8084");
        System.out.println("  Eureka: http://localhost:8761");
        System.out.println("========================================");
    }

}
