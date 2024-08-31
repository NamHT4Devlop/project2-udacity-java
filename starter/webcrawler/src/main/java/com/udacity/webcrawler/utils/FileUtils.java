package com.udacity.webcrawler.utils;

import com.fasterxml.jackson.databind.ObjectMapper;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.io.OutputStreamWriter;
import java.io.Reader;
import java.io.Writer;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.Objects;
import java.util.function.Consumer;
import java.util.function.Function;
import java.util.function.Supplier;

public class FileUtils {

    private static final ObjectMapper objectMapper = new ObjectMapper();
    private static final Logger logger = LoggerFactory.getLogger(FileUtils.class);

    public static ObjectMapper getObjectMapper() {
        return objectMapper;
    }

    private static void logOperation(Runnable operation, String successMessage, String errorMessage) {
        try {
            operation.run();
            logger.info(successMessage);
        } catch (Exception e) {
            logger.error(errorMessage, e);
        }
    }

    private static <T> T logOperation(Supplier<T> operation, String successMessage, String errorMessage) {
        try {
            T result = operation.get();
            logger.info(successMessage);
            return result;
        } catch (Exception e) {
            logger.error(errorMessage, e);
            return null;
        }
    }

    public static void writeToFile(Path path, Consumer<Writer> writeAction) {
        logOperation(() -> {
                    try (Writer writer = Files.newBufferedWriter(path)) {
                        writeAction.accept(writer);
                    } catch (IOException e) {
                        throw new RuntimeException(e);
                    }
                }, "Successfully wrote to file at path: " + path,
                "Failed to write to file at path: " + path);
    }

    public static <T> T readFromFile(Path path, Function<Reader, T> readAction) {
        return logOperation(() -> {
                    try (Reader reader = Files.newBufferedReader(path)) {
                        return readAction.apply(reader);
                    } catch (IOException e) {
                        throw new RuntimeException(e);
                    }
                }, "Successfully read from file at path: " + path,
                "Failed to read from file at path: " + path);
    }

    public static <T> T readFromReader(Reader reader, Class<T> valueType) {
        Objects.requireNonNull(reader, "Reader cannot be null");
        Objects.requireNonNull(valueType, "Value type cannot be null");

        return logOperation(() -> {
                    try {
                        return objectMapper.readValue(reader, valueType);
                    } catch (IOException e) {
                        throw new RuntimeException(e);
                    }
                }, "Successfully read value from reader for type: " + valueType.getName(),
                "Failed to read value from reader for type: " + valueType.getName());
    }

    public static void writeData(String pathString, Consumer<Writer> writeAction) {
        logOperation(() -> {
                    if (pathString != null && !pathString.isEmpty()) {
                        Path path = Paths.get(pathString);
                        writeToFile(path, writeAction);
                    } else {
                        try (Writer writer = new OutputStreamWriter(System.out)) {
                            writeAction.accept(writer);
                        } catch (IOException e) {
                            throw new RuntimeException(e);
                        }
                    }
                }, "\n=== Success ===\nSuccessfully wrote data to path: " + pathString + "\n================\n",
                "\n=== Error ===\nFailed to write data to path: " + pathString + "\n===============\n");
    }

}
