package com.example.ResumeAnalyzerPro_Final.controller;

import com.example.ResumeAnalyzerPro_Final.entity.Analysis;
import com.example.ResumeAnalyzerPro_Final.entity.User;
import com.example.ResumeAnalyzerPro_Final.service.ResumeService;
import com.example.ResumeAnalyzerPro_Final.service.UserService;
import jakarta.servlet.http.HttpSession;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.Page;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;
import java.util.HashMap;

@Controller
@RequestMapping("/admin")
public class AdminController {

    @Autowired
    private UserService userService;

    @Autowired
    private ResumeService resumeService;

    private boolean isAdmin(HttpSession session) {
        Boolean isAdminFlag = (Boolean) session.getAttribute("admin");
        if (Boolean.TRUE.equals(isAdminFlag)) {
            return true;
        }
        User user = (User) session.getAttribute("user");
        return user != null && "ADMIN".equals(user.getRole());
    }

    private String getAdminName(HttpSession session) {
        String adminName = (String) session.getAttribute("adminName");
        if (adminName != null && !adminName.isBlank()) {
            return adminName;
        }
        User user = (User) session.getAttribute("user");
        if (user != null && "ADMIN".equals(user.getRole())) {
            return user.getName();
        }
        return "Administrator";
    }

    @GetMapping("/login")
    public String loginPage(HttpSession session, Model model) {
        if (isAdmin(session)) {
            return "redirect:/admin/dashboard";
        }

        // Check if admin account exists
        List<User> admins = userService.getAllUsers().stream()
                .filter(u -> "ADMIN".equals(u.getRole()))
                .toList();

        if (admins.isEmpty()) {
            model.addAttribute("error",
                    "No admin account found. Please register an admin account with email 'admin@admin.com' first.");
        }

        return "admin-login";
    }

    @PostMapping("/login")
    public String login(@RequestParam String email, @RequestParam String password, HttpSession session, Model model) {
        User admin = userService.loginAdmin(email, password);
        if (admin != null) {
            session.setAttribute("user", admin);
            return "redirect:/admin/dashboard";
        }

        model.addAttribute("error",
                "Invalid admin credentials. Please register an admin account with email 'admin@admin.com' first.");
        return "admin-login";
    }

    @GetMapping("/dashboard")
    public String dashboard(HttpSession session, Model model) {
        if (!isAdmin(session)) {
            return "redirect:/admin/login";
        }

        List<User> users = userService.getAllUsers();
        List<Analysis> analyses = resumeService.getAllAnalyses();

        model.addAttribute("users", users);
        model.addAttribute("analyses", analyses);
        model.addAttribute("adminName", getAdminName(session));
        model.addAttribute("admin", session.getAttribute("user"));
        return "admin-dashboard";
    }

    @GetMapping("/user/{id}")
    public String viewUserDetails(@PathVariable Long id,
            @RequestParam(value = "page", defaultValue = "0") int page,
            HttpSession session,
            Model model) {
        if (!isAdmin(session)) {
            return "redirect:/admin/login";
        }

        User selectedUser = userService.getUserById(id);
        if (selectedUser == null) {
            return "redirect:/admin/dashboard";
        }

        Page<Analysis> analysisPage = resumeService.getHistoryPage(selectedUser, page);

        List<com.example.ResumeAnalyzerPro_Final.entity.Resume> resumes = resumeService.getResumesByUser(selectedUser);
        Map<String, String> resumeContents = new HashMap<>();
        for (com.example.ResumeAnalyzerPro_Final.entity.Resume r : resumes) {
            resumeContents.put(r.getFileName(), r.getContent());
        }

        model.addAttribute("userDetails", selectedUser);
        model.addAttribute("resumes", resumes);
        model.addAttribute("analysisPage", analysisPage);
        model.addAttribute("currentPage", page);
        model.addAttribute("hasMore", analysisPage.hasNext());
        model.addAttribute("resumeContents", resumeContents);
        model.addAttribute("adminName", getAdminName(session));
        return "admin-user-details";
    }

    @GetMapping("/user/{userId}/analysis/{analysisId}")
    public String viewUserAnalysis(@PathVariable Long userId,
            @PathVariable Long analysisId,
            HttpSession session,
            Model model) {
        if (!isAdmin(session)) {
            return "redirect:/admin/login";
        }

        User selectedUser = userService.getUserById(userId);
        if (selectedUser == null) {
            return "redirect:/admin/dashboard";
        }

        Analysis analysis = resumeService.getAnalysisById(analysisId);
        if (analysis == null || analysis.getUser() == null || !analysis.getUser().getId().equals(userId)) {
            return "redirect:/admin/user/" + userId;
        }

        List<com.example.ResumeAnalyzerPro_Final.entity.Resume> resumes = resumeService.getResumesByUser(selectedUser);
        Map<String, String> resumeContents = new HashMap<>();
        for (com.example.ResumeAnalyzerPro_Final.entity.Resume r : resumes) {
            resumeContents.put(r.getFileName(), r.getContent());
        }

        model.addAttribute("userDetails", selectedUser);
        model.addAttribute("analysis", analysis);
        model.addAttribute("resumeContents", resumeContents);
        model.addAttribute("adminName", getAdminName(session));
        return "admin-analysis-details";
    }

    @PostMapping("/user/{id}/delete")
    public String deleteUser(@PathVariable Long id, HttpSession session) {
        if (!isAdmin(session)) {
            return "redirect:/admin/login";
        }

        User currentUser = (User) session.getAttribute("user");
        if (currentUser != null && currentUser.getId() != null && currentUser.getId().equals(id)) {
            return "redirect:/admin/dashboard";
        }

        userService.deleteUser(id);
        return "redirect:/admin/dashboard";
    }

    @PostMapping("/user/{id}/toggle-lock")
    public String toggleLockUser(@PathVariable Long id, HttpSession session) {
        if (!isAdmin(session)) {
            return "redirect:/admin/login";
        }

        User admin = (User) session.getAttribute("user");
        if (admin == null || admin.getId() == null || !admin.getId().equals(id)) {
            userService.toggleUserLock(id);
        }
        return "redirect:/admin/dashboard";
    }

    @GetMapping("/logout")
    public String logout(HttpSession session) {
        session.invalidate();
        return "redirect:/admin/login";
    }
}
