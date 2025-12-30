import { Link } from "react-router-dom";
import { Navbar } from "../layout/Navbar";

export function HomePage() {
  return (
    <div className="min-h-screen bg-slate-950 text-slate-100">
      <Navbar />
      <main className="mx-auto max-w-5xl px-4 py-10">
        <section className="rounded-2xl border border-slate-800 bg-slate-900 p-6 shadow-sm">
          <div className="flex flex-col gap-5 md:flex-row md:items-center md:justify-between">
            <div className="space-y-2">
              <h1 className="text-2xl font-bold tracking-tight md:text-3xl">
                Revive the usability of old textbooks and papers.
              </h1>
              <p className="max-w-xl text-sm leading-relaxed text-slate-300">
                Upload your old textbooks and research papers as PDFs, and let TextLift
                find and correct outdated, incorrect, or biased information within them. 
              </p>
            </div>

            <div className="flex gap-2">
              <Link
                to="/upload"
                className="rounded-xl bg-cyan-500 px-4 py-2 text-sm font-semibold text-slate-950 hover:bg-cyan-400"
              >
                Start an upload
              </Link>
              <Link
                to="/documents"
                className="rounded-xl border border-slate-800 bg-slate-950 px-4 py-2 text-sm font-semibold text-slate-100 hover:bg-slate-800"
              >
                View documents
              </Link>
            </div>
          </div>
        </section>

        <section className="mt-6 grid gap-4 md:grid-cols-3">
          <Card
            title="Upload PDF"
            desc="Create a new upload session and send a file."
            to="/upload"
            cta="Upload"
          />
          <Card
            title="Browse documents"
            desc="See processed PDFs and their current status."
            to="/documents"
            cta="Open list"
          />
          <Card
            title="Tips"
            desc="Best results: clear scans, selectable text, no photos."
            to="/upload"
            cta="Learn more"
            subtle
          />
        </section>

        {/* moneygrab :) */}
            <section className="mt-6 rounded-2xl border border-slate-800 bg-slate-900 p-6">
            <div className="space-y-1">
                <h2 className="text-sm font-semibold text-slate-100">Account Tiers</h2>
                <p className="text-xs leading-relaxed text-slate-400">
                There isn't a difference between account tiers! You can choose to upgrade
                your TextLift account simply if you want to help with the solo development
                and improvement of TextLift.
                </p>
            </div>

            <div className="mt-4">
            <button
                type="button"
                onClick={() => alert("Coming soon 🙂")}
                className="inline-flex items-center justify-center rounded-xl bg-cyan-500 px-4 py-2 text-sm font-semibold text-slate-950 hover:bg-cyan-400"
            >
                Pro Tier — $1/month
            </button>
            </div>
        </section>
      </main>
    </div>
  );
}

function Card({
  title,
  desc,
  to,
  cta,
  subtle,
}: {
  title: string;
  desc: string;
  to: string;
  cta: string;
  subtle?: boolean;
}) {
  return (
    <div
      className={
        "rounded-2xl border p-5 shadow-sm " +
        (subtle
          ? "border-slate-800 bg-slate-950"
          : "border-slate-800 bg-slate-900")
      }
    >
      <div className="text-sm font-semibold">{title}</div>
      <p className="mt-1 text-xs leading-relaxed text-slate-400">{desc}</p>

      <Link
        to={to}
        className="mt-4 inline-flex items-center justify-center rounded-xl border border-slate-800 bg-slate-950 px-3 py-2 text-xs font-semibold text-slate-100 hover:bg-slate-800"
      >
        {cta}
      </Link>
    </div>
  );
}
