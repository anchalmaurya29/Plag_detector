package com.plagiarism.detector.algorithm;

import java.util.*;

public class RabinKarpMatcher {

    private static final long BASE = 31L;
    private static final long MOD  = 1_000_000_007L;

    public Set<Long> computeFingerprints(List<String> tokens, int k) {
        if (tokens.size() < k) return Collections.emptySet();

        Set<Long> fingerprints = new HashSet<>();
        long[] prefix = new long[tokens.size() + 1];
        long[] power  = new long[tokens.size() + 1];
        power[0] = 1;

        for (int i = 0; i < tokens.size(); i++) {
            long h = tokens.get(i).hashCode() & 0x7FFFFFFFL;
            prefix[i + 1] = (prefix[i] * BASE + h) % MOD;
            power[i + 1]  = (power[i] * BASE) % MOD;
        }

        for (int i = 0; i <= tokens.size() - k; i++) {
            long h = (prefix[i + k] - prefix[i] * power[k] % MOD + MOD * MOD) % MOD;
            fingerprints.add(h);
        }
        return fingerprints;
    }

    public List<int[]> findMatchingSegments(List<String> tokensA,
                                            List<String> tokensB,
                                            int k) {
        if (tokensA.size() < k || tokensB.size() < k) return Collections.emptyList();

        Map<Long, List<Integer>> bIndex = buildIndex(tokensB, k);
        List<int[]> segments = new ArrayList<>();

        for (int i = 0; i <= tokensA.size() - k; i++) {
            long h = hash(tokensA.subList(i, i + k));
            if (bIndex.containsKey(h)) {
                for (int j : bIndex.get(h)) {
                    if (tokensA.subList(i, i + k).equals(tokensB.subList(j, j + k))) {
                        int len = extendMatch(tokensA, tokensB, i, j, k);
                        segments.add(new int[]{i, j, len});
                    }
                }
            }
        }
        return mergeOverlaps(segments);
    }

    private long hash(List<String> tokens) {
        long h = 0;
        for (String t : tokens) h = (h * BASE + (t.hashCode() & 0x7FFFFFFFL)) % MOD;
        return h;
    }

    private Map<Long, List<Integer>> buildIndex(List<String> tokens, int k) {
        Map<Long, List<Integer>> index = new HashMap<>();
        for (int i = 0; i <= tokens.size() - k; i++) {
            long h = hash(tokens.subList(i, i + k));
            index.computeIfAbsent(h, x -> new ArrayList<>()).add(i);
        }
        return index;
    }

    private int extendMatch(List<String> a, List<String> b, int ia, int ib, int k) {
        int len = k;
        while (ia + len < a.size()
                && ib + len < b.size()
                && a.get(ia + len).equals(b.get(ib + len))) {
            len++;
        }
        return len;
    }

    private List<int[]> mergeOverlaps(List<int[]> segments) {
        if (segments.isEmpty()) return segments;
        segments.sort(Comparator.comparingInt(s -> s[0]));
        List<int[]> merged = new ArrayList<>();
        int[] cur = segments.get(0).clone();
        for (int i = 1; i < segments.size(); i++) {
            int[] next = segments.get(i);
            if (next[0] <= cur[0] + cur[2]) {
                cur[2] = Math.max(cur[2], next[0] - cur[0] + next[2]);
            } else {
                merged.add(cur);
                cur = next.clone();
            }
        }
        merged.add(cur);
        return merged;
    }
}
