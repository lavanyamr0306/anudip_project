package com.example.ResumeAnalyzerPro_Final.repository;

import org.springframework.data.jpa.repository.JpaRepository;
import com.example.ResumeAnalyzerPro_Final.entity.Resume;

import com.example.ResumeAnalyzerPro_Final.entity.User;

public interface ResumeRepository extends JpaRepository<Resume, Long> {

    void deleteByUser(User user);

    java.util.List<Resume> findByUser(User user);
}
