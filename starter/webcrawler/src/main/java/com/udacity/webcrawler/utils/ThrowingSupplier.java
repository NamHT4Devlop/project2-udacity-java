package com.udacity.webcrawler.utils;

/**
 * A functional interface that allows a supplier to throw a checked exception.
 *
 * @param <T> the type of results supplied by this supplier
 */
@FunctionalInterface
public interface ThrowingSupplier<T> {
    T get() throws Throwable;
}
