package com.udacity.webcrawler.utils;

import java.io.IOException;

@FunctionalInterface
public interface IOOperation {
    void execute() throws IOException;
}
