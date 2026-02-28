package com.smartmobility.notificationservice;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.cloud.client.discovery.EnableDiscoveryClient;

@SpringBootApplication
@EnableDiscoveryClient
public class NotificationServiceApplication {

    public static void main(String[] args) {
        SpringApplication.run(NotificationServiceApplication.class, args);
        System.out.println("========================================");
        System.out.println("  NOTIFICATION SERVICE démarré ✅");
        System.out.println("  Port: 8085");
        System.out.println("  Eureka: http://localhost:8761");
        System.out.println("  RabbitMQ: localhost:5672");
        System.out.println("========================================");
    }
}