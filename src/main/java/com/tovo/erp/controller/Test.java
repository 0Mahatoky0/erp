package com.tovo.erp.controller;

import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
public class Test {
    @GetMapping("/")
    public String welcome() {
        return "Tonga soa ianao ";
    }
}
