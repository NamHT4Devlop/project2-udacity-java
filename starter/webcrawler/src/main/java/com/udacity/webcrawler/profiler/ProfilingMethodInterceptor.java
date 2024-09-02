package com.udacity.webcrawler.profiler;

import com.udacity.webcrawler.utils.ThrowingSupplier;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.lang.reflect.InvocationHandler;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.time.Clock;
import java.time.Duration;
import java.time.Instant;
import java.util.Objects;

/**
 * A method interceptor that checks whether {@link Method}s are annotated with the {@link Profiled}
 * annotation. If they are, the method interceptor records how long the method invocation took.
 */
final class ProfilingMethodInterceptor implements InvocationHandler {

    private static final Logger logger = LoggerFactory.getLogger(ProfilingMethodInterceptor.class);

    private final Clock clock;
    private final ProfilingState profilingState;
    private final Object targetObject;

    // TODO: You will need to add more instance fields and constructor arguments to this class.
    ProfilingMethodInterceptor(Clock clock, ProfilingState profilingState, Object targetObject) {
        this.clock = Objects.requireNonNull(clock);
        this.profilingState = Objects.requireNonNull(profilingState);
        this.targetObject = Objects.requireNonNull(targetObject);
    }

    @Override
    public Object invoke(Object proxy, Method method, Object[] args) throws Throwable {
        // TODO: This method interceptor should inspect the called method to see if it is a profiled
        //       method. For profiled methods, the interceptor should record the start time, then
        //       invoke the method using the object that is being profiled. Finally, for profiled
        //       methods, the interceptor should record how long the method call took, using the
        //       ProfilingState methods.

        if (method.isAnnotationPresent(Profiled.class)) {
            return profileMethod(() -> executeMethod(method, args), method);
        } else {
            return executeMethod(method, args);
        }
    }

    private Object profileMethod(ThrowingSupplier<Object> supplier, Method method) throws Throwable {
        return TimedExecution.of(clock).run(supplier, profilingState, targetObject.getClass(), method);
    }


    /**
     * Invokes a method via reflection with detailed logging.
     *
     * @param method     The method to be invoked
     * @param parameters The parameters to pass to the method
     * @return The result returned by the invoked method
     * @throws Throwable If the invoked method throws an exception
     */
    public Object executeMethod(Method method, Object[] parameters) throws Throwable {
        logger.debug("Starting method invocation: {}", method.getName());

        try {
            Object result = method.invoke(targetObject, parameters);
            logger.debug("Method {} executed successfully with result: {}", method.getName(), result);
            return result;
        } catch (InvocationTargetException ite) {
            Throwable originalException = ite.getCause();
            logger.error("Method {} threw an exception: {}", method.getName(), originalException.toString());
            throw originalException;
        } catch (IllegalAccessException iae) {
            logger.error("Cannot access method {}: {}", method.getName(), iae.toString());
            // Wrap IllegalAccessException in a RuntimeException
            throw new RuntimeException("Illegal access during method invocation", iae);
        } catch (RuntimeException re) {
            logger.error("Runtime exception occurred while invoking method {}: {}", method.getName(), re.toString());
            throw re;
        } catch (Exception e) {
            logger.error("Unexpected exception occurred while invoking method {}: {}", method.getName(), e.toString());
            throw new RuntimeException("Unexpected exception during method invocation", e);
        } finally {
            logger.debug("Method invocation completed: {}", method.getName());
        }
    }



    /**
     * Utility class to encapsulate timing and profiling logic.
     */
    private static class TimedExecution {
        private final Clock clock;
        private Instant start;

        private TimedExecution(Clock clock) {
            this.clock = clock;
        }

        static TimedExecution of(Clock clock) {
            return new TimedExecution(clock);
        }

        <T> T run(ThrowingSupplier<T> action, ProfilingState profilingState, Class<?> targetClass, Method method) throws Throwable {
            start = clock.instant();
            try {
                return action.get();
            } finally {
                // Inline calculation of duration using milliseconds difference
                long elapsedMillis = clock.millis() - start.toEpochMilli();
                profilingState.record(targetClass, method, Duration.ofMillis(elapsedMillis));
            }
        }
    }

}
