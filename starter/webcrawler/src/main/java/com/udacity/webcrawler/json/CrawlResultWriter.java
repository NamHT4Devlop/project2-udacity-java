package com.udacity.webcrawler.json;

import com.udacity.webcrawler.utils.FileUtils;

import java.io.IOException;
import java.io.Writer;
import java.nio.file.Path;
import java.util.Objects;

/**
 * Utility class to write a {@link CrawlResult} to file.
 */
public final class CrawlResultWriter {
    private final CrawlResult result;

    /**
     * Creates a new {@link CrawlResultWriter} that will write the given {@link CrawlResult}.
     */
    public CrawlResultWriter(CrawlResult result) {
        this.result = Objects.requireNonNull(result);
    }

    /**
     * Formats the {@link CrawlResult} as JSON and writes it to the given {@link Path}.
     *
     * <p>If a file already exists at the path, the existing file should not be deleted; new data
     * should be appended to it.
     *
     * @param path the file path where the crawl result data should be written.
     */
    public void write(Path path) {
        Objects.requireNonNull(path, "Path cannot be null");

        // Use FileUtils.writeToFile to handle file writing
        FileUtils.writeToFile(path, writer -> {
            try {
                // Wrap the writer with a NonClosingWriter to prevent it from being closed by ObjectMapper
                FileUtils.getObjectMapper().writeValue(new NonClosingWriter(writer), result);
            } catch (IOException e) {
                throw new RuntimeException("Failed to write CrawlResult", e);
            }
        });
    }

    /**
     * Formats the {@link CrawlResult} as JSON and writes it to the given {@link Writer}.
     *
     * @param writer the destination where the crawl result data should be written.
     */
    public void write(Writer writer) {
        Objects.requireNonNull(writer, "Writer cannot be null");

        try {
            // Wrap the writer with a NonClosingWriter to prevent it from being closed by ObjectMapper
            FileUtils.getObjectMapper().writeValue(new NonClosingWriter(writer), result);
        } catch (IOException e) {
            throw new RuntimeException("Failed to write CrawlResult", e);
        }
    }

    /**
     * A custom Writer wrapper that prevents the underlying Writer from being closed.
     */
    private static class NonClosingWriter extends Writer {
        private final Writer originalWriter;

        NonClosingWriter(Writer originalWriter) {
            this.originalWriter = originalWriter;
        }

        @Override
        public void write(char[] cbuf, int off, int len) throws IOException {
            originalWriter.write(cbuf, off, len);
        }

        @Override
        public void flush() throws IOException {
            originalWriter.flush();
        }

        @Override
        public void close() throws IOException {
            // Do nothing to prevent closing the underlying Writer
        }
    }
}
