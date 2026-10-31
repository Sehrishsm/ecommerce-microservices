package com.ecommerce.notification.controller;

import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
public class NotificationController {
    @GetMapping("/notifications/ping")
    public String ping() {
        return "Hello from Notification Service!";
    }
}