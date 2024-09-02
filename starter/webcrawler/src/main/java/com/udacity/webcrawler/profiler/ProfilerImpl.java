package com.udacity.webcrawler.profiler;

import com.udacity.webcrawler.utils.FileUtils;

import javax.inject.Inject;
import java.io.IOException;
import java.io.Writer;
import java.lang.reflect.Method;
import java.lang.reflect.Proxy;
import java.nio.file.Path;
import java.time.Clock;
import java.time.ZonedDateTime;
import java.util.HashSet;
import java.util.Objects;
import java.util.Set;

import static java.time.format.DateTimeFormatter.RFC_1123_DATE_TIME;

/**
 * Concrete implementation of the {@link Profiler}.
 */
final class ProfilerImpl implements Profiler {

    private final Clock clock;
    private final ProfilingState state = new ProfilingState();
    private final ZonedDateTime startTime;
    private final Set<Object> alternativeProxies = new HashSet<>();

    @Inject
    ProfilerImpl(Clock clock) {
        this.clock = Objects.requireNonNull(clock);
        this.startTime = ZonedDateTime.now(clock);
    }

    @Override
    public <T> T wrap(Class<T> klass, T delegate) {
        // Validate that neither the class nor the delegate instance is null.
        validateInputs(klass, delegate);

        // TODO: Use a dynamic proxy (java.lang.reflect.Proxy) to "wrap" the delegate in a
        //       ProfilingMethodInterceptor and return a dynamic proxy from this method.
        //       See https://docs.oracle.com/javase/10/docs/api/java/lang/reflect/Proxy.html.

        // Ensure that the class interface has at least one method annotated with @Profiled.
        ensureHasProfiledMethods(klass);

        // Create a dynamic proxy that wraps the delegate object, which will be used for profiling.
        T proxy = createProxy(klass, delegate);

        // Register the created proxy to keep track of it.
        registerProxy(proxy);

        // Return the created proxy.
        return proxy;
    }

    @Override
    public void writeData(Path path) {
        // TODO: Write the ProfilingState data to the given file path. If a file already exists at that
        //       path, the new data should be appended to the existing file.
        FileUtils.writeToFile(path, writer -> {
            try {
                writeData(writer);
            } catch (IOException e) {
                throw new RuntimeException(e);
            }
        });
    }

    @Override
    public void writeData(Writer writer) throws IOException {
        writer.write("Run at " + RFC_1123_DATE_TIME.format(startTime));
        writer.write(System.lineSeparator());
        state.write(writer);
        writer.write(System.lineSeparator());
    }

    private <T> void validateInputs(Class<T> klass, T delegate) {
        // Check that the class and delegate are not null.
        Objects.requireNonNull(klass, "The class must not be null.");
        Objects.requireNonNull(delegate, "The delegate instance must not be null.");
    }

    private <T> void ensureHasProfiledMethods(Class<T> klass) {
        // Get all declared methods from the class interface.
        Method[] methods = klass.getDeclaredMethods();

        // Use a flag to track if at least one method is annotated with @Profiled.
        boolean hasProfiledMethod = false;

        for (Method method : methods) {
            // Use an alternative way to check for annotation presence.
            if (method.getAnnotationsByType(Profiled.class).length > 0) {
                hasProfiledMethod = true;
                break;
            }
        }
        if (!hasProfiledMethod) {
            throw new IllegalArgumentException("No methods with @Profiled in the interface.");
        }
    }

    private <T> T createProxy(Class<T> klass, T delegate) {
        // Create and return a dynamic proxy that intercepts method calls to the delegate object.
        // The proxy uses ProfilingMethodInterceptor to measure and record execution time for @Profiled methods.
        return (T) Proxy.newProxyInstance(
                klass.getClassLoader(),
                new Class<?>[]{klass},
                new ProfilingMethodInterceptor(clock, state, delegate)
        );
    }

    private <T> void registerProxy(T proxy) {
        // Add the created proxy to the set of proxies to prevent it from being garbage collected.
        alternativeProxies.add(proxy);
    }
}
