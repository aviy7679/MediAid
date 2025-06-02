
import React from 'react'
import { ArrowLeft, Search, Stethoscope } from 'lucide-react'
import MedicationSearch from './MedicationSearch'

export default function FillUserData() {
  const navigate = (path) => {
    window.location.href = path;
  };

  return (
    <div className="min-h-screen bg-gray-50">
      {/* Header */}
      <div className="bg-white shadow-sm border-b border-gray-200">
        <div className="max-w-7xl mx-auto px-8 py-6">
          <div className="flex items-center justify-between">
            <div className="flex items-center">
              <button
                onClick={() => navigate('/homePage')}
                className="mr-6 p-3 text-gray-500 hover:text-gray-700 rounded-lg hover:bg-gray-100 transition-colors"
              >
                <ArrowLeft className="w-6 h-6" />
              </button>
              <div>
                <h1 className="text-3xl font-bold text-blue-600 flex items-center">
                  <Search className="w-8 h-8 mr-4" />
                  Medical Information Search
                </h1>
                <p className="text-xl text-gray-600 mt-2">Search for medications, diseases, and medical information</p>
              </div>
            </div>
          </div>
        </div>
      </div>

      <div className="max-w-7xl mx-auto px-8 py-12">
        {/* Welcome Section */}
        <div className="text-center mb-12">
          <div className="flex items-center justify-center mb-6">
            <Stethoscope className="w-16 h-16 text-blue-600 mr-4" />
            <div>
              <h2 className="text-4xl font-bold text-gray-900">Medical Database Search</h2>
              <p className="text-xl text-gray-600 mt-2">
                Access comprehensive medical information at your fingertips
              </p>
            </div>
          </div>
        </div>

        {/* Search Tools Grid */}
        <div className="grid grid-cols-1 xl:grid-cols-2 gap-12 mb-12">
          {/* Medication Search Section */}
          <div className="bg-white rounded-xl shadow-lg border-2 border-gray-200 p-8">
            <div className="flex items-center mb-6">
              <div className="p-4 bg-blue-100 rounded-full mr-4">
                <Search className="w-8 h-8 text-blue-600" />
              </div>
              <div>
                <h3 className="text-2xl font-bold text-gray-900">Medication Search</h3>
                <p className="text-gray-600">Find detailed information about medications</p>
              </div>
            </div>
            <MedicationSearch />
          </div>

          {/* Disease Search Section - Coming Soon */}
          <div className="bg-white rounded-xl shadow-lg border-2 border-gray-200 p-8">
            <div className="flex items-center mb-6">
              <div className="p-4 bg-purple-100 rounded-full mr-4">
                <Stethoscope className="w-8 h-8 text-purple-600" />
              </div>
              <div>
                <h3 className="text-2xl font-bold text-gray-900">Disease Information</h3>
                <p className="text-gray-600">Search for diseases and conditions</p>
              </div>
            </div>
            
            <div className="text-center py-16">
              <Stethoscope className="w-20 h-20 mx-auto mb-6 text-gray-300" />
              <h4 className="text-xl font-semibold text-gray-700 mb-3">Coming Soon</h4>
              <p className="text-gray-500 mb-6">
                Disease search functionality will be available in the next update.
              </p>
              <div className="bg-purple-50 border-2 border-purple-200 rounded-xl p-6">
                <h5 className="font-semibold text-purple-900 mb-2">What's Coming:</h5>
                <ul className="text-purple-700 space-y-2">
                  <li>• Comprehensive disease database</li>
                  <li>• Symptom checker</li>
                  <li>• Treatment information</li>
                  <li>• Related conditions</li>
                </ul>
              </div>
            </div>
          </div>
        </div>

        {/* Features Overview */}
        <div className="bg-gradient-to-r from-blue-50 to-indigo-50 rounded-xl p-8 border-2 border-blue-200">
          <h3 className="text-2xl font-bold text-gray-900 mb-6 text-center">Available Features</h3>
          <div className="grid grid-cols-1 md:grid-cols-3 gap-8">
            <div className="text-center">
              <div className="bg-blue-100 p-4 rounded-full w-16 h-16 mx-auto mb-4 flex items-center justify-center">
                <Search className="w-8 h-8 text-blue-600" />
              </div>
              <h4 className="text-xl font-semibold text-gray-900 mb-2">Medication Search</h4>
              <p className="text-gray-600">
                Search through our comprehensive medication database with detailed information
              </p>
            </div>
            
            <div className="text-center">
              <div className="bg-green-100 p-4 rounded-full w-16 h-16 mx-auto mb-4 flex items-center justify-center">
                <Stethoscope className="w-8 h-8 text-green-600" />
              </div>
              <h4 className="text-xl font-semibold text-gray-900 mb-2">Real-time Results</h4>
              <p className="text-gray-600">
                Get instant search results as you type with our advanced search algorithms
              </p>
            </div>
            
            <div className="text-center">
              <div className="bg-purple-100 p-4 rounded-full w-16 h-16 mx-auto mb-4 flex items-center justify-center">
                <ArrowLeft className="w-8 h-8 text-purple-600 transform rotate-180" />
              </div>
              <h4 className="text-xl font-semibold text-gray-900 mb-2">Easy Integration</h4>
              <p className="text-gray-600">
                Seamlessly add found medications to your personal health profile
              </p>
            </div>
          </div>
        </div>

        {/* Quick Actions */}
        <div className="mt-12 text-center">
          <h3 className="text-2xl font-bold text-gray-900 mb-6">Quick Actions</h3>
          <div className="flex flex-wrap justify-center gap-4">
            <button
              onClick={() => navigate('/uploadUserData')}
              className="px-6 py-3 bg-blue-600 text-white rounded-xl hover:bg-blue-700 transition-colors font-medium"
            >
              Upload Medical Data
            </button>
            <button
              onClick={() => navigate('/treatment-guidelines')}
              className="px-6 py-3 bg-purple-600 text-white rounded-xl hover:bg-purple-700 transition-colors font-medium"
            >
              View Treatment Guidelines
            </button>
            <button
              onClick={() => navigate('/profile')}
              className="px-6 py-3 border-2 border-gray-300 text-gray-700 rounded-xl hover:bg-gray-50 transition-colors font-medium"
            >
              View Profile
            </button>
          </div>
        </div>
      </div>
    </div>
  )
}