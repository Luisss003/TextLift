import axios from "axios";
import * as auth from "../auth/token";

export const http = axios.create({
  baseURL: import.meta.env.VITE_API_BASE_URL,
});

const PUBLIC_PATHS = ["/login", "/signup"];

http.interceptors.request.use((config) => {
  const url = config.url ?? "";

  // Never block public endpoints
  if (PUBLIC_PATHS.some((p) => url.startsWith(p))) return config;

  // Attach token for protected endpoints only
  const token = auth.getToken();
  if (token) {
    config.headers = config.headers ?? {};
    config.headers.Authorization = `Bearer ${token}`;
  }
  return config;
});

http.interceptors.response.use(
  (res) => res,
  (error) => {
    const status = error?.response?.status;
    if (status === 401 || status === 403) {
      auth.clearSession();
      // optional: route to login (but better to let UI/router handle it)
      // window.location.href = "/login";
    }
    return Promise.reject(error);
  }
);


http.interceptors.response.use(
  (res) => res,
  (error) => {
    const status = error?.response?.status;
    if (status === 401) {
      auth.clearSession();
      window.location.href = "/login";
    }
    return Promise.reject(error);
  }
);
