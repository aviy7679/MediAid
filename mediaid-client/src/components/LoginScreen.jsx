import React, { useState } from 'react';
import { Heart, User, Lock, Mail, ArrowRight, UserPlus } from 'lucide-react';
import { API_ENDPOINTS } from '../apiConfig';

const LoginScreen = () => {
  const [isLogin, setIsLogin] = useState(true);
  const [errorMessage, setErrorMessage] = useState('');
  const [isLoading, setIsLoading] = useState(false);
  const [showWelcome, setShowWelcome] = useState(false);
  const [loginData, setLoginData] = useState({ mail: '', password: '' });

  const navigate = (path) => {
    window.location.href = path;
  };

  const handleInputChange = (e) => {
    const { name, value } = e.target;
    setLoginData(prev => ({ ...prev, [name]: value }));
  };

  const handleLogin = async () => {
    setIsLoading(true);
    setErrorMessage('');
    
    try {
      const response = await fetch(API_ENDPOINTS.LOGIN, {
        method: 'POST',
        headers: { 'Content-Type': 'application/json' },
        body: JSON.stringify(loginData)
      });
      
      if (response.ok) {
        const responseData = await response.json();
        
        // Save token and user data
        localStorage.setItem('mediaid_token', responseData.token);
        localStorage.setItem('mediaid_user', JSON.stringify({
          username: responseData.username,
          email: responseData.email
        }));
        
        navigate('/homePage');
      } else {
        const errorData = await response.json();
        setErrorMessage(errorData.message || 'Login failed. Please check your credentials.');
      }
    } catch (error) {
      console.error('Login error:', error);
      setErrorMessage('Connection error. Please try again.');
    } finally {
      setIsLoading(false);
    }
  };

  const handleQuickSignup = () => {
    setShowWelcome(true);
    setTimeout(() => {
      navigate('/setup');
    }, 1500);
  };

  if (showWelcome) {
    return (
      <div className="min-h-screen bg-gradient-to-br from-blue-50 to-indigo-100 flex items-center justify-center">
        <div className="text-center">
          <div className="animate-pulse mb-8">
            <Heart className="w-20 h-20 text-blue-600 mx-auto" />
          </div>
          <h2 className="text-3xl font-bold text-gray-900 mb-4">Welcome to MediAid!</h2>
          <p className="text-xl text-gray-600">Let's set up your health profile...</p>
          <div className="mt-6">
            <div className="w-48 h-2 bg-blue-200 rounded-full mx-auto">
              <div className="w-full h-2 bg-blue-600 rounded-full animate-pulse"></div>
            </div>
          </div>
        </div>
      </div>
    );
  }

  return (
    <div className="min-h-screen bg-gradient-to-br from-blue-50 to-indigo-100 flex items-center justify-center p-8">
      <div className="w-full max-w-6xl flex bg-white rounded-3xl shadow-2xl overflow-hidden">
        {/* Left Side - Branding */}
        <div className="hidden lg:flex lg:w-1/2 bg-gradient-to-br from-blue-600 to-indigo-700 p-12 flex-col justify-center">
          <div className="text-white">
            <div className="flex items-center mb-8">
              <Heart className="w-16 h-16 text-white mr-4" />
              <h1 className="text-4xl font-bold">MediAid</h1>
            </div>
            <h2 className="text-3xl font-bold mb-6">Your Personal Health Assistant</h2>
            <p className="text-xl mb-8 text-blue-100">
              Take control of your health with our comprehensive medical management system
            </p>
            <div className="space-y-4">
              <div className="flex items-center">
                <div className="w-3 h-3 bg-white rounded-full mr-4"></div>
                <span className="text-lg">Track medications and medical history</span>
              </div>
              <div className="flex items-center">
                <div className="w-3 h-3 bg-white rounded-full mr-4"></div>
                <span className="text-lg">Get personalized health insights</span>
              </div>
              <div className="flex items-center">
                <div className="w-3 h-3 bg-white rounded-full mr-4"></div>
                <span className="text-lg">Access emergency diagnosis tools</span>
              </div>
              <div className="flex items-center">
                <div className="w-3 h-3 bg-white rounded-full mr-4"></div>
                <span className="text-lg">Secure and private health data</span>
              </div>
            </div>
          </div>
        </div>

        {/* Right Side - Login/Register */}
        <div className="w-full lg:w-1/2 p-12">
          {/* Mobile Logo (shown only on small screens) */}
          <div className="lg:hidden text-center mb-8">
            <div className="flex items-center justify-center mb-4">
              <Heart className="w-12 h-12 text-blue-600 mr-3" />
              <h1 className="text-3xl font-bold text-gray-900">MediAid</h1>
            </div>
            <p className="text-gray-600">Your Personal Health Assistant</p>
          </div>

          {/* Toggle Buttons */}
          <div className="flex bg-gray-100 rounded-xl p-2 mb-8">
            <button
              onClick={() => setIsLogin(true)}
              className={`flex-1 py-3 px-6 rounded-lg text-base font-medium transition-colors ${
                isLogin 
                  ? 'bg-white text-gray-900 shadow-sm' 
                  : 'text-gray-500 hover:text-gray-700'
              }`}
            >
              Sign In
            </button>
            <button
              onClick={() => setIsLogin(false)}
              className={`flex-1 py-3 px-6 rounded-lg text-base font-medium transition-colors ${
                !isLogin 
                  ? 'bg-white text-gray-900 shadow-sm' 
                  : 'text-gray-500 hover:text-gray-700'
              }`}
            >
              New User
            </button>
          </div>

          {isLogin ? (
            /* Login Form */
            <div className="space-y-8">
              <div>
                <h2 className="text-2xl font-bold text-gray-900 mb-2">Welcome Back</h2>
                <p className="text-gray-600">Sign in to your MediAid account</p>
              </div>

              <div className="space-y-6">
                <div>
                  <label className="block text-base font-medium text-gray-700 mb-3">
                    Email Address
                  </label>
                  <div className="relative">
                    <Mail className="absolute left-4 top-1/2 transform -translate-y-1/2 w-5 h-5 text-gray-400" />
                    <input
                      type="email"
                      name="mail"
                      value={loginData.mail}
                      onChange={handleInputChange}
                      required
                      className="w-full pl-12 pr-4 py-4 border border-gray-300 rounded-xl focus:ring-2 focus:ring-blue-500 focus:border-transparent text-base"
                      placeholder="Enter your email"
                    />
                  </div>
                </div>

                <div>
                  <label className="block text-base font-medium text-gray-700 mb-3">
                    Password
                  </label>
                  <div className="relative">
                    <Lock className="absolute left-4 top-1/2 transform -translate-y-1/2 w-5 h-5 text-gray-400" />
                    <input
                      type="password"
                      name="password"
                      value={loginData.password}
                      onChange={handleInputChange}
                      required
                      className="w-full pl-12 pr-4 py-4 border border-gray-300 rounded-xl focus:ring-2 focus:ring-blue-500 focus:border-transparent text-base"
                      placeholder="Enter your password"
                    />
                  </div>
                </div>

                <div className="flex items-center justify-between">
                  <label className="flex items-center">
                    <input type="checkbox" className="rounded border-gray-300 text-blue-600 focus:ring-blue-500" />
                    <span className="ml-2 text-base text-gray-600">Remember me</span>
                  </label>
                  <button type="button" className="text-base text-blue-600 hover:text-blue-700">
                    Forgot password?
                  </button>
                </div>

                <button
                  type="button"
                  onClick={handleLogin}
                  disabled={isLoading}
                  className={`w-full py-4 px-6 rounded-xl font-medium text-base transition-colors flex items-center justify-center ${
                    isLoading
                      ? 'bg-gray-400 text-gray-200 cursor-not-allowed'
                      : 'bg-blue-600 text-white hover:bg-blue-700'
                  }`}
                >
                  {isLoading ? (
                    <div className="w-6 h-6 border-2 border-gray-300 border-t-white rounded-full animate-spin"></div>
                  ) : (
                    <>
                      Sign In
                      <ArrowRight className="w-5 h-5 ml-2" />
                    </>
                  )}
                </button>

                {errorMessage && (
                  <div className="p-4 bg-red-50 border border-red-200 rounded-xl">
                    <p className="text-red-600 text-base">{errorMessage}</p>
                  </div>
                )}
              </div>
            </div>
          ) : (
            /* New User Section */
            <div className="text-center space-y-8">
              <div>
                <h2 className="text-2xl font-bold text-gray-900 mb-2">Join MediAid</h2>
                <p className="text-gray-600">Create your health profile and take control</p>
              </div>

              <div className="p-8 bg-blue-50 rounded-xl">
                <UserPlus className="w-16 h-16 text-blue-600 mx-auto mb-6" />
                <h3 className="text-xl font-semibold text-gray-900 mb-4">
                  Create Your Health Profile
                </h3>
                <p className="text-gray-600 mb-6 text-base">
                  Join MediAid and take control of your health with our comprehensive health management system.
                </p>
                <ul className="text-base text-gray-600 space-y-3 mb-8">
                  <li className="flex items-center justify-center">
                    <div className="w-3 h-3 bg-blue-600 rounded-full mr-3"></div>
                    Track medications and medical history
                  </li>
                  <li className="flex items-center justify-center">
                    <div className="w-3 h-3 bg-blue-600 rounded-full mr-3"></div>
                    Get personalized health insights
                  </li>
                  <li className="flex items-center justify-center">
                    <div className="w-3 h-3 bg-blue-600 rounded-full mr-3"></div>
                    Access emergency diagnosis tools
                  </li>
                  <li className="flex items-center justify-center">
                    <div className="w-3 h-3 bg-blue-600 rounded-full mr-3"></div>
                    Secure and private health data
                  </li>
                </ul>
              </div>

              <button
                onClick={handleQuickSignup}
                className="w-full py-4 px-6 bg-green-600 text-white text-base rounded-xl font-medium hover:bg-green-700 transition-colors flex items-center justify-center"
              >
                Get Started - It's Free!
                <ArrowRight className="w-5 h-5 ml-2" />
              </button>

              <p className="text-sm text-gray-500">
                By signing up, you agree to our Terms of Service and Privacy Policy
              </p>
            </div>
          )}
        </div>
      </div>

      {/* Footer */}
      <div className="absolute bottom-8 left-1/2 transform -translate-x-1/2">
        <p className="text-sm text-gray-500">
          © 2024 MediAid. Your health, our priority.
        </p>
      </div>
    </div>
  );
};

export default LoginScreen;