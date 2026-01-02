import type { ReactNode } from "react";
import { Outlet } from "react-router-dom";
import { Navbar } from "./Navbar";

type AppShellProps = {
    children?: ReactNode;
};

export function AppShell({ children }: AppShellProps) {
    return (
        <div className="flex min-h-screen flex-col">
            <Navbar />
            <main className="flex-grow bg-gray-100 p-4">
                {children ?? <Outlet />}
            </main>
        </div>
    );
}
