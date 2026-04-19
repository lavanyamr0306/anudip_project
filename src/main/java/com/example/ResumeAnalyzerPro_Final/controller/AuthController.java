package com.example.ResumeAnalyzerPro_Final.controller;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.*;

import jakarta.servlet.http.HttpSession;

import com.example.ResumeAnalyzerPro_Final.entity.User;
import com.example.ResumeAnalyzerPro_Final.service.UserService;

@Controller
public class AuthController {

    @Autowired
    private UserService service;

    @GetMapping("/")
    public String home(HttpSession session) {
        return "landing";
    }

    @GetMapping("/login")
    public String login(HttpSession session) {
        if (session.getAttribute("user") != null) {
            return "redirect:/dashboard";
        }
        return "login";
    }

    @GetMapping("/register")
    public String register(HttpSession session) {
        if (session.getAttribute("user") != null) {
            return "redirect:/dashboard";
        }
        return "register";
    }

    @PostMapping("/register")
    public String register(User user, Model model) {

        try {
            service.register(user);
            model.addAttribute("success", "Registration successful. Please log in.");
            return "login";
        } catch (RuntimeException ex) {
            model.addAttribute("error", ex.getMessage());
            model.addAttribute("user", user);
            return "register";
        }

    }

    @PostMapping("/login")
    public String login(String email, String password,
            HttpSession session,
            Model model) {

        User user = service.login(email, password);

        if (user == null) {

            model.addAttribute("error", "Invalid email or password");

            return "login";

        }

        if (user.isLocked()) {
            model.addAttribute("error", "Your account has been suspended by an administrator.");
            return "login";
        }

        session.setAttribute("user", user);

        if ("ADMIN".equals(user.getRole())) {
            return "redirect:/admin/dashboard";
        }

        return "redirect:/dashboard";

    }

    @GetMapping("/logout")
    public String logout(HttpSession session) {
        session.invalidate();
        return "redirect:/login";
    }

}
