package com.suitespot.controller;

import com.suitespot.entity.User;
import com.suitespot.service.UserService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.core.Authentication;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.*;

@Controller
public class AuthController {

    @Autowired
    private UserService userService;

    @GetMapping("/login")
    public String loginPage() {
        return "login";
    }

    @GetMapping("/register")
    public String registerPage(Model model) {
        model.addAttribute("roles", User.Role.values());
        return "register";
    }

    @PostMapping("/register")
    public String register(
            @RequestParam String username,
            @RequestParam String email,
            @RequestParam String fullName,
            @RequestParam String password,
            @RequestParam String confirmPassword,
            Model model) {

        // Validate inputs
        if (!password.equals(confirmPassword)) {
            model.addAttribute("error", "Passwords do not match");
            return "register";
        }

        if (userService.getUserByUsername(username).isPresent()) {
            model.addAttribute("error", "Username already exists");
            return "register";
        }

        try {
            userService.createUser(username, email, fullName, password, User.Role.RECEPTIONIST);
            model.addAttribute("success", "Registration successful. Please login.");
            return "redirect:/login";
        } catch (Exception e) {
            model.addAttribute("error", "Registration failed: " + e.getMessage());
            return "register";
        }
    }

    @GetMapping("/")
    public String redirectToDashboard(Authentication authentication) {
        // Temporarily bypass login - redirect directly to dashboard for testing
        return "redirect:/dashboard";
    }

    @GetMapping("/profile")
    public String profilePage(Authentication authentication, Model model) {
        if (authentication != null && authentication.getName() != null) {
            User user = userService.getUserByUsername(authentication.getName()).orElse(null);
            if (user != null) {
                model.addAttribute("user", user);
            } else {
                model.addAttribute("error", "User not found. Please log in again.");
            }
        } else {
            model.addAttribute("error", "You must be logged in to view your profile.");
        }
        return "profile";
    }

    @GetMapping("/about")
    public String aboutPage(Model model) {
        return "about";
    }
}
