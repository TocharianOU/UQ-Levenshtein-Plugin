package org.elasticsearch.example.expertscript;

import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;
import java.util.AbstractMap.SimpleEntry;
import java.util.concurrent.ConcurrentHashMap;

public class SimilarityUtils {

    private static final double[][] similarityMatrix = new double[128][128];
    private static final Map<String, Double> distanceCache = new ConcurrentHashMap<>();
    private static final Map<String, Double> jaccardCache = new ConcurrentHashMap<>();

    static {
        setSimilarity('A', '4', 0.8);
        setSimilarity('B', '8', 0.9);
        setSimilarity('B', '3', 0.6);
        setSimilarity('D', 'O', 0.8);
        setSimilarity('D', '0', 0.8);
        setSimilarity('E', '3', 0.7);
        setSimilarity('G', '6', 0.8);
        setSimilarity('C', 'G', 0.5);
        setSimilarity('I', '1', 0.95);
        setSimilarity('I', 'L', 0.6);
        setSimilarity('O', '0', 0.9);
        setSimilarity('O', 'Q', 0.7);
        setSimilarity('S', '5', 0.9);
        setSimilarity('Z', '2', 0.85);
        setSimilarity('T', '7', 0.85);
        setSimilarity('L', '1', 0.85);
        setSimilarity('P', 'R', 0.6);
        setSimilarity('U', 'V', 0.75);
        setSimilarity('V', 'Y', 0.5);
        setSimilarity('M', 'N', 0.45);
        setSimilarity('K', 'X', 0.5);
    }

    private static void setSimilarity(char c1, char c2, double similarity) {
        similarityMatrix[c1][c2] = similarity;
        similarityMatrix[c2][c1] = similarity;
    }

    public static double getSimilarity(char c1, char c2) {
        if (c1 == c2) return 1.0;
        return similarityMatrix[c1][c2];
    }

    public static double customDamerauLevenshtein(String str1, String str2, int maxDist) {
        String key = str1 + ":" + str2;
        if (distanceCache.containsKey(key)) {
            return distanceCache.get(key);
        }

        int len1 = str1.length();
        int len2 = str2.length();

        if (Math.abs(len1 - len2) > maxDist) {
            return Double.POSITIVE_INFINITY;
        }

        double[] prev = new double[len2 + 1];
        double[] curr = new double[len2 + 1];

        for (int j = 0; j <= len2; j++) prev[j] = j;

        for (int i = 1; i <= len1; i++) {
            curr[0] = i;
            boolean withinMaxDist = false;
            for (int j = 1; j <= len2; j++) {
                double costSubstitution = 1 - getSimilarity(str1.charAt(i - 1), str2.charAt(j - 1));
                curr[j] = Math.min(
                    prev[j] + 1, 
                    Math.min(
                        curr[j - 1] + 1, 
                        prev[j - 1] + costSubstitution 
                    )
                );

                if (i > 1 && j > 1 && str1.charAt(i - 1) == str2.charAt(j - 2) && str1.charAt(i - 2) == str2.charAt(j - 1)) {
                    curr[j] = Math.min(curr[j], prev[j - 2] + 1); 
                }

                if (curr[j] <= maxDist) withinMaxDist = true;
            }

            if (!withinMaxDist) {
                distanceCache.put(key, Double.POSITIVE_INFINITY);
                return Double.POSITIVE_INFINITY;
            }

            double[] temp = prev;
            prev = curr;
            curr = temp;
        }

        double finalDistance = prev[len2];
        distanceCache.put(key, finalDistance);
        return finalDistance > maxDist ? Double.POSITIVE_INFINITY : finalDistance;
    }

    public static double jaccardSimilarity(String str1, String str2, double similarityThreshold) {
        String key = str1 + ":" + str2;
        if (jaccardCache.containsKey(key)) {
            return jaccardCache.get(key);
        }

        Set<Character> set1 = new HashSet<>();
        Set<Character> set2 = new HashSet<>();
        for (char c : str1.toCharArray()) set1.add(c);
        for (char c : str2.toCharArray()) set2.add(c);

        double intersection = set1.parallelStream()
            .flatMapToDouble(char1 -> set2.stream()
                .mapToDouble(char2 -> getSimilarity(char1, char2))
                .filter(similarity -> similarity > similarityThreshold)
            ).sum();

        int union = set1.size() + set2.size() - (int) intersection;

        double jaccard = union == 0 ? 0 : intersection / union;
        jaccardCache.put(key, jaccard);
        return jaccard;
    }

    public static double mixedSimilarity(String str1, String str2, double alpha, double beta, int maxDist) {
        double dlDistance = customDamerauLevenshtein(str1, str2, maxDist);

        if (Double.isInfinite(dlDistance)) {
            return 0.0; // // If the distance exceeds maxDist, return the minimum similarity
        }

        double dlSimilarity = 1 / (1 + dlDistance);
        double jaccardSim = jaccardSimilarity(str1, str2, 0.5);

        return alpha * dlSimilarity + beta * jaccardSim;
    }
}
