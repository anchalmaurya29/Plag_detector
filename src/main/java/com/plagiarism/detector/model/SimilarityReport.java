package com.plagiarism.detector.model;

import java.time.Instant;
import java.util.List;

public class SimilarityReport {

    private String fileA;
    private String fileB;
    private int tokenCountA;
    private int tokenCountB;
    private double jaccardScore;
    private double weightedScore;
    private double coverageScore;
    private double finalSimilarityScore;
    private String verdict;
    private List<Segment> matchedSegments;
    private String analysedAt = Instant.now().toString();

    // ── Builder ──────────────────────────────────────────────────────────────

    public static Builder builder() { return new Builder(); }

    public static class Builder {
        private final SimilarityReport r = new SimilarityReport();

        public Builder fileA(String v)               { r.fileA = v; return this; }
        public Builder fileB(String v)               { r.fileB = v; return this; }
        public Builder tokenCountA(int v)            { r.tokenCountA = v; return this; }
        public Builder tokenCountB(int v)            { r.tokenCountB = v; return this; }
        public Builder jaccardScore(double v)        { r.jaccardScore = v; return this; }
        public Builder weightedScore(double v)       { r.weightedScore = v; return this; }
        public Builder coverageScore(double v)       { r.coverageScore = v; return this; }
        public Builder finalSimilarityScore(double v){ r.finalSimilarityScore = v; return this; }
        public Builder verdict(String v)             { r.verdict = v; return this; }
        public Builder matchedSegments(List<Segment> v) { r.matchedSegments = v; return this; }
        public SimilarityReport build()              { return r; }
    }

    public static SimilarityReport empty(String a, String b, String reason) {
        return builder()
                .fileA(a).fileB(b)
                .verdict("ERROR — " + reason)
                .matchedSegments(List.of())
                .build();
    }

    // ── Segment inner class ───────────────────────────────────────────────────

    public static class Segment {
        private int startIndexA;
        private int endIndexA;
        private int startIndexB;
        private int endIndexB;
        private int length;
        private String previewA;
        private String previewB;

        public static Builder builder() { return new Builder(); }

        public static class Builder {
            private final Segment s = new Segment();
            public Builder startIndexA(int v)  { s.startIndexA = v; return this; }
            public Builder endIndexA(int v)    { s.endIndexA = v; return this; }
            public Builder startIndexB(int v)  { s.startIndexB = v; return this; }
            public Builder endIndexB(int v)    { s.endIndexB = v; return this; }
            public Builder length(int v)       { s.length = v; return this; }
            public Builder previewA(String v)  { s.previewA = v; return this; }
            public Builder previewB(String v)  { s.previewB = v; return this; }
            public Segment build()             { return s; }
        }

        // Getters
        public int getStartIndexA()  { return startIndexA; }
        public int getEndIndexA()    { return endIndexA; }
        public int getStartIndexB()  { return startIndexB; }
        public int getEndIndexB()    { return endIndexB; }
        public int getLength()       { return length; }
        public String getPreviewA()  { return previewA; }
        public String getPreviewB()  { return previewB; }
    }

    // ── Getters ───────────────────────────────────────────────────────────────

    public String getFileA()                { return fileA; }
    public String getFileB()                { return fileB; }
    public int getTokenCountA()             { return tokenCountA; }
    public int getTokenCountB()             { return tokenCountB; }
    public double getJaccardScore()         { return jaccardScore; }
    public double getWeightedScore()        { return weightedScore; }
    public double getCoverageScore()        { return coverageScore; }
    public double getFinalSimilarityScore() { return finalSimilarityScore; }
    public String getVerdict()              { return verdict; }
    public List<Segment> getMatchedSegments() { return matchedSegments; }
    public String getAnalysedAt()           { return analysedAt; }
}
