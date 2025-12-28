type Props = React.ButtonHTMLAttributes<HTMLButtonElement> & {
    variant?: 'primary' | 'secondary';
};

export function Button({variant = "primary", className = "", ...props}: Props){
    const base = "rounded-md px-4 py-2 font-semibold focus:outline-none focus:ring-2 focus:ring-offset-2";
    const styles = variant === "primary"
        ? "bg-blue-600 text-white hover:bg-blue-700 focus:ring-blue-500"
        : "bg-gray-200 text-gray-800 hover:bg-gray-300 focus:ring-gray-400";

    return <button className={`${base} ${styles} ${className}`} {...props} />;
}