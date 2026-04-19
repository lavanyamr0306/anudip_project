package com.example.ResumeAnalyzerPro_Final.repository;

import com.example.ResumeAnalyzerPro_Final.entity.Analysis;
import com.example.ResumeAnalyzerPro_Final.entity.User;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;

public interface AnalysisRepository extends JpaRepository<Analysis, Long> {

    List<Analysis> findByUserOrderByCreatedAtDesc(User user);

    Page<Analysis> findByUserOrderByCreatedAtDesc(User user, Pageable pageable);

    Optional<Analysis> findFirstByUserOrderByCreatedAtDesc(User user);

    Optional<Analysis> findByIdAndUser(Long id, User user);

    long countByUser(User user);

    void deleteByUser(User user);
}
