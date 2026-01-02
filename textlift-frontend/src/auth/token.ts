const SESSION_KEY = "session_active";

export function getToken(): null {
  // JWT is stored as an HttpOnly cookie; not readable from JS
  return null;
}

export function setSession(): void {
  // Track login state locally so the UI can render correctly
  localStorage.setItem(SESSION_KEY, "1");
}

export function isSessionValid(): boolean {
  return localStorage.getItem(SESSION_KEY) === "1";
}

export function clearSession(): void {
  localStorage.removeItem(SESSION_KEY);
}
