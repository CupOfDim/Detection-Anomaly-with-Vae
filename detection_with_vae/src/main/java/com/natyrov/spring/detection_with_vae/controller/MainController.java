package com.natyrov.spring.detection_with_vae.controller;

import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.GetMapping;

@Controller
public class MainController {

    @GetMapping("/dashboard")
    public String dashboard(){
        return "dashboard";
    }

}
