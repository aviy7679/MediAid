import React from 'react';
import axios from 'axios';
export default function SignIn() {

    const handleSubmit=async(event)=>{
        event.preventDefault();
        const data = new FormData(event.target);
        const formJSON = Object.fromEntries(data.entries());
        await axios.post('http://localhost:8080/signIn', formJSON)
        .then(response => {
            console.log(response);
            console.log(response.data);
        }
    )
    }

    return (
        <div>
            <h1>New User Registration</h1>
            <form onSubmit={handleSubmit}>
            <label>
                First Name:
                <input type="text" name="firstName" />
            </label>
            <br></br>
            <label>
                Last Name:
                <input type="text" name="lastName" />
            </label>
            <br></br>

            <label>
                Email Address:
                <input type="email" name="email" />
            </label>
            <br></br>

            <label>
                Password:
                <input type="password" name="password" />
            </label>
            <br></br>
            <button type="submit">Register</button>
            </form>
        </div>
    );
}