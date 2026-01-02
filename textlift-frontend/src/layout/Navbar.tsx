import { NavLink } from "react-router-dom";
import { Button } from "../components/ui/Button";
import { isLoggedIn } from "../auth/requireAuth";
import { logout } from "../api/apiRequests";
import { clearSession } from "../auth/token";

// Nav item with dark theme styles
function NavItem({ to, label }: { to: string; label: string }) {
  return (
    <NavLink
      to={to}
      className={({ isActive }) =>
        [
          "rounded-lg px-3 py-2 text-sm font-semibold transition-colors",
          "border border-slate-800",
          isActive
            ? "bg-slate-900 text-slate-100"
            : "bg-slate-950 text-slate-300 hover:bg-slate-900 hover:text-slate-100",
        ].join(" ")
      }
    >
      {label}
    </NavLink>
  );
}

export function Navbar() {
  const handleLogout = async () => {
    try {
      await logout();
    } catch (e) {
      // Even if logout fails, still clear client state
      console.error("Logout failed", e);
    } finally {
      clearSession();
      window.location.href = "/";
      alert("You have been logged out.");
    }
  };

  return (
    <header className="border-b border-slate-800 bg-slate-950 text-slate-100">
      <div className="mx-auto flex h-14 max-w-5xl items-center justify-between px-4">
        {/* Brand */}
        <div className="flex items-center gap-3">
          <div className="grid h-9 w-9 place-items-center rounded-xl border border-slate-800 bg-slate-900">
            <span className="text-sm font-bold tracking-tight">TL</span>
          </div>

          <div className="leading-tight">
            <div className="text-sm font-semibold">
              <NavLink to="/" className="hover:text-slate-50">
                TextLift
              </NavLink>
            </div>
            <div className="text-xs text-slate-400">Lift your textbooks into notes</div>
          </div>
        </div>

        {/* Nav */}
        <nav className="flex items-center gap-2">
          {isLoggedIn() ? (
            <div className="flex items-center gap-2">
              <NavItem to="/upload" label="Upload" />
              <NavItem to="/documents" label="Documents" />

              <Button
                type="button"
                onClick={handleLogout}
                variant="secondary"
                className="border border-slate-800 bg-slate-900 text-slate-100 hover:bg-red-600 hover:border-red-500"
              >
                Log out
              </Button>
            </div>
          ) : (
            <div className="flex items-center gap-2">
              <NavItem to="/signup" label="Register" />
              <NavItem to="/login" label="Login" />
            </div>
          )}
        </nav>
      </div>
    </header>
  );
}
