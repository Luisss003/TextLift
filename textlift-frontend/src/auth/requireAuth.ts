import { redirect } from "react-router-dom";
import { isSessionValid } from "./token";

export function requireAuth() {
  // Redirect unauthenticated users before rendering protected routes
  if (!isSessionValid()) {
    return redirect("/login");
  }
  return null;
}

export function isLoggedIn(): boolean {
  return isSessionValid();
}
