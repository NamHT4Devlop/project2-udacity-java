package com.udacity.webcrawler;

import com.udacity.webcrawler.json.CrawlResult;

import com.udacity.webcrawler.parser.PageParserFactory;

import javax.inject.Inject;
import java.time.Clock;
import java.time.Duration;
import java.time.Instant;
import java.util.Collections;
import java.util.List;
import java.util.Optional;
import java.util.concurrent.*;
import java.util.regex.Pattern;
import java.util.stream.Collectors;
import java.util.logging.Level;
import java.util.logging.Logger;

/**
 * A concrete implementation of {@link WebCrawler} that runs multiple threads on a
 * {@link ForkJoinPool} to fetch and process multiple web pages in parallel.
 */
public final class ParallelWebCrawler implements WebCrawler {

    private static final Logger logger = Logger.getLogger(ParallelWebCrawler.class.getName());

    private final Clock clock;
    private final Duration timeout;
    private final int popularWordCount;
    private final ForkJoinPool pool;
    private final int maximumDepth;
    private final List<Pattern> skippedURLs;
    private final PageParserFactory parserBuilder;

    @Inject
    ParallelWebCrawler(
            Clock clock,
            @Timeout Duration timeout,
            @PopularWordCount int popularWordCount,
            @TargetParallelism int threadCount,
            @MaxDepth int maximumDepth,
            @IgnoredUrls List<Pattern> skippedURLs,
            PageParserFactory parserBuilder) {
        this.clock = clock;
        this.timeout = timeout;
        this.popularWordCount = popularWordCount;
        this.pool = new ForkJoinPool(Math.min(threadCount, getMaxParallelism()));
        this.maximumDepth = maximumDepth;
        this.skippedURLs = skippedURLs;
        this.parserBuilder = parserBuilder;
    }

    @Override
    public CrawlResult crawl(List<String> startingUrls) {
        Instant completionDate = clock.instant().plus(timeout);
        ConcurrentHashMap<String, Integer> stringIntegerConcurrentHashMap = new ConcurrentHashMap<>();
        ConcurrentSkipListSet<String> stringConcurrentSkipListSet = new ConcurrentSkipListSet<>();

        try {
            logger.info("Starting crawl with URLs: " + startingUrls);
            List<ForkJoinTask<Void>> tasks = startingUrls.stream()
                    .map(url -> new CrawlTask(new CrawlTaskData(url, completionDate, maximumDepth, stringIntegerConcurrentHashMap, stringConcurrentSkipListSet)))
                    .collect(Collectors.toList());

            ForkJoinTask.invokeAll(tasks);
            logger.info("Crawl tasks submitted for all starting URLs.");
        } catch (Exception e) {
            logger.log(Level.SEVERE, "Error during crawl execution", e);
        }

        return Optional.of(stringIntegerConcurrentHashMap)
                .filter(map -> !map.isEmpty())
                .map(map -> {
                    logger.info("Crawl completed successfully with word counts and visited URLs.");
                    return new CrawlResult.Builder()
                            .setWordCounts(WordCounts.sort(map, popularWordCount))
                            .setUrlsVisited(stringConcurrentSkipListSet.size())
                            .build();
                })
                .orElseGet(() -> {
                    logger.info("No word counts found during crawl.");
                    return new CrawlResult.Builder()
                            .setWordCounts(stringIntegerConcurrentHashMap)
                            .setUrlsVisited(stringConcurrentSkipListSet.size())
                            .build();
                });
    }

    @Override
    public int getMaxParallelism() {
        return Runtime.getRuntime().availableProcessors();
    }

    // Record for CrawlTask data
    private record CrawlTaskData(
            String hyperlink,
            Instant dueDate,
            int depthLimit,
            ConcurrentHashMap<String, Integer> integerConcurrentHashMap,
            ConcurrentSkipListSet<String> concurrentSkipListSet) {
    }

    private class CrawlTask extends RecursiveTask<Void> {
        private final CrawlTaskData data;

        CrawlTask(CrawlTaskData data) {
            this.data = data;
        }

        @Override
        protected Void compute() {
            try {
                if (shouldProcessUrl()) {
                    logger.info("URL skipped (not processed): " + data.hyperlink());
                    return null;
                }

                data.concurrentSkipListSet().add(data.hyperlink());
                logger.info("Processing URL: " + data.hyperlink());
                processPage();

                invokeAll(createSubtasks());
                logger.info("Subtasks created and invoked for URL: " + data.hyperlink());
            } catch (Exception e) {
                logger.log(Level.SEVERE, "Error processing URL: " + data.hyperlink(), e);
            }
            return null;
        }

        private boolean shouldProcessUrl() {
            try {
                return data.depthLimit() <= 0 ||
                        Instant.now().isAfter(data.dueDate()) ||
                        skippedURLs.stream().anyMatch(pattern -> pattern.matcher(data.hyperlink()).matches()) ||
                        !data.concurrentSkipListSet().add(data.hyperlink());
            } catch (Exception e) {
                logger.log(Level.WARNING, "Failed to determine if URL should be processed: " + data.hyperlink(), e);
                return true;
            }
        }

        private void processPage() {
            try {
                var result = parserBuilder.get(data.hyperlink()).parse();
                result.getWordCounts().forEach((word, count) ->
                        data.integerConcurrentHashMap().merge(word, count, Integer::sum)
                );
                logger.info("Page processed successfully for URL: " + data.hyperlink());
            } catch (Exception e) {
                logger.log(Level.SEVERE, "Error parsing page for URL: " + data.hyperlink(), e);
            }
        }

        private List<CrawlTask> createSubtasks() {
            try {
                return parserBuilder.get(data.hyperlink()).parse().getLinks().stream()
                        .map(link -> new CrawlTask(new CrawlTaskData(
                                link,
                                data.dueDate(),
                                data.depthLimit() - 1,
                                data.integerConcurrentHashMap(),
                                data.concurrentSkipListSet()
                        )))
                        .collect(Collectors.toList());
            } catch (Exception e) {
                logger.log(Level.SEVERE, "Error creating subtasks for URL: " + data.hyperlink(), e);
                return Collections.emptyList();
            }
        }
    }
}
