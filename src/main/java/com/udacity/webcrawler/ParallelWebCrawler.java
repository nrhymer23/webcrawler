package com.udacity.webcrawler;

import com.udacity.webcrawler.json.CrawlResult;
import com.udacity.webcrawler.parser.PageParserFactory;

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
import java.util.concurrent.ConcurrentSkipListSet;
import java.util.concurrent.ForkJoinPool;
import java.util.regex.Pattern;
import java.util.stream.Collectors;

/**
 * A concrete implementation of {@link WebCrawler} that runs multiple threads on a
 * {@link ForkJoinPool} to fetch and process multiple web pages in parallel.
 */
final class ParallelWebCrawler implements WebCrawler {
  private final Clock newClock;
  private final Duration newTimeout;
  private final int newPopularWordCount;
  private final ForkJoinPool newPool;
  private final PageParserFactory newParserFactory;
  private final int newMaxDepth;
  private final List<Pattern> newIgnoredUrls;


  @Inject
  ParallelWebCrawler(
      Clock newClock,
      @Timeout Duration newTimeout,
      @PopularWordCount int newPopularWordCount,
      @TargetParallelism int threadCount,
      PageParserFactory newParserFactory,
      @MaxDepth int newMaxDepth,
      @IgnoredUrls List<Pattern> newIgnoredUrls) {

    this.newClock = newClock;
    this.newTimeout = newTimeout;
    this.newPopularWordCount = newPopularWordCount;
    this.newPool = new ForkJoinPool(Math.min(threadCount, getMaxParallelism()));
    this.newParserFactory = newParserFactory;
    this.newMaxDepth = newMaxDepth;
    this.newIgnoredUrls = newIgnoredUrls;
  }

  
  @Override
  public CrawlResult crawl(List<String> startingUrls) {

    Instant newDeadline = newClock.instant().plus(newTimeout);
    ConcurrentHashMap<String, Integer> newCounts = new ConcurrentHashMap<>();
    ConcurrentSkipListSet<String> newVisitedUrls = new ConcurrentSkipListSet<>();

    for(String newUrl : startingUrls) {
      if(!newClock.instant().isAfter(newDeadline)){
        CrawlTask crawlTask = new CrawlTask.Builder()
                .setCounts(newCounts)
                .setUrl(newUrl)
                .setDeadline(newDeadline)
                .setClock(newClock)
                .setMaxDepth(newMaxDepth)
                .setIgnoredUrls(newIgnoredUrls)
                .setParserFactory(newParserFactory)
                .setVisitedUrls(newVisitedUrls)
                .build();

        newPool.invoke(crawlTask);
      }

    }

    if(newCounts.isEmpty()) {
      return new CrawlResult.Builder()
              .setWordCounts(newCounts)
              .setUrlsVisited(newVisitedUrls.size())
              .build();
    }

    return new CrawlResult.Builder()
            .setWordCounts(WordCounts.sort(newCounts, newPopularWordCount))
            .setUrlsVisited(newVisitedUrls.size())
            .build();
  }

  @Override
  public int getMaxParallelism() {
    return Runtime.getRuntime().availableProcessors();
  }
}
