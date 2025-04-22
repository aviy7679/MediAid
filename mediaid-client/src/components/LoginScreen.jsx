import axios from 'axios';
import { useState } from 'react';
import { useNavigate } from 'react-router-dom';

export default function LoginScreen() {
    const [errorMessage, setErrorMessage] = useState('');
    const navigate = useNavigate();

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
    };

    return (
        <div className="flex items-center justify-center min-h-screen bg-gray-100">
            <div className="bg-white p-8 rounded-xl shadow-lg w-full max-w-md">
                <div className="flex justify-between mb-4">
                    <h1 className="text-2xl font-bold text-gray-800">Login to MediAid</h1>
                    <button
                        className="text-sm text-blue-600 hover:underline"
                        onClick={() => navigate('/signIn')}
                    >
                        Sign Up
                    </button>
                </div>

                <form onSubmit={handleSignIn} className="space-y-4">
                    <div>
                        <label className="block text-gray-700">Email</label>
                        <input
                            type="email"
                            name="mail"
                            required
                            className="w-full px-4 py-2 border border-gray-300 rounded-md focus:outline-none focus:ring-2 focus:ring-blue-400"
                        />
                    </div>

                    <div>
                        <label className="block text-gray-700">Password</label>
                        <input
                            type="password"
                            name="password"
                            required
                            className="w-full px-4 py-2 border border-gray-300 rounded-md focus:outline-none focus:ring-2 focus:ring-blue-400"
                        />
                    </div>

                    <div className="flex justify-end">
                        <button
                            type="button"
                            className="text-sm text-blue-500 hover:underline"
                        >
                            I forgot my password
                        </button>
                    </div>

                    <button
                        type="submit"
                        className="w-full bg-blue-600 text-white py-2 rounded-md hover:bg-blue-700 transition"
                    >
                        Login
                    </button>

                    <button
                        type="button"
                        disabled
                        className="w-full mt-2 bg-gray-200 text-gray-600 py-2 rounded-md cursor-not-allowed"
                    >
                        Connect with Google (Not working)
                    </button>
                </form>

                {errorMessage && (
                    <p className="mt-4 text-red-600 text-sm text-center">{errorMessage}</p>
                )}
            </div>
        </div>
    );
}
