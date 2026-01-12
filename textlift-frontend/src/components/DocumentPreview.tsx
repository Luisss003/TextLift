import { useEffect, useState, type MouseEvent } from "react";
import { getUserUploads, deleteFile } from "../api/apiRequests";
import { useNavigate } from "react-router-dom";

export type DocumentPreview = {
  textBookTitle: string;
  documentStatus: string;
  documentId: string;
};

function StatusPill({ status }: { status: string }) {
  // simple mapping; tweak labels as you like
  const base =
    "inline-flex items-center rounded-full border px-2 py-1 text-xs font-semibold";
  const s = status.toLowerCase();

  if (s.includes("ready")) {
    return (
      <span className={`${base} border-emerald-900/60 bg-emerald-950/40 text-emerald-200`}>
        {status}
      </span>
    );
  }
  if (s.includes("fail") || s.includes("error")) {
    return (
      <span className={`${base} border-rose-900/60 bg-rose-950/40 text-rose-200`}>
        {status}
      </span>
    );
  }
  return (
    <span className={`${base} border-slate-700 bg-slate-950 text-slate-200`}>
      {status}
    </span>
  );
}

export default function DocumentPreview() {
  const [docPreviews, setDocPreviews] = useState<DocumentPreview[]>([]);
  const [loading, setLoading] = useState(true);
  const [error, setError] = useState<string | null>(null);
  const [deletingId, setDeletingId] = useState<string | null>(null);

  const navigate = useNavigate();

  useEffect(() => {
    let cancelled = false;

    (async () => {
      try {
        setLoading(true);
        setError(null);
        const previews = await getUserUploads();
        if (cancelled) return;

        setDocPreviews(Array.isArray(previews) ? previews : []);
      } catch {
        if (!cancelled) setError("Failed to load documents.");
      } finally {
        if (!cancelled) setLoading(false);
      }
    })();

    return () => {
      cancelled = true;
    };
  }, []);

  const handleDelete = async (
    event: MouseEvent<HTMLButtonElement>,
    documentId: string,
  ) => {
    event.stopPropagation();

    try {
      setDeletingId(documentId);
      await deleteFile(documentId);
      setDocPreviews((prev) =>
        prev.filter((doc) => doc.documentId !== documentId),
      );
    } catch {
      setError("Failed to delete document.");
    } finally {
      setDeletingId(null);
    }
  };

  if (loading) {
    return <div className="text-sm text-slate-400">Loading documents…</div>;
  }

  if (error) {
    return (
      <div className="rounded-xl border border-rose-900/50 bg-rose-950/30 p-4 text-sm text-rose-200">
        {error}
      </div>
    );
  }

  if (docPreviews.length === 0) {
    return (
      <div className="rounded-xl border border-dashed border-slate-700 bg-slate-950 p-6">
        <div className="text-sm text-slate-300">No documents yet.</div>
        <div className="mt-1 text-xs text-slate-500">
          Upload a PDF and it’ll appear here with its processing status.
        </div>
      </div>
    );
  }

  return (
    <div className="space-y-3">
      {docPreviews.map((doc) => (
        <div
          key={doc.documentId}
          onClick={() => navigate(`/annotations/document/${doc.documentId}`)}
          role="button"
          tabIndex={0}
          onKeyDown={(event) => {
            if (event.key === "Enter" || event.key === " ") {
              event.preventDefault();
              navigate(`/annotations/document/${doc.documentId}`);
            }
          }}
          className="group w-full rounded-2xl border border-slate-800 bg-slate-950 p-5 text-left shadow-sm transition hover:bg-slate-900 active:scale-[0.99]"
        >
          <div className="flex items-start justify-between gap-4">
            <div className="min-w-0">
              <div className="flex flex-wrap items-center gap-2">
                <h2 className="truncate text-sm font-semibold text-slate-100">
                  {doc.textBookTitle}
                </h2>
                <StatusPill status={doc.documentStatus} />
                {doc.documentStatus === "ANNOTATIONS_READY" && (
                  <button
                    type="button"
                    className="rounded-lg border border-red-800 bg-red-950 px-3 py-1 text-xs font-semibold text-red-200 transition hover:bg-red-900 disabled:cursor-not-allowed disabled:opacity-60"
                    onClick={(event) => handleDelete(event, doc.documentId)}
                    disabled={deletingId === doc.documentId}
                  >
                    {deletingId === doc.documentId ? "Deleting…" : "Delete"}
                  </button>
                )}
              </div>

              <p className="mt-2 text-xs text-slate-500">
                ID: <span className="font-mono text-slate-400">{doc.documentId}</span>
              </p>
            </div>

            <span className="shrink-0 rounded-xl border border-slate-800 bg-slate-950 px-3 py-2 text-xs font-semibold text-slate-200 transition group-hover:border-slate-700 group-hover:bg-slate-900">
              Open
            </span>
          </div>
        </div>
      ))}
    </div>
  );
}
