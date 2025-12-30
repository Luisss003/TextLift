const TOKEN_KEY= "textlift_token";
const EXP_KEY = "textlift_token_exp";

export function getToken(): string | null {
    return localStorage.getItem(TOKEN_KEY);
}

export function getTokenExp():number | null{
    const v = localStorage.getItem(EXP_KEY);
    return v ? Number(v) : null;
}

export function setSession(token: string, expiresInMS: number): void{
    localStorage.setItem(TOKEN_KEY, token);
    localStorage.setItem(EXP_KEY, (Date.now() + expiresInMS).toString());
}

export function clearSession(): void {
  localStorage.removeItem(TOKEN_KEY);
  localStorage.removeItem(EXP_KEY);
}

export function isSessionValid(skewMs = 30_000): boolean {
  const t = getToken();
  const exp = getTokenExp();
  if (!t || !exp) return false;
  return Date.now() + skewMs < exp; // treat near-expiry as expired
}