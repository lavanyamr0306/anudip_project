package com.example.ResumeAnalyzerPro_Final.controller;

import com.example.ResumeAnalyzerPro_Final.entity.Analysis;
import com.example.ResumeAnalyzerPro_Final.entity.Resume;
import com.example.ResumeAnalyzerPro_Final.entity.User;
import com.example.ResumeAnalyzerPro_Final.service.ResumeService;
import com.itextpdf.kernel.colors.ColorConstants;
import com.itextpdf.kernel.pdf.PdfDocument;
import com.itextpdf.kernel.pdf.PdfWriter;
import com.itextpdf.layout.Document;
import com.itextpdf.layout.element.ListItem;
import com.itextpdf.layout.element.Paragraph;
import com.itextpdf.layout.element.Text;
import jakarta.servlet.http.HttpSession;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.Page;
import org.springframework.http.ContentDisposition;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.ResponseBody;
import org.springframework.web.multipart.MultipartFile;

import java.io.ByteArrayOutputStream;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.List;

@Controller
public class ResumeController {

    @Autowired
    private ResumeService service;

    @GetMapping("/dashboard")
    public String dashboard(HttpSession session,
            Model model) {
        User user = (User) session.getAttribute("user");
        if (user == null) {
            return "redirect:/login";
        }
        if ("ADMIN".equals(user.getRole())) {
            return "redirect:/admin/dashboard";
        }

        populateDashboardModel(model, user, service.getLatestAnalysis(user));
        return "dashboard";
    }

    @PostMapping("/analyze")
    public String analyze(@RequestParam(value = "file", required = false) MultipartFile file,
            @RequestParam(value = "resumeContent", required = false) String resumeContent,
            @RequestParam(value = "jobDesc", required = false) String jobDesc,
            HttpSession session,
            Model model) {

        User user = (User) session.getAttribute("user");
        if (user == null) {
            return "redirect:/login";
        }

        try {
            String normalizedJobDesc = jobDesc == null ? "" : jobDesc.trim();
            String normalizedResumeContent = resumeContent == null ? "" : resumeContent.trim();

            Resume resume = null;
            if (file != null && !file.isEmpty()) {
                resume = service.upload(file, user);
                if (resume == null) {
                    populateDashboardModel(model, user, service.getLatestAnalysis(user));
                    model.addAttribute("error",
                            "File processing failed. Please try a different file format or paste text instead.");
                    return "dashboard";
                }
                normalizedResumeContent = resume.getContent();
            } else if (!normalizedResumeContent.isBlank()) {
                resume = service.saveTextResume(normalizedResumeContent, user);
            }

            if (normalizedResumeContent.isBlank() || normalizedJobDesc.isBlank()) {
                populateDashboardModel(model, user, service.getLatestAnalysis(user));
                model.addAttribute("error",
                        "Please upload a resume or paste resume text, and provide job description.");
                return "dashboard";
            }

            int keywordMatch = service.calculateKeywordMatchScore(normalizedResumeContent, normalizedJobDesc);
            int trackedSkills = service.calculateTrackedSkillsScore(normalizedResumeContent, normalizedJobDesc);
            int completeness = service.calculateCompletenessScore(normalizedResumeContent);
            int finalScore = service.calculateFinalScore(keywordMatch, trackedSkills, completeness);

            List<String> matchedSkills = service.findMatchedSkills(normalizedResumeContent, normalizedJobDesc);
            List<String> missingSkills = service.findMissingSkills(normalizedResumeContent, normalizedJobDesc);
            List<String> recommendedJobs = service.recommendJobs(normalizedResumeContent, normalizedJobDesc);
            String fileName;

            if (file != null && !file.isEmpty()) {
                fileName = file.getOriginalFilename(); // uploaded file name
            } else {
                fileName = "pasted-resume.txt"; // fallback
            }
            Analysis analysis = service.saveAnalysis(
                    user,
                    finalScore,
                    keywordMatch,
                    trackedSkills,
                    completeness,
                    matchedSkills,
                    missingSkills,
                    recommendedJobs,
                    fileName,
                    normalizedJobDesc);

            populateDashboardModel(model, user, analysis);
            model.addAttribute("resumeText", normalizedResumeContent);
            model.addAttribute("jobDesc", normalizedJobDesc);
            model.addAttribute("sourceFileName", resume != null ? resume.getFileName() : "pasted-resume.txt");
            return "dashboard";

        } catch (Exception e) {
            populateDashboardModel(model, user, service.getLatestAnalysis(user));
            model.addAttribute("error",
                    "An error occurred during analysis. Please try again. Error: " + e.getMessage());
            return "dashboard";
        }
    }

    @GetMapping("/analysis/{id}/view")
    public String viewAnalysis(@PathVariable Long id,
            HttpSession session,
            Model model) {
        User user = (User) session.getAttribute("user");
        if (user == null) {
            return "redirect:/login";
        }

        Analysis analysis = service.getAnalysisByIdForUser(id, user);
        if (analysis == null) {
            populateDashboardModel(model, user, service.getLatestAnalysis(user));
            model.addAttribute("error", "Analysis not found.");
            return "dashboard";
        }

        populateDashboardModel(model, user, analysis);
        return "dashboard";
    }

    @GetMapping("/history/more")
    @ResponseBody
    public ResponseEntity<HistoryChunkResponse> loadMoreHistory(
            @RequestParam(value = "offset", defaultValue = "10") int offset,
            HttpSession session) {
        User user = (User) session.getAttribute("user");
        if (user == null) {
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED).build();
        }

        int safeOffset = Math.max(0, offset);
        int pageSize = ResumeService.HISTORY_PAGE_SIZE;
        int pageIndex = safeOffset / pageSize;

        Page<Analysis> chunk = service.getHistoryPage(user, pageIndex);
        List<HistoryItem> items = chunk.getContent().stream()
                .map(analysis -> new HistoryItem(
                        analysis.getId(),
                        analysis.getFinalScore(),
                        analysis.getStatus(),
                        analysis.getFileName(),
                        analysis.getInsight(),
                        analysis.getCreatedAt().format(DateTimeFormatter.ofPattern("dd MMM yyyy, hh:mm a"))))
                .toList();

        int loadedCount = safeOffset + items.size();
        boolean hasMore = loadedCount < service.getTotalAnalyses(user);
        return ResponseEntity.ok(new HistoryChunkResponse(items, hasMore, loadedCount));
    }

    @GetMapping("/analysis/{id}/pdf")
    public ResponseEntity<byte[]> downloadPdf(@PathVariable Long id, HttpSession session) {
        User user = (User) session.getAttribute("user");
        if (user == null) {
            return ResponseEntity.status(HttpStatus.FOUND)
                    .header(HttpHeaders.LOCATION, "/login")
                    .build();
        }

        Analysis analysis = service.getAnalysisByIdForUser(id, user);
        if (analysis == null) {
            return ResponseEntity.notFound().build();
        }

        byte[] pdfBytes = buildAnalysisPdf(analysis, user.getName());
        String filename = "analysis-" + analysis.getId() + ".pdf";

        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.APPLICATION_PDF);
        headers.setContentDisposition(ContentDisposition.attachment().filename(filename).build());

        return new ResponseEntity<>(pdfBytes, headers, HttpStatus.OK);
    }

    private void populateDashboardModel(Model model, User user, Analysis selectedAnalysis) {
        Page<Analysis> historyPageData = service.getHistoryPage(user, 0);
        List<Analysis> history = historyPageData.getContent();
        Analysis latest = service.getLatestAnalysis(user);
        long totalAnalyses = service.getTotalAnalyses(user);

        Analysis active = selectedAnalysis != null ? selectedAnalysis : latest;

        model.addAttribute("user", user);
        model.addAttribute("totalAnalyses", totalAnalyses);
        model.addAttribute("latestScore", latest != null ? latest.getFinalScore() : 0);
        model.addAttribute("latestStatus", latest != null ? latest.getStatus() : "No Analysis Yet");
        model.addAttribute("recommendedJobsCount", latest != null ? latest.getRecommendedJobs().size() : 0);

        model.addAttribute("history", history);
        model.addAttribute("historyLoadedCount", history.size());
        model.addAttribute("historyHasMore", history.size() < totalAnalyses);
        model.addAttribute("activeAnalysis", active);

        List<Analysis> graphHistory = service.getLatestAnalyses(user, 10);
        List<String> graphLabels = new ArrayList<>();
        List<Integer> graphScores = new ArrayList<>();
        int graphSize = graphHistory.size();
        int startRun = Math.max(1, (int) (totalAnalyses - graphSize + 1));
        for (int i = graphSize - 1; i >= 0; i--) {
            Analysis analysis = graphHistory.get(i);
            graphLabels.add("Run " + (startRun + (graphSize - 1 - i)));
            graphScores.add(analysis.getFinalScore());
        }

        model.addAttribute("graphLabels", graphLabels);
        model.addAttribute("graphScores", graphScores);
        model.addAttribute("dateFormatter", DateTimeFormatter.ofPattern("dd MMM yyyy, hh:mm a"));
    }

    private byte[] buildAnalysisPdf(Analysis analysis, String userName) {
        ByteArrayOutputStream outputStream = new ByteArrayOutputStream();
        PdfWriter writer = new PdfWriter(outputStream);
        PdfDocument pdfDocument = new PdfDocument(writer);
        Document document = new Document(pdfDocument);

        document.add(new Paragraph("Resume Analyzer Pro - Analysis Report")
                .setFontSize(18)
                .setBold());

        document.add(new Paragraph("Candidate: " + userName));
        document.add(new Paragraph(
                "Generated: " + analysis.getCreatedAt().format(DateTimeFormatter.ofPattern("dd MMM yyyy, hh:mm a"))));

        document.add(new Paragraph(" "));
        document.add(new Paragraph("Final Score: " + analysis.getFinalScore() + "%")
                .setFontSize(14)
                .setBold()
                .setFontColor(ColorConstants.BLUE));

        document.add(new Paragraph("Status: " + analysis.getStatus()));

        document.add(new Paragraph(" "));
        document.add(new Paragraph("Job Description").setBold());
        document.add(new Paragraph(
                analysis.getJobDescription() == null || analysis.getJobDescription().isBlank()
                        ? "No job description provided."
                        : analysis.getJobDescription()));

        document.add(new Paragraph(" "));
        document.add(new Paragraph("Score Breakdown").setBold());
        document.add(new Paragraph("Keyword Match: " + analysis.getKeywordMatchScore() + "%"));
        document.add(new Paragraph("Tracked Skills: " + analysis.getSkillMatchScore() + "%"));
        document.add(new Paragraph("Completeness: " + analysis.getCompletenessScore() + "%"));

        document.add(new Paragraph(" "));
        document.add(new Paragraph("Matched Skills").setBold());
        document.add(buildList(analysis.getMatchedSkills(), "No matched skills."));

        document.add(new Paragraph(" "));
        document.add(new Paragraph("Missing Skills").setBold());
        document.add(buildList(analysis.getMissingSkills(), "No missing skills."));

        document.add(new Paragraph(" "));
        document.add(new Paragraph("Recommended Jobs").setBold());
        document.add(buildList(analysis.getRecommendedJobs(), "No recommended jobs."));

        document.close();
        return outputStream.toByteArray();
    }

    private com.itextpdf.layout.element.IBlockElement buildList(List<String> values, String emptyText) {
        if (values == null || values.isEmpty()) {
            return new Paragraph(new Text(emptyText));
        }

        com.itextpdf.layout.element.List list = new com.itextpdf.layout.element.List();
        for (String value : values) {
            list.add(new ListItem(value));
        }
        return list;
    }

    private record HistoryChunkResponse(List<HistoryItem> items, boolean hasMore, int loadedCount) {
    }

    private record HistoryItem(Long id,
            int finalScore,
            String status,
            String fileName,
            String insight,
            String createdAt) {
    }
}