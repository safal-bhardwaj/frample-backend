# Frample Backend

REST API for **Frample**, a free marketplace where producers upload, stream and download music samples.

Frontend repo: [<link to frample-frontend>](https://github.com/safal-bhardwaj/frample-frontend)

## Features

- **Authentication**: signup/login with BCrypt-hashed passwords, plus **Sign in with Google** (ID token verified server-side)
- **Sessions**: short-lived JWT access token and a rotating refresh token stored in an **httpOnly cookie**, so JavaScript can never read it
- **Authorization**: stateless JWT filter, role assignment on signup, ownership checks on every edit/delete
- **Direct-to-storage uploads**: audio goes from the browser straight to **Cloudflare R2** through presigned URLs, so file bytes never pass through the API
- **Streaming and downloads**: temporary presigned GET URLs, with server-side play and download counters and clean download filenames (`Title_Minor_96.wav`)
- **Search and filters**: title/uploader search, genre, type, key, scale and BPM range, built with JPA Specifications and paginated
- **Manage uploads**: edit metadata and delete samples (database row and R2 object)

## Tech stack

Java, Spring Boot, Spring Security, Spring Data JPA (Hibernate), MySQL, JWT, AWS SDK v2 (S3-compatible client for R2), Google API Client, ModelMapper, Lombok

## Architecture notes

- **Presigned upload flow**: the client asks `POST /samples/presign-upload` for a 10-minute upload URL, uploads the file straight to R2, then calls `POST /samples` to save the metadata. The R2 credentials never leave the server.
- **Object keys stay private**: the database stores an R2 object key, and a fresh signed URL is generated per play or download. The key is never returned by the API.
- **Waveforms**: the browser computes ~100 amplitude peaks at upload time and the API stores them, so list pages draw waveforms without downloading any audio.

## API overview

| Area | Endpoints |
|---|---|
| Auth | `POST /api/v1/auth/signup`, `/login`, `/refresh`, `/logout`, `/oauth/google` |
| Profile | `PUT /api/v1/user/me` |
| Samples | `GET /api/v1/samples` (filters and paging), `GET /api/v1/samples/mine` |
| | `POST /api/v1/samples/presign-upload`, `POST /api/v1/samples` |
| | `PUT /api/v1/samples/{id}`, `DELETE /api/v1/samples/{id}` |
| Media | `GET /api/v1/samples/{id}/stream`, `GET /api/v1/samples/{id}/download` |

Everything except `/api/v1/auth/**` requires a valid access token.

## Running locally

**Prerequisites:** JDK (version in `pom.xml`), MySQL, a Cloudflare R2 bucket, a Google OAuth client ID.

1. Create the database: `CREATE DATABASE frample_db;`
2. Set these environment variables (see `application.properties.example` for the full list of properties):

| Variable | Purpose |
|---|---|
| `DB_USERNAME`, `DB_PASSWORD` | MySQL credentials |
| `JWT_SECRET` | Random string of 64+ characters used to sign tokens |
| `GOOGLE_CLIENT_ID` | OAuth client ID from Google Cloud Console |
| `R2_ENDPOINT` | `https://<account-id>.r2.cloudflarestorage.com` |
| `R2_BUCKET` | Bucket name |
| `R2_ACCESSKEY`, `R2_SECRETKEY` | R2 API token (Object Read & Write, scoped to the bucket) |

3. Add a CORS policy to the R2 bucket that allows `PUT` and `GET` from `http://localhost:4200`, with the `Content-Type` header allowed.
4. Start the API: `./mvnw spring-boot:run` (Windows: `mvnw.cmd spring-boot:run`). It runs on `http://localhost:8080`.

## Security notes

- Secrets are read from environment variables only and are never committed.
- Refresh tokens are stateless, so they can't yet be revoked before they expire.
- Planned: rate limiting on auth endpoints, refresh-token revocation, Microsoft and Apple sign-in, automated tests.
