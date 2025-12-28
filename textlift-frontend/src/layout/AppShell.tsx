import { Outlet } from "react-router-dom";
import { Navbar } from "./Navbar";

export function AppShell() {
    return (
        <div className="flex min-h-screen flex-col">
            <Navbar />
            <main className="flex-grow bg-gray-100 p-4"><Outlet /></main>
        </div>
    )
}