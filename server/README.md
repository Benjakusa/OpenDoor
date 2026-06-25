# OpenDoor Backend Server

Extracts direct video/audio URLs from Facebook, TikTok, YouTube, Instagram, and other social media platforms using `yt-dlp`.

## Prerequisites

- [Node.js](https://nodejs.org/) 18+
- [yt-dlp](https://github.com/yt-dlp/yt-dlp) installed and available in PATH
- (Optional) [ffmpeg](https://ffmpeg.org/) for better format merging

## Quick Start

```bash
npm install
npm start
```

The server starts on `http://localhost:3000`.

## API

### `POST /api/extract`

**Request:** `{ "url": "https://www.facebook.com/watch?v=..." }`

**Response:**
```json
{
  "title": "Video Title",
  "author": "Creator Name",
  "duration": 480,
  "thumbnailUrl": "https://...",
  "platform": "Facebook",
  "uploadDate": "20260115",
  "formats": [
    { "quality": "720p", "ext": "mp4", "fileSize": 52000000, "downloadUrl": "https://...", "hasVideo": true, "hasAudio": true },
    { "quality": "360p", "ext": "mp4", "fileSize": 18000000, "downloadUrl": "https://...", "hasVideo": true, "hasAudio": true }
  ],
  "videoFormats": [...],
  "audioFormats": [...]
}
```

## Deployment

### Docker
```bash
docker build -t opendoor-backend .
docker run -p 3000:3000 opendoor-backend
```

### Cloud
Deploy to any Node.js host (Render, Railway, Fly.io, etc.). Set `PORT` env var if needed.

## Android App Configuration

Add your server URL to the Android project's `.env` file:

```
BACKEND_URL=https://your-server.com
```
