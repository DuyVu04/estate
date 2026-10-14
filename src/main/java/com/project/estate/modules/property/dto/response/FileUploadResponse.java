package com.project.estate.modules.property.dto.response;

import lombok.Builder;

@Builder
public record FileUploadResponse(
    String originalFileName, String objectName, String url, String contentType, long size) {}
