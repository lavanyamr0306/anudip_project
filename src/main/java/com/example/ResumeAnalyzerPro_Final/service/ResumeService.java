package com.example.ResumeAnalyzerPro_Final.service;

import com.example.ResumeAnalyzerPro_Final.entity.Analysis;
import com.example.ResumeAnalyzerPro_Final.entity.Resume;
import com.example.ResumeAnalyzerPro_Final.entity.User;
import com.example.ResumeAnalyzerPro_Final.repository.AnalysisRepository;
import com.example.ResumeAnalyzerPro_Final.repository.ResumeRepository;
import com.example.ResumeAnalyzerPro_Final.util.TikaParser;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;

@Service
public class ResumeService {

    public static final int HISTORY_PAGE_SIZE = 10;

    @Autowired
    private ResumeRepository repo;

    @Autowired
    private TikaParser parser;

    @Autowired
    private AnalysisRepository analysisRepository;

    // Core skills (for basic resume score)
    private final List<String> coreSkills = Arrays.asList(
            "java", "python", "mysql", "html", "css");

    // Normalize text (remove punctuation)
    private String normalize(String text) {
        if (text == null)
            return "";

        return text.toLowerCase()
                .replaceAll("[^a-z0-9+#. ]", " ") // remove punctuation
                .replaceAll("\\s+", " ") // remove extra spaces
                .trim();
    }

    private boolean containsWord(String text, String skill) {
        if (text == null || skill == null)
            return false;

        String normalizedText = " " + text + " ";
        String normalizedSkill = skill.toLowerCase();

        // handle plural (api vs apis)
        if (normalizedText.contains(" " + normalizedSkill + " ") ||
                normalizedText.contains(" " + normalizedSkill + "s ")) {
            return true;
        }

        return false;
    }

    // All skills (for matching and recommendations)
    private final List<String> allSkills = loadSkills();

    public List<String> loadSkills() {

        try (java.io.InputStream is = getClass().getClassLoader().getResourceAsStream("skills.txt")) {

            if (is == null) {
                return Arrays.asList("java", "python", "mysql", "html", "css");
            }

            try (java.io.BufferedReader reader = new java.io.BufferedReader(new java.io.InputStreamReader(is))) {

                return reader.lines()
                        .map(String::trim)
                        .filter(line -> !line.isBlank())
                        .map(String::toLowerCase)
                        .collect(Collectors.toList());
            }

        } catch (Exception e) {
            return Arrays.asList("java", "python", "mysql", "html", "css");
        }
    }

    // FILE UPLOAD
    public Resume upload(MultipartFile file, User user) {
        try {
            String content = parser.parse(file.getInputStream());
            if (content == null || content.trim().isEmpty()) {
                return null; // No content extracted
            }

            int score = scoreResume(content);

            Resume resume = new Resume();
            resume.setFileName(file.getOriginalFilename());
            resume.setContent(content);
            resume.setScore(score);
            resume.setUser(user);

            return repo.save(resume);

        } catch (Exception e) {
            System.err.println("Resume upload error: " + e.getMessage());
            return null;
        }
    }

    public Resume saveTextResume(String resumeContent, User user) {
        if (resumeContent == null || resumeContent.isBlank()) {
            return null;
        }

        Resume resume = new Resume();
        resume.setFileName("pasted-resume.txt");
        resume.setContent(resumeContent);
        resume.setScore(scoreResume(resumeContent));
        resume.setUser(user);
        return repo.save(resume);
    }

    // RESUME SCORE
    public int scoreResume(String text) {
        if (text == null || text.isBlank()) {
            return 0;
        }

        int score = 0;
        String lower = text.toLowerCase();

        for (String skill : coreSkills) {
            if (lower.contains(skill)) {
                score += 10;
            }
        }

        return score;
    }

    // JOB FIT SCORE
    public int calculateJobFit(String resumeText, String jobDesc) {
        if (resumeText == null || jobDesc == null || resumeText.isBlank() || jobDesc.isBlank()) {
            return 0;
        }

        String resume = resumeText.toLowerCase();
        String jd = jobDesc.toLowerCase();

        int requiredSkills = 0;
        int matchedSkills = 0;

        for (String skill : allSkills) {
            if (jd.contains(skill)) {
                requiredSkills++;
                if (resume.contains(skill)) {
                    matchedSkills++;
                }
            }
        }

        if (requiredSkills == 0) {
            return 0;
        }

        return (matchedSkills * 100) / requiredSkills;
    }

    // MATCHED SKILLS
    public List<String> findMatchedSkills(String resumeText, String jobDesc) {

        String resume = normalize(resumeText);
        String jd = normalize(jobDesc);

        return allSkills.stream()
                .filter(skill -> containsWord(jd, skill))
                .filter(skill -> containsWord(resume, skill))
                .map(this::titleCase)
                .collect(Collectors.toList());
    }

    // MISSING SKILLS
    public List<String> findMissingSkills(String resumeText, String jobDesc) {
        if (resumeText == null || jobDesc == null) {
            return List.of();
        }

        String resume = normalize(resumeText);
        String jd = normalize(jobDesc);

        return allSkills.stream()
                .filter(skill -> containsWord(jd, skill))
                .filter(skill -> !containsWord(resume, skill))
                .map(this::titleCase)
                .collect(Collectors.toList());
    }

    // FEEDBACK
    public String buildFeedback(int score) {
        if (score >= 50) {
            return "Strong resume match. Your content already includes many of the tracked technical skills.";
        }
        if (score >= 30) {
            return "Good start. Add more project details, tools, and measurable impact to improve the score.";
        }
        return "Your resume needs more relevant skill keywords and stronger project descriptions.";
    }

    public int calculateKeywordMatchScore(String resumeText, String jobDesc) {
        Set<String> jobKeywords = tokenizeKeywords(jobDesc);
        if (jobKeywords.isEmpty()) {
            return 0;
        }

        Set<String> resumeKeywords = tokenizeKeywords(resumeText);
        long matched = jobKeywords.stream().filter(resumeKeywords::contains).count();
        return (int) Math.round((matched * 100.0) / jobKeywords.size());
    }

    public int calculateTrackedSkillsScore(String resumeText, String jobDesc) {
        List<String> requiredSkills = allSkills.stream()
                .filter(skill -> safeLower(jobDesc).contains(skill))
                .toList();

        if (requiredSkills.isEmpty()) {
            return 0;
        }

        long matched = requiredSkills.stream()
                .filter(skill -> safeLower(resumeText).contains(skill))
                .count();

        return (int) Math.round((matched * 100.0) / requiredSkills.size());
    }

    public String generateInsight(int score) {

        if (score >= 80) {
            return "Excellent Alignment - Your resume strongly matches the job requirements.";
        } else if (score >= 50) {
            return "Moderate Alignment - Your resume partially matches the job requirements.";
        } else {
            return "Low Alignment - Your resume needs improvement for this role.";
        }
    }

    public int calculateCompletenessScore(String resumeText) {
        String text = resumeText == null ? "" : resumeText.trim();
        if (text.isEmpty()) {
            return 0;
        }

        int wordCount = text.split("\\s+").length;
        return Math.min(100, (int) Math.round((wordCount * 100.0) / 300.0));
    }

    public int calculateFinalScore(int keywordMatchScore, int trackedSkillsScore, int completenessScore) {
        return (int) Math.round((keywordMatchScore + trackedSkillsScore + completenessScore) / 3.0);
    }

    public String deriveStatus(int finalScore) {
        if (finalScore >= 80) {
            return "Highly Matched";
        }
        if (finalScore >= 50) {
            return "Moderate Match";
        }
        return "Low Match";
    }

    public List<String> recommendJobs(String resumeText, String jobDesc) {

        String jdLower = safeLower(jobDesc);

        // 🔥 Extract ONLY JD skills
        Set<String> jdSkills = allSkills.stream()
                .filter(jdLower::contains)
                .collect(Collectors.toSet());

        List<String> roles = new ArrayList<>();

        if (hasAll(jdSkills, "java", "spring")) {
            roles.add("Backend Java Developer");
        }

        if (hasAll(jdSkills, "java", "html", "css")) {
            roles.add("Full Stack Developer");
        }

        if (jdSkills.contains("python")) {
            roles.add("Python Developer");
        }

        if (jdSkills.contains("python") &&
                (jdSkills.contains("pandas") || jdSkills.contains("numpy") || jdSkills.contains("machine learning"))) {
            roles.add("Data Scientist");
        }

        if (hasAll(jdSkills, "aws", "docker")) {
            roles.add("Cloud Application Developer");
        }

        if (jdSkills.contains("testing") || jdSkills.contains("selenium") || jdSkills.contains("junit")) {
            roles.add("QA Automation Engineer");
        }

        if (jdSkills.contains("cybersecurity") || jdSkills.contains("network security")
                || jdSkills.contains("security")) {
            roles.add("Security Engineer");
        }

        if (jdSkills.contains("cybersecurity") || jdSkills.contains("network security")) {
            roles.add("Network Security Specialist");
        }

        if (roles.isEmpty() && (jdSkills.contains("java") || jdSkills.contains("python"))) {
            roles.add("Software Engineer");
        }

        return roles.stream().distinct().toList();
    }

    public Analysis saveAnalysis(User user,
            int finalScore,
            int keywordMatchScore,
            int skillMatchScore,
            int completenessScore,
            List<String> matchedSkills,
            List<String> missingSkills,
            List<String> recommendedJobs,
            String fileName,
            String jobDescription) {

        Analysis analysis = new Analysis();
        analysis.setUser(user);
        analysis.setFileName(fileName);
        analysis.setFinalScore(finalScore);
        analysis.setKeywordMatchScore(keywordMatchScore);
        analysis.setSkillMatchScore(skillMatchScore);
        analysis.setCompletenessScore(completenessScore);
        analysis.setStatus(deriveStatus(finalScore));
        analysis.setInsight(generateInsight(finalScore));
        analysis.setJobDescription(jobDescription);
        analysis.setMatchedSkills(matchedSkills == null ? List.of() : matchedSkills);
        analysis.setMissingSkills(missingSkills == null ? List.of() : missingSkills);
        analysis.setRecommendedJobs(recommendedJobs == null ? List.of() : recommendedJobs);
        return analysisRepository.save(analysis);
    }

    public List<Analysis> getHistory(User user) {
        return analysisRepository.findByUserOrderByCreatedAtDesc(user);
    }

    public Page<Analysis> getHistoryPage(User user, int page) {
        int safePage = Math.max(page, 0);
        Pageable pageable = PageRequest.of(safePage, HISTORY_PAGE_SIZE, Sort.by(Sort.Direction.DESC, "createdAt"));
        return analysisRepository.findByUserOrderByCreatedAtDesc(user, pageable);
    }

    public List<Analysis> getLatestAnalyses(User user, int limit) {
        int safeLimit = Math.max(limit, 1);
        Pageable pageable = PageRequest.of(0, safeLimit, Sort.by(Sort.Direction.DESC, "createdAt"));
        return analysisRepository.findByUserOrderByCreatedAtDesc(user, pageable).getContent();
    }

    public Analysis getLatestAnalysis(User user) {
        return analysisRepository.findFirstByUserOrderByCreatedAtDesc(user).orElse(null);
    }

    public long getTotalAnalyses(User user) {
        return analysisRepository.countByUser(user);
    }

    public Analysis getAnalysisByIdForUser(Long id, User user) {
        return analysisRepository.findByIdAndUser(id, user).orElse(null);
    }

    public Analysis getAnalysisById(Long id) {
        return analysisRepository.findById(id).orElse(null);
    }

    public List<Analysis> getAllAnalyses() {
        return analysisRepository.findAll();
    }

    public java.util.List<Resume> getResumesByUser(User user) {
        return repo.findByUser(user);
    }

    private Set<String> tokenizeKeywords(String text) {
        if (text == null || text.isBlank()) {
            return Set.of();
        }

        Set<String> stopWords = Set.of(
                "the", "and", "for", "with", "your", "you", "are", "this", "that", "have", "from",
                "our", "will", "all", "but", "into", "been", "their", "them", "who", "was", "were",
                "to", "of", "in", "on", "at", "a", "an", "or", "as", "by", "is", "it", "be");

        return Arrays.stream(text.toLowerCase().split("[^a-z0-9+#.]+"))
                .filter(token -> token.length() > 2)
                .filter(token -> !stopWords.contains(token))
                .collect(Collectors.toCollection(LinkedHashSet::new));
    }

    private String safeLower(String text) {
        return text == null ? "" : text.toLowerCase();
    }

    private boolean hasAll(Set<String> skills, String... required) {
        return Arrays.stream(required).allMatch(skills::contains);
    }

    private String titleCase(String value) {
        if (value == null || value.isBlank()) {
            return value;
        }
        return Character.toUpperCase(value.charAt(0)) + value.substring(1);
    }
}
