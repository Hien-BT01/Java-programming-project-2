package com.udacity.webcrawler.profiler;

import javax.inject.Inject;
import java.io.BufferedWriter;
import java.io.IOException;
import java.io.Writer;
import java.lang.reflect.Proxy;
import java.nio.file.Files;
import java.nio.file.Path;
import java.time.Clock;
import java.time.ZonedDateTime;
import java.util.Arrays;
import java.util.Objects;

import static java.time.format.DateTimeFormatter.RFC_1123_DATE_TIME;

/**
 * Concrete implementation of the {@link Profiler}.
 */
final class ProfilerImpl implements Profiler {

  private final Clock clock;
  private final ProfilingState state = new ProfilingState();
  private final ZonedDateTime startTime;

  @Inject
  ProfilerImpl(Clock clock) {
    this.clock = Objects.requireNonNull(clock);
    this.startTime = ZonedDateTime.now(clock);
  }

  private boolean profileClass(Class<?> clazz) {
    // Use Arrays.stream instead of creating an ArrayList
    return Arrays.stream(clazz.getDeclaredMethods())
            .anyMatch(method -> method.isAnnotationPresent(Profiled.class));
  }

  @SuppressWarnings("unchecked")
  @Override
  public <T> T wrap(Class<T> klass, T delegate) {
    Objects.requireNonNull(klass, "Class cannot be null"); // More descriptive message

    if (!profileClass(klass)) {
      throw new IllegalArgumentException("Class must be annotated with @Profiled");
    }

    ProfilingMethodInterceptor interceptor = new ProfilingMethodInterceptor(clock, delegate, state);

    return (T) Proxy.newProxyInstance(
            ProfilerImpl.class.getClassLoader(),
            new Class[]{klass},
            interceptor
    );
  }

  @Override
  public void writeData(Path path) {
    Objects.requireNonNull(path);

    try(BufferedWriter writer = Files.newBufferedWriter(path)) {
      writeData(writer);
      writer.flush();
    } catch (IOException ex) {
      throw new RuntimeException(ex);
    }
  }

  @Override
  public void writeData(Writer writer) throws IOException {
    writer.write("Run at " + RFC_1123_DATE_TIME.format(startTime));
    writer.write(System.lineSeparator());
    state.write(writer);
    writer.write(System.lineSeparator());
  }
}
