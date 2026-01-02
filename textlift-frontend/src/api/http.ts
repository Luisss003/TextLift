import axios from "axios";
import { clearSession } from "../auth/token";

export const http = axios.create({
  baseURL: import.meta.env.VITE_API_BASE_URL,
  withCredentials: true,
});

http.interceptors.response.use(
  (res) => res,
  (error) => {
    const status = error?.response?.status;
    if (status === 401) {
      clearSession();
      // Use a hard redirect to clear app state; consider using your router if available
      window.location.href = "/login";
    }
    return Promise.reject(error);
  }
);
