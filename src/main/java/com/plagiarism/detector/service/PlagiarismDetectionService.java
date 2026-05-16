package com.plagiarism.detector.service;

import com.plagiarism.detector.algorithm.RabinKarpMatcher;
import com.plagiarism.detector.algorithm.Winnowing;
import com.plagiarism.detector.model.SimilarityReport;
import com.plagiarism.detector.util.CodeTokenizer;
import org.springframework.stereotype.Service;

import java.util.*;

@Service
public class PlagiarismDetectionService {

    private static final int KGRAM_SIZE  = 7;
    private static final int WINDOW_SIZE = 4;
    private static final int MIN_MATCH   = 5;

    private final CodeTokenizer    tokenizer = new CodeTokenizer();
    private final RabinKarpMatcher rkMatcher = new RabinKarpMatcher();
    private final Winnowing        winnowing = new Winnowing(KGRAM_SIZE, WINDOW_SIZE);

    public SimilarityReport compare(String codeA, String codeB,
                                    String fileA, String fileB) {

        List<String> tokensA = tokenizer.tokenValues(codeA);
        List<String> tokensB = tokenizer.tokenValues(codeB);

        if (tokensA.isEmpty() || tokensB.isEmpty()) {
            return SimilarityReport.empty(fileA, fileB,
                    "One or both files produced no tokens.");
        }

        // Rabin-Karp segment detection
        List<int[]> segments = rkMatcher
                .findMatchingSegments(tokensA, tokensB, KGRAM_SIZE)
                .stream()
                .filter(s -> s[2] >= MIN_MATCH)
                .toList();

        // Winnowing similarity scores
        Set<Long> fpA = winnowing.fingerprint(tokensA);
        Set<Long> fpB = winnowing.fingerprint(tokensB);

        double jaccardScore  = winnowing.jaccardSimilarity(fpA, fpB);
        double weightedScore = winnowing.weightedSimilarity(fpA, fpB);

        // Coverage score
        int matchedTokens = segments.stream().mapToInt(s -> s[2]).sum();
        double coverageScore = Math.min(
                (double) matchedTokens / Math.min(tokensA.size(), tokensB.size()),
                1.0
        );

        // Final blended score
        double finalScore = 0.35 * jaccardScore
                + 0.35 * weightedScore
                + 0.30 * coverageScore;

        return SimilarityReport.builder()
                .fileA(fileA)
                .fileB(fileB)
                .tokenCountA(tokensA.size())
                .tokenCountB(tokensB.size())
                .matchedSegments(formatSegments(segments, tokensA, tokensB))
                .jaccardScore(round(jaccardScore))
                .weightedScore(round(weightedScore))
                .coverageScore(round(coverageScore))
                .finalSimilarityScore(round(finalScore))
                .verdict(verdict(finalScore))
                .build();
    }

    private double round(double v) {
        return Math.round(v * 10_000.0) / 10_000.0;
    }

    private String verdict(double score) {
        if (score >= 0.80) return "HIGH — likely plagiarised";
        if (score >= 0.50) return "MEDIUM — suspicious similarity";
        if (score >= 0.25) return "LOW — minor overlap";
        return "NONE — files appear independent";
    }

    private List<SimilarityReport.Segment> formatSegments(List<int[]> raw,
                                                          List<String> tA,
                                                          List<String> tB) {
        List<SimilarityReport.Segment> out = new ArrayList<>();
        for (int[] s : raw) {
            int startA = s[0], startB = s[1], len = s[2];
            int endA = Math.min(startA + len, tA.size());
            int endB = Math.min(startB + len, tB.size());

            out.add(SimilarityReport.Segment.builder()
                    .startIndexA(startA)
                    .endIndexA(endA - 1)
                    .startIndexB(startB)
                    .endIndexB(endB - 1)
                    .length(len)
                    .previewA(excerpt(tA, startA, endA))
                    .previewB(excerpt(tB, startB, endB))
                    .build());
        }
        return out;
    }

    private String excerpt(List<String> tokens, int start, int end) {
        List<String> slice = tokens.subList(start, Math.min(end, tokens.size()));
        String joined = String.join(" ", slice);
        return joined.length() > 120 ? joined.substring(0, 117) + "…" : joined;
    }
}
