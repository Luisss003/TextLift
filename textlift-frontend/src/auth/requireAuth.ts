import { redirect } from "react-router-dom";
import { isSessionValid, clearSession } from "../auth/token";

export function requireAuth() {
    //If user attemps to access a protected route without a valid session, 
    // log them out, and redirect to login
    if (!isSessionValid()) {
        clearSession();
        return redirect("/login");
    }
    return null;
}

export function isLoggedIn(): boolean {
    return isSessionValid();
}

