package com.plagiarism.detector.controller;

import com.plagiarism.detector.model.SimilarityReport;
import com.plagiarism.detector.service.PlagiarismDetectionService;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.nio.charset.StandardCharsets;

@RestController
@RequestMapping("/api/v1")
public class PlagiarismController {

    private final PlagiarismDetectionService service;

    public PlagiarismController(PlagiarismDetectionService service) {
        this.service = service;
    }

    @PostMapping(value = "/detect", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    public ResponseEntity<SimilarityReport> detect(
            @RequestPart("fileA") MultipartFile fileA,
            @RequestPart("fileB") MultipartFile fileB) throws IOException {

        validateFile(fileA);
        validateFile(fileB);

        String codeA = new String(fileA.getBytes(), StandardCharsets.UTF_8);
        String codeB = new String(fileB.getBytes(), StandardCharsets.UTF_8);

        SimilarityReport report = service.compare(
                codeA, codeB,
                fileA.getOriginalFilename(),
                fileB.getOriginalFilename()
        );

        return ResponseEntity.ok(report);
    }

    @PostMapping("/detect/text")
    public ResponseEntity<SimilarityReport> detectText(
            @RequestBody TextRequest req) {

        SimilarityReport report = service.compare(
                req.codeA(), req.codeB(),
                req.nameA(), req.nameB()
        );
        return ResponseEntity.ok(report);
    }

    @GetMapping("/health")
    public ResponseEntity<String> health() {
        return ResponseEntity.ok("Plagiarism Detector is running!");
    }

    private void validateFile(MultipartFile f) {
        if (f == null || f.isEmpty())
            throw new IllegalArgumentException("File must not be empty.");
        if (f.getSize() > 512L * 1024)
            throw new IllegalArgumentException("File exceeds 512 KB limit.");
    }

    public record TextRequest(
            String codeA,
            String codeB,
            String nameA,
            String nameB
    ) {}
}
