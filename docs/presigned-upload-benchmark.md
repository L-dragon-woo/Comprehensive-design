# Presigned URL upload benchmark

This app now supports two image-analysis upload paths so you can compare request time.

## Modes

- Multipart baseline: browser uploads the image to the backend with `POST /api/analyses`.
- Presigned upload: browser asks the backend for a presigned S3 `PUT`, uploads the image to S3 directly, then calls `POST /api/analyses/from-s3` with the S3 object key.

The default mode is multipart. Enable presigned mode with any one of these:

```js
localStorage.setItem("skinai:analysis-upload-mode", "presigned")
```

or open the capture page with:

```text
/capture?upload=presigned
```

or set the frontend environment variable:

```env
VITE_ANALYSIS_UPLOAD_MODE=presigned
```

To return to the baseline:

```js
localStorage.setItem("skinai:analysis-upload-mode", "multipart")
```

## Timing data

Each analysis response saved in the app includes:

```json
{
  "uploadBenchmark": {
    "mode": "multipart",
    "fileBytes": 123456,
    "timingsMs": {
      "compress": 10,
      "multipartAnalyze": 5000,
      "totalBeforeSummary": 5010
    }
  }
}
```

In presigned mode, `timingsMs` includes `presign`, `s3Upload`, `backendAnalyze`, and `totalBeforeSummary`.

## Required S3 setup

Backend environment:

```env
AWS_REGION=ap-northeast-2
S3_BUCKET=<bucket-name>
S3_PRESIGNED_URL_TTL=PT1H
```

The browser uploads directly to S3, so the bucket needs CORS that allows the frontend origin to `PUT`.

Example local-development CORS:

```json
[
  {
    "AllowedHeaders": ["*"],
    "AllowedMethods": ["GET", "PUT"],
    "AllowedOrigins": ["http://localhost:5173"],
    "ExposeHeaders": ["ETag"],
    "MaxAgeSeconds": 3000
  }
]
```
