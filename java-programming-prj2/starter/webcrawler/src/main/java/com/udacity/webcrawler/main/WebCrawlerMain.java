package com.udacity.webcrawler.main;

import com.google.inject.Guice;
import com.udacity.webcrawler.WebCrawler;
import com.udacity.webcrawler.WebCrawlerModule;
import com.udacity.webcrawler.json.ConfigurationLoader;
import com.udacity.webcrawler.json.CrawlResult;
import com.udacity.webcrawler.json.CrawlResultWriter;
import com.udacity.webcrawler.json.CrawlerConfiguration;
import com.udacity.webcrawler.profiler.Profiler;
import com.udacity.webcrawler.profiler.ProfilerModule;

import javax.inject.Inject;
import java.io.BufferedWriter;
import java.io.OutputStreamWriter;
import java.io.Writer;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.Objects;
import java.util.logging.Logger;

public final class WebCrawlerMain {

  private final CrawlerConfiguration config;

  private WebCrawlerMain(CrawlerConfiguration config) {
    this.config = Objects.requireNonNull(config);
  }

  private static final Logger LOGGER = Logger.getLogger(WebCrawlerMain.class.getName());

  @Inject
  private WebCrawler crawler;

  @Inject
  private Profiler profiler;

  private void run() throws Exception {
    Guice.createInjector(new WebCrawlerModule(config), new ProfilerModule()).injectMembers(this);

    CrawlResult result = crawler.crawl(config.getStartPages());
    CrawlResultWriter resultWriter = new CrawlResultWriter(result);

    String resultPath = config.getResultPath();
    System.out.println(config);
    System.out.println(config.getProfileOutputPath());
    String profileOutputPath = config.getProfileOutputPath();
    try (Writer outputStreamWriter = new OutputStreamWriter(System.out)) {
        if (resultPath.isEmpty()) {
            resultWriter.write(outputStreamWriter);
            outputStreamWriter.flush();
        } else {
            Path path = Paths.get(resultPath);
            resultWriter.write(path);
        }
        if (profileOutputPath.isEmpty()) {
            profiler.writeData(outputStreamWriter);
            outputStreamWriter.flush();
        } else {
            Path path = Paths.get(profileOutputPath);
            profiler.writeData(path);
        }

    } catch (Exception ex) {
        LOGGER.info("Error at WeCrawlerMain: " + ex.getMessage());
    }
  }

  public static void main(String[] args) throws Exception {
    if (args.length != 1) {
      System.out.println("Usage: WebCrawlerMain [starting-url]");
      return;
    }

    CrawlerConfiguration config = new ConfigurationLoader(Path.of(args[0])).load();
    new WebCrawlerMain(config).run();
  }
}
