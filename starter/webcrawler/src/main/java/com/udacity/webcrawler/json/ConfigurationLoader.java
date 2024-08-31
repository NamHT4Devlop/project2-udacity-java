package com.udacity.webcrawler.json;

import com.fasterxml.jackson.databind.annotation.JsonDeserialize;
import com.udacity.webcrawler.utils.FileUtils;

import java.io.IOException;
import java.io.Reader;
import java.nio.file.Path;
import java.util.Objects;

/**
 * A static utility class that loads a JSON configuration file.
 */
@JsonDeserialize(builder = CrawlerConfiguration.Builder.class)
public final class ConfigurationLoader {

    private final Path path;

    /**
     * Create a {@link ConfigurationLoader} that loads configuration from the given {@link Path}.
     */
    public ConfigurationLoader(Path path) {
        this.path = Objects.requireNonNull(path);
    }

    /**
     * Loads configuration from this {@link ConfigurationLoader}'s path.
     *
     * @return the loaded {@link CrawlerConfiguration}.
     */
    public CrawlerConfiguration load() {
        // TODO: Fill in this method.
        // Load configuration from the file using FileUtils with a NonClosingReader
        return FileUtils.readFromFile(path, reader -> FileUtils.readFromReader(new NonClosingReader(reader), CrawlerConfiguration.class));
    }

    /**
     * Loads crawler configuration from the given reader.
     *
     * @param reader a Reader pointing to a JSON string that contains crawler configuration.
     * @return a crawler configuration.
     */
    public static CrawlerConfiguration read(Reader reader) {
        // TODO: Fill in this method.
        // Read configuration from the provided reader using FileUtils with a NonClosingReader
        return FileUtils.readFromReader(new NonClosingReader(reader), CrawlerConfiguration.class);
    }

    /**
     * A custom Reader wrapper that prevents the underlying Reader from being closed.
     */
    private static class NonClosingReader extends Reader {
        private final Reader originalReader;

        NonClosingReader(Reader originalReader) {
            this.originalReader = originalReader;
        }

        @Override
        public int read(char[] cbuf, int off, int len) throws IOException {
            return originalReader.read(cbuf, off, len);
        }

        @Override
        public void close() {
            // Override close to prevent closing the underlying reader
        }

        @Override
        public boolean ready() throws IOException {
            return originalReader.ready();
        }

        @Override
        public void mark(int readAheadLimit) throws IOException {
            originalReader.mark(readAheadLimit);
        }

        @Override
        public void reset() throws IOException {
            originalReader.reset();
        }

        @Override
        public long skip(long n) throws IOException {
            return originalReader.skip(n);
        }

        @Override
        public boolean markSupported() {
            return originalReader.markSupported();
        }
    }
}
