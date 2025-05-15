// import React from 'react';
// import axios from 'axios';
// import { useNavigate } from 'react-router-dom';

// export default function SignIn() {
//     const navigate=useNavigate();
//     const handleSubmit = async (event) => {
//         event.preventDefault();
//         const data = new FormData(event.target);
//         const formJSON = Object.fromEntries(data.entries());

//         try {
//             const response = await axios.post('http://localhost:8080/signIn', formJSON);
//             console.log(response);
//             console.log(response.data);
//             navigate('/homePage');
//         } catch (error) {
//             console.error('Error during sign in:', error);
//         }
//     };

//     return (
//         <div className="flex items-center justify-center min-h-screen bg-gray-100 px-4">
//             <div className="bg-white p-6 sm:p-8 rounded-xl shadow-lg w-full max-w-md">
//                 <h1 className="text-2xl font-bold mb-6 text-center text-gray-800">New User Registration</h1>
//                 <form onSubmit={handleSubmit} className="space-y-4">
//                     <div>
//                         <label className="block text-gray-700 mb-1">First Name</label>
//                         <input
//                             type="text"
//                             name="firstName"
//                             required
//                             className="w-full px-4 py-2 border border-gray-300 rounded-md focus:outline-none focus:ring-2 focus:ring-blue-400"
//                         />
//                     </div>
//                     <div>
//                         <label className="block text-gray-700 mb-1">Last Name</label>
//                         <input
//                             type="text"
//                             name="lastName"
//                             required
//                             className="w-full px-4 py-2 border border-gray-300 rounded-md focus:outline-none focus:ring-2 focus:ring-blue-400"
//                         />
//                     </div>
//                     <div>
//                         <label className="block text-gray-700 mb-1">Email Address</label>
//                         <input
//                             type="email"
//                             name="email"
//                             required
//                             className="w-full px-4 py-2 border border-gray-300 rounded-md focus:outline-none focus:ring-2 focus:ring-blue-400"
//                         />
//                     </div>
//                     <div>
//                         <label className="block text-gray-700 mb-1">Password</label>
//                         <input
//                             type="password"
//                             name="password"
//                             required
//                             className="w-full px-4 py-2 border border-gray-300 rounded-md focus:outline-none focus:ring-2 focus:ring-blue-400"
//                         />
//                     </div>
//                     <button
//                         type="submit"
//                         className="w-full bg-blue-600 text-white py-2 rounded-md hover:bg-blue-700 transition"
//                     >
//                         Register
//                     </button>
//                 </form>
//             </div>
//         </div>
//     );
// }
import React, { useState } from 'react';
import axios from 'axios';
import { useNavigate, Link } from 'react-router-dom';

const API_URL = 'http://localhost:8080';

function SignIn() {
  const [formData, setFormData] = useState({
    username: '',
    email: '',
    password: '',
    dateOfBirth: '',
    gender: '',
    height: '',
    weight: ''
  });
  const [error, setError] = useState('');
  const [success, setSuccess] = useState('');
  const [isLoading, setIsLoading] = useState(false);
  const navigate = useNavigate();

  const handleChange = (e) => {
    const { name, value } = e.target;
    setFormData(prevData => ({
      ...prevData,
      [name]: value
    }));
  };

  const handleSubmit = async (e) => {
    e.preventDefault();
    setError('');
    setSuccess('');
    setIsLoading(true);
    
    try {
      // Validate form inputs
      if (!formData.username || !formData.email || !formData.password) {
        setError('Username, email, and password are required');
        setIsLoading(false);
        return;
      }
      
      // Email validation
      const emailRegex = /^[^\s@]+@[^\s@]+\.[^\s@]+$/;
      if (!emailRegex.test(formData.email)) {
        setError('Please enter a valid email address');
        setIsLoading(false);
        return;
      }
      
      // Make API call
      const response = await axios.post(`${API_URL}/signUp`, formData, {
        headers: {
          'Content-Type': 'application/json'
        }
      });
      
      console.log('Sign up successful:', response.data);
      setSuccess('Account created successfully! Redirecting to login...');
      
      // Redirect to login after successful registration
      setTimeout(() => {
        navigate('/login');
      }, 2000);
      
    } catch (error) {
      console.error('Error during sign in:', error);
      
      if (error.response) {
        // Server responded with an error status
        setError(error.response.data || 'Registration failed. Please try again.');
      } else if (error.request) {
        // No response received from server
        setError('Cannot connect to server. Please check your internet connection or try again later.');
      } else {
        // Error in setting up the request
        setError('An unexpected error occurred. Please try again.');
      }
    } finally {
      setIsLoading(false);
    }
  };

  return (
    <div className="min-h-screen flex items-center justify-center bg-gray-50">
      <div className="max-w-md w-full p-6 bg-white rounded-lg shadow-md">
        <h2 className="text-2xl font-bold text-center text-gray-800 mb-6">Create an Account</h2>
        
        {error && (
          <div className="mb-4 p-3 bg-red-100 border border-red-400 text-red-700 rounded">
            {error}
          </div>
        )}
        
        {success && (
          <div className="mb-4 p-3 bg-green-100 border border-green-400 text-green-700 rounded">
            {success}
          </div>
        )}
        
        <form onSubmit={handleSubmit} className="space-y-4">
          <div>
            <label className="block text-gray-700">Username</label>
            <input
              required
              className="w-full px-4 py-2 border border-gray-300 rounded-md focus:outline-none focus:ring-2 focus:ring-blue-400"
              type="text"
              name="username"
              value={formData.username}
              onChange={handleChange}
            />
          </div>
          
          <div>
            <label className="block text-gray-700">Email</label>
            <input
              required
              className="w-full px-4 py-2 border border-gray-300 rounded-md focus:outline-none focus:ring-2 focus:ring-blue-400"
              type="email"
              name="email"
              value={formData.email}
              onChange={handleChange}
            />
          </div>
          
          <div>
            <label className="block text-gray-700">Password</label>
            <input
              required
              className="w-full px-4 py-2 border border-gray-300 rounded-md focus:outline-none focus:ring-2 focus:ring-blue-400"
              type="password"
              name="password"
              autoComplete="new-password"
              value={formData.password}
              onChange={handleChange}
            />
          </div>
          
          <div>
            <label className="block text-gray-700">Date of Birth</label>
            <input
              className="w-full px-4 py-2 border border-gray-300 rounded-md focus:outline-none focus:ring-2 focus:ring-blue-400"
              type="date"
              name="dateOfBirth"
              value={formData.dateOfBirth}
              onChange={handleChange}
            />
          </div>
          
          <div>
            <label className="block text-gray-700">Gender</label>
            <select
              className="w-full px-4 py-2 border border-gray-300 rounded-md focus:outline-none focus:ring-2 focus:ring-blue-400"
              name="gender"
              value={formData.gender}
              onChange={handleChange}
            >
              <option value="">Select Gender</option>
              <option value="male">Male</option>
              <option value="female">Female</option>
              <option value="other">Other</option>
            </select>
          </div>
          
          <div>
            <label className="block text-gray-700">Height (cm)</label>
            <input
              className="w-full px-4 py-2 border border-gray-300 rounded-md focus:outline-none focus:ring-2 focus:ring-blue-400"
              type="number"
              name="height"
              value={formData.height}
              onChange={handleChange}
            />
          </div>
          
          <div>
            <label className="block text-gray-700">Weight (kg)</label>
            <input
              className="w-full px-4 py-2 border border-gray-300 rounded-md focus:outline-none focus:ring-2 focus:ring-blue-400"
              type="number"
              name="weight"
              value={formData.weight}
              onChange={handleChange}
            />
          </div>
          
          <button
            type="submit"
            className="w-full py-2 px-4 bg-blue-500 text-white font-semibold rounded-md hover:bg-blue-600 focus:outline-none focus:ring-2 focus:ring-blue-400 disabled:bg-blue-300"
            disabled={isLoading}
          >
            {isLoading ? 'Creating Account...' : 'Create Account'}
          </button>
        </form>
        
        <div className="mt-4 text-center">
          <p className="text-gray-600">
            Already have an account?{' '}
            <Link to="/login" className="text-blue-500 hover:underline">
              Log In
            </Link>
          </p>
        </div>
      </div>
    </div>
  );
}

export default SignIn;
