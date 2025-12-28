/**
 * Component which displays an empty state with a title and an action button
 * @param param0 
 * @returns 
 */
export function EmptyState({title, actionLabel, onAction}: 
    {title: string, actionLabel: string, onAction: () => void}){
    return (
        <div className="flex flex-col items-center justify-center p-6">
            <p>{title}</p>
            <button
                onClick={onAction}
                className="mt-2 rounded bg-blue-600 px-4 py-2 text-white hover:bg-blue-500"
            >
                {actionLabel}
            </button>
        </div>
    )
}