import axios from "axios";
import { useState } from "react";
import { useForm, type SubmitHandler } from "react-hook-form";
import type { LoginRequest } from "../api/apiRequests";
import { login } from "../api/apiRequests";
import { setSession } from "../auth/token";
import { useNavigate, NavLink } from "react-router-dom";
import { Navbar } from "../layout/Navbar";

// Catch Axios errors
function getErrorMsg(err: unknown) {
  if (axios.isAxiosError(err)) {
    // common patterns: { message }, or plain string
    const msg =
      (err.response?.data as any)?.message ||
      (typeof err.response?.data === "string" ? err.response?.data : null) ||
      err.message;

    return msg || "Login failed.";
  }
  return "Login failed due to an unknown error.";
}

export function LoginPage() {
  const navigate = useNavigate();
  const [serverError, setServerError] = useState<string | null>(null);

  const {
    register,
    handleSubmit,
    formState: { errors, isSubmitting },
  } = useForm<LoginRequest>({
    defaultValues: { email: "", password: "" },
    mode: "onTouched",
  });

  const onSubmit: SubmitHandler<LoginRequest> = async (data) => {
    setServerError(null);
    try {
      await login(data);
      setSession();
      // no alert — just route
      navigate("/", { replace: true });
    } catch (err) {
      setServerError(getErrorMsg(err));
    }
  };

  return (
    <div className="min-h-screen bg-slate-950 text-slate-100">
      <Navbar />

      <main className="mx-auto max-w-5xl px-4 py-10">
        <div className="mx-auto w-full max-w-md rounded-2xl border border-slate-800 bg-slate-900 p-6 shadow-sm">
          <div className="space-y-1">
            <h1 className="text-xl font-bold tracking-tight">Welcome back</h1>
            <p className="text-sm text-slate-400">
              Log in to continue to TextLift.
            </p>
          </div>

          {serverError && (
            <div className="mt-4 rounded-xl border border-red-900/50 bg-slate-950 p-3 text-sm text-red-300">
              {serverError}
            </div>
          )}

          <form className="mt-5 space-y-4" onSubmit={handleSubmit(onSubmit)}>
            <div>
              <label className="mb-1 block text-xs font-semibold text-slate-300">
                Email
              </label>
              <input
                {...register("email", {
                  required: "Email is required",
                  pattern: {
                    value:
                      /^[a-zA-Z0-9._%+-]+@[a-zA-Z0-9.-]+\.[a-zA-Z]{2,}$/,
                    message: "Invalid email address",
                  },
                })}
                disabled={isSubmitting}
                type="email"
                inputMode="email"
                autoComplete="email"
                placeholder="you@example.com"
                className={[
                  "w-full rounded-xl border bg-slate-950 px-3 py-2 text-sm text-slate-100 placeholder:text-slate-600",
                  "focus:outline-none focus:ring-2 focus:ring-cyan-500/40",
                  errors.email ? "border-red-600" : "border-slate-800",
                  isSubmitting ? "opacity-70" : "",
                ].join(" ")}
              />
              {errors.email && (
                <div className="mt-1 text-xs text-red-400">
                  {errors.email.message}
                </div>
              )}
            </div>

            <div>
              <label className="mb-1 block text-xs font-semibold text-slate-300">
                Password
              </label>
              <input
                {...register("password", {
                  required: "Password is required",
                  minLength: {
                    value: 6,
                    message: "Password must be at least 6 characters long",
                  },
                })}
                disabled={isSubmitting}
                type="password"
                autoComplete="current-password"
                placeholder="••••••••"
                className={[
                  "w-full rounded-xl border bg-slate-950 px-3 py-2 text-sm text-slate-100 placeholder:text-slate-600",
                  "focus:outline-none focus:ring-2 focus:ring-cyan-500/40",
                  errors.password ? "border-red-600" : "border-slate-800",
                  isSubmitting ? "opacity-70" : "",
                ].join(" ")}
              />
              {errors.password && (
                <div className="mt-1 text-xs text-red-400">
                  {errors.password.message}
                </div>
              )}
            </div>

            <button
              disabled={isSubmitting}
              type="submit"
              className={[
                "w-full rounded-xl px-4 py-2 text-sm font-semibold",
                "bg-cyan-500 text-slate-950 hover:bg-cyan-400 active:bg-cyan-600",
                "disabled:cursor-not-allowed disabled:opacity-70",
              ].join(" ")}
            >
              {isSubmitting ? "Logging in..." : "Log in"}
            </button>

            <div className="flex items-center justify-between text-xs text-slate-400">
              <span>Don’t have an account?</span>
              <NavLink
                to="/signup"
                className="font-semibold text-cyan-300 hover:text-cyan-200"
              >
                Create one
              </NavLink>
            </div>
          </form>
        </div>
      </main>
    </div>
  );
}
