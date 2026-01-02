import {http} from "./http";
import type { DocumentPreview } from "../components/DocumentPreview";

//Login
export type LoginRequest = {
    email: string;
    password: string;
};

export async function login(data: LoginRequest): Promise<void> {
    await http.post("/api/v1/auth/login", data);
}

//Signup
export type SignupRequest = {
    email: string;
    password: string;
    fullName: string;
}

export async function signup(data: SignupRequest): Promise<void> {
    await http.post("/api/v1/auth/signup", data);
}

//Upload Requests/Responses
export type CreateUploadRequest = {
    hash: string;
    sizeBytes: number;
}

export type CreateUploadResponse = {
    uploadMode: string;
    uploadId: string;
    uploadStatus: string;
    documentId: string;
};

export type FinalizeUploadResponse = {
    documentId: string;
    documentStatus: string;
}

export async function createUpload(data: CreateUploadRequest): Promise<CreateUploadResponse> {
    const res = await http.post<CreateUploadResponse>("/api/v1/upload", data);
    return res.data;
}

export async function uploadFile(uploadId: string, file: File): Promise<void> {
    const formData = new FormData();
    formData.append("file", file);

    await http.post(`/api/v1/upload/${uploadId}/file`, formData, {
        headers: {
            "Content-Type": "multipart/form-data",
        },
    });
}

export async function finalizeUpload(uploadId: string): Promise<FinalizeUploadResponse> {
    const res = await http.post(`/api/v1/upload/${uploadId}/finalize`);
    return res.data;
}


export async function getUserUploads(): Promise<DocumentPreview[]> {
    const res = await http.get<DocumentPreview[]>("/api/v1/documents/uploads");
    return res.data;
}

export async function getAnnotationByDocumentId(id: string): Promise<JSON> {
    const res = await http.get<JSON>(`/api/v1/annotation/document/${id}`);
    return res.data;
}

export async function logout(): Promise<void> {
    await http.post("/api/v1/auth/logout");
}
