package Similarity;

import Utils.Loader.ConfigurationLoader;
import javafx.util.Pair;
import org.sotorrent.stringsimilarity.edit.Variants;

public class StringSimilarityProcessor {

    private static String [] userMetrics = ConfigurationLoader.getSimilarityMetrics();

    public static Pair<String,Double> computeSimilarity(String s1, String s2){
        String maxMetric = "";
        double maxSimilarity = -1;

        for (int i = 0; i < userMetrics.length; i++) {
            double similarity = computeSimilarity(s1, s2, userMetrics[i]);
            if(similarity > maxSimilarity){
                maxSimilarity = similarity;
                maxMetric = userMetrics[i];
            }
        }

        return new Pair<>(maxMetric,maxSimilarity);
    }

    public static double computeSimilarity(String s1, String s2, String metric) {
        try {
            switch (metric) {
                // Equal
                case "Equal": return org.sotorrent.stringsimilarity.equal.Variants.equal(s1,s2);
                case "Equal Normalized": return org.sotorrent.stringsimilarity.equal.Variants.equalNormalized(s1,s2);
                case "Token Equal": return org.sotorrent.stringsimilarity.equal.Variants.tokenEqual(s1,s2);
                case "Token Equal Normalized": return org.sotorrent.stringsimilarity.equal.Variants.tokenEqualNormalized(s1,s2);

                // Edit
                case "Levenshtein": return Variants.levenshtein(s1, s2);
                case "Levenshtein Normalized": return Variants.levenshteinNormalized(s1, s2);
                case "Damerau-Levenshtein": return Variants.damerauLevenshtein(s1, s2);
                case "Damerau-Levenshtein Normalized": return Variants.damerauLevenshteinNormalized(s1, s2);
                case "Optimal-Alignment": return Variants.optimalAlignment(s1, s2);
                case "Optimal-Alignment Normalized": return Variants.optimalAlignmentNormalized(s1, s2);
                case "Longest-Common-Subsequence": return Variants.longestCommonSubsequence(s1, s2);
                case "Longest-Common-Subsequence Normalized": return Variants.longestCommonSubsequenceNormalized(s1, s2);

                // Set
                // Token
                case "Jaccard Token": return org.sotorrent.stringsimilarity.set.Variants.tokenJaccard(s1, s2);
                case "Jaccard Token Normalized": return org.sotorrent.stringsimilarity.set.Variants.tokenJaccardNormalized(s1, s2);
                case "Sorensen-Dice Token": return org.sotorrent.stringsimilarity.set.Variants.tokenDice(s1, s2);
                case "Sorensen-Dice Token Normalized": return org.sotorrent.stringsimilarity.set.Variants.tokenDiceNormalized(s1, s2);
                case "Overlap Token": return org.sotorrent.stringsimilarity.set.Variants.tokenOverlap(s1, s2);
                case "Overlap Token Normalized": return org.sotorrent.stringsimilarity.set.Variants.tokenOverlapNormalized(s1, s2);

                // nGrams
                case "Jaccard 2-grams": return org.sotorrent.stringsimilarity.set.Variants.twoGramJaccard(s1, s2);
                case "Jaccard 2-grams Normalized": return org.sotorrent.stringsimilarity.set.Variants.twoGramJaccardNormalized(s1, s2);
                case "Jaccard 2-grams Normalized Padding": return org.sotorrent.stringsimilarity.set.Variants.twoGramJaccardNormalizedPadding(s1, s2);
                case "Jaccard 3-grams": return org.sotorrent.stringsimilarity.set.Variants.threeGramJaccard(s1, s2);
                case "Jaccard 3-grams Normalized": return org.sotorrent.stringsimilarity.set.Variants.threeGramJaccardNormalized(s1, s2);
                case "Jaccard 3-grams Normalized Padding": return org.sotorrent.stringsimilarity.set.Variants.threeGramJaccardNormalizedPadding(s1, s2);
                case "Jaccard 4-grams": return org.sotorrent.stringsimilarity.set.Variants.fourGramJaccard(s1, s2);
                case "Jaccard 4-grams Normalized": return org.sotorrent.stringsimilarity.set.Variants.fourGramJaccardNormalized(s1, s2);
                case "Jaccard 4-grams Normalized Padding": return org.sotorrent.stringsimilarity.set.Variants.fourGramJaccardNormalizedPadding(s1, s2);
                case "Jaccard 5-grams": return org.sotorrent.stringsimilarity.set.Variants.fiveGramJaccard(s1, s2);
                case "Jaccard 5-grams Normalized": return org.sotorrent.stringsimilarity.set.Variants.fiveGramJaccardNormalized(s1, s2);
                case "Jaccard 5-grams Normalized Padding": return org.sotorrent.stringsimilarity.set.Variants.fiveGramJaccardNormalized(s1, s2);

                case "Sorensen-Dice 2-grams": return org.sotorrent.stringsimilarity.set.Variants.twoGramDice(s1, s2);
                case "Sorensen-Dice 2-grams Normalized": return org.sotorrent.stringsimilarity.set.Variants.twoGramDiceNormalized(s1, s2);
                case "Sorensen-Dice 2-grams Normalized Padding": return org.sotorrent.stringsimilarity.set.Variants.twoGramDiceNormalizedPadding(s1, s2);
                case "Sorensen-Dice 3-grams": return org.sotorrent.stringsimilarity.set.Variants.threeGramDice(s1, s2);
                case "Sorensen-Dice 3-grams Normalized": return org.sotorrent.stringsimilarity.set.Variants.threeGramDiceNormalized(s1, s2);
                case "Sorensen-Dice 3-grams Normalized Padding": return org.sotorrent.stringsimilarity.set.Variants.threeGramDiceNormalizedPadding(s1, s2);
                case "Sorensen-Dice 4-grams": return org.sotorrent.stringsimilarity.set.Variants.fourGramDice(s1, s2);
                case "Sorensen-Dice 4-grams Normalized": return org.sotorrent.stringsimilarity.set.Variants.fourGramDiceNormalized(s1, s2);
                case "Sorensen-Dice 4-grams Normalized Padding": return org.sotorrent.stringsimilarity.set.Variants.fourGramDiceNormalizedPadding(s1, s2);
                case "Sorensen-Dice 5-grams": return org.sotorrent.stringsimilarity.set.Variants.fiveGramDice(s1, s2);
                case "Sorensen-Dice 5-grams Normalized": return org.sotorrent.stringsimilarity.set.Variants.fiveGramDiceNormalized(s1, s2);
                case "Sorensen-Dice 5-grams Normalized Padding": return org.sotorrent.stringsimilarity.set.Variants.fiveGramDiceNormalizedPadding(s1, s2);

                case "Overlap 2-grams": return org.sotorrent.stringsimilarity.set.Variants.twoGramOverlap(s1, s2);
                case "Overlap 2-grams Normalized": return org.sotorrent.stringsimilarity.set.Variants.twoGramOverlapNormalized(s1, s2);
                case "Overlap 2-grams Normalized Padding": return org.sotorrent.stringsimilarity.set.Variants.twoGramOverlapNormalizedPadding(s1, s2);
                case "Overlap 3-grams": return org.sotorrent.stringsimilarity.set.Variants.threeGramOverlap(s1, s2);
                case "Overlap 3-grams Normalized": return org.sotorrent.stringsimilarity.set.Variants.threeGramOverlapNormalized(s1, s2);
                case "Overlap 3-grams Normalized Padding": return org.sotorrent.stringsimilarity.set.Variants.threeGramOverlapNormalizedPadding(s1, s2);
                case "Overlap 4-grams": return org.sotorrent.stringsimilarity.set.Variants.fourGramOverlap(s1, s2);
                case "Overlap 4-grams Normalized": return org.sotorrent.stringsimilarity.set.Variants.fourGramOverlapNormalized(s1, s2);
                case "Overlap 4-grams Normalized Padding": return org.sotorrent.stringsimilarity.set.Variants.fourGramOverlapNormalizedPadding(s1, s2);
                case "Overlap 5-grams": return org.sotorrent.stringsimilarity.set.Variants.fiveGramOverlap(s1, s2);
                case "Overlap 5-grams Normalized": return org.sotorrent.stringsimilarity.set.Variants.fiveGramOverlapNormalized(s1, s2);
                case "Overlap 5-grams Normalized Padding": return org.sotorrent.stringsimilarity.set.Variants.fiveGramOverlapNormalizedPadding(s1, s2);

                // Shingles
                case "Jaccard 2-shingles": return org.sotorrent.stringsimilarity.set.Variants.twoShingleJaccard(s1, s2);
                case "Jaccard 2-shingles Normalized": return org.sotorrent.stringsimilarity.set.Variants.twoShingleJaccardNormalized(s1, s2);
                case "Jaccard 3-shingles": return org.sotorrent.stringsimilarity.set.Variants.threeShingleJaccard(s1, s2);
                case "Jaccard 3-shingles Normalized": return org.sotorrent.stringsimilarity.set.Variants.threeShingleJaccardNormalized(s1, s2);

                case "Sorensen-Dice 2-shingles": return org.sotorrent.stringsimilarity.set.Variants.twoShingleDice(s1, s2);
                case "Sorensen-Dice 2-shingles Normalized": return org.sotorrent.stringsimilarity.set.Variants.twoShingleDiceNormalized(s1, s2);
                case "Sorensen-Dice 3-shingles": return org.sotorrent.stringsimilarity.set.Variants.threeShingleDice(s1, s2);
                case "Sorensen-Dice 3-shingles Normalized": return org.sotorrent.stringsimilarity.set.Variants.threeShingleDiceNormalized(s1, s2);

                case "Overlap 2-shingles": return org.sotorrent.stringsimilarity.set.Variants.twoShingleOverlap(s1, s2);
                case "Overlap 2-shingles Normalized": return org.sotorrent.stringsimilarity.set.Variants.twoShingleOverlapNormalized(s1, s2);
                case "Overlap 3-shingles": return org.sotorrent.stringsimilarity.set.Variants.threeShingleOverlap(s1, s2);
                case "Overlap 3-shingles Normalized": return org.sotorrent.stringsimilarity.set.Variants.threeShingleOverlapNormalized(s1, s2);
            }
            return 0;
        } catch (Exception e) {
            return 0;
        }
    }
}
