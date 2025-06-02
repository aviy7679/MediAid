import React, { useState, useEffect } from 'react';
import { ChevronRight, ChevronLeft, Check, User, Heart, Pill, Stethoscope, Activity } from 'lucide-react';
import { API_ENDPOINTS, buildSearchUrl } from '../apiConfig';

// Step Components
const BasicInfoStep = ({ data, onUpdate, onNext }) => {
  const [formData, setFormData] = useState(data);
  const [errors, setErrors] = useState({});

  const handleChange = (e) => {
    const { name, value } = e.target;
    setFormData(prev => ({ ...prev, [name]: value }));
    if (errors[name]) {
      setErrors(prev => ({ ...prev, [name]: '' }));
    }
  };

  const validateAndNext = () => {
    const newErrors = {};
    if (!formData.username) newErrors.username = 'Username is required';
    if (!formData.email) newErrors.email = 'Email is required';
    if (!formData.password) newErrors.password = 'Password is required';
    if (formData.password && formData.password.length < 8) {
      newErrors.password = 'Password must be at least 8 characters';
    }
    if (!formData.dateOfBirth) newErrors.dateOfBirth = 'Date of birth is required';
    if (!formData.gender) newErrors.gender = 'Gender is required';

    if (Object.keys(newErrors).length > 0) {
      setErrors(newErrors);
      return;
    }

    onUpdate(formData);
    onNext();
  };

  return (
    <div className="max-w-4xl mx-auto">
      <div className="text-center mb-12">
        <User className="w-20 h-20 text-blue-500 mx-auto mb-6" />
        <h2 className="text-4xl font-bold text-gray-800 mb-4">Basic Information</h2>
        <p className="text-xl text-gray-600">Let's start with your basic details</p>
      </div>

      <div className="grid grid-cols-1 lg:grid-cols-2 gap-8">
        <div>
          <label className="block text-lg font-medium text-gray-700 mb-3">Username</label>
          <input
            type="text"
            name="username"
            value={formData.username || ''}
            onChange={handleChange}
            className={`w-full px-6 py-4 border-2 rounded-xl focus:ring-2 focus:ring-blue-500 focus:border-transparent text-lg ${errors.username ? 'border-red-500' : 'border-gray-300'}`}
            placeholder="Enter your username"
          />
          {errors.username && <p className="text-red-500 text-base mt-2">{errors.username}</p>}
        </div>

        <div>
          <label className="block text-lg font-medium text-gray-700 mb-3">Email</label>
          <input
            type="email"
            name="email"
            value={formData.email || ''}
            onChange={handleChange}
            className={`w-full px-6 py-4 border-2 rounded-xl focus:ring-2 focus:ring-blue-500 focus:border-transparent text-lg ${errors.email ? 'border-red-500' : 'border-gray-300'}`}
            placeholder="Enter your email"
          />
          {errors.email && <p className="text-red-500 text-base mt-2">{errors.email}</p>}
        </div>

        <div>
          <label className="block text-lg font-medium text-gray-700 mb-3">Password</label>
          <input
            type="password"
            name="password"
            value={formData.password || ''}
            onChange={handleChange}
            className={`w-full px-6 py-4 border-2 rounded-xl focus:ring-2 focus:ring-blue-500 focus:border-transparent text-lg ${errors.password ? 'border-red-500' : 'border-gray-300'}`}
            placeholder="Create a password (min 8 characters)"
          />
          {errors.password && <p className="text-red-500 text-base mt-2">{errors.password}</p>}
        </div>

        <div>
          <label className="block text-lg font-medium text-gray-700 mb-3">Date of Birth</label>
          <input
            type="date"
            name="dateOfBirth"
            value={formData.dateOfBirth || ''}
            onChange={handleChange}
            className={`w-full px-6 py-4 border-2 rounded-xl focus:ring-2 focus:ring-blue-500 focus:border-transparent text-lg ${errors.dateOfBirth ? 'border-red-500' : 'border-gray-300'}`}
          />
          {errors.dateOfBirth && <p className="text-red-500 text-base mt-2">{errors.dateOfBirth}</p>}
        </div>

        <div>
          <label className="block text-lg font-medium text-gray-700 mb-3">Gender</label>
          <select
            name="gender"
            value={formData.gender || ''}
            onChange={handleChange}
            className={`w-full px-6 py-4 border-2 rounded-xl focus:ring-2 focus:ring-blue-500 focus:border-transparent text-lg ${errors.gender ? 'border-red-500' : 'border-gray-300'}`}
          >
            <option value="">Select Gender</option>
            <option value="male">Male</option>
            <option value="female">Female</option>
            <option value="other">Other</option>
          </select>
          {errors.gender && <p className="text-red-500 text-base mt-2">{errors.gender}</p>}
        </div>

        <div>
          <label className="block text-lg font-medium text-gray-700 mb-3">Height (cm)</label>
          <input
            type="number"
            name="height"
            value={formData.height || ''}
            onChange={handleChange}
            className="w-full px-6 py-4 border-2 border-gray-300 rounded-xl focus:ring-2 focus:ring-blue-500 focus:border-transparent text-lg"
            placeholder="Enter your height"
          />
        </div>

        <div>
          <label className="block text-lg font-medium text-gray-700 mb-3">Weight (kg)</label>
          <input
            type="number"
            name="weight"
            value={formData.weight || ''}
            onChange={handleChange}
            className="w-full px-6 py-4 border-2 border-gray-300 rounded-xl focus:ring-2 focus:ring-blue-500 focus:border-transparent text-lg"
            placeholder="Enter your weight"
          />
        </div>
      </div>

      <div className="flex justify-end pt-12">
        <button
          onClick={validateAndNext}
          className="px-12 py-4 bg-blue-600 text-white font-semibold text-lg rounded-xl hover:bg-blue-700 transition-colors flex items-center gap-3"
        >
          Next Step
          <ChevronRight className="w-6 h-6" />
        </button>
      </div>
    </div>
  );
};

const RiskFactorsStep = ({ data, onUpdate, onNext, onPrev }) => {
  const [formData, setFormData] = useState(data);
  const [bmi, setBmi] = useState(null);

  useEffect(() => {
    const weight = parseFloat(formData.weight);
    const height = parseFloat(formData.height);
    
    if (weight > 0 && height > 0) {
      const heightInMeters = height / 100;
      const calculatedBmi = weight / (heightInMeters * heightInMeters);
      setBmi(Math.round(calculatedBmi * 100) / 100);
    } else {
      setBmi(null);
    }
  }, [formData.weight, formData.height]);

  const getBmiCategory = (bmi) => {
    if (!bmi) return '';
    if (bmi < 18.5) return 'Underweight';
    if (bmi < 25) return 'Normal';
    if (bmi < 30) return 'Overweight';
    if (bmi < 35) return 'Obese class 1';
    if (bmi < 40) return 'Obese class 2';
    return 'Obese class 3';
  };

  const getBmiColor = (bmi) => {
    if (!bmi) return 'text-gray-500';
    if (bmi < 18.5) return 'text-blue-500';
    if (bmi < 25) return 'text-green-500';
    if (bmi < 30) return 'text-yellow-500';
    return 'text-red-500';
  };

  const handleChange = (e) => {
    const { name, value } = e.target;
    setFormData(prev => ({ ...prev, [name]: value }));
  };

  const handleNext = () => {
    onUpdate({ ...formData, bmi });
    onNext();
  };

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

  return (
    <div className="max-w-5xl mx-auto">
      <div className="text-center mb-12">
        <Heart className="w-20 h-20 text-red-500 mx-auto mb-6" />
        <h2 className="text-4xl font-bold text-gray-800 mb-4">Health Risk Factors</h2>
        <p className="text-xl text-gray-600">Help us understand your health profile</p>
      </div>

      {/* BMI Display */}
      {bmi && (
        <div className="bg-gray-50 p-8 rounded-xl text-center mb-12">
          <h3 className="text-2xl font-semibold mb-4 flex items-center justify-center">
            <Activity className="w-8 h-8 mr-3" />
            Your BMI
          </h3>
          <div className="flex items-center justify-center gap-6">
            <span className={`text-5xl font-bold ${getBmiColor(bmi)}`}>{bmi}</span>
            <span className={`text-2xl font-semibold ${getBmiColor(bmi)}`}>{getBmiCategory(bmi)}</span>
          </div>
        </div>
      )}

      <div className="grid grid-cols-1 lg:grid-cols-2 gap-8">
        {Object.entries(riskFactorOptions).map(([key, options]) => (
          <div key={key}>
            <label className="block text-lg font-medium text-gray-700 mb-3 capitalize">
              {key.replace(/([A-Z])/g, ' $1').replace(/^./, str => str.toUpperCase())}
            </label>
            <select
              name={key}
              value={formData[key] || ''}
              onChange={handleChange}
              className="w-full px-6 py-4 border-2 border-gray-300 rounded-xl focus:ring-2 focus:ring-blue-500 focus:border-transparent text-lg"
            >
              <option value="">Select...</option>
              {options.map(opt => (
                <option key={opt.value} value={opt.value}>{opt.label}</option>
              ))}
            </select>
          </div>
        ))}
      </div>

      <div className="flex justify-between pt-12">
        <button
          onClick={onPrev}
          className="px-10 py-4 border-2 border-gray-300 text-gray-700 font-semibold text-lg rounded-xl hover:bg-gray-50 transition-colors flex items-center gap-3"
        >
          <ChevronLeft className="w-6 h-6" />
          Previous
        </button>
        <button
          onClick={handleNext}
          className="px-12 py-4 bg-blue-600 text-white font-semibold text-lg rounded-xl hover:bg-blue-700 transition-colors flex items-center gap-3"
        >
          Next Step
          <ChevronRight className="w-6 h-6" />
        </button>
      </div>
    </div>
  );
};

const MedicationsStep = ({ data, onUpdate, onNext, onPrev }) => {
  const [currentMedications, setCurrentMedications] = useState(data.medications || []);
  const [searchQuery, setSearchQuery] = useState('');
  const [searchResults, setSearchResults] = useState([]);
  const [loading, setLoading] = useState(false);

  const searchMedications = async () => {
    if (!searchQuery || searchQuery.length < 2) {
      setSearchResults([]);
      return;
    }

    setLoading(true);
    try {
      const url = buildSearchUrl(API_ENDPOINTS.SEARCH_MEDICATIONS, searchQuery, 10);
      const response = await fetch(url);
      if (response.ok) {
        const data = await response.json();
        setSearchResults(data);
      } else {
        console.error('Failed to search medications:', response.status);
        setSearchResults([]);
      }
    } catch (error) {
      console.error('Error searching medications:', error);
      setSearchResults([]);
    } finally {
      setLoading(false);
    }
  };

  useEffect(() => {
    const timeoutId = setTimeout(searchMedications, 300);
    return () => clearTimeout(timeoutId);
  }, [searchQuery]);

  const addMedication = (medication) => {
    const newMedication = {
      id: Date.now(),
      cui: medication.cui,
      name: medication.name,
      dosage: '',
      frequency: '',
      startDate: '',
      isActive: true
    };
    setCurrentMedications(prev => [...prev, newMedication]);
    setSearchQuery('');
    setSearchResults([]);
  };

  const updateMedication = (id, field, value) => {
    setCurrentMedications(prev =>
      prev.map(med => med.id === id ? { ...med, [field]: value } : med)
    );
  };

  const removeMedication = (id) => {
    setCurrentMedications(prev => prev.filter(med => med.id !== id));
  };

  const handleNext = () => {
    onUpdate({ ...data, medications: currentMedications });
    onNext();
  };

  return (
    <div className="max-w-5xl mx-auto">
      <div className="text-center mb-12">
        <Pill className="w-20 h-20 text-green-500 mx-auto mb-6" />
        <h2 className="text-4xl font-bold text-gray-800 mb-4">Current Medications</h2>
        <p className="text-xl text-gray-600">Add medications you're currently taking</p>
      </div>

      {/* Search for medications */}
      <div className="bg-white p-8 border-2 border-gray-200 rounded-xl mb-8">
        <h3 className="text-2xl font-semibold mb-6">Search and Add Medication</h3>
        <div className="relative">
          <input
            type="text"
            value={searchQuery}
            onChange={(e) => setSearchQuery(e.target.value)}
            placeholder="Type to search for medications..."
            className="w-full px-6 py-4 border-2 border-gray-300 rounded-xl focus:ring-2 focus:ring-blue-500 focus:border-transparent text-lg"
          />
          {loading && (
            <div className="absolute right-4 top-4">
              <div className="w-6 h-6 border-2 border-gray-300 border-t-blue-500 rounded-full animate-spin"></div>
            </div>
          )}
        </div>

        {searchResults.length > 0 && (
          <div className="mt-4 border-2 border-gray-200 rounded-xl max-h-80 overflow-y-auto">
            {searchResults.map((medication) => (
              <div
                key={medication.cui}
                className="p-4 hover:bg-gray-50 cursor-pointer border-b border-gray-100 last:border-b-0"
                onClick={() => addMedication(medication)}
              >
                <div className="font-medium text-lg">{medication.name}</div>
                <div className="text-gray-500">CUI: {medication.cui}</div>
              </div>
            ))}
          </div>
        )}
      </div>

      {/* Current medications list */}
      <div className="space-y-6">
        <h3 className="text-2xl font-semibold">Your Current Medications ({currentMedications.length})</h3>
        {currentMedications.length === 0 ? (
          <div className="text-center py-16 text-gray-500">
            <Pill className="w-16 h-16 mx-auto mb-4 text-gray-300" />
            <p className="text-xl">No medications added yet. Search and add medications above.</p>
          </div>
        ) : (
          <div className="space-y-6">
            {currentMedications.map((medication) => (
              <div key={medication.id} className="bg-gray-50 p-6 rounded-xl">
                <div className="flex justify-between items-start mb-4">
                  <h4 className="font-medium text-xl">{medication.name}</h4>
                  <button
                    onClick={() => removeMedication(medication.id)}
                    className="text-red-500 hover:text-red-700 font-medium"
                  >
                    Remove
                  </button>
                </div>
                <div className="grid grid-cols-1 md:grid-cols-3 gap-6">
                  <div>
                    <label className="block text-gray-600 mb-2">Dosage</label>
                    <input
                      type="text"
                      value={medication.dosage}
                      onChange={(e) => updateMedication(medication.id, 'dosage', e.target.value)}
                      placeholder="e.g., 10mg"
                      className="w-full px-4 py-3 border-2 border-gray-300 rounded-lg focus:ring-2 focus:ring-blue-500 focus:border-transparent"
                    />
                  </div>
                  <div>
                    <label className="block text-gray-600 mb-2">Frequency</label>
                    <input
                      type="text"
                      value={medication.frequency}
                      onChange={(e) => updateMedication(medication.id, 'frequency', e.target.value)}
                      placeholder="e.g., Once daily"
                      className="w-full px-4 py-3 border-2 border-gray-300 rounded-lg focus:ring-2 focus:ring-blue-500 focus:border-transparent"
                    />
                  </div>
                  <div>
                    <label className="block text-gray-600 mb-2">Start Date</label>
                    <input
                      type="date"
                      value={medication.startDate}
                      onChange={(e) => updateMedication(medication.id, 'startDate', e.target.value)}
                      className="w-full px-4 py-3 border-2 border-gray-300 rounded-lg focus:ring-2 focus:ring-blue-500 focus:border-transparent"
                    />
                  </div>
                </div>
              </div>
            ))}
          </div>
        )}
      </div>

      <div className="flex justify-between pt-12">
        <button
          onClick={onPrev}
          className="px-10 py-4 border-2 border-gray-300 text-gray-700 font-semibold text-lg rounded-xl hover:bg-gray-50 transition-colors flex items-center gap-3"
        >
          <ChevronLeft className="w-6 h-6" />
          Previous
        </button>
        <button
          onClick={handleNext}
          className="px-12 py-4 bg-blue-600 text-white font-semibold text-lg rounded-xl hover:bg-blue-700 transition-colors flex items-center gap-3"
        >
          Next Step
          <ChevronRight className="w-6 h-6" />
        </button>
      </div>
    </div>
  );
};

const DiseasesStep = ({ data, onUpdate, onNext, onPrev }) => {
  const [currentDiseases, setCurrentDiseases] = useState(data.diseases || []);
  const [searchQuery, setSearchQuery] = useState('');
  const [searchResults, setSearchResults] = useState([]);
  const [loading, setLoading] = useState(false);
  const [isSubmitting, setIsSubmitting] = useState(false);

  const searchDiseases = async () => {
    if (!searchQuery || searchQuery.length < 2) {
      setSearchResults([]);
      return;
    }

    setLoading(true);
    try {
      const url = buildSearchUrl(API_ENDPOINTS.SEARCH_DISEASES, searchQuery, 10);
      const response = await fetch(url);
      if (response.ok) {
        const data = await response.json();
        setSearchResults(data);
      } else {
        console.error('Failed to search diseases:', response.status);
        setSearchResults([]);
      }
    } catch (error) {
      console.error('Error searching diseases:', error);
      setSearchResults([]);
    } finally {
      setLoading(false);
    }
  };

  useEffect(() => {
    const timeoutId = setTimeout(searchDiseases, 300);
    return () => clearTimeout(timeoutId);
  }, [searchQuery]);

  const addDisease = (disease) => {
    const newDisease = {
      id: Date.now(),
      cui: disease.cui,
      name: disease.name,
      diagnosisDate: '',
      severity: '',
      status: 'active'
    };
    setCurrentDiseases(prev => [...prev, newDisease]);
    setSearchQuery('');
    setSearchResults([]);
  };

  const updateDisease = (id, field, value) => {
    setCurrentDiseases(prev =>
      prev.map(disease => disease.id === id ? { ...disease, [field]: value } : disease)
    );
  };

  const removeDisease = (id) => {
    setCurrentDiseases(prev => prev.filter(disease => disease.id !== id));
  };

  const handleNext = () => {
    setIsSubmitting(true);
    onUpdate({ ...data, diseases: currentDiseases });
    onNext();
  };

  return (
    <div className="max-w-5xl mx-auto">
      <div className="text-center mb-12">
        <Stethoscope className="w-20 h-20 text-purple-500 mx-auto mb-6" />
        <h2 className="text-4xl font-bold text-gray-800 mb-4">Medical History</h2>
        <p className="text-xl text-gray-600">Add any current or past medical conditions</p>
      </div>

      {/* Search for diseases */}
      <div className="bg-white p-8 border-2 border-gray-200 rounded-xl mb-8">
        <h3 className="text-2xl font-semibold mb-6">Search and Add Medical Condition</h3>
        <div className="relative">
          <input
            type="text"
            value={searchQuery}
            onChange={(e) => setSearchQuery(e.target.value)}
            placeholder="Type to search for medical conditions..."
            className="w-full px-6 py-4 border-2 border-gray-300 rounded-xl focus:ring-2 focus:ring-blue-500 focus:border-transparent text-lg"
          />
          {loading && (
            <div className="absolute right-4 top-4">
              <div className="w-6 h-6 border-2 border-gray-300 border-t-blue-500 rounded-full animate-spin"></div>
            </div>
          )}
        </div>

        {searchResults.length > 0 && (
          <div className="mt-4 border-2 border-gray-200 rounded-xl max-h-80 overflow-y-auto">
            {searchResults.map((disease) => (
              <div
                key={disease.cui}
                className="p-4 hover:bg-gray-50 cursor-pointer border-b border-gray-100 last:border-b-0"
                onClick={() => addDisease(disease)}
              >
                <div className="font-medium text-lg">{disease.name}</div>
                <div className="text-gray-500">CUI: {disease.cui}</div>
              </div>
            ))}
          </div>
        )}
      </div>

      {/* Current diseases list */}
      <div className="space-y-6">
        <h3 className="text-2xl font-semibold">Your Medical Conditions ({currentDiseases.length})</h3>
        {currentDiseases.length === 0 ? (
          <div className="text-center py-16 text-gray-500">
            <Stethoscope className="w-16 h-16 mx-auto mb-4 text-gray-300" />
            <p className="text-xl">No medical conditions added yet. Search and add conditions above.</p>
          </div>
        ) : (
          <div className="space-y-6">
            {currentDiseases.map((disease) => (
              <div key={disease.id} className="bg-gray-50 p-6 rounded-xl">
                <div className="flex justify-between items-start mb-4">
                  <h4 className="font-medium text-xl">{disease.name}</h4>
                  <button
                    onClick={() => removeDisease(disease.id)}
                    className="text-red-500 hover:text-red-700 font-medium"
                  >
                    Remove
                  </button>
                </div>
                <div className="grid grid-cols-1 md:grid-cols-3 gap-6">
                  <div>
                    <label className="block text-gray-600 mb-2">Diagnosis Date</label>
                    <input
                      type="date"
                      value={disease.diagnosisDate}
                      onChange={(e) => updateDisease(disease.id, 'diagnosisDate', e.target.value)}
                      className="w-full px-4 py-3 border-2 border-gray-300 rounded-lg focus:ring-2 focus:ring-blue-500 focus:border-transparent"
                    />
                  </div>
                  <div>
                    <label className="block text-gray-600 mb-2">Severity</label>
                    <select
                      value={disease.severity}
                      onChange={(e) => updateDisease(disease.id, 'severity', e.target.value)}
                      className="w-full px-4 py-3 border-2 border-gray-300 rounded-lg focus:ring-2 focus:ring-blue-500 focus:border-transparent"
                    >
                      <option value="">Select severity</option>
                      <option value="mild">Mild</option>
                      <option value="moderate">Moderate</option>
                      <option value="severe">Severe</option>
                    </select>
                  </div>
                  <div>
                    <label className="block text-gray-600 mb-2">Status</label>
                    <select
                      value={disease.status}
                      onChange={(e) => updateDisease(disease.id, 'status', e.target.value)}
                      className="w-full px-4 py-3 border-2 border-gray-300 rounded-lg focus:ring-2 focus:ring-blue-500 focus:border-transparent"
                    >
                      <option value="active">Active</option>
                      <option value="resolved">Resolved</option>
                      <option value="chronic">Chronic</option>
                    </select>
                  </div>
                </div>
              </div>
            ))}
          </div>
        )}
      </div>

      <div className="flex justify-between pt-12">
        <button
          onClick={onPrev}
          className="px-10 py-4 border-2 border-gray-300 text-gray-700 font-semibold text-lg rounded-xl hover:bg-gray-50 transition-colors flex items-center gap-3"
        >
          <ChevronLeft className="w-6 h-6" />
          Previous
        </button>
        <button
          onClick={handleNext}
          disabled={isSubmitting}
          className={`px-12 py-4 font-semibold text-lg rounded-xl transition-colors flex items-center gap-3 ${
            isSubmitting 
              ? 'bg-gray-400 text-gray-200 cursor-not-allowed' 
              : 'bg-green-600 text-white hover:bg-green-700'
          }`}
        >
          {isSubmitting ? (
            <>
              <div className="w-6 h-6 border-2 border-gray-300 border-t-white rounded-full animate-spin"></div>
              Creating Account...
            </>
          ) : (
            <>
              Complete Setup
              <Check className="w-6 h-6" />
            </>
          )}
        </button>
      </div>
    </div>
  );
};

// Main Wizard Component
const UserDataWizard = () => {
  const [currentStep, setCurrentStep] = useState(0);
  const [userData, setUserData] = useState({});
  const [isSubmitting, setIsSubmitting] = useState(false);

  const steps = [
    { title: 'Basic Info', component: BasicInfoStep, icon: User },
    { title: 'Risk Factors', component: RiskFactorsStep, icon: Heart },
    { title: 'Medications', component: MedicationsStep, icon: Pill },
    { title: 'Medical History', component: DiseasesStep, icon: Stethoscope }
  ];

  const updateStepData = (stepData) => {
    setUserData(prev => ({ ...prev, ...stepData }));
  };

  const nextStep = () => {
    if (currentStep < steps.length - 1) {
      setCurrentStep(prev => prev + 1);
    }
  };

  const prevStep = () => {
    if (currentStep > 0) {
      setCurrentStep(prev => prev - 1);
    }
  };

  const submitAllData = async () => {
    setIsSubmitting(true);
    try {
      // Step 1: Create user account
      const signUpResponse = await fetch(API_ENDPOINTS.CREATE_ACCOUNT, {
        method: 'POST',
        headers: { 'Content-Type': 'application/json' },
        body: JSON.stringify({
          username: userData.username,
          email: userData.email,
          password: userData.password || 'tempPassword123',
          dateOfBirth: userData.dateOfBirth,
          gender: userData.gender,
          height: userData.height,
          weight: userData.weight
        })
      });

      if (!signUpResponse.ok) {
        const errorData = await signUpResponse.json();
        throw new Error(errorData.message || 'Failed to create account');
      }

      const signUpData = await signUpResponse.json();
      const token = signUpData.token;
      
      // Store authentication token
      localStorage.setItem('mediaid_token', token);

      // Step 2: Submit risk factors if provided
      if (userData.smokingStatus || userData.alcoholConsumption || userData.physicalActivity) {
        const riskFactorsResponse = await fetch(API_ENDPOINTS.RISK_FACTORS, {
          method: 'POST',
          headers: { 
            'Content-Type': 'application/json',
            'Authorization': `Bearer ${token}`
          },
          body: JSON.stringify({
            smokingStatus: userData.smokingStatus,
            alcoholConsumption: userData.alcoholConsumption,
            physicalActivity: userData.physicalActivity,
            bloodPressure: userData.bloodPressure,
            stressLevel: userData.stressLevel,
            ageGroup: userData.ageGroup,
            familyHeartDisease: userData.familyHeartDisease,
            familyCancer: userData.familyCancer,
            height: userData.height,
            weight: userData.weight,
            bmi: userData.bmi
          })
        });

        if (!riskFactorsResponse.ok) {
          console.warn('Failed to submit risk factors');
        }
      }

      // Step 3: Submit medications if provided
      if (userData.medications && userData.medications.length > 0) {
        const medicationsResponse = await fetch(API_ENDPOINTS.USER_MEDICATIONS, {
          method: 'POST',
          headers: { 
            'Content-Type': 'application/json',
            'Authorization': `Bearer ${token}`
          },
          body: JSON.stringify({
            medications: userData.medications.map(med => ({
              cui: med.cui,
              name: med.name,
              dosage: med.dosage,
              frequency: med.frequency,
              startDate: med.startDate,
              endDate: med.endDate,
              administrationRoute: med.administrationRoute || 'oral',
              isActive: med.isActive !== false,
              notes: med.notes
            }))
          })
        });

        if (!medicationsResponse.ok) {
          console.warn('Failed to submit medications');
        }
      }

      // Step 4: Submit diseases if provided
      if (userData.diseases && userData.diseases.length > 0) {
        const diseasesResponse = await fetch(API_ENDPOINTS.USER_DISEASES, {
          method: 'POST',
          headers: { 
            'Content-Type': 'application/json',
            'Authorization': `Bearer ${token}`
          },
          body: JSON.stringify({
            diseases: userData.diseases.map(disease => ({
              cui: disease.cui,
              name: disease.name,
              diagnosisDate: disease.diagnosisDate,
              endDate: disease.endDate,
              status: disease.status || 'active',
              severity: disease.severity,
              notes: disease.notes
            }))
          })
        });

        if (!diseasesResponse.ok) {
          console.warn('Failed to submit diseases');
        }
      }

      // Success - redirect to profile or dashboard
      alert('Account created successfully! Welcome to MediAid!');
      window.location.href = '/homePage';
      
    } catch (error) {
      console.error('Error submitting data:', error);
      alert(`Error creating account: ${error.message}. Please try again.`);
    } finally {
      setIsSubmitting(false);
    }
  };

  const handleFinalStep = () => {
    submitAllData();
  };

  const StepComponent = steps[currentStep].component;

  return (
    <div className="min-h-screen bg-gray-50 py-12">
      <div className="max-w-7xl mx-auto px-8">
        {/* Progress Bar */}
        <div className="mb-16">
          <div className="flex items-center justify-between">
            {steps.map((step, index) => {
              const Icon = step.icon;
              const isCompleted = index < currentStep;
              const isCurrent = index === currentStep;
              
              return (
                <div key={index} className="flex items-center">
                  <div className={`flex items-center justify-center w-16 h-16 rounded-full border-4 transition-colors ${
                    isCompleted ? 'bg-green-500 border-green-500 text-white' :
                    isCurrent ? 'bg-blue-500 border-blue-500 text-white' :
                    'bg-white border-gray-300 text-gray-400'
                  }`}>
                    {isCompleted ? <Check className="w-8 h-8" /> : <Icon className="w-8 h-8" />}
                  </div>
                  <div className="ml-4">
                    <div className={`text-lg font-medium ${isCurrent ? 'text-blue-600' : isCompleted ? 'text-green-600' : 'text-gray-500'}`}>
                      Step {index + 1}
                    </div>
                    <div className={`text-base ${isCurrent ? 'text-blue-600' : isCompleted ? 'text-green-600' : 'text-gray-500'}`}>
                      {step.title}
                    </div>
                  </div>
                  {index < steps.length - 1 && (
                    <div className={`flex-1 h-2 mx-8 rounded-full ${isCompleted ? 'bg-green-500' : 'bg-gray-300'}`} />
                  )}
                </div>
              );
            })}
          </div>
        </div>

        {/* Step Content */}
        <div className="bg-white rounded-2xl shadow-xl p-12">
          <StepComponent
            data={userData}
            onUpdate={updateStepData}
            onNext={currentStep === steps.length - 1 ? handleFinalStep : nextStep}
            onPrev={prevStep}
            isSubmitting={isSubmitting}
          />
        </div>
      </div>
    </div>
  );
};

export default UserDataWizard;