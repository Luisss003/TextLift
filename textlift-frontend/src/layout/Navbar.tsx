import { NavLink } from "react-router-dom";
import { Button } from "../components/ui/Button";
import { clearToken } from "../auth/token";

//Takes in destination and label of nav item as prompts, and returns a NavLink component.
function NavItem({ to, label }: { to: string; label: string }) {  
    return (
        <NavLink
            to={to}
            className={({isActive}) => 
                "rounded-md px-3 py-2 text-sm" + 
                (isActive ? " bg-gray-900 text-white" : " text-black-300 bg-gray-300 hover:bg-gray-700 hover:text-white")
            }
        >
            {label}
        </NavLink>
    );
}


export function Navbar() {

    const handleLogout = () => {
      clearToken();
      window.location.href = "/login";
      alert("You have been logged out.");
    };

  return (
    <header className="border-b border-gray-200 bg-white">
      <div className="mx-auto flex h-14 max-w-5xl items-center justify-between px-4">
        <div className="text-sm font-semibold">
          <NavItem to="/" label="TextLift" />
        </div>
        <nav className="flex gap-1">
          <NavItem to="/signup" label="Register" />
          <NavItem to="/login" label="Login" />
          <NavItem to="/upload" label="Upload" />
          <NavItem to="/annotations" label="Annotations"/>
          <Button type="button" className="bg-red-500" onClick={handleLogout} variant="secondary">Log out</Button>
        </nav>
      </div>
    </header>
  );
}