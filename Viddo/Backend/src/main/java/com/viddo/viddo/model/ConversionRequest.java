package com.viddo.viddo.model;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.Data;

@Data
public class ConversionRequest {

    @NotBlank(message = "URL is required")
    private String url;

    @NotNull(message = "Output format is required")
    private ConversionJob.Format outputFormat;
}