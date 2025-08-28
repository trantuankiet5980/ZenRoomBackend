package vn.edu.iuh.fit.controllers;

import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
public class TestController {
    @GetMapping("/public/ping")
    public String publicPing() {
        return "public ok";
    }

    @GetMapping("/tenant/hello")
    public String tenantHello() {
        return "hello tenant (or admin)";
    }

    @GetMapping("/landlord/hello")
    public String landlordHello() {
        return "hello landlord (or admin)";
    }

    @GetMapping("/admin/hello")
    public String adminHello() {
        return "hello admin";
    }
}
