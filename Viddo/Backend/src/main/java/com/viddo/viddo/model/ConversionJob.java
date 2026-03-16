package com.viddo.viddo.model;

import lombok.Data;
import java.time.LocalDateTime;
import java.util.UUID;

@Data
public class ConversionJob {

    public enum Status { PENDING, DOWNLOADING, CONVERTING, DONE, ERROR }
    public enum Format { MP4, MP3, WEBM, AVI, MOV }

    private final String id = UUID.randomUUID().toString();
    private String url;
    private Format outputFormat;
    private Status status = Status.PENDING;
    private String message = "Queued...";
    private String outputFilePath;
    private String originalFilename;
    private LocalDateTime createdAt = LocalDateTime.now();
    private int progressPercent = 0;
}