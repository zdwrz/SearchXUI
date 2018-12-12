package com.antra.tools.realtime;

import java.util.ArrayList;
import java.util.List;

public class SearchResultPojo {
    private String fileName;
    private List<String> matchingLines;

    public SearchResultPojo() {
    }

    public SearchResultPojo(String fileName) {
        this.fileName = fileName;
        matchingLines = new ArrayList<>();
    }

    public SearchResultPojo(String fileName, List<String> matchingLines) {
        this.fileName = fileName;
        this.matchingLines = matchingLines;
    }

    public String getFileName() {
        return fileName;
    }

    public List<String> getMatchingLines() {
        return matchingLines;
    }
}
