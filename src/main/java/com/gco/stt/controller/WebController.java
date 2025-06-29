package com.gco.stt.controller;

import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.GetMapping;

@Controller
public class WebController {
    
    @GetMapping("/")
    public String index() {
        return "recorder";
    }
    
    @GetMapping("/recognizer")
    public String recognizer() {
        return "recognizer";
    }
}