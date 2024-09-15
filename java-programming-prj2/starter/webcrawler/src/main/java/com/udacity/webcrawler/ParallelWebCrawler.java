package com.udacity.webcrawler;

import com.udacity.webcrawler.json.CrawlResult;
import com.udacity.webcrawler.parser.PageParser;
import com.udacity.webcrawler.parser.PageParserFactory;
import com.udacity.webcrawler.profiler.Profiler;

import javax.inject.Inject;
import javax.inject.Provider;
import java.time.Clock;
import java.time.Duration;
import java.time.Instant;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;
import java.util.concurrent.ConcurrentSkipListSet;
import java.util.concurrent.ForkJoinPool;
import java.util.regex.Pattern;
import java.util.stream.Collectors;

/**
 * A concrete implementation of {@link WebCrawler} that runs multiple threads on a
 * {@link ForkJoinPool} to fetch and process multiple web pages in parallel.
 */
final class ParallelWebCrawler implements WebCrawler {
  private final Clock clock;
  private final Duration timeout;
  private final int popularWordCount;
  private final ForkJoinPool pool;
  private final int maxDepth;
  private final List<Pattern> ignoredUrls;
  private final PageParserFactory parserFactory;

  @Inject
  ParallelWebCrawler(
          Clock clock,
          PageParserFactory parserFactory,
          @Timeout Duration timeout,
          @PopularWordCount int popularWordCount,
          @TargetParallelism int threadCount,
          @MaxDepth int maxDepth,
          @IgnoredUrls List<Pattern> ignoredUrls) {
    this.clock = clock;
    this.parserFactory = parserFactory;
    this.timeout = timeout;
    this.popularWordCount = popularWordCount;
    this.maxDepth = maxDepth;
    this.ignoredUrls = ignoredUrls;
    this.pool = new ForkJoinPool(Math.min(threadCount, getMaxParallelism()));
  }


  @Override
  public CrawlResult crawl(List<String> startingUrls) {
    Instant deadline = clock.instant().plus(timeout);
    ConcurrentHashMap<String, Integer> wordCounts = new ConcurrentHashMap<>();
    ConcurrentSkipListSet<String> visitedUrls = new ConcurrentSkipListSet<>();

// Stream through starting URLs and submit tasks to the pool
    startingUrls.forEach(url ->
            pool.invoke(new CrawlerTask(url, clock, deadline, maxDepth, ignoredUrls,
                    visitedUrls, wordCounts, parserFactory))
    );

// Build the result based on whether wordCounts is empty or not
    CrawlResult.Builder resultBuilder = new CrawlResult.Builder()
            .setUrlsVisited(visitedUrls.size());

    if (!wordCounts.isEmpty()) {
      resultBuilder.setWordCounts(WordCounts.sort(wordCounts, popularWordCount));
    } else {
      resultBuilder.setWordCounts(wordCounts);
    }

    return resultBuilder.build();
  }

  @Override
  public int getMaxParallelism() {
    return Runtime.getRuntime().availableProcessors();
  }
}
