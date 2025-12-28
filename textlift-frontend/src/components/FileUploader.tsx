import { useState } from "react";
import { createUpload, uploadFile } from "../api/apiRequests";
import type { CreateUploadResponse } from "../api/apiRequests";
import { Spinner } from "./ui/Spinner";
import { LoadingState } from "./states/LoadingState";

async function generateFilehash(file: File): Promise<string> {
  const buf = await file.arrayBuffer();
  const hashBuf = await crypto.subtle.digest("SHA-256", buf);

  const hashArr = Array.from(new Uint8Array(hashBuf));
  return hashArr.map(b => b.toString(16).padStart(2, "0")).join("");
}

export default function FileUploader(){
    const [file, setFile] = useState<File | null>(null);
    const [uploadStatus, setUploadStatus] = useState<string>("");
    const [uploadMode, setUploadMode] = useState<string>("");

    //Handles file selection
    const handleFileChange = (e: React.ChangeEvent<HTMLInputElement>) => {
        //If a file was uploaded set it to state (persist the file)
        const file = e.target.files?.[0];
        if (!file) return;
        setFile(file);
    }

    const handleFileUpload = async() => {
        if (!file) return;
          setUploadMode("");
          setUploadStatus("PENDING");
        const hash = await generateFilehash(file);
        try {
            const res: CreateUploadResponse = await createUpload({
                hash: hash,
                sizeBytes: file.size,
            });
            //First, we want to upload the metadata, and 
            //check to see if this specific file has already been processed,
            //or is currently being processed
            if(res.uploadMode === "CACHE_HIT"){
                setUploadStatus("UPLOADED");
                setUploadMode("CACHE_HIT");
                return;
            }
            if((res.uploadMode === "CACHE_HIT_WAIT")){
                setUploadMode("CACHE_HIT_WAIT");
                return;
            }

            //Otherwise, continue with the upload process
            setUploadMode("NEW_UPLOAD");
            setUploadStatus("UPLOADING");
            await uploadFile(res.uploadId, file);
            console.log("File uploaded successfully");

            //After uploading ,set to success
            setUploadStatus("UPLOADED");
            


        } catch(e){
            setUploadStatus("FAILED");
            console.error("Upload failed", e);
            return;
        }


    } 


    return (
        <div className="p-4 border border-dashed border-gray-300 rounded-md space-y-4">
            <input type="file" onChange={handleFileChange} />
            {file ? (
                <div>
                    <p>Selected file: {file.name}</p>
                    <button onClick={() => setFile(null)}>Remove File</button>
                </div>
            ) : (
                <p>No file selected</p>
            )}
            {file && uploadStatus !== "UPLOADING" && (
                <button onClick={handleFileUpload}>Upload File</button>
            )}

            {uploadStatus && uploadMode === "CACHE_HIT" && (
                <p>File already processed. No upload necessary.</p>
            )}
            {uploadStatus && uploadMode === "CACHE_HIT_WAIT" && (
                <p>File is being processed. Please wait.</p>
            )}
            {uploadStatus === "PENDING" && uploadMode === "NEW_UPLOAD" && (
                <p>Preparing to upload...</p>
            )}
            {uploadStatus === "UPLOADING" && (
                <LoadingState label="Uploading file, please wait..." />
            )}
            {uploadStatus === "UPLOADED" && uploadMode === "NEW_UPLOAD" && (
                <p>File uploaded successfully! Check back later for annotations</p>
            )}
            {uploadStatus === "FAILED" && (
                <p className="text-red-500">Upload failed. Please try again.</p>
            )}
        </div>
    );
}