import axios from 'axios';
import { useState, useRef } from 'react';

export default function LoginScreen() {
    const [errorMessage, setErrorMessage] = useState('');


    const handleSignIn = async (event) => {
        event.preventDefault();
        const data = new FormData(event.target);
        const formJSON = Object.fromEntries(data.entries());
        try {
            const response = await axios.post('http://localhost:8080/logIn', formJSON);
            console.log(response);
            setErrorMessage(''); 
        } catch (error) {
            if (error.response) {
                console.error('Error:', error.response.data);
                setErrorMessage(`Error: ${error.response.data}`);
            } else if (error.request) {
                console.error('No response received:', error.request);
                setErrorMessage('No response received from the server');
            } else {
                console.error('Error setting up request:', error.message);
                setErrorMessage('Error setting up request');
            }
        }
    }
    return (
        
        <div>
          <button>sign in</button>
            <h1>login to MediAid</h1>
            <form onSubmit={handleSignIn}>
            <label>
                email:
                <input type="email" name="mail" />
            </label>
            <br></br>
            <label>
                password:
                <input type="password" name="password" />
            </label>
            <br></br>
            <button>i forgot the password</button>
            <br></br>
            <button type="submit">login</button>
            <br></br>
            <button>connect by Google</button>
            </form>
            {errorMessage && <p style={{ color: 'red' }}>{errorMessage}</p>}
        </div>
    );
}