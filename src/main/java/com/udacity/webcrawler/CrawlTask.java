package com.udacity.webcrawler;

import com.udacity.webcrawler.parser.PageParser;
import com.udacity.webcrawler.parser.PageParserFactory;

import java.time.Clock;
import java.time.Instant;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentSkipListSet;
import java.util.concurrent.RecursiveAction;
import java.util.regex.Pattern;
import java.util.stream.Collectors;

public class CrawlTask extends RecursiveAction {
    private final Clock newClock;
    private final Instant newDeadline;
    private final PageParserFactory newParserFactory;
    private final int newMaxDepth;
    private final List<Pattern> newIgnoredUrls;
    private final String newUrl;
    private final ConcurrentHashMap<String, Integer> newCounts;
    private final ConcurrentSkipListSet<String> newVisitedUrls;

    public CrawlTask(Builder builder) {
        this.newClock = builder.newClock;
        this.newDeadline = builder.newDeadline;
        this.newParserFactory = builder.newParserFactory;
        this.newMaxDepth = builder.newMaxDepth;
        this.newIgnoredUrls = builder.newIgnoredUrls;
        this.newUrl = builder.newUrl;
        this.newCounts = builder.newCounts;
        this.newVisitedUrls = builder.newVisitedUrls;
    }

    @Override
    protected void compute() {
        if (shouldStopCrawling()) {
            return;
        }

        if (isUrlIgnoredOrVisited()) {
            return;
        }

        PageParser.Result result = newParserFactory.get(newUrl).parse();
        updateCounts(result);
        List<CrawlTask> subTasks = createSubTasks(result);
        invokeAll(subTasks);
    }

    private boolean shouldStopCrawling() {
        return newMaxDepth == 0 || newClock.instant().isAfter(newDeadline);
    }

    private boolean isUrlIgnoredOrVisited() {
        for (Pattern pattern : newIgnoredUrls) {
            if (pattern.matcher(newUrl).matches()) {
                return true;
            }
        }
        return !newVisitedUrls.add(newUrl);
    }

    private void updateCounts(PageParser.Result result) {
        result.getWordCounts().forEach((word, count) ->
                newCounts.merge(word, count, Integer::sum));
    }

    private List<CrawlTask> createSubTasks(PageParser.Result result) {
        return result.getLinks().stream()
                .map(link -> new Builder()
                        .setClock(newClock)
                        .setDeadline(newDeadline)
                        .setParserFactory(newParserFactory)
                        .setMaxDepth(newMaxDepth - 1)
                        .setIgnoredUrls(newIgnoredUrls)
                        .setUrl(link)
                        .setCounts(newCounts)
                        .setVisitedUrls(newVisitedUrls)
                        .build())
                .collect(Collectors.toList());
    }

    public static final class Builder {
        private Clock newClock;
        private Instant newDeadline;
        private PageParserFactory newParserFactory;
        private int newMaxDepth;
        private List<Pattern> newIgnoredUrls;
        private String newUrl;
        private ConcurrentHashMap<String, Integer> newCounts;
        private ConcurrentSkipListSet<String> newVisitedUrls;

        public CrawlTask build() {
            return new CrawlTask(this);
        }

        public Builder setClock(Clock newClock) {
            this.newClock = newClock;
            return this;
        }

        public Builder setDeadline(Instant newDeadline) {
            this.newDeadline = newDeadline;
            return this;
        }

        public Builder setParserFactory(PageParserFactory newParserFactory) {
            this.newParserFactory = newParserFactory;
            return this;
        }

        public Builder setMaxDepth(int newMaxDepth) {
            this.newMaxDepth = newMaxDepth;
            return this;
        }

        public Builder setIgnoredUrls(List<Pattern> newIgnoredUrls) {
            this.newIgnoredUrls = newIgnoredUrls;
            return this;
        }

        public Builder setUrl(String newUrl) {
            this.newUrl = newUrl;
            return this;
        }

        public Builder setCounts(ConcurrentHashMap<String, Integer> newCounts) {
            this.newCounts = newCounts;
            return this;
        }

        public Builder setVisitedUrls(ConcurrentSkipListSet<String> newVisitedUrls) {
            this.newVisitedUrls = newVisitedUrls;
            return this;
        }
    }
}
