package com.viddo.viddo.service;

import com.viddo.viddo.model.ConversionJob;
import com.viddo.viddo.model.ConversionJob.Format;
import com.viddo.viddo.model.ConversionJob.Status;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Service;
import java.io.BufferedReader;
import java.io.InputStreamReader;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

@Slf4j
@Service
public class ConverterService {

    private final Map<String, ConversionJob> jobs = new ConcurrentHashMap<>();

    @Value("${converter.output-dir:./converted}")
    private String outputDir;

    @Value("${converter.ytdlp-path:yt-dlp}")
    private String ytDlpPath;

    @Value("${converter.ffmpeg-path:ffmpeg}")
    private String ffmpegPath;

    public ConversionJob submitJob(String url, Format format) {
        ConversionJob job = new ConversionJob();
        job.setUrl(url);
        job.setOutputFormat(format);
        jobs.put(job.getId(), job);
        processJob(job);
        return job;
    }

    public ConversionJob getJob(String jobId) {
        return jobs.get(jobId);
    }

    public Map<String, ConversionJob> getAllJobs() {
        return jobs;
    }

    @Async
    public void processJob(ConversionJob job) {
        try {
            Path outPath = Paths.get(outputDir);
            Files.createDirectories(outPath);

            Path downloadedFile = downloadVideo(job, outPath);
            Path finalFile = convertVideo(job, downloadedFile, outPath);

            job.setOutputFilePath(finalFile.toAbsolutePath().toString());
            job.setOriginalFilename(finalFile.getFileName().toString());
            job.setStatus(Status.DONE);
            job.setMessage("Ready to download!");
            job.setProgressPercent(100);

        } catch (Exception e) {
            log.error("Job {} failed: {}", job.getId(), e.getMessage(), e);
            job.setStatus(Status.ERROR);
            job.setMessage("Error: " + e.getMessage());
        }
    }

    private Path downloadVideo(ConversionJob job, Path outPath) throws Exception {
        job.setStatus(Status.DOWNLOADING);
        job.setMessage("Downloading from URL...");
        job.setProgressPercent(10);

        String outputTemplate = outPath.toAbsolutePath().resolve("%(title)s.%(ext)s").toString();

        List<String> command = List.of(
            ytDlpPath, "--no-playlist",
            "--output", outputTemplate,
            "--print", "after_move:filepath",
            job.getUrl()
        );

        ProcessBuilder pb = new ProcessBuilder(command);
        pb.redirectErrorStream(true);
        Process process = pb.start();

        String downloadedFilename = null;
        try (BufferedReader reader = new BufferedReader(
                new InputStreamReader(process.getInputStream()))) {
            String line;
            while ((line = reader.readLine()) != null) {
                log.info("[yt-dlp] {}", line);
                if (!line.isEmpty()) {
                    downloadedFilename = line.trim();
                }
            }
        }

        int exitCode = process.waitFor();
        if (exitCode != 0) throw new RuntimeException("yt-dlp failed with code " + exitCode);
        if (downloadedFilename == null) throw new RuntimeException("Could not determine downloaded filename");

        job.setProgressPercent(50);
        job.setMessage("Download complete, preparing conversion...");
        return Paths.get(downloadedFilename);
    }

    private Path convertVideo(ConversionJob job, Path inputFile, Path outPath) throws Exception {
        String inputExt = getExtension(inputFile.getFileName().toString());
        String targetExt = job.getOutputFormat().name().toLowerCase();

        if (inputExt.equalsIgnoreCase(targetExt)) return inputFile;

        job.setStatus(Status.CONVERTING);
        job.setMessage("Converting to " + targetExt.toUpperCase() + "...");
        job.setProgressPercent(60);

        String baseName = stripExtension(inputFile.getFileName().toString());
        Path outputFile = outPath.toAbsolutePath().resolve(baseName + "." + targetExt);

        List<String> command = buildFfmpegCommand(inputFile, outputFile, job.getOutputFormat());

        ProcessBuilder pb = new ProcessBuilder(command);
        pb.redirectErrorStream(true);
        Process process = pb.start();

        try (BufferedReader reader = new BufferedReader(
                new InputStreamReader(process.getInputStream()))) {
            while (reader.readLine() != null) {}
        }

        int exitCode = process.waitFor();
        if (exitCode != 0) throw new RuntimeException("FFmpeg failed with exit code " + exitCode);

        Files.deleteIfExists(inputFile);
        job.setProgressPercent(90);
        return outputFile;
    }

    private List<String> buildFfmpegCommand(Path input, Path output, Format format) {
        return switch (format) {
            case MP3 -> List.of(ffmpegPath, "-i", input.toString(),
                    "-vn", "-acodec", "libmp3lame", "-q:a", "2", "-y", output.toString());
            case MP4 -> List.of(ffmpegPath, "-i", input.toString(),
                    "-vcodec", "libx264", "-acodec", "aac", "-y", output.toString());
            case WEBM -> List.of(ffmpegPath, "-i", input.toString(),
                    "-vcodec", "libvpx-vp9", "-acodec", "libopus", "-y", output.toString());
            default -> List.of(ffmpegPath, "-i", input.toString(), "-y", output.toString());
        };
    }

    private String getExtension(String filename) {
        int dot = filename.lastIndexOf('.');
        return dot >= 0 ? filename.substring(dot + 1) : "";
    }

    private String stripExtension(String filename) {
        int dot = filename.lastIndexOf('.');
        return dot >= 0 ? filename.substring(0, dot) : filename;
    }
}