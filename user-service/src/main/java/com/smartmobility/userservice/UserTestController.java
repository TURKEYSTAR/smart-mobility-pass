package com.smartmobility.userservice;

import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/users")
public class UserTestController {

    @GetMapping("/ping")
    public String ping() {
        return "✅ User Service is UP and running !";
    }
}