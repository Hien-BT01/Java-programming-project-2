package com.udacity.webcrawler.json;

import com.fasterxml.jackson.core.JsonParser;
import com.fasterxml.jackson.core.exc.StreamReadException;
import com.fasterxml.jackson.databind.DatabindException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.json.JsonMapper;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.Reader;
import java.io.UncheckedIOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Objects;
import java.util.logging.Logger;

/**
 * A static utility class that loads a JSON configuration file.
 */
public final class ConfigurationLoader {

  private final Path path;

  private static final Logger LOGGER = Logger.getLogger(ConfigurationLoader.class.getName());

  /**
   * Create a {@link ConfigurationLoader} that loads configuration from the given {@link Path}.
   */
  public ConfigurationLoader(Path path) {
    this.path = Objects.requireNonNull(path);
  }

  /**
   * Loads configuration from this {@link ConfigurationLoader}'s path
   *
   * @return the loaded {@link CrawlerConfiguration}.
   */
  public CrawlerConfiguration load() {
    try (BufferedReader reader = Files.newBufferedReader(path)) {
      return read(reader);
    } catch (IOException e) {
      LOGGER.info("error: " + e.getMessage());
    }
    return null;
  }

  /**
   * Loads crawler configuration from the given reader.
   *
   * @param reader a Reader pointing to a JSON string that contains crawler configuration.
   * @return a crawler configuration
   */
  public static CrawlerConfiguration read(Reader reader) {
    // This is here to get rid of the unused variable warning.
    Objects.requireNonNull(reader);
    // read the JSON input and parse it into a CrawlerConfiguration using the Jackson JSON library
    ObjectMapper mapper = new ObjectMapper();
    mapper.disable(JsonParser.Feature.AUTO_CLOSE_SOURCE);
    try {
      return mapper.readValue(reader, CrawlerConfiguration.class);
    } catch (IOException e) {
      LOGGER.info("Error: " + e.getMessage());
    }
    return null;
  }
}
