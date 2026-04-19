package com.example.ResumeAnalyzerPro_Final.repository;

import org.springframework.data.jpa.repository.JpaRepository;
import com.example.ResumeAnalyzerPro_Final.entity.User;

public interface UserRepository extends JpaRepository<User, Long> {

    User findFirstByEmail(String email);

    User findFirstByEmailIgnoreCase(String email);

    User findFirstByEmailIgnoreCaseAndRole(String email, String role);

}
