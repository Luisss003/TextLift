import { useState, useEffect } from "react";
import { LoadingState } from "../components/states/LoadingState";

export function AnnotationPage(){
    const [loading, setLoading] = useState(true);
    
    //We can simulate a fetch data then loading screen
    useEffect(() => {
        setTimeout(() => setLoading(false), 1000);
    }, []);

    if(loading){
        return <LoadingState label="Loading Annotations..." />
    }


    return (
        <h1>Annotation Page</h1>

    )
}