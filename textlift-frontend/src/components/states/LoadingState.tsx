import { Spinner } from "../ui/Spinner";
// A simple loading state component that shows a spinner and a loading message
export function LoadingState({label}: {label: string}) {
    return (
        <div className="flex flex-col items-center justify-center p-6">
            <p>{label}</p>
            <Spinner />
        </div>
    )
}