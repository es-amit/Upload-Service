package com.lcwd.uploadservice.service.impl;

import com.lcwd.uploadservice.entity.UploadSession;
import com.lcwd.uploadservice.entity.UploadStatus;
import com.lcwd.uploadservice.exceptions.ResourceNotFoundException;
import com.lcwd.uploadservice.repository.UploadSessionRepository;
import com.lcwd.uploadservice.service.StorageService;
import com.lcwd.uploadservice.service.TranscodeService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Service;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.net.URL;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.time.Duration;
import java.time.Instant;
import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.Deque;
import java.util.List;
import java.util.UUID;
import java.util.concurrent.TimeUnit;
import java.util.stream.Stream;

@Slf4j
@Service
@RequiredArgsConstructor
public class TranscodeServiceImpl implements TranscodeService {

    // The full quality ladder, ordered low to high. Rungs taller than the source are filtered
    // out at select time (see selectRenditions) so nothing ever gets upscaled.
    private static final List<Rendition> LADDER = List.of(
            new Rendition(360, 800), // low
            new Rendition(480, 1400), // medium
            new Rendition(720, 2800), // high
            new Rendition(1080, 5000) // hd
    );

    @Value("${transcode.ffmpeg-path:ffmpeg}")
    private String ffmpegPath;

    @Value("${transcode.ffprobe-path:ffprobe}")
    private String ffprobePath;

    @Value("${transcode.work-dir}")
    private String workDir;

    @Value("${transcode.timeout-minutes:120}")
    private long timeoutMinutes;

    private final UploadSessionRepository repository;
    private final StorageService storageService;

    // One rendition rung: output height + target video bitrate (kbps).
    private record Rendition(int height, int bitrateKbps) {
    }

    // Minimal probe result — only what the rendition ladder / command builder need.
    private record VideoInfo(int width, int height, boolean hasAudio) {
    }

    @Override
    @Async("transcodeExecutor")
    public void transcode(UUID sessionId) {
        UploadSession session = repository
                .findById(sessionId)
                .orElseThrow(() ->
                        new ResourceNotFoundException("Session with id " + sessionId + " doesn't exist"));

        Duration timeout = Duration.ofMinutes(timeoutMinutes);
        // The presign has to outlive the whole single-pass encode below, not just a quick
        // download — ffmpeg streams the source directly from this URL instead of us
        // downloading it to local disk first.
        URL sourceUrl = storageService.presignDownload(session.getObjectKey(), timeout);

        Path workDirPath = null;
        try {
            workDirPath = Files.createTempDirectory(Path.of(workDir), "hls-" + sessionId);

            VideoInfo source = probeSource(sourceUrl.toString());
            List<Rendition> renditions = selectRenditions(source.height());

            List<String> command = buildFfmpegCommand(sourceUrl.toString(), workDirPath, renditions, source.hasAudio());
            runProcess(command, timeout);

            uploadRenditionOutputs(workDirPath, sessionId);

            session.setStatus(UploadStatus.READY);
            session.setHlsMasterKey(sessionId + "/master.m3u8");
            session.setUpdatedAt(Instant.now());
            repository.save(session);
            log.info("Transcode succeeded for session {}", sessionId);
        } catch (Exception e) {
            log.error("Transcode failed for session {}", sessionId, e);
            session.setStatus(UploadStatus.FAILED);
            session.setTranscodeError(truncate(e.getMessage()));
            session.setUpdatedAt(Instant.now());
            repository.save(session);
        } finally {
            if (workDirPath != null) {
                deleteRecursively(workDirPath);
            }
        }
    }

    // Runs ffprobe against the presigned URL and reads back "width,height", plus whether the
    // source has an audio track at all — only the container header gets read over HTTP for
    // this, not the full file, so both probes are cheap.
    private VideoInfo probeSource(String sourceUrl) throws IOException, InterruptedException {
        List<String> dimensionsCommand = List.of(
                ffprobePath,
                "-v", "error",
                "-select_streams", "v:0",
                "-show_entries", "stream=width,height",
                "-of", "csv=p=0",
                sourceUrl
        );

        String output = runProcess(dimensionsCommand, Duration.ofMinutes(2), true);
        String[] parts = output.trim().split(",");
        if (parts.length < 2) {
            throw new IllegalStateException("Could not probe video dimensions, ffprobe output: " + output);
        }

        List<String> audioCommand = List.of(
                ffprobePath,
                "-v", "error",
                "-select_streams", "a",
                "-show_entries", "stream=index",
                "-of", "csv=p=0",
                sourceUrl
        );
        boolean hasAudio = !runProcess(audioCommand, Duration.ofMinutes(2), true).isBlank();

        return new VideoInfo(Integer.parseInt(parts[0].trim()), Integer.parseInt(parts[1].trim()), hasAudio);
    }

    // Keeps every ladder rung at or below the source height so nothing gets upscaled; falls
    // back to a single rung at the source height if the source is smaller than the lowest rung.
    private List<Rendition> selectRenditions(int srcHeight) {
        List<Rendition> selected = LADDER.stream()
                .filter(r -> r.height() <= srcHeight)
                .toList();
        if (selected.isEmpty()) {
            return List.of(new Rendition(srcHeight, LADDER.get(0).bitrateKbps()));
        }
        return selected;
    }

    // Builds the single ffmpeg command that reads the source exactly once and produces every
    // selected rendition via -filter_complex split, letting ffmpeg's own HLS muxer also write
    // the per-rendition playlists and the master playlist (-master_pl_name / -var_stream_map)
    // instead of us hand-writing master.m3u8. Audio mapping is all-or-nothing across every
    // rendition (not per-rung) — -var_stream_map's "a:i" indexing only makes sense if audio is
    // either present for every variant or absent for every variant.
    private List<String> buildFfmpegCommand(String sourceUrl, Path workDirPath, List<Rendition> renditions, boolean hasAudio) {
        int n = renditions.size();
        List<String> command = new ArrayList<>();
        command.add(ffmpegPath);
        command.add("-y");
        command.add("-i");
        command.add(sourceUrl);

        StringBuilder filterComplex = new StringBuilder("[0:v]split=" + n);
        for (int i = 0; i < n; i++) {
            filterComplex.append("[v").append(i).append("]");
        }
        filterComplex.append(";");
        for (int i = 0; i < n; i++) {
            filterComplex.append("[v").append(i).append("]scale=-2:").append(renditions.get(i).height())
                    .append("[v").append(i).append("out]");
            if (i < n - 1) {
                filterComplex.append(";");
            }
        }
        command.add("-filter_complex");
        command.add(filterComplex.toString());

        StringBuilder varStreamMap = new StringBuilder();
        for (int i = 0; i < n; i++) {
            Rendition r = renditions.get(i);
            command.add("-map");
            command.add("[v" + i + "out]");
            command.add("-c:v:" + i);
            command.add("libx264");
            command.add("-preset");
            command.add("veryfast");
            command.add("-crf");
            command.add("23");
            command.add("-b:v:" + i);
            command.add(r.bitrateKbps() + "k");
            command.add("-maxrate:v:" + i);
            command.add(r.bitrateKbps() + "k");
            command.add("-bufsize:v:" + i);
            command.add((r.bitrateKbps() * 2) + "k");
            command.add("-g");
            command.add("48");
            command.add("-keyint_min");
            command.add("48");
            command.add("-sc_threshold");
            command.add("0");
            if (hasAudio) {
                command.add("-map");
                command.add("0:a");
                command.add("-c:a:" + i);
                command.add("aac");
                command.add("-b:a:" + i);
                command.add("128k");
                command.add("-ac");
                command.add("2");
            }

            if (i > 0) {
                varStreamMap.append(" ");
            }
            varStreamMap.append("v:").append(i);
            if (hasAudio) {
                varStreamMap.append(",a:").append(i);
            }
            varStreamMap.append(",name:").append(r.height()).append("p");
        }

        command.add("-f");
        command.add("hls");
        command.add("-hls_time");
        command.add("6");
        command.add("-hls_playlist_type");
        command.add("vod");
        command.add("-master_pl_name");
        command.add("master.m3u8");
        command.add("-var_stream_map");
        command.add(varStreamMap.toString());
        command.add("-hls_segment_filename");
        command.add(workDirPath.resolve("%v").resolve("segment_%03d.ts").toString());
        command.add(workDirPath.resolve("%v").resolve("index.m3u8").toString());

        return command;
    }

    // Walks the output-only temp dir and uploads every file to {sessionId}/{relativePath},
    // matching the layout ffmpeg produced ({h}p/index.m3u8, {h}p/segment_*.ts, master.m3u8).
    private void uploadRenditionOutputs(Path workDirPath, UUID sessionId) throws IOException {
        try (Stream<Path> files = Files.walk(workDirPath)) {
            List<Path> fileList = files.filter(Files::isRegularFile).toList();
            for (Path file : fileList) {
                String relativePath = workDirPath.relativize(file).toString().replace('\\', '/');
                String objectKey = sessionId + "/" + relativePath;
                String contentType = relativePath.endsWith(".m3u8")
                        ? "application/vnd.apple.mpegurl"
                        : "video/mp2t";
                storageService.uploadFile(objectKey, file, contentType);
            }
        }
    }

    private void runProcess(List<String> command, Duration timeout) throws IOException, InterruptedException {
        runProcess(command, timeout, false);
    }

    // Generic process runner: drains combined stdout/stderr to the debug log as it goes (an
    // unread pipe can make ffmpeg/ffprobe hang), enforces the timeout, and throws with the tail
    // of the output on a non-zero exit or timeout so the failure reason ends up in
    // transcodeError. When capture is true, the full output is returned instead of discarded
    // (used by probeSource to read ffprobe's csv result).
    private String runProcess(List<String> command, Duration timeout, boolean capture) throws IOException, InterruptedException {
        log.debug("Running process: {}", String.join(" ", command));
        ProcessBuilder builder = new ProcessBuilder(command).redirectErrorStream(true);
        Process process = builder.start();

        Deque<String> tail = new ArrayDeque<>();
        StringBuilder captured = capture ? new StringBuilder() : null;

        try (BufferedReader reader = new BufferedReader(new InputStreamReader(process.getInputStream(), StandardCharsets.UTF_8))) {
            String line;
            while ((line = reader.readLine()) != null) {
                log.debug("[process] {}", line);
                if (capture) {
                    captured.append(line).append('\n');
                }
                tail.addLast(line);
                if (tail.size() > 20) {
                    tail.removeFirst();
                }
            }
        }

        boolean finished = process.waitFor(timeout.toMillis(), TimeUnit.MILLISECONDS);
        if (!finished) {
            process.destroyForcibly();
            throw new IllegalStateException("Process timed out after " + timeout + ": " + command.get(0)
                    + "\nLast output:\n" + String.join("\n", tail));
        }

        int exitCode = process.exitValue();
        if (exitCode != 0) {
            throw new IllegalStateException("Process exited with code " + exitCode + ": " + command.get(0)
                    + "\nLast output:\n" + String.join("\n", tail));
        }

        return capture ? captured.toString() : null;
    }

    private void deleteRecursively(Path path) {
        try (Stream<Path> walk = Files.walk(path)) {
            walk.sorted(Comparator.reverseOrder())
                    .forEach(p -> {
                        try {
                            Files.deleteIfExists(p);
                        } catch (IOException e) {
                            log.warn("Failed to delete temp file {}: {}", p, e.getMessage());
                        }
                    });
        } catch (IOException e) {
            log.warn("Failed to clean up temp directory {}: {}", path, e.getMessage());
        }
    }

    private String truncate(String message) {
        if (message == null) {
            return "Unknown error";
        }
        return message.length() > 1000 ? message.substring(0, 1000) : message;
    }
}
