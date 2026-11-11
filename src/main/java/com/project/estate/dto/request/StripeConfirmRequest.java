package com.project.estate.dto.request;

import com.fasterxml.jackson.annotation.JsonAlias;
import lombok.Builder;

@Builder
public record StripeConfirmRequest(@JsonAlias({"session_id", "sessionId"}) String sessionId) {}
