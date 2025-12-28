import axios from "axios";
import {useForm, type SubmitHandler} from "react-hook-form";
import type { LoginRequest } from "../api/apiRequests";
import {login} from "../api/apiRequests";
import { useNavigate, useLocation, redirect, Navigate } from "react-router-dom";
import { Button } from "../components/ui/Button";

//TODO: possibly implement Zod?
//USED https://www.youtube.com/watch?v=cc_xmawJ8Kg

//Catch Axios errors
function getErrorMsg(err: unknown){
    if(axios.isAxiosError(err)){
        return err.response?.data?.message || err.message;
    }
    return "Login failed due to an unknown error.";
}

export function LoginPage(){
    const navigate = useNavigate();
    
    //When the user fills out a form and clicks submit, that data is sent here to our
    //obj
    const {register, 
        handleSubmit, 
        formState: {
            errors,
            isSubmitting
        },
    } = useForm<LoginRequest>();

    const onSubmit: SubmitHandler<LoginRequest> = async(data) => {
        //Data is being verified by formState below, so we can send it off
        try {
            await login(data);
            alert("Login successful!");
            navigate("/");
        }
        catch(err){
            getErrorMsg(err);
        }
    }

    return (
        <div className="">

            <h1>Login Page...</h1>
            <form className="" onSubmit={handleSubmit(onSubmit)}>
                <input {...register("email", {
                    required: "Email is required (duh)",
                    pattern: {
                        value: /^[a-zA-Z0-9._%+-]+@[a-zA-Z0-9.-]+\.[a-zA-Z]{2,}$/,
                        message: "Invalid email address"
                    }
                })} type="text" placeholder="Email" />
                {errors.email && (
                    <div className="text-red-500">{errors.email.message}</div>
                )}

                <input {...register("password",{
                    required: "Password is required",
                    minLength: {
                        value: 6,
                        message: "Password must be at least 6 characters long"
                    }
                })} type="password" placeholder="Password" />
                {errors.password && (
                    <div className="text-red-500">{errors.password.message}</div>
                )}

                <button disabled={isSubmitting}type="submit">
                    {isSubmitting ? "Logging in..." : "Login"}
                </button>
            </form>

            <Button onClick={() => navigate('/signup')}>Click here to register...</Button>
        </div>
    )
}