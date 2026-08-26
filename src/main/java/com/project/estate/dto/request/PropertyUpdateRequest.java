package com.project.estate.dto.request;

import com.project.estate.enums.PropertyType;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;
import java.math.BigDecimal;
import java.util.List;
import lombok.Builder;

@Builder
public record PropertyUpdateRequest(
    @NotBlank(message = "Title is required") String title,
    @NotBlank(message = "Description is required") String description,
    @NotNull(message = "Property type is required") PropertyType propertyType,
    @NotBlank(message = "Address is required") String address,
    @NotBlank(message = "Ward is required") String ward,
    @NotBlank(message = "District is required") String district,
    @NotBlank(message = "City is required") String city,
    @NotNull(message = "Area is required") @Positive(message = "Area must be greater than 0")
        BigDecimal area,
    @NotNull(message = "Price is required") @Positive(message = "Price must be greater than 0")
        BigDecimal price,
    List<String> imageUrls) {}
