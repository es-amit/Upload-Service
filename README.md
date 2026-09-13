# Upload-Service

A Spring Boot backend service for uploading large video files directly to S3-compatible object storage (MinIO) in chunks via presigned URLs, automatically transcoding completed uploads into adaptive-bitrate HLS, and serving them back for streaming. A scheduled job cleans up stale/abandoned upload sessions so storage isn't wasted.

## Features

- **Chunked, presigned uploads** — the server never proxies file bytes. Clients request presigned part URLs and upload directly to MinIO/S3.
- **Upload session tracking** — session status (`INITIATED` → `UPLOADING` → `PROCESSING` → `READY` / `FAILED` / `ABORTED`), including uploaded parts/bytes.
- **Stale session cleanup** — a cron job runs every 6 hours and aborts sessions that have been sitting idle past a configurable timeout, freeing storage.
- **Async HLS transcoding** — once an upload completes, the uploaded MP4 is transcoded (via `ffmpeg`/`ffprobe`) into a multi-rendition adaptive-bitrate ladder (360p/480p/720p/1080p, without upscaling above the source resolution).
- **HLS streaming** — once ready, the master playlist and segments are served back through the API, with a `streamUrl` included in the status response.

## Tech Stack

- **Java 17**
- **Spring Boot 4.1.1** (Web MVC, Spring Data JPA, Validation)
- **Flyway** (`flyway-mysql`) — database migrations
- **MySQL** (`mysql-connector-j`)
- **AWS SDK v2 (`software.amazon.awssdk:s3`)** — S3 client + `S3Presigner`, pointed at a **MinIO** endpoint (any S3-compatible store works)
- **ffmpeg / ffprobe** — external binaries invoked for video transcoding
- **Lombok**
- **Maven** (with `mvnw` wrapper)

## Project Structure

```
src/main/java/com/lcwd/uploadservice/
├── config/          # AsyncConfig, S3ClientConfig
├── controller/       # UploadController — REST API
├── dto/              # Request/response payloads
├── entity/           # UploadSession, UploadStatus, PartSummary
├── exceptions/        # GlobalExceptionHandler, ResourceNotFoundException
├── repository/        # UploadSessionRepository (Spring Data JPA)
└── service/            # UploadService, StorageService, TranscodeService (+ impl)

src/main/resources/
├── application.yaml   # configuration
└── db/migration/       # Flyway migration scripts (V1–V3)
```

## Prerequisites

- JDK 17
- A running **MySQL** instance
- A running **MinIO** (or other S3-compatible) instance
- **ffmpeg** and **ffprobe** installed locally, with their paths available to the app
- No local Maven install required — use the bundled `./mvnw` / `mvnw.cmd` wrapper

## Configuration

All configuration lives in `src/main/resources/application.yaml`. The committed file contains local development defaults (including plaintext credentials) — override them via environment-specific config or environment variables for anything beyond local development.

| Property | Description | Default |
|---|---|---|
| `spring.datasource.url` | MySQL JDBC URL | `jdbc:mysql://localhost:3306/upload_service?createdDatabaseIfNotExist=true` |
| `spring.datasource.username` / `password` | MySQL credentials | `root` / `root` |
| `spring.jpa.hibernate.ddl-auto` | Hibernate schema handling (schema is managed by Flyway) | `validate` |
| `spring.jpa.show-sql` | Log SQL statements | `true` |
| `spring.flyway.baseline-on-migrate` | Baseline an existing DB before running migrations | `true` |
| `spring.flyway.baseline-version` | Baseline version | `1` |
| `minio.endpoint` | S3-compatible endpoint URL | `http://localhost:9000` |
| `minio.access-key` / `minio.secret-key` | Storage credentials | `admin` / `password` |
| `minio.bucket` | Bucket used for uploads/HLS output | `videos` |
| `minio.chunk-size` | Multipart chunk size in bytes | `10485760` (10 MB) |
| `minio.presign-expiry-minutes` | Presigned URL expiry | `15` |
| `minio.stale-after-hours` | Age after which an incomplete session is considered stale and eligible for cleanup | `24` |
| `transcode.ffmpeg-path` / `transcode.ffprobe-path` | Absolute paths to the ffmpeg/ffprobe binaries | — |
| `transcode.work-dir` | Scratch directory used during transcoding | `${java.io.tmpdir}` |
| `transcode.timeout-minutes` | Max time allowed for a transcode job | `120` |
| `server.port` | HTTP port | `8080` |

## Setup & Run

1. **Start dependencies** — MySQL and a MinIO server. For example:
   ```bash
   docker run -d --name mysql -e MYSQL_ROOT_PASSWORD=root -p 3306:3306 mysql:8
   docker run -d --name minio -p 9000:9000 -p 9001:9001 -e MINIO_ROOT_USER=admin -e MINIO_ROOT_PASSWORD=password minio/minio server /data --console-address ":9001"
   ```
   Then create the bucket referenced by `minio.bucket` (default `videos`) via the MinIO console or `mc`.

2. **Install ffmpeg/ffprobe** locally and update `transcode.ffmpeg-path` / `transcode.ffprobe-path` in `application.yaml` to match your installation.

3. **Configure** `application.yaml` (or override via environment variables / a Spring profile) with your database and object storage credentials.

4. **Build**
   ```bash
   ./mvnw clean install
   ```

5. **Run**
   ```bash
   ./mvnw spring-boot:run
   ```
   Flyway migrations run automatically against the configured database on startup.

The service starts on `http://localhost:8080` by default.

## API Overview

| Method | Path | Description |
|---|---|---|
| `POST` | `/api/uploads` | Initiate a new upload session |
| `GET` | `/api/uploads/{sessionId}` | Get session status (parts uploaded, state, `streamUrl` when ready) |
| `POST` | `/api/uploads/{sessionId}/parts/presign` | Get presigned URLs for one or more part numbers |
| `POST` | `/api/uploads/{sessionId}/complete` | Complete the multipart upload and trigger transcoding |
| `DELETE` | `/api/uploads/{sessionId}` | Abort an in-progress upload session |
| `GET` | `/api/uploads/{sessionId}/hls/**` | Stream HLS playlist/segments for a ready session |

## Upload Lifecycle

```
INITIATED → UPLOADING → PROCESSING → READY
                  └──────────────────→ FAILED
INITIATED / UPLOADING → ABORTED (client abort, or cron cleanup of stale sessions)
```
