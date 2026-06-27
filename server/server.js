const express = require("express");
const { exec } = require("child_process");
const cors = require("cors");

const app = express();
app.use(cors());
app.use(express.json({ limit: "1mb" }));

app.post("/api/extract", (req, res) => {
  const { url } = req.body;
  if (!url) {
    return res.status(400).json({ error: "URL is required" });
  }

  const sanitized = url.replace(/["`$\\]/g, "");

  const isInstagram = /instagram\.com/i.test(sanitized);
  const extraArgs = isInstagram ? `--extractor-args "instagram:api=web"` : "";
  const cmd = `yt-dlp -J --no-playlist --no-warnings --extractor-retries 3 --retries 3 ${extraArgs} "${sanitized}"`.replace(/\s+/g, " ");

  exec(
    cmd,
    {
      maxBuffer: 50 * 1024 * 1024,
      timeout: 120000,
      windowsHide: true,
    },
    (error, stdout, stderr) => {
      if (error) {
        const msg = stderr ? stderr.slice(0, 500) : "Extraction failed. Check yt-dlp installation.";
        console.error("yt-dlp error:", stderr || error.message);
        return res.json({ error: true, message: msg });
      }

      try {
        const data = JSON.parse(stdout);
        const formats = (data.formats || [])
          .filter((f) => f.url && (f.vcodec !== "none" || f.acodec !== "none"))
          .map((f) => ({
            quality: f.height ? `${f.height}p` : f.format_note || f.format_id || "audio",
            ext: f.ext || "mp4",
            fileSize: f.filesize || f.filesize_approx || 0,
            downloadUrl: f.url,
            hasVideo: f.vcodec !== "none",
            hasAudio: f.acodec !== "none",
          }));

        res.json({
          error: false,
          title: data.title || "Unknown",
          author: data.uploader || data.channel || data.creator || "Unknown",
          duration: data.duration || 0,
          thumbnailUrl: data.thumbnail || "",
          platform: data.extractor_key || "Unknown",
          uploadDate: data.upload_date || "",
          formats,
          videoFormats: formats.filter((f) => f.hasVideo),
          audioFormats: formats.filter((f) => !f.hasVideo && f.hasAudio),
        });
      } catch (e) {
        console.error("JSON parse error:", e.message);
        res.json({ error: true, message: "Failed to parse video information" });
      }
    }
  );
});

app.get("/api/health", (_req, res) => {
  res.json({ status: "ok" });
});

app.get("/api/version", (_req, res) => {
  exec("yt-dlp --version", { timeout: 10000 }, (err, stdout) => {
    res.json({ ytDlp: err ? "unknown" : stdout.trim() });
  });
});

const PORT = process.env.PORT || 3000;
app.listen(PORT, "0.0.0.0", () => {
  console.log(`OpenDoor backend running on port ${PORT}`);
});
