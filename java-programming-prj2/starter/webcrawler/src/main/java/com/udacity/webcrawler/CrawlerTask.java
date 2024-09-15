package com.udacity.webcrawler;

import com.udacity.webcrawler.parser.PageParser;
import com.udacity.webcrawler.parser.PageParserFactory;

import java.time.Clock;
import java.time.Instant;
import java.util.List;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentSkipListSet;
import java.util.concurrent.RecursiveAction;
import java.util.regex.Pattern;
import java.util.stream.Collectors;

public class CrawlerTask extends RecursiveAction {
    private final String url;
    private final Clock clock;
    private final Instant dl;
    private final int maxDepth;
    private final List<Pattern> ignoredUrls;
    private final ConcurrentSkipListSet<String> urlsSkip;
    private final ConcurrentHashMap<String, Integer> counts;
    private final PageParserFactory parserFactory;

    public CrawlerTask(String url, Clock clock, Instant dl, int maxDepth, List<Pattern> ignoredUrls, ConcurrentSkipListSet<String> urlsSkip, ConcurrentHashMap<String, Integer> counts, PageParserFactory parserFactory) {
        this.url = url;
        this.clock = clock;
        this.dl = dl;
        this.maxDepth = maxDepth;
        this.ignoredUrls = ignoredUrls;
        this.urlsSkip = urlsSkip;
        this.counts = counts;
        this.parserFactory = parserFactory;
    }

    @Override
    protected void compute() {
        // End task
        if (maxDepth == 0 || clock.instant().isAfter(dl)) {
            return;
        }

        // Ignore urls
        for (Pattern pattern : ignoredUrls) {
            if (pattern.matcher(url).matches()) {
                return;
            }
        }

        // Skip visited urls
        if (!urlsSkip.add(url)) {
            return;
        }

        // Crawler data
        PageParser.Result result = parserFactory.get(url).parse();

        // Update word counts concurrently
        result.getWordCounts().forEach(
                (word, count) -> counts.compute(word, (k, v) -> v == null ? count : v + count)
        );

        // Fork subtasks concurrently
        List<CrawlerTask> subtasks = result.getLinks().stream()
                .map(childUrl -> new CrawlerTask(childUrl, clock, dl, maxDepth - 1, ignoredUrls, urlsSkip, counts, parserFactory))
                .collect(Collectors.toList());
        invokeAll(subtasks);
    }
}
