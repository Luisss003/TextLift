import axios from "axios";
import { useState } from "react";
import { createUpload, uploadFile, finalizeUpload } from "../api/apiRequests";
import type { CreateUploadResponse } from "../api/apiRequests";
import { LoadingState } from "./states/LoadingState";

type UploadResult =
  | { mode: "CACHE_HIT"; documentId: string }
  | { mode: "CACHE_HIT_WAIT" }
  | { mode: "NEW_UPLOAD"; uploadId: string };

type FileUploaderProps = {
  onResult?: (result: UploadResult) => void;
};

async function generateFilehash(file: File): Promise<string> {
  const buf = await file.arrayBuffer();
  const hashBuf = await crypto.subtle.digest("SHA-256", buf);

  const hashArr = Array.from(new Uint8Array(hashBuf));
  return hashArr.map((b) => b.toString(16).padStart(2, "0")).join("");
}

export default function FileUploader({ onResult }: FileUploaderProps) {
  const [file, setFile] = useState<File | null>(null);
  const [uploadStatus, setUploadStatus] = useState<string>("");
  const [uploadMode, setUploadMode] = useState<string>("");
  const [error, setError] = useState<string | null>(null);

  const MAX_SIZE_BYTES = 50 * 1024 * 1024;

  const handleFileChange = (e: React.ChangeEvent<HTMLInputElement>) => {
    const f = e.target.files?.[0];
    if (!f) return;
    setFile(f);
    setError(null);
    setUploadStatus("");
    setUploadMode("");
  };

  const handleFileUpload = async () => {
    if (!file) return;

    if (file.size > MAX_SIZE_BYTES) {
      setError("File exceeds the 50MB limit.");
      setUploadStatus("FAILED");
      return;
    }

    if (file.type && file.type !== "application/pdf") {
      setError("Only PDF uploads are allowed.");
      setUploadStatus("FAILED");
      return;
    }

    setUploadMode("");
    setUploadStatus("PENDING");
    setError(null);

    const hash = await generateFilehash(file);

    try {
      const res: CreateUploadResponse = await createUpload({
        hash,
        sizeBytes: file.size,
      });

      if (res.uploadMode === "CACHE_HIT") {
        setUploadStatus("UPLOADED");
        setUploadMode("CACHE_HIT");
        onResult?.({ mode: "CACHE_HIT", documentId: res.documentId! });
        return;
      }

      if (res.uploadMode === "CACHE_HIT_WAIT") {
        setUploadMode("CACHE_HIT_WAIT");
        onResult?.({ mode: "CACHE_HIT_WAIT" });
        return;
      }

      setUploadMode("NEW_UPLOAD");
      setUploadStatus("UPLOADING");
      await uploadFile(res.uploadId, file);

      setUploadStatus("UPLOADED");
      await finalizeUpload(res.uploadId);

      onResult?.({ mode: "NEW_UPLOAD", uploadId: res.uploadId });
    } catch (e) {
      setUploadStatus("FAILED");
      setError(extractProblemDetail(e));
      console.error("Upload failed", e);
    }
  };

  const isUploading = uploadStatus === "UPLOADING";

  return (
    <div className="rounded-2xl border border-slate-800 bg-slate-900 p-6 shadow-sm">
      <div className="flex items-start justify-between gap-4">
        <div>
          <h2 className="text-sm font-semibold text-slate-100">Upload a PDF</h2>
          <p className="mt-1 text-xs text-slate-400">
            Best results: selectable text or clean scans.
          </p>
        </div>

        <span className="rounded-full border border-slate-800 bg-slate-950 px-2 py-1 text-xs text-slate-300">
          50Mb Upload Limit
        </span>
      </div>

      {/* File picker */}
      <div className="mt-4 rounded-xl border border-dashed border-slate-700 bg-slate-950 p-4">
        <label className="block text-xs font-semibold text-slate-200">
          Choose file
        </label>

        <input
          type="file"
          accept="application/pdf"
          onChange={handleFileChange}
          disabled={isUploading}
          className="mt-2 block w-full cursor-pointer text-sm text-slate-200
                     file:mr-4 file:rounded-xl file:border file:border-slate-800
                     file:bg-slate-900 file:px-4 file:py-2 file:text-sm file:font-semibold
                     file:text-slate-100 hover:file:bg-slate-800"
        />

        {!file ? (
          <p className="mt-3 text-xs text-slate-500">No file selected.</p>
        ) : (
          <div className="mt-3 flex flex-wrap items-center justify-between gap-3">
            <div className="min-w-0">
              <p className="text-sm font-semibold text-slate-100 truncate">
                {file.name}
              </p>
              <p className="mt-1 text-xs text-slate-500">
                {(file.size / (1024 * 1024)).toFixed(2)} MB
              </p>
            </div>

            <button
              type="button"
              onClick={() => setFile(null)}
              disabled={isUploading}
              className="rounded-xl border border-slate-800 bg-slate-950 px-3 py-2 text-xs font-semibold
                         text-slate-200 hover:bg-slate-900 disabled:opacity-50"
            >
              Remove
            </button>
          </div>
        )}
      </div>

      {/* Actions */}
      <div className="mt-4 flex flex-wrap gap-2">
        <button
          type="button"
          onClick={handleFileUpload}
          disabled={!file || isUploading}
          className="rounded-xl bg-cyan-500 px-4 py-2 text-sm font-semibold text-slate-950
                     hover:bg-cyan-400 disabled:opacity-50"
        >
          {isUploading ? "Uploading…" : "Upload"}
        </button>
      </div>

      {/* Status */}
      <div className="mt-4 space-y-2">
        {error && (
          <div className="rounded-xl border border-rose-900/60 bg-rose-950/30 p-4 text-sm text-rose-200">
            {error}
          </div>
        )}

        {uploadStatus === "PENDING" && (
          <div className="rounded-xl border border-slate-800 bg-slate-950 p-4 text-sm text-slate-200">
            Preparing upload…
          </div>
        )}

        {uploadMode === "CACHE_HIT_WAIT" && (
          <div className="rounded-xl border border-slate-800 bg-slate-950 p-4 text-sm text-slate-200">
            This file is already being processed. Check back soon.
          </div>
        )}

        {uploadStatus === "UPLOADING" && (
          <div className="rounded-xl border border-slate-800 bg-slate-950 p-4">
            <LoadingState label="Uploading file, please wait..." />
          </div>
        )}

        {uploadStatus === "UPLOADED" && uploadMode === "NEW_UPLOAD" && (
          <div className="rounded-xl border border-emerald-900/60 bg-emerald-950/30 p-4 text-sm text-emerald-200">
            File uploaded successfully! Check back later for annotations.
          </div>
        )}

        {uploadStatus === "FAILED" && (
          <div className="rounded-xl border border-rose-900/60 bg-rose-950/30 p-4 text-sm text-rose-200">
            {error ?? "Upload failed. Please try again."}
          </div>
        )}
      </div>
    </div>
  );
}

function extractProblemDetail(err: unknown): string {
  if (axios.isAxiosError(err) && err.response?.data) {
    const detail =
      (err.response.data as any).detail ||
      (err.response.data as any).description ||
      (typeof err.response.data === "string" ? err.response.data : null);
    if (detail) return detail;
  }
  return "Upload failed. Please try again.";
}
