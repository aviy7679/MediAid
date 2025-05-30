import React, { useState, useEffect } from 'react';
import { 
  User, Heart, Pill, Stethoscope, Upload, Search, AlertTriangle, 
  TrendingUp, Calendar, Activity, Bell, Settings, LogOut, Plus, FileText
} from 'lucide-react';
import { API_ENDPOINTS } from '../apiConfig';


const MainMenu = () => {
  const [userProfile, setUserProfile] = useState(null);
  const [healthAlerts, setHealthAlerts] = useState([]);
  const [loading, setLoading] = useState(true);

  // Navigation function (you would replace this with your actual navigation method)
  const navigate = (path) => {
    window.location.href = path;
  };

  useEffect(() => {
    fetchUserData();
  }, []);

  const fetchUserData = async () => {
    try {
      const token = localStorage.getItem('mediaid_token');
      if (!token) {
        navigate('/login');
        return;
      }

      const response = await fetch(API_ENDPOINTS.USER_PROFILE, {
        headers: {
          'Authorization': `Bearer ${token}`,
          'Content-Type': 'application/json'
        }
      });

      if (response.ok) {
        const data = await response.json();
        setUserProfile(data);
        generateHealthAlerts(data);
      } else if (response.status === 401) {
        localStorage.removeItem('mediaid_token');
        navigate('/login');
      }
    } catch (error) {
      console.error('Error fetching user data:', error);
    } finally {
      setLoading(false);
    }
  };

  const generateHealthAlerts = (profile) => {
    const alerts = [];
    
    // Check profile completeness
    if (profile.stats.profileCompleteness < 80) {
      alerts.push({
        type: 'info',
        title: 'Complete Your Profile',
        message: `Your profile is ${Math.round(profile.stats.profileCompleteness)}% complete. Add more information for better health insights.`,
        action: () => navigate('/update-profile')
      });
    }

    // Check BMI alerts
    if (profile.basicInfo.bmi) {
      const bmi = profile.basicInfo.bmi;
      if (bmi >= 30) {
        alerts.push({
          type: 'warning',
          title: 'BMI Alert',
          message: 'Your BMI indicates obesity. Consider consulting with a healthcare provider.',
          action: () => navigate('/profile')
        });
      } else if (bmi >= 25) {
        alerts.push({
          type: 'info',
          title: 'Weight Management',
          message: 'Your BMI indicates overweight. Consider a balanced diet and regular exercise.',
          action: () => navigate('/profile')
        });
      }
    }

    // Check high risk factors
    if (profile.riskFactors.riskLevel === 'High Risk' || profile.riskFactors.riskLevel === 'Very High Risk') {
      alerts.push({
        type: 'warning',
        title: 'High Health Risk',
        message: 'Your risk assessment shows elevated risk factors. Consider consulting with a healthcare provider.',
        action: () => navigate('/profile')
      });
    }

    // Check medication alerts
    if (profile.stats.activeMedications > 5) {
      alerts.push({
        type: 'info',
        title: 'Medication Review',
        message: `You're taking ${profile.stats.activeMedications} medications. Consider reviewing with your doctor.`,
        action: () => navigate('/profile')
      });
    }

    setHealthAlerts(alerts);
  };

  const handleLogout = () => {
    localStorage.removeItem('mediaid_token');
    localStorage.removeItem('mediaid_user');
    navigate('/login');
  };

  const quickActions = [
    {
      title: 'Emergency Diagnosis',
      description: 'Quick symptom assessment and emergency guidance',
      icon: AlertTriangle,
      color: 'bg-red-500 hover:bg-red-600',
      onClick: () => navigate('/emergency-diagnosis')
    },
    {
      title: 'Upload Medical Data',
      description: 'Add new medical documents, prescriptions, or test results',
      icon: Upload,
      color: 'bg-blue-500 hover:bg-blue-600',
      onClick: () => navigate('/uploadUserData')
    },
    {
      title: 'Search Medical Info',
      description: 'Look up diseases, medications, and symptoms',
      icon: Search,
      color: 'bg-green-500 hover:bg-green-600',
      onClick: () => navigate('/fillUserData')
    },
    {
      title: 'Treatment Guidelines',
      description: 'Access evidence-based treatment recommendations',
      icon: FileText,
      color: 'bg-purple-500 hover:bg-purple-600',
      onClick: () => navigate('/treatment-guidelines')
    },
    {
      title: 'Health Profile',
      description: 'View and manage your complete health information',
      icon: User,
      color: 'bg-indigo-500 hover:bg-indigo-600',
      onClick: () => navigate('/profile')
    }
  ];

  if (loading) {
    return (
      <div className="min-h-screen bg-gray-50 flex items-center justify-center">
        <div className="text-center">
          <div className="animate-spin rounded-full h-12 w-12 border-b-2 border-blue-600 mx-auto mb-4"></div>
          <p className="text-gray-600">Loading your dashboard...</p>
        </div>
      </div>
    );
  }

  return (
    <div className="min-h-screen bg-gray-50">
      {/* Header */}
      <header className="bg-white shadow-sm border-b border-gray-200">
        <div className="max-w-7xl mx-auto px-4 py-4">
          <div className="flex justify-between items-center">
            <div className="flex items-center">
              <div className="flex items-center">
                <Heart className="w-8 h-8 text-blue-600 mr-3" />
                <h1 className="text-2xl font-bold text-gray-900">MediAid</h1>
              </div>
            </div>
            <div className="flex items-center space-x-4">
              <button className="p-2 text-gray-400 hover:text-gray-600">
                <Bell className="w-5 h-5" />
              </button>
              <button 
                onClick={() => navigate('/profile')}
                className="flex items-center space-x-2 text-gray-700 hover:text-gray-900"
              >
                <User className="w-5 h-5" />
                <span className="hidden md:block">{userProfile?.basicInfo?.username || 'User'}</span>
              </button>
              <button 
                onClick={handleLogout}
                className="p-2 text-gray-400 hover:text-red-600"
                title="Logout"
              >
                <LogOut className="w-5 h-5" />
              </button>
            </div>
          </div>
        </div>
      </header>

      <div className="max-w-7xl mx-auto px-4 py-8">
        {/* Welcome Section */}
        <div className="mb-8">
          <h2 className="text-3xl font-bold text-gray-900 mb-2">
            Welcome back, {userProfile?.basicInfo?.username || 'User'}!
          </h2>
          <p className="text-gray-600">How can we assist you with your health today?</p>
        </div>

        {/* Health Alerts */}
        {healthAlerts.length > 0 && (
          <div className="mb-8">
            <h3 className="text-lg font-semibold text-gray-900 mb-4 flex items-center">
              <Bell className="w-5 h-5 mr-2" />
              Health Alerts
            </h3>
            <div className="space-y-3">
              {healthAlerts.map((alert, index) => (
                <div 
                  key={index}
                  className={`p-4 rounded-lg border-l-4 cursor-pointer transition-colors ${
                    alert.type === 'warning' 
                      ? 'bg-yellow-50 border-yellow-400 hover:bg-yellow-100' 
                      : 'bg-blue-50 border-blue-400 hover:bg-blue-100'
                  }`}
                  onClick={alert.action}
                >
                  <div className="flex items-start">
                    <AlertTriangle className={`w-5 h-5 mt-0.5 mr-3 ${
                      alert.type === 'warning' ? 'text-yellow-600' : 'text-blue-600'
                    }`} />
                    <div>
                      <h4 className="font-medium text-gray-900">{alert.title}</h4>
                      <p className="text-sm text-gray-600 mt-1">{alert.message}</p>
                    </div>
                  </div>
                </div>
              ))}
            </div>
          </div>
        )}

        {/* Health Summary Cards */}
        {userProfile && (
          <div className="mb-8">
            <h3 className="text-lg font-semibold text-gray-900 mb-4">Health Summary</h3>
            <div className="grid grid-cols-1 md:grid-cols-2 lg:grid-cols-4 gap-6">
              <div className="bg-white p-6 rounded-lg shadow-sm border border-gray-200">
                <div className="flex items-center">
                  <div className="p-3 rounded-full bg-blue-100">
                    <Activity className="w-6 h-6 text-blue-600" />
                  </div>
                  <div className="ml-4">
                    <p className="text-sm font-medium text-gray-600">BMI Status</p>
                    <p className="text-lg font-bold text-gray-900">
                      {userProfile.basicInfo?.bmi?.toFixed(1) || 'N/A'}
                    </p>
                    <p className="text-sm text-gray-500">{userProfile.basicInfo?.bmiCategory}</p>
                  </div>
                </div>
              </div>

              <div className="bg-white p-6 rounded-lg shadow-sm border border-gray-200">
                <div className="flex items-center">
                  <div className="p-3 rounded-full bg-green-100">
                    <TrendingUp className="w-6 h-6 text-green-600" />
                  </div>
                  <div className="ml-4">
                    <p className="text-sm font-medium text-gray-600">Risk Level</p>
                    <p className={`text-lg font-bold ${
                      userProfile.riskFactors?.riskLevel === 'Low Risk' ? 'text-green-600' :
                      userProfile.riskFactors?.riskLevel === 'Moderate Risk' ? 'text-yellow-600' :
                      'text-red-600'
                    }`}>
                      {userProfile.riskFactors?.riskLevel?.replace(' Risk', '') || 'N/A'}
                    </p>
                  </div>
                </div>
              </div>

              <div className="bg-white p-6 rounded-lg shadow-sm border border-gray-200">
                <div className="flex items-center">
                  <div className="p-3 rounded-full bg-purple-100">
                    <Pill className="w-6 h-6 text-purple-600" />
                  </div>
                  <div className="ml-4">
                    <p className="text-sm font-medium text-gray-600">Active Medications</p>
                    <p className="text-lg font-bold text-gray-900">
                      {userProfile.stats?.activeMedications || 0}
                    </p>
                  </div>
                </div>
              </div>

              <div className="bg-white p-6 rounded-lg shadow-sm border border-gray-200">
                <div className="flex items-center">
                  <div className="p-3 rounded-full bg-red-100">
                    <Stethoscope className="w-6 h-6 text-red-600" />
                  </div>
                  <div className="ml-4">
                    <p className="text-sm font-medium text-gray-600">Active Conditions</p>
                    <p className="text-lg font-bold text-gray-900">
                      {userProfile.stats?.activeDiseases || 0}
                    </p>
                  </div>
                </div>
              </div>
            </div>
          </div>
        )}

        {/* Quick Actions */}
        <div className="mb-8">
          <h3 className="text-lg font-semibold text-gray-900 mb-4">Quick Actions</h3>
          <div className="grid grid-cols-1 md:grid-cols-2 lg:grid-cols-3 xl:grid-cols-5 gap-6">
            {quickActions.map((action, index) => {
              const Icon = action.icon;
              return (
                <button
                  key={index}
                  onClick={action.onClick}
                  className={`${action.color} text-white p-6 rounded-lg shadow-sm hover:shadow-md transition-all duration-200 transform hover:scale-105`}
                >
                  <div className="flex flex-col items-center text-center">
                    <Icon className="w-8 h-8 mb-3" />
                    <h4 className="font-semibold text-lg mb-2">{action.title}</h4>
                    <p className="text-sm opacity-90">{action.description}</p>
                  </div>
                </button>
              );
            })}
          </div>
        </div>

        {/* Recent Activity */}
        <div className="grid grid-cols-1 lg:grid-cols-2 gap-8">
          {/* Recent Medications */}
          <div className="bg-white p-6 rounded-lg shadow-sm border border-gray-200">
            <div className="flex justify-between items-center mb-4">
              <h3 className="text-lg font-semibold text-gray-900 flex items-center">
                <Pill className="w-5 h-5 mr-2" />
                Recent Medications
              </h3>
              <button 
                onClick={() => navigate('/profile')}
                className="text-sm text-blue-600 hover:text-blue-700"
              >
                View All
              </button>
            </div>
            {userProfile?.medications?.slice(0, 3).map((medication) => (
              <div key={medication.id} className="flex items-center p-3 border-b border-gray-100 last:border-b-0">
                <div className="flex-1">
                  <p className="font-medium text-gray-900">{medication.name}</p>
                  <p className="text-sm text-gray-600">{medication.dosage} - {medication.frequency}</p>
                </div>
                <span className={`px-2 py-1 rounded text-xs font-medium ${
                  medication.isActive ? 'bg-green-100 text-green-800' : 'bg-gray-100 text-gray-800'
                }`}>
                  {medication.status}
                </span>
              </div>
            )) || (
              <div className="text-center py-4 text-gray-500">
                <Pill className="w-8 h-8 mx-auto mb-2 text-gray-300" />
                <p>No medications added yet</p>
                <button 
                  onClick={() => navigate('/profile')}
                  className="text-blue-600 hover:text-blue-700 text-sm mt-1"
                >
                  Add medications
                </button>
              </div>
            )}
          </div>

          {/* Recent Medical History */}
          <div className="bg-white p-6 rounded-lg shadow-sm border border-gray-200">
            <div className="flex justify-between items-center mb-4">
              <h3 className="text-lg font-semibold text-gray-900 flex items-center">
                <Stethoscope className="w-5 h-5 mr-2" />
                Medical History
              </h3>
              <button 
                onClick={() => navigate('/profile')}
                className="text-sm text-blue-600 hover:text-blue-700"
              >
                View All
              </button>
            </div>
            {userProfile?.diseases?.slice(0, 3).map((disease) => (
              <div key={disease.id} className="flex items-center p-3 border-b border-gray-100 last:border-b-0">
                <div className="flex-1">
                  <p className="font-medium text-gray-900">{disease.name}</p>
                  <p className="text-sm text-gray-600">
                    Diagnosed: {new Date(disease.diagnosisDate).toLocaleDateString()}
                  </p>
                </div>
                <span className={`px-2 py-1 rounded text-xs font-medium ${
                  disease.status === 'active' ? 'bg-red-100 text-red-800' : 
                  disease.status === 'resolved' ? 'bg-green-100 text-green-800' :
                  'bg-yellow-100 text-yellow-800'
                }`}>
                  {disease.status}
                </span>
              </div>
            )) || (
              <div className="text-center py-4 text-gray-500">
                <Stethoscope className="w-8 h-8 mx-auto mb-2 text-gray-300" />
                <p>No medical history added yet</p>
                <button 
                  onClick={() => navigate('/profile')}
                  className="text-blue-600 hover:text-blue-700 text-sm mt-1"
                >
                  Add medical history
                </button>
              </div>
            )}
          </div>
        </div>
      </div>
    </div>
  );
};

export default MainMenu;