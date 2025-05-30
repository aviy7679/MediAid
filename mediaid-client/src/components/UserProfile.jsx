import React, { useState, useEffect } from 'react';
import { User, Heart, Pill, Stethoscope, Activity, Calendar, TrendingUp, AlertTriangle, Edit, Plus } from 'lucide-react';
import { API_ENDPOINTS } from '../apiConfig';

const UserProfile = () => {
  const [profileData, setProfileData] = useState(null);
  const [loading, setLoading] = useState(true);
  const [error, setError] = useState('');
  const [activeTab, setActiveTab] = useState('overview');

  useEffect(() => {
    fetchUserProfile();
  }, []);

  const fetchUserProfile = async () => {
    try {
      const token = localStorage.getItem('mediaid_token');
      const response = await fetch(API_ENDPOINTS.USER_PROFILE, {
        headers: {
          'Authorization': `Bearer ${token}`,
          'Content-Type': 'application/json'
        }
      });

      if (response.ok) {
        const data = await response.json();
        setProfileData(data);
      } else {
        setError('Failed to load profile data');
      }
    } catch (err) {
      setError('Error loading profile: ' + err.message);
    } finally {
      setLoading(false);
    }
  };

  const getBmiColor = (bmi) => {
    if (!bmi) return 'text-gray-500';
    if (bmi < 18.5) return 'text-blue-500';
    if (bmi < 25) return 'text-green-500';
    if (bmi < 30) return 'text-yellow-500';
    return 'text-red-500';
  };

  const getRiskLevelColor = (riskLevel) => {
    switch (riskLevel?.toLowerCase()) {
      case 'low risk': return 'text-green-500 bg-green-100';
      case 'moderate risk': return 'text-yellow-600 bg-yellow-100';
      case 'high risk': return 'text-orange-600 bg-orange-100';
      case 'very high risk': return 'text-red-600 bg-red-100';
      default: return 'text-gray-500 bg-gray-100';
    }
  };

  const formatDate = (dateString) => {
    if (!dateString) return 'Not specified';
    return new Date(dateString).toLocaleDateString();
  };

  const OverviewTab = () => (
    <div className="space-y-6">
      {/* Health Summary Cards */}
      <div className="grid grid-cols-1 md:grid-cols-2 lg:grid-cols-4 gap-6">
        <div className="bg-white p-6 rounded-lg border border-gray-200">
          <div className="flex items-center">
            <div className="p-3 rounded-full bg-blue-100">
              <User className="w-6 h-6 text-blue-600" />
            </div>
            <div className="ml-4">
              <p className="text-sm font-medium text-gray-600">Age</p>
              <p className="text-2xl font-bold text-gray-900">
                {profileData?.basicInfo?.dateOfBirth ? 
                  new Date().getFullYear() - new Date(profileData.basicInfo.dateOfBirth).getFullYear() : 'N/A'}
              </p>
            </div>
          </div>
        </div>

        <div className="bg-white p-6 rounded-lg border border-gray-200">
          <div className="flex items-center">
            <div className="p-3 rounded-full bg-green-100">
              <Activity className="w-6 h-6 text-green-600" />
            </div>
            <div className="ml-4">
              <p className="text-sm font-medium text-gray-600">BMI</p>
              <p className={`text-2xl font-bold ${getBmiColor(profileData?.basicInfo?.bmi)}`}>
                {profileData?.basicInfo?.bmi?.toFixed(1) || 'N/A'}
              </p>
              <p className="text-sm text-gray-500">{profileData?.basicInfo?.bmiCategory}</p>
            </div>
          </div>
        </div>

        <div className="bg-white p-6 rounded-lg border border-gray-200">
          <div className="flex items-center">
            <div className="p-3 rounded-full bg-purple-100">
              <Pill className="w-6 h-6 text-purple-600" />
            </div>
            <div className="ml-4">
              <p className="text-sm font-medium text-gray-600">Active Medications</p>
              <p className="text-2xl font-bold text-gray-900">
                {profileData?.stats?.activeMedications || 0}
              </p>
            </div>
          </div>
        </div>

        <div className="bg-white p-6 rounded-lg border border-gray-200">
          <div className="flex items-center">
            <div className="p-3 rounded-full bg-red-100">
              <Stethoscope className="w-6 h-6 text-red-600" />
            </div>
            <div className="ml-4">
              <p className="text-sm font-medium text-gray-600">Active Conditions</p>
              <p className="text-2xl font-bold text-gray-900">
                {profileData?.stats?.activeDiseases || 0}
              </p>
            </div>
          </div>
        </div>
      </div>

      {/* Risk Assessment */}
      <div className="bg-white p-6 rounded-lg border border-gray-200">
        <h3 className="text-lg font-semibold text-gray-900 mb-4 flex items-center">
          <TrendingUp className="w-5 h-5 mr-2" />
          Health Risk Assessment
        </h3>
        <div className="flex items-center justify-between">
          <div>
            <div className="flex items-center">
              <span className={`px-3 py-1 rounded-full text-sm font-medium ${getRiskLevelColor(profileData?.riskFactors?.riskLevel)}`}>
                {profileData?.riskFactors?.riskLevel || 'Not Assessed'}
              </span>
              <span className="ml-3 text-sm text-gray-600">
                Score: {profileData?.riskFactors?.overallRiskScore?.toFixed(2) || 'N/A'}
              </span>
            </div>
            <p className="text-sm text-gray-500 mt-2">
              Based on your lifestyle factors and medical history
            </p>
          </div>
          <button className="px-4 py-2 bg-blue-600 text-white rounded-lg hover:bg-blue-700 transition-colors">
            View Details
          </button>
        </div>
      </div>

      {/* Profile Completeness */}
      <div className="bg-white p-6 rounded-lg border border-gray-200">
        <h3 className="text-lg font-semibold text-gray-900 mb-4">Profile Completeness</h3>
        <div className="flex items-center">
          <div className="flex-1">
            <div className="bg-gray-200 rounded-full h-3">
              <div 
                className="bg-green-500 h-3 rounded-full transition-all duration-300"
                style={{ width: `${profileData?.stats?.profileCompleteness || 0}%` }}
              ></div>
            </div>
          </div>
          <span className="ml-4 text-sm font-medium text-gray-600">
            {Math.round(profileData?.stats?.profileCompleteness || 0)}%
          </span>
        </div>
        <p className="text-sm text-gray-500 mt-2">
          Complete your profile to get better health insights
        </p>
      </div>
    </div>
  );

  const BasicInfoTab = () => (
    <div className="bg-white p-6 rounded-lg border border-gray-200">
      <div className="flex justify-between items-center mb-6">
        <h3 className="text-lg font-semibold text-gray-900">Basic Information</h3>
        <button className="px-4 py-2 bg-blue-600 text-white rounded-lg hover:bg-blue-700 transition-colors flex items-center">
          <Edit className="w-4 h-4 mr-2" />
          Edit
        </button>
      </div>

      <div className="grid grid-cols-1 md:grid-cols-2 gap-6">
        <div>
          <label className="block text-sm font-medium text-gray-600 mb-1">Username</label>
          <p className="text-lg text-gray-900">{profileData?.basicInfo?.username || 'Not specified'}</p>
        </div>
        <div>
          <label className="block text-sm font-medium text-gray-600 mb-1">Email</label>
          <p className="text-lg text-gray-900">{profileData?.basicInfo?.email || 'Not specified'}</p>
        </div>
        <div>
          <label className="block text-sm font-medium text-gray-600 mb-1">Date of Birth</label>
          <p className="text-lg text-gray-900">{formatDate(profileData?.basicInfo?.dateOfBirth)}</p>
        </div>
        <div>
          <label className="block text-sm font-medium text-gray-600 mb-1">Gender</label>
          <p className="text-lg text-gray-900 capitalize">{profileData?.basicInfo?.gender || 'Not specified'}</p>
        </div>
        <div>
          <label className="block text-sm font-medium text-gray-600 mb-1">Height</label>
          <p className="text-lg text-gray-900">
            {profileData?.basicInfo?.height ? `${profileData.basicInfo.height} cm` : 'Not specified'}
          </p>
        </div>
        <div>
          <label className="block text-sm font-medium text-gray-600 mb-1">Weight</label>
          <p className="text-lg text-gray-900">
            {profileData?.basicInfo?.weight ? `${profileData.basicInfo.weight} kg` : 'Not specified'}
          </p>
        </div>
        {profileData?.basicInfo?.bmi && (
          <>
            <div>
              <label className="block text-sm font-medium text-gray-600 mb-1">BMI</label>
              <p className={`text-lg font-medium ${getBmiColor(profileData.basicInfo.bmi)}`}>
                {profileData.basicInfo.bmi.toFixed(1)}
              </p>
            </div>
            <div>
              <label className="block text-sm font-medium text-gray-600 mb-1">BMI Category</label>
              <p className={`text-lg font-medium ${getBmiColor(profileData.basicInfo.bmi)}`}>
                {profileData.basicInfo.bmiCategory}
              </p>
            </div>
          </>
        )}
      </div>
    </div>
  );

  const RiskFactorsTab = () => (
    <div className="space-y-6">
      <div className="bg-white p-6 rounded-lg border border-gray-200">
        <div className="flex justify-between items-center mb-6">
          <h3 className="text-lg font-semibold text-gray-900">Risk Factors Assessment</h3>
          <button className="px-4 py-2 bg-blue-600 text-white rounded-lg hover:bg-blue-700 transition-colors flex items-center">
            <Edit className="w-4 h-4 mr-2" />
            Update
          </button>
        </div>

        <div className="grid grid-cols-1 md:grid-cols-2 gap-6">
          <div>
            <label className="block text-sm font-medium text-gray-600 mb-1">Smoking Status</label>
            <p className="text-lg text-gray-900">
              {profileData?.riskFactors?.smokingStatus?.replace(/_/g, ' ').toLowerCase().replace(/\b\w/g, l => l.toUpperCase()) || 'Not specified'}
            </p>
          </div>
          <div>
            <label className="block text-sm font-medium text-gray-600 mb-1">Alcohol Consumption</label>
            <p className="text-lg text-gray-900">
              {profileData?.riskFactors?.alcoholConsumption?.replace(/_/g, ' ').toLowerCase().replace(/\b\w/g, l => l.toUpperCase()) || 'Not specified'}
            </p>
          </div>
          <div>
            <label className="block text-sm font-medium text-gray-600 mb-1">Physical Activity</label>
            <p className="text-lg text-gray-900">
              {profileData?.riskFactors?.physicalActivity?.replace(/_/g, ' ').toLowerCase().replace(/\b\w/g, l => l.toUpperCase()) || 'Not specified'}
            </p>
          </div>
          <div>
            <label className="block text-sm font-medium text-gray-600 mb-1">Blood Pressure</label>
            <p className="text-lg text-gray-900">
              {profileData?.riskFactors?.bloodPressure?.replace(/_/g, ' ').toLowerCase().replace(/\b\w/g, l => l.toUpperCase()) || 'Not specified'}
            </p>
          </div>
          <div>
            <label className="block text-sm font-medium text-gray-600 mb-1">Stress Level</label>
            <p className="text-lg text-gray-900">
              {profileData?.riskFactors?.stressLevel?.replace(/_/g, ' ').toLowerCase().replace(/\b\w/g, l => l.toUpperCase()) || 'Not specified'}
            </p>
          </div>
          <div>
            <label className="block text-sm font-medium text-gray-600 mb-1">Age Group</label>
            <p className="text-lg text-gray-900">
              {profileData?.riskFactors?.ageGroup?.replace(/_/g, ' ').toLowerCase().replace(/\b\w/g, l => l.toUpperCase()) || 'Not specified'}
            </p>
          </div>
          <div>
            <label className="block text-sm font-medium text-gray-600 mb-1">Family Heart Disease History</label>
            <p className="text-lg text-gray-900">
              {profileData?.riskFactors?.familyHeartDisease?.replace(/_/g, ' ').toLowerCase().replace(/\b\w/g, l => l.toUpperCase()) || 'Not specified'}
            </p>
          </div>
          <div>
            <label className="block text-sm font-medium text-gray-600 mb-1">Family Cancer History</label>
            <p className="text-lg text-gray-900">
              {profileData?.riskFactors?.familyCancer?.replace(/_/g, ' ').toLowerCase().replace(/\b\w/g, l => l.toUpperCase()) || 'Not specified'}
            </p>
          </div>
        </div>

        <div className="mt-6 p-4 bg-gray-50 rounded-lg">
          <h4 className="font-medium text-gray-900 mb-2">Overall Risk Assessment</h4>
          <div className="flex items-center justify-between">
            <div>
              <span className={`px-3 py-1 rounded-full text-sm font-medium ${getRiskLevelColor(profileData?.riskFactors?.riskLevel)}`}>
                {profileData?.riskFactors?.riskLevel || 'Not Assessed'}
              </span>
              <span className="ml-3 text-sm text-gray-600">
                Score: {profileData?.riskFactors?.overallRiskScore?.toFixed(2) || 'N/A'}
              </span>
            </div>
          </div>
        </div>
      </div>
    </div>
  );

  const MedicationsTab = () => (
    <div className="space-y-6">
      <div className="bg-white p-6 rounded-lg border border-gray-200">
        <div className="flex justify-between items-center mb-6">
          <h3 className="text-lg font-semibold text-gray-900">Current Medications</h3>
          <button className="px-4 py-2 bg-blue-600 text-white rounded-lg hover:bg-blue-700 transition-colors flex items-center">
            <Plus className="w-4 h-4 mr-2" />
            Add Medication
          </button>
        </div>

        {profileData?.medications?.length > 0 ? (
          <div className="space-y-4">
            {profileData.medications.map((medication) => (
              <div key={medication.id} className="p-4 border border-gray-200 rounded-lg">
                <div className="flex justify-between items-start">
                  <div className="flex-1">
                    <h4 className="font-medium text-gray-900">{medication.name}</h4>
                    <div className="mt-2 grid grid-cols-2 md:grid-cols-4 gap-4 text-sm">
                      <div>
                        <span className="text-gray-600">Dosage:</span>
                        <p className="font-medium">{medication.dosage || 'Not specified'}</p>
                      </div>
                      <div>
                        <span className="text-gray-600">Frequency:</span>
                        <p className="font-medium">{medication.frequency || 'Not specified'}</p>
                      </div>
                      <div>
                        <span className="text-gray-600">Start Date:</span>
                        <p className="font-medium">{formatDate(medication.startDate)}</p>
                      </div>
                      <div>
                        <span className="text-gray-600">Status:</span>
                        <span className={`px-2 py-1 rounded text-xs font-medium ${
                          medication.isActive ? 'bg-green-100 text-green-800' : 'bg-gray-100 text-gray-800'
                        }`}>
                          {medication.status}
                        </span>
                      </div>
                    </div>
                  </div>
                  <button className="ml-4 p-2 text-gray-400 hover:text-gray-600">
                    <Edit className="w-4 h-4" />
                  </button>
                </div>
              </div>
            ))}
          </div>
        ) : (
          <div className="text-center py-8 text-gray-500">
            <Pill className="w-12 h-12 mx-auto mb-4 text-gray-300" />
            <p>No medications added yet</p>
            <button className="mt-2 text-blue-600 hover:text-blue-700">
              Add your first medication
            </button>
          </div>
        )}
      </div>
    </div>
  );

  const DiseasesTab = () => (
    <div className="space-y-6">
      <div className="bg-white p-6 rounded-lg border border-gray-200">
        <div className="flex justify-between items-center mb-6">
          <h3 className="text-lg font-semibold text-gray-900">Medical History</h3>
          <button className="px-4 py-2 bg-blue-600 text-white rounded-lg hover:bg-blue-700 transition-colors flex items-center">
            <Plus className="w-4 h-4 mr-2" />
            Add Condition
          </button>
        </div>

        {profileData?.diseases?.length > 0 ? (
          <div className="space-y-4">
            {profileData.diseases.map((disease) => (
              <div key={disease.id} className="p-4 border border-gray-200 rounded-lg">
                <div className="flex justify-between items-start">
                  <div className="flex-1">
                    <h4 className="font-medium text-gray-900">{disease.name}</h4>
                    <div className="mt-2 grid grid-cols-2 md:grid-cols-4 gap-4 text-sm">
                      <div>
                        <span className="text-gray-600">Diagnosis Date:</span>
                        <p className="font-medium">{formatDate(disease.diagnosisDate)}</p>
                      </div>
                      <div>
                        <span className="text-gray-600">Severity:</span>
                        <p className="font-medium capitalize">{disease.severity || 'Not specified'}</p>
                      </div>
                      <div>
                        <span className="text-gray-600">Status:</span>
                        <span className={`px-2 py-1 rounded text-xs font-medium ${
                          disease.status === 'active' ? 'bg-red-100 text-red-800' : 
                          disease.status === 'resolved' ? 'bg-green-100 text-green-800' :
                          'bg-yellow-100 text-yellow-800'
                        }`}>
                          {disease.status || 'Unknown'}
                        </span>
                      </div>
                      <div>
                        <span className="text-gray-600">Duration:</span>
                        <p className="font-medium">
                          {disease.durationInMonths ? `${disease.durationInMonths} months` : 'N/A'}
                        </p>
                      </div>
                    </div>
                  </div>
                  <button className="ml-4 p-2 text-gray-400 hover:text-gray-600">
                    <Edit className="w-4 h-4" />
                  </button>
                </div>
              </div>
            ))}
          </div>
        ) : (
          <div className="text-center py-8 text-gray-500">
            <Stethoscope className="w-12 h-12 mx-auto mb-4 text-gray-300" />
            <p>No medical conditions added yet</p>
            <button className="mt-2 text-blue-600 hover:text-blue-700">
              Add your medical history
            </button>
          </div>
        )}
      </div>
    </div>
  );

  if (loading) {
    return (
      <div className="min-h-screen bg-gray-50 flex items-center justify-center">
        <div className="text-center">
          <div className="animate-spin rounded-full h-12 w-12 border-b-2 border-blue-600 mx-auto mb-4"></div>
          <p className="text-gray-600">Loading your profile...</p>
        </div>
      </div>
    );
  }

  if (error) {
    return (
      <div className="min-h-screen bg-gray-50 flex items-center justify-center">
        <div className="text-center">
          <AlertTriangle className="w-12 h-12 text-red-500 mx-auto mb-4" />
          <p className="text-red-600 mb-4">{error}</p>
          <button 
            onClick={fetchUserProfile}
            className="px-4 py-2 bg-blue-600 text-white rounded-lg hover:bg-blue-700 transition-colors"
          >
            Try Again
          </button>
        </div>
      </div>
    );
  }

  const tabs = [
    { id: 'overview', label: 'Overview', icon: User },
    { id: 'basic', label: 'Basic Info', icon: User },
    { id: 'risk', label: 'Risk Factors', icon: Heart },
    { id: 'medications', label: 'Medications', icon: Pill },
    { id: 'diseases', label: 'Medical History', icon: Stethoscope }
  ];

  return (
    <div className="min-h-screen bg-gray-50">
      <div className="max-w-7xl mx-auto px-4 py-8">
        <div className="mb-8">
          <h1 className="text-3xl font-bold text-gray-900">My Health Profile</h1>
          <p className="text-gray-600 mt-2">Manage your health information and track your wellness journey</p>
        </div>

        {/* Tab Navigation */}
        <div className="bg-white rounded-lg shadow-sm border border-gray-200 mb-6">
          <div className="border-b border-gray-200">
            <nav className="flex space-x-8 px-6">
              {tabs.map((tab) => {
                const Icon = tab.icon;
                return (
                  <button
                    key={tab.id}
                    onClick={() => setActiveTab(tab.id)}
                    className={`py-4 px-1 border-b-2 font-medium text-sm flex items-center ${
                      activeTab === tab.id
                        ? 'border-blue-500 text-blue-600'
                        : 'border-transparent text-gray-500 hover:text-gray-700 hover:border-gray-300'
                    }`}
                  >
                    <Icon className="w-4 h-4 mr-2" />
                    {tab.label}
                  </button>
                );
              })}
            </nav>
          </div>
        </div>

        {/* Tab Content */}
        <div>
          {activeTab === 'overview' && <OverviewTab />}
          {activeTab === 'basic' && <BasicInfoTab />}
          {activeTab === 'risk' && <RiskFactorsTab />}
          {activeTab === 'medications' && <MedicationsTab />}
          {activeTab === 'diseases' && <DiseasesTab />}
        </div>
      </div>
    </div>
  );
};

export default UserProfile;