/**
 * Basic error state component that displays an error message and an optional retry button.
 * @param param0 
 * @returns 
 */
export function ErrorState({message, onRetry}: {message: string, onRetry?: () => void}) {
    return (
        <div className="flex flex-col items-center justify-center p-6">
            <p className="text-red-600">Error: {message}</p>
            {onRetry && (
                <button onClick={onRetry} className="mt-2 rounded bg-gray-800 px-4 py-2 text-white hover:bg-gray-700">
                    Retry
                </button>
            )}
        </div>
    );
}