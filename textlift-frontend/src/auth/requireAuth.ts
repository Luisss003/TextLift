import { redirect } from "react-router-dom";
import { getToken } from "../auth/token";

export function requireAuth() {
    const token = getToken();
    if (!token) {
        alert("You must be logged in to access this page.");
        return redirect("/login");
    }
    return null;
}

export function isLoggedIn(): boolean {
    return getToken() !== null;
}

