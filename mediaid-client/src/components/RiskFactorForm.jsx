import React, { useState, useEffect } from 'react';
import { Heart, Activity, TrendingUp, AlertCircle, CheckCircle, User } from 'lucide-react';
import { auth } from '../authUtils'; 
import { API_ENDPOINTS } from '../apiConfig';

const RiskFactorForm = () => {
  // Form state - each field is saved here
  const [formData, setFormData] = useState({
    smokingStatus: '',
    alcoholConsumption: '',
    physicalActivity: '',
    bloodPressure: '',
    stressLevel: '',
    ageGroup: '',
    familyHeartDisease: '',
    familyCancer: '',
    height: '',
    weight: '',
    bmi: null 
  });

  const [isSubmitting, setIsSubmitting] = useState(false);
  const [submitStatus, setSubmitStatus] = useState('');

  // Risk factor options based on ENUM descriptions
  const riskFactorOptions = {
    smokingStatus: [
      { value: 'NEVER', label: 'Never smoked' },
      { value: 'FORMER_LIGHT', label: 'Former smoker - light' },
      { value: 'FORMER_HEAVY', label: 'Former smoker - heavy' },
      { value: 'CURRENT_LIGHT', label: 'Current smoker - light' },
      { value: 'CURRENT_HEAVY', label: 'Current smoker - heavy' }
    ],
    alcoholConsumption: [
      { value: 'NEVER', label: 'Never drinks' },
      { value: 'LIGHT', label: 'Drinks lightly' },
      { value: 'MODERATE', label: 'Drinks moderately' },
      { value: 'HEAVY', label: 'Drinks heavily' },
      { value: 'EXCESSIVE', label: 'Drinks excessively' }
    ],
    physicalActivity: [
      { value: 'VERY_ACTIVE', label: 'Very active' },
      { value: 'ACTIVE', label: 'Active' },
      { value: 'MODERATE', label: 'Moderate' },
      { value: 'LOW', label: 'Low activity' },
      { value: 'SEDENTARY', label: 'Sedentary' }
    ],
    bloodPressure: [
      { value: 'NORMAL', label: 'Normal' },
      { value: 'ELEVATED', label: 'Elevated' },
      { value: 'STAGE_1', label: 'Stage 1 Hypertension' },
      { value: 'STAGE_2', label: 'Stage 2 Hypertension' },
      { value: 'CRISIS', label: 'Hypertensive crisis' }
    ],
    stressLevel: [
      { value: 'LOW', label: 'Low' },
      { value: 'MODERATE', label: 'Moderate' },
      { value: 'HIGH', label: 'High' },
      { value: 'VERY_HIGH', label: 'Very high' }
    ],
    ageGroup: [
      { value: 'UNDER_30', label: 'Under 30' },
      { value: 'AGE_30_40', label: '30-40' },
      { value: 'AGE_40_50', label: '40-50' },
      { value: 'AGE_50_60', label: '50-60' },
      { value: 'AGE_60_70', label: '60-70' },
      { value: 'OVER_70', label: 'Over 70' }
    ],
    familyHeartDisease: [
      { value: 'NONE', label: 'No known history' },
      { value: 'DISTANT', label: 'Distant relative' },
      { value: 'SIBLING', label: 'Sibling' },
      { value: 'PARENT', label: 'Parent' },
      { value: 'MULTIPLE', label: 'Multiple family members' }
    ],
    familyCancer: [
      { value: 'NONE', label: 'No known history' },
      { value: 'DISTANT', label: 'Distant relative' },
      { value: 'SIBLING', label: 'Sibling' },
      { value: 'PARENT', label: 'Parent' },
      { value: 'MULTIPLE', label: 'Multiple family members' }
    ]
  };

  // Automatic BMI calculation when height or weight changes
  useEffect(() => {
    const weight = parseFloat(formData.weight);
    const height = parseFloat(formData.height);
    
    if (weight > 0 && height > 0) {
      const heightInMeters = height / 100;
      const bmi = weight / (heightInMeters * heightInMeters);
      setFormData(prevState => ({
        ...prevState,
        bmi: Math.round(bmi * 100) / 100
      }));
    } else {
      setFormData(prevState => ({
        ...prevState,
        bmi: null
      }));
    }
  }, [formData.weight, formData.height]);

  // Function to determine BMI category
  const getBmiCategory = (bmi) => {
    if (!bmi) return '';
    if (bmi < 18.5) return 'Underweight';
    if (bmi < 25) return 'Normal';
    if (bmi < 30) return 'Overweight';
    if (bmi < 35) return 'Obese class 1';
    if (bmi < 40) return 'Obese class 2';
    return 'Obese class 3';
  };

  // Function to determine BMI color
  const getBmiColor = (bmi) => {
    if (!bmi) return 'text-gray-500';
    if (bmi < 18.5) return 'text-blue-500';
    if (bmi < 25) return 'text-green-500';
    if (bmi < 30) return 'text-yellow-500';
    return 'text-red-500';
  };

  // Handle field changes
  const handleChange = (e) => {
    const { name, value } = e.target;
    setFormData(prevState => ({
      ...prevState,
      [name]: value
    }));
  };

  // Submit function to server
  const handleSubmit = async (e) => {
    e.preventDefault();
    setIsSubmitting(true);
    setSubmitStatus('');

    try {
      // Check if user is authenticated
      if (!auth.isAuthenticated()) {
        throw new Error('Authentication required');
      }

      // Get access token
      const token = auth.getToken();
      if (!token) {
        throw new Error('Access token not found');
      }

      // Basic validation
      if (!formData.weight || !formData.height) {
        throw new Error('Please fill in height and weight fields');
      }

      // Prepare data for submission
      const dataToSend = {
        smokingStatus: formData.smokingStatus || null,
        alcoholConsumption: formData.alcoholConsumption || null,
        physicalActivity: formData.physicalActivity || null,
        bloodPressure: formData.bloodPressure || null,
        stressLevel: formData.stressLevel || null,
        ageGroup: formData.ageGroup || null,
        familyHeartDisease: formData.familyHeartDisease || null,
        familyCancer: formData.familyCancer || null,
        height: parseFloat(formData.height),
        weight: parseFloat(formData.weight),
        bmi: formData.bmi
      };

      console.log('Sending data:', dataToSend);

      const response = await fetch(API_ENDPOINTS.RISK_FACTORS, {
        method: 'POST',
        headers: {
          'Content-Type': 'application/json',
          'Authorization': `Bearer ${token}`
        },
        body: JSON.stringify(dataToSend)
      });

      if (!response.ok) {
        if (response.status === 401) {
          auth.logout();
          throw new Error('Session expired, please log in again');
        }
        const errorData = await response.json().catch(() => ({}));
        throw new Error(errorData.message || `Server error: ${response.status}`);
      }

      const result = await response.json();
      setSubmitStatus('Data saved successfully!');
      console.log('Server response:', result);

      // Display additional information from response
      if (result.overallRiskScore !== undefined) {
        setSubmitStatus(`Data saved successfully! Overall risk score: ${result.overallRiskScore.toFixed(2)} (${result.riskLevel})`);
      }

    } catch (error) {
      console.error('Error submitting data:', error);
      setSubmitStatus(`Error: ${error.message}`);
    } finally {
      setIsSubmitting(false);
    }
  };

  // Check if user is authenticated
  useEffect(() => {
    if (!auth.isAuthenticated()) {
      setSubmitStatus('Authentication required');
    }
  }, []);

  return (
    <div className="min-h-screen bg-gray-50 py-12">
      <div className="max-w-6xl mx-auto px-8">
        <div className="bg-white rounded-2xl shadow-xl p-12">
          {/* Header */}
          <div className="text-center mb-12">
            <Heart className="w-16 h-16 text-red-500 mx-auto mb-6" />
            <h2 className="text-4xl font-bold text-gray-900 mb-4">Medical Risk Factor Assessment</h2>
            <p className="text-xl text-gray-600">Help us understand your health profile for personalized recommendations</p>
          </div>

          <form onSubmit={handleSubmit} className="space-y-12">
            {/* Basic Information Section */}
            <div className="bg-gray-50 p-8 rounded-xl">
              <h3 className="text-2xl font-semibold text-gray-900 mb-8 flex items-center">
                <User className="w-6 h-6 mr-3" />
                Basic Information
              </h3>
              <div className="grid grid-cols-1 lg:grid-cols-3 gap-8">
                <div>
                  <label className="block text-lg font-medium text-gray-700 mb-3">Age Group</label>
                  <select 
                    name="ageGroup" 
                    value={formData.ageGroup} 
                    onChange={handleChange}
                    className="w-full px-4 py-3 border-2 border-gray-300 rounded-lg focus:ring-2 focus:ring-blue-500 focus:border-transparent text-lg"
                  >
                    <option value="">Select age group...</option>
                    {riskFactorOptions.ageGroup.map(opt => (
                      <option key={opt.value} value={opt.value}>{opt.label}</option>
                    ))}
                  </select>
                </div>
                
                <div>
                  <label className="block text-lg font-medium text-gray-700 mb-3">Height (cm)</label>
                  <div className="relative">
                    <input
                      type="number"
                      name="height"
                      value={formData.height}
                      onChange={handleChange}
                      placeholder="e.g. 170"
                      step="0.1"
                      min="0"
                      className="w-full px-4 py-3 border-2 border-gray-300 rounded-lg focus:ring-2 focus:ring-blue-500 focus:border-transparent text-lg"
                    />
                    <span className="absolute right-4 top-1/2 transform -translate-y-1/2 text-gray-500 font-medium">cm</span>
                  </div>
                </div>
                
                <div>
                  <label className="block text-lg font-medium text-gray-700 mb-3">Weight (kg)</label>
                  <div className="relative">
                    <input
                      type="number"
                      name="weight"
                      value={formData.weight}
                      onChange={handleChange}
                      placeholder="e.g. 70"
                      step="0.1"
                      min="0"
                      className="w-full px-4 py-3 border-2 border-gray-300 rounded-lg focus:ring-2 focus:ring-blue-500 focus:border-transparent text-lg"
                    />
                    <span className="absolute right-4 top-1/2 transform -translate-y-1/2 text-gray-500 font-medium">kg</span>
                  </div>
                </div>
              </div>
              
              {/* BMI Display */}
              {formData.bmi && (
                <div className="mt-8 p-6 bg-white rounded-xl border-2 border-blue-200">
                  <div className="text-center">
                    <h4 className="text-2xl font-semibold text-gray-900 mb-2 flex items-center justify-center">
                      <Activity className="w-6 h-6 mr-2" />
                      Your BMI
                    </h4>
                    <div className="flex items-center justify-center space-x-4">
                      <span className={`text-4xl font-bold ${getBmiColor(formData.bmi)}`}>
                        {formData.bmi}
                      </span>
                      <span className={`text-xl font-semibold ${getBmiColor(formData.bmi)}`}>
                        {getBmiCategory(formData.bmi)}
                      </span>
                    </div>
                  </div>
                </div>
              )}
            </div>

            {/* Lifestyle Section */}
            <div className="bg-gray-50 p-8 rounded-xl">
              <h3 className="text-2xl font-semibold text-gray-900 mb-8 flex items-center">
                <Activity className="w-6 h-6 mr-3" />
                Lifestyle Factors
              </h3>
              <div className="grid grid-cols-1 lg:grid-cols-3 gap-8">
                <div>
                  <label className="block text-lg font-medium text-gray-700 mb-3">Smoking Status</label>
                  <select 
                    name="smokingStatus" 
                    value={formData.smokingStatus} 
                    onChange={handleChange}
                    className="w-full px-4 py-3 border-2 border-gray-300 rounded-lg focus:ring-2 focus:ring-blue-500 focus:border-transparent text-lg"
                  >
                    <option value="">Select smoking status...</option>
                    {riskFactorOptions.smokingStatus.map(opt => (
                      <option key={opt.value} value={opt.value}>{opt.label}</option>
                    ))}
                  </select>
                </div>
                
                <div>
                  <label className="block text-lg font-medium text-gray-700 mb-3">Alcohol Consumption</label>
                  <select 
                    name="alcoholConsumption" 
                    value={formData.alcoholConsumption} 
                    onChange={handleChange}
                    className="w-full px-4 py-3 border-2 border-gray-300 rounded-lg focus:ring-2 focus:ring-blue-500 focus:border-transparent text-lg"
                  >
                    <option value="">Select alcohol consumption...</option>
                    {riskFactorOptions.alcoholConsumption.map(opt => (
                      <option key={opt.value} value={opt.value}>{opt.label}</option>
                    ))}
                  </select>
                </div>
                
                <div>
                  <label className="block text-lg font-medium text-gray-700 mb-3">Physical Activity</label>
                  <select 
                    name="physicalActivity" 
                    value={formData.physicalActivity} 
                    onChange={handleChange}
                    className="w-full px-4 py-3 border-2 border-gray-300 rounded-lg focus:ring-2 focus:ring-blue-500 focus:border-transparent text-lg"
                  >
                    <option value="">Select physical activity level...</option>
                    {riskFactorOptions.physicalActivity.map(opt => (
                      <option key={opt.value} value={opt.value}>{opt.label}</option>
                    ))}
                  </select>
                </div>
              </div>
            </div>

            {/* Medical Status Section */}
            <div className="bg-gray-50 p-8 rounded-xl">
              <h3 className="text-2xl font-semibold text-gray-900 mb-8 flex items-center">
                <Heart className="w-6 h-6 mr-3" />
                Medical Status
              </h3>
              <div className="grid grid-cols-1 lg:grid-cols-2 gap-8">
                <div>
                  <label className="block text-lg font-medium text-gray-700 mb-3">Blood Pressure</label>
                  <select 
                    name="bloodPressure" 
                    value={formData.bloodPressure} 
                    onChange={handleChange}
                    className="w-full px-4 py-3 border-2 border-gray-300 rounded-lg focus:ring-2 focus:ring-blue-500 focus:border-transparent text-lg"
                  >
                    <option value="">Select blood pressure status...</option>
                    {riskFactorOptions.bloodPressure.map(opt => (
                      <option key={opt.value} value={opt.value}>{opt.label}</option>
                    ))}
                  </select>
                </div>
                
                <div>
                  <label className="block text-lg font-medium text-gray-700 mb-3">Stress Level</label>
                  <select 
                    name="stressLevel" 
                    value={formData.stressLevel} 
                    onChange={handleChange}
                    className="w-full px-4 py-3 border-2 border-gray-300 rounded-lg focus:ring-2 focus:ring-blue-500 focus:border-transparent text-lg"
                  >
                    <option value="">Select stress level...</option>
                    {riskFactorOptions.stressLevel.map(opt => (
                      <option key={opt.value} value={opt.value}>{opt.label}</option>
                    ))}
                  </select>
                </div>
              </div>
            </div>

            {/* Family History Section */}
            <div className="bg-gray-50 p-8 rounded-xl">
              <h3 className="text-2xl font-semibold text-gray-900 mb-8 flex items-center">
                <TrendingUp className="w-6 h-6 mr-3" />
                Family History
              </h3>
              <div className="grid grid-cols-1 lg:grid-cols-2 gap-8">
                <div>
                  <label className="block text-lg font-medium text-gray-700 mb-3">Family History of Heart Disease</label>
                  <select 
                    name="familyHeartDisease" 
                    value={formData.familyHeartDisease} 
                    onChange={handleChange}
                    className="w-full px-4 py-3 border-2 border-gray-300 rounded-lg focus:ring-2 focus:ring-blue-500 focus:border-transparent text-lg"
                  >
                    <option value="">Select family heart disease history...</option>
                    {riskFactorOptions.familyHeartDisease.map(opt => (
                      <option key={opt.value} value={opt.value}>{opt.label}</option>
                    ))}
                  </select>
                </div>
                
                <div>
                  <label className="block text-lg font-medium text-gray-700 mb-3">Family History of Cancer</label>
                  <select 
                    name="familyCancer" 
                    value={formData.familyCancer} 
                    onChange={handleChange}
                    className="w-full px-4 py-3 border-2 border-gray-300 rounded-lg focus:ring-2 focus:ring-blue-500 focus:border-transparent text-lg"
                  >
                    <option value="">Select family cancer history...</option>
                    {riskFactorOptions.familyCancer.map(opt => (
                      <option key={opt.value} value={opt.value}>{opt.label}</option>
                    ))}
                  </select>
                </div>
              </div>
            </div>

            {/* Submit Section */}
            <div className="text-center pt-8">
              <button 
                type="submit" 
                disabled={isSubmitting || !auth.isAuthenticated()}
                className={`px-12 py-4 text-xl font-semibold rounded-xl transition-all ${
                  isSubmitting || !auth.isAuthenticated()
                    ? 'bg-gray-400 text-gray-200 cursor-not-allowed' 
                    : 'bg-blue-600 text-white hover:bg-blue-700 transform hover:scale-105'
                }`}
              >
                {isSubmitting ? (
                  <span className="flex items-center">
                    <div className="w-6 h-6 border-2 border-gray-300 border-t-white rounded-full animate-spin mr-3"></div>
                    Submitting...
                  </span>
                ) : (
                  <span className="flex items-center">
                    <CheckCircle className="w-6 h-6 mr-3" />
                    Save Risk Assessment
                  </span>
                )}
              </button>
              
              {submitStatus && (
                <div className={`mt-6 p-4 rounded-xl ${
                  submitStatus.includes('Error') || submitStatus.includes('required')
                    ? 'bg-red-50 border border-red-200 text-red-700' 
                    : 'bg-green-50 border border-green-200 text-green-700'
                }`}>
                  <div className="flex items-center justify-center">
                    {submitStatus.includes('Error') || submitStatus.includes('required') ? (
                      <AlertCircle className="w-5 h-5 mr-2" />
                    ) : (
                      <CheckCircle className="w-5 h-5 mr-2" />
                    )}
                    <span className="text-lg font-medium">{submitStatus}</span>
                  </div>
                </div>
              )}
            </div>
          </form>
        </div>
      </div>
    </div>
  );
};

export default RiskFactorForm;