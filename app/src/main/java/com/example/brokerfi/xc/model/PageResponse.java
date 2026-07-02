package com.example.brokerfi.xc.model;

import java.util.List;

public class PageResponse<T> {

    private List<T> content;

    private int number;     // Current page
    private int size;       // page size
    private int totalPages; // Total pages
    private long totalElements;

    public List<T> getContent() {
        return content;
    }

    public int getNumber() {
        return number;
    }

    public int getTotalPages() {
        return totalPages;
    }
}
