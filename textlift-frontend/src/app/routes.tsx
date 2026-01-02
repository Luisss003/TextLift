import { createBrowserRouter } from "react-router-dom";
import { AppShell } from "../layout/AppShell";
import { HomePage } from "../pages/HomePage";
import { UploadPage } from "../pages/UploadPage";
import { LoginPage } from "../pages/LoginPage";
import { RegisterPage } from "../pages/RegisterPage";
import { requireAuth } from "../auth/requireAuth";
import AnnotationDetailsPage from "../pages/AnnotationDetailsPage";
import UploadedDocumentsPage from "../pages/UploadedDocumentsPage";


export const router = createBrowserRouter([
    //Public Routes
    {
        path: "/",
        element: <HomePage />,
    },
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
        loader: requireAuth,
        element: <AppShell />,
        children: [
            {path: "documents", element: <UploadedDocumentsPage />},
            {path: "upload", element: <UploadPage />},
            {path: "annotations/document/:documentId", element: <AnnotationDetailsPage />},
        ]
    }

])
