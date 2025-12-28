import { createBrowserRouter } from "react-router-dom";
import { AppShell } from "../layout/AppShell";
import { HomePage } from "../pages/HomePage";
import { AnnotationPage } from "../pages/AnnotationPage";
import { UploadPage } from "../pages/UploadPage";
import { LoginPage } from "../pages/LoginPage";
import { RegisterPage } from "../pages/RegisterPage";
import { requireAuth } from "../auth/requireAuth";


export const router = createBrowserRouter([
    //Public Routes
    {
        path: "/login",
        element: <LoginPage />,
    },
    {
        path: "/signup",
        element: <RegisterPage />,
    },
    {

        //Rather than taking in a component to create a page inside AppShell,
        //we simply output a <Outlet /> in AppShell and define child routes here.
        path: "/",
       // loader: requireAuth,
        element: <AppShell />,
        children: [
            {index: true, element: <HomePage />},
            {path: "annotations", element: <AnnotationPage />},
            {path: "upload", element: <UploadPage />},
        ]
    }

])
