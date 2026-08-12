package com.project.estate.common.response;

import java.util.List;
import org.springframework.data.domain.Page;

public record PageResponse<T>(List<T> items, Pagination pagination) {
  public static <T> PageResponse<T> of(Page<T> page) {
    return new PageResponse<>(
        page.getContent(),
        new Pagination(
            page.getNumber() + 1,
            page.getSize(),
            page.getTotalElements(),
            page.getTotalPages(),
            page.hasNext(),
            page.hasPrevious()));
  }
}
