package com.udacity.webcrawler;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.Comparator;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.PriorityQueue;
import java.util.stream.Collectors;

/**
 * Utility class that sorts the map of word counts.
 *
 * <p>TODO: Reimplement the sort() method using only the Stream API and lambdas and/or method
 * references.
 */
final class WordCounts {
    private static final Logger logger = LoggerFactory.getLogger(WordCounts.class);

    /**
     * Given an unsorted map of word counts, returns a new map whose word counts are sorted according
     * to the provided {@link WordCountComparator}, and includes only the top
     * {@param popluarWordCount} words and counts.
     *
     * <p>TODO: Reimplement this method using only the Stream API and lambdas and/or method
     * references.
     *
     * @param wordCounts       the unsorted map of word counts.
     * @param popularWordCount the number of popular words to include in the result map.
     * @return a map containing the top {@param popularWordCount} words and counts in the right order.
     */
    static Map<String, Integer> sort(Map<String, Integer> wordCounts, int popularWordCount) {

        // TODO: Reimplement this method using only the Stream API and lambdas and/or method references.
        logger.debug("Starting the sorting process for word counts. Target number of popular words: {}", popularWordCount);

        // Stream processing to sort the map based on custom criteria
        Map<String, Integer> sortedWordCounts = wordCounts.entrySet().stream()
                .sorted((entry1, entry2) -> {
                    int frequencyComparison = Integer.compare(entry2.getValue(), entry1.getValue()); // Descending by frequency
                    if (frequencyComparison != 0) return frequencyComparison;

                    int lengthComparison = Integer.compare(entry2.getKey().length(), entry1.getKey().length()); // Descending by word length
                    if (lengthComparison != 0) return lengthComparison;

                    return entry1.getKey().compareTo(entry2.getKey()); // Alphabetical order
                })
                .limit(popularWordCount)
                .collect(Collectors.toMap(
                        Map.Entry::getKey,
                        Map.Entry::getValue,
                        (existing, replacement) -> existing, // Handle duplicate keys (should not occur)
                        LinkedHashMap::new // Preserve insertion order
                ));

        logger.debug("Sorting completed. Result: {}", sortedWordCounts);
        return sortedWordCounts;
    }

    /**
     * A {@link Comparator} that sorts word count pairs correctly:
     *
     * <p>
     * <ol>
     *   <li>First sorting by word count, ranking more frequent words higher.</li>
     *   <li>Then sorting by word length, ranking longer words higher.</li>
     *   <li>Finally, breaking ties using alphabetical order.</li>
     * </ol>
     */
    private static final class WordCountComparator implements Comparator<Map.Entry<String, Integer>> {
        @Override
        public int compare(Map.Entry<String, Integer> a, Map.Entry<String, Integer> b) {
            if (!a.getValue().equals(b.getValue())) {
                return b.getValue() - a.getValue();
            }
            if (a.getKey().length() != b.getKey().length()) {
                return b.getKey().length() - a.getKey().length();
            }
            return a.getKey().compareTo(b.getKey());
        }
    }

    private WordCounts() {
        // This class cannot be instantiated
    }
}