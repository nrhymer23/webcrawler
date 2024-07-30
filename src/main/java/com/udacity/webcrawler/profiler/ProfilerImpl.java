package com.udacity.webcrawler.profiler;

import javax.inject.Inject;
import java.io.IOException;
import java.io.Writer;
import java.lang.reflect.Proxy;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardOpenOption;
import java.time.Clock;
import java.time.ZonedDateTime;
import java.util.Arrays;
import java.util.Objects;

import static java.time.format.DateTimeFormatter.RFC_1123_DATE_TIME;

/**
 * Concrete implementation of the {@link Profiler}.
 */
final class ProfilerImpl implements Profiler {

  private final Clock newClock;
  private final ProfilingState newState = new ProfilingState();
  private final ZonedDateTime newStartTime;

  @Inject
  ProfilerImpl(Clock newClock) {
    this.newClock = Objects.requireNonNull(newClock);
    this.newStartTime = ZonedDateTime.now(newClock);
  }


  private Boolean profiledClass(Class<?>klass){
    return Arrays.stream(klass.getDeclaredMethods())
            .anyMatch(method -> method.isAnnotationPresent(Profiled.class));
  }


  @Override
  public <T> T wrap(Class<T> klass, T delegate) {
    Objects.requireNonNull(klass);

    // TODO: Use a dynamic proxy (java.lang.reflect.Proxy) to "wrap" the delegate in a
    //       ProfilingMethodInterceptor and return a dynamic proxy from this method.
    //       See https://docs.oracle.com/javase/10/docs/api/java/lang/reflect/Proxy.html.

  
    if (profiledClass(klass) == false) {
      throw new IllegalArgumentException("No methods annotated with @Profiled found in " + klass.getName());
  }
  

    ProfilingMethodInterceptor interceptor = new ProfilingMethodInterceptor(newClock, delegate, newState);

    Object proxy = Proxy.newProxyInstance(
      ProfilerImpl.class.getClassLoader(), // The class loader to define the proxy class
      new Class<?>[]{klass}, // The interfaces to be implemented by the proxy class
      interceptor // The invocation handler to dispatch method invocations to
  );

    return (T) proxy;
  }

  @Override
  public void writeData(Path path) {
    // TODO: Write the ProfilingState data to the given file path. If a file already exists at that
    //       path, the new data should be appended to the existing file.

    Objects.requireNonNull(path);

    try (Writer writer = Files.newBufferedWriter(path, StandardOpenOption.CREATE, StandardOpenOption.APPEND)) {
      writeData(writer);
  } catch (IOException e) {
      System.err.println("Error writing data to file: " + e.getMessage());
      e.printStackTrace();
  }
}
  @Override
  public void writeData(Writer writer) throws IOException {
    writer.write(System.lineSeparator());
    writer.write("Run at " + RFC_1123_DATE_TIME.format(newStartTime));
    writer.write(System.lineSeparator());
    newState.write(writer);
    writer.write(System.lineSeparator());
  }
}
