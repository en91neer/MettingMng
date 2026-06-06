package com.example.demo.controller;

import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;

@Controller
public class ForwardController {

    @GetMapping("/{path:[^\\.]*}")
    public String redirectGet() {
        return "forward:/index.html";
    }
    
    @PostMapping("/{path:[^\\.]*}")
    public String redirectPost() {
        return "forward:/index.html";
    }
}