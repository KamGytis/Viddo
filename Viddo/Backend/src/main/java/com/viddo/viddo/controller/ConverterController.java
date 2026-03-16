package com.viddo.viddo.controller;

import com.viddo.viddo.model.ConversionJob;
import com.viddo.viddo.model.ConversionRequest;
import com.viddo.viddo.service.ConverterService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.core.io.FileSystemResource;
import org.springframework.core.io.Resource;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import java.io.File;
import java.util.Map;

@RestController
@RequestMapping("/api")
@RequiredArgsConstructor
public class ConverterController {

    private final ConverterService converterService;

    @PostMapping("/convert")
    public ResponseEntity<ConversionJob> submitConversion(
            @Valid @RequestBody ConversionRequest request) {
        ConversionJob job = converterService.submitJob(request.getUrl(), request.getOutputFormat());
        return ResponseEntity.accepted().body(job);
    }

    @GetMapping("/jobs/{jobId}")
    public ResponseEntity<ConversionJob> getJobStatus(@PathVariable String jobId) {
        ConversionJob job = converterService.getJob(jobId);
        if (job == null) return ResponseEntity.notFound().build();
        return ResponseEntity.ok(job);
    }

    @GetMapping("/jobs/{jobId}/download")
    public ResponseEntity<Resource> downloadFile(@PathVariable String jobId) {
        ConversionJob job = converterService.getJob(jobId);
        if (job == null) return ResponseEntity.notFound().build();
        if (job.getStatus() != ConversionJob.Status.DONE) {
            return ResponseEntity.status(HttpStatus.CONFLICT).build();
        }
        File file = new File(job.getOutputFilePath());
        if (!file.exists()) return ResponseEntity.status(HttpStatus.GONE).build();

        return ResponseEntity.ok()
                .header(HttpHeaders.CONTENT_DISPOSITION,
                        "attachment; filename=\"" + job.getOriginalFilename() + "\"")
                .contentType(MediaType.APPLICATION_OCTET_STREAM)
                .body(new FileSystemResource(file));
    }

    @GetMapping("/jobs")
    public ResponseEntity<Map<String, ConversionJob>> listJobs() {
        return ResponseEntity.ok(converterService.getAllJobs());
    }
}