package com.example.tax.dto;

import java.util.Collections;
import java.util.List;

public class Page<T> {

    private final List<T> items;
    private final int page;
    private final int size;
    private final long total;

    public Page(List<T> items, int page, int size, long total) {
        this.items = items == null ? Collections.emptyList() : items;
        this.page = page;
        this.size = size;
        this.total = total;
    }

    public List<T> getItems() {
        return items;
    }

    public int getPage() {
        return page;
    }

    public int getSize() {
        return size;
    }

    public long getTotal() {
        return total;
    }

    public int getTotalPages() {
        if (total <= 0) {
            return 0;
        }
        return (int) ((total + size - 1) / size);
    }

    public boolean isHasPrevious() {
        return page > 1;
    }

    public boolean isHasNext() {
        return page < getTotalPages();
    }

    public int getPreviousPage() {
        return Math.max(page - 1, 1);
    }

    public int getNextPage() {
        return Math.min(page + 1, Math.max(getTotalPages(), 1));
    }

    public int getFromRow() {
        return total == 0 ? 0 : (page - 1) * size + 1;
    }

    public int getToRow() {
        return (int) Math.min((long) page * size, total);
    }
}
