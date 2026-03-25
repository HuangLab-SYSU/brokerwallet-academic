package com.example.brokerfi.xc.model;

import java.util.List;

public class PageResponse<T> {

    private List<T> content;

    private int number;     // 当前页
    private int size;       // 每页大小
    private int totalPages; // 总页数
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
