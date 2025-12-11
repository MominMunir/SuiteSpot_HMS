package com.suitespot.controller;

import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.GetMapping;

@Controller
public class RedirectController {

    @GetMapping("/checkin")
    public String redirectToCheckIn() {
        return "redirect:/checkin-checkout/checkin";
    }

    @GetMapping("/checkout")
    public String redirectToCheckOut() {
        return "redirect:/checkin-checkout/checkout";
    }
}

