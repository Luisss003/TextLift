import type { SignupRequest } from "../api/apiRequests";
import { signup } from "../api/apiRequests";
import {useForm, type SubmitHandler} from "react-hook-form";
import axios from "axios";

//USED https://www.youtube.com/watch?v=cc_xmawJ8Kg

function getErrorMsg(err: unknown){
    if(axios.isAxiosError(err)){
        return err.response?.data?.message || err.message;
    }
    return "Registration failed due to an unknown error.";
}

export function RegisterPage(){
    const {register, 
        handleSubmit, 
        formState: {
            errors,
            isSubmitting
        },
    } = useForm<SignupRequest>();

    const onSubmit: SubmitHandler<SignupRequest> = (async(data) => {
        try {
            await signup(data);
             window.location.href = "/"
            
        } catch(err){
            console.error("Signup failed:", err);
            console.error(getErrorMsg(err));
        }
    });

    return (
        <div className="">
            <h1>Register Page...</h1>
            <form className="" onSubmit={handleSubmit(onSubmit)}>
                <input {...register("fullName", {
                    required: "Full name is required"
                })} type="text" placeholder="Full Name" />
                {errors.fullName && (
                    <div className="text-red-500">{errors.fullName.message}</div>
                )}

                <input {...register("email", {
                    required: "Email is required",
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

                <button disabled={isSubmitting} type="submit">
                    {isSubmitting ? "Registering..." : "Register"}
                </button>
            </form>
        </div>
    );
}