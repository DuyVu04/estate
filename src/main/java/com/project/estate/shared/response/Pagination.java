package com.project.estate.shared.response;

public record Pagination(
    int page, int size, long totalElements, int totalPages, boolean hasNext, boolean hasPrevious) {}
