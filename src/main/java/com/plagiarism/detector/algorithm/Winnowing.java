package com.plagiarism.detector.algorithm;

import java.util.*;

public class Winnowing {

    private final int k;
    private final int w;

    public Winnowing(int k, int w) {
        if (w < 1 || k < 1) throw new IllegalArgumentException("k and w must be >= 1");
        this.k = k;
        this.w = w;
    }

    public Set<Long> fingerprint(List<String> tokens) {
        if (tokens.size() < k) return Collections.emptySet();
        List<Long> kgrams = computeKgramHashes(tokens);
        return winnow(kgrams);
    }

    public double jaccardSimilarity(Set<Long> fpA, Set<Long> fpB) {
        if (fpA.isEmpty() && fpB.isEmpty()) return 1.0;
        if (fpA.isEmpty() || fpB.isEmpty()) return 0.0;

        Set<Long> intersection = new HashSet<>(fpA);
        intersection.retainAll(fpB);

        Set<Long> union = new HashSet<>(fpA);
        union.addAll(fpB);

        return (double) intersection.size() / union.size();
    }

    public double weightedSimilarity(Set<Long> fpA, Set<Long> fpB) {
        double jaccard = jaccardSimilarity(fpA, fpB);
        if (fpA.isEmpty() || fpB.isEmpty()) return jaccard;

        Set<Long> intersection = new HashSet<>(fpA);
        intersection.retainAll(fpB);
        int shared = intersection.size();

        double containmentA = (double) shared / fpA.size();
        double containmentB = (double) shared / fpB.size();
        double maxContainment = Math.max(containmentA, containmentB);

        return 0.60 * jaccard + 0.40 * maxContainment;
    }

    private static final long BASE = 31L;
    private static final long MOD  = 1_000_000_007L;

    private List<Long> computeKgramHashes(List<String> tokens) {
        List<Long> hashes = new ArrayList<>();
        for (int i = 0; i <= tokens.size() - k; i++) {
            long h = 0;
            for (int j = i; j < i + k; j++) {
                h = (h * BASE + ((long) tokens.get(j).hashCode() & 0x7FFFFFFFL)) % MOD;
            }
            hashes.add(h);
        }
        return hashes;
    }

    private Set<Long> winnow(List<Long> hashes) {
        Set<Long> fingerprints = new HashSet<>();
        if (hashes.size() < w) {
            fingerprints.addAll(hashes);
            return fingerprints;
        }

        Deque<long[]> window = new ArrayDeque<>();
        int prevMin = -1;

        for (int i = 0; i < hashes.size(); i++) {
            long h = hashes.get(i);

            while (!window.isEmpty() && window.peekFirst()[1] <= i - w)
                window.pollFirst();

            while (!window.isEmpty() && window.peekLast()[0] >= h)
                window.pollLast();

            window.addLast(new long[]{h, i});

            if (i >= w - 1) {
                long minHash = window.peekFirst()[0];
                int  minIdx  = (int) window.peekFirst()[1];
                if (minIdx != prevMin) {
                    fingerprints.add(minHash);
                    prevMin = minIdx;
                }
            }
        }
        return fingerprints;
    }
}
