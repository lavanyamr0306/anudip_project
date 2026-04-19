package com.example.ResumeAnalyzerPro_Final.service;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import jakarta.annotation.PostConstruct;
import jakarta.transaction.Transactional;
import java.util.List;

import com.example.ResumeAnalyzerPro_Final.repository.UserRepository;
import com.example.ResumeAnalyzerPro_Final.repository.AnalysisRepository;
import com.example.ResumeAnalyzerPro_Final.repository.ResumeRepository;
import com.example.ResumeAnalyzerPro_Final.entity.User;

@Service
public class UserService {

    @Autowired
    private UserRepository repo;

    @Autowired
    private AnalysisRepository analysisRepo;

    @Autowired
    private ResumeRepository resumeRepo;

    public List<User> getAllUsers() {
        return repo.findAll();
    }

    public User loginAdmin(String email, String password) {
        String normalizedEmail = normalizeEmail(email);
        String normalizedPassword = normalizePassword(password);
        if (normalizedEmail == null || normalizedPassword == null) {
            return null;
        }

        User user = repo.findFirstByEmailIgnoreCaseAndRole(normalizedEmail, "ADMIN");
        if (user == null) {
            user = repo.findFirstByEmailIgnoreCase(normalizedEmail);
            if (user == null) {
                return null;
            }
            String role = user.getRole();
            if (role == null || !role.trim().equalsIgnoreCase("ADMIN")) {
                return null;
            }
        }
        return user.getPassword().equals(normalizedPassword) ? user : null;
    }

    public User getUserById(Long id) {
        return repo.findById(id).orElse(null);
    }

    @Transactional
    public void deleteUser(Long id) {
        User user = repo.findById(id).orElse(null);
        if (user != null) {
            analysisRepo.deleteByUser(user);
            resumeRepo.deleteByUser(user);
            repo.delete(user);
        }
    }

    @Transactional
    public void toggleUserLock(Long id) {
        User user = repo.findById(id).orElse(null);
        if (user != null) {
            user.setLocked(!user.isLocked());
            repo.save(user); // Triggers update
        }
    }

    private String normalizeEmail(String email) {
        if (email == null) {
            return null;
        }
        return email.trim().toLowerCase();
    }

    private String normalizePassword(String password) {
        if (password == null) {
            return null;
        }
        return password.trim();
    }

    public User register(User user) {
        if (user.getName() == null || user.getName().isBlank()) {
            throw new RuntimeException("Name is required.");
        }
        if (user.getEmail() == null || user.getEmail().isBlank()) {
            throw new RuntimeException("Email is required.");
        }
        if (user.getPassword() == null || user.getPassword().isBlank()) {
            throw new RuntimeException("Password is required.");
        }

        String normalizedEmail = normalizeEmail(user.getEmail());
        String normalizedName = user.getName().trim();
        String normalizedPassword = normalizePassword(user.getPassword());

        User existingUser = repo.findFirstByEmailIgnoreCase(normalizedEmail);
        if (existingUser != null) {
            throw new RuntimeException("An account with this email already exists.");
        }

        user.setEmail(normalizedEmail);
        user.setName(normalizedName);
        user.setPassword(normalizedPassword);
        if ("admin@admin.com".equals(normalizedEmail)) {
            user.setRole("ADMIN");
        } else {
            user.setRole("USER");
        }
        return repo.save(user);
    }

    public User login(String email, String password) {
        String normalizedEmail = normalizeEmail(email);
        String normalizedPassword = normalizePassword(password);
        if (normalizedEmail == null || normalizedPassword == null) {
            return null;
        }

        User user = repo.findFirstByEmailIgnoreCase(normalizedEmail);
        if (user == null) {
            return null;
        }
        if (user.getPassword().equals(normalizedPassword)) {
            return user;
        }
        return null;
    }
}
