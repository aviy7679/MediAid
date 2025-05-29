import React, { useState, useEffect } from 'react';
import { auth } from '../authUtils'; 

const RiskFactorForm = () => {
  // מצב הטופס - כל שדה נשמר כאן
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

  // אפשרויות לבחירה בשדות - לפי התיאורים מה-ENUM
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

  // חישוב BMI אוטומטי כאשר גובה או משקל משתנים
  useEffect(() => {
    const weight = parseFloat(formData.weight);
    const height = parseFloat(formData.height);
    
    if (weight > 0 && height > 0) {
      const heightInMeters = height / 100;
      const bmi = weight / (heightInMeters * heightInMeters);
      setFormData(prevState => ({
        ...prevState,
        bmi: Math.round(bmi * 100) / 100 // עיגול לשני מקומות אחרי הנקודה
      }));
    } else {
      setFormData(prevState => ({
        ...prevState,
        bmi: null
      }));
    }
  }, [formData.weight, formData.height]);

  // פונקציה לקביעת קטגוריית BMI
  const getBmiCategory = (bmi) => {
    if (!bmi) return '';
    if (bmi < 18.5) return 'Underweight';
    if (bmi < 25) return 'Normal';
    if (bmi < 30) return 'Overweight';
    if (bmi < 35) return 'Obese class 1';
    if (bmi < 40) return 'Obese class 2';
    return 'Obese class 3';
  };

  // פונקציה לקביעת צבע BMI
  const getBmiColor = (bmi) => {
    if (!bmi) return '#333';
    if (bmi < 18.5) return '#3498db'; // כחול - תת משקל
    if (bmi < 25) return '#27ae60'; // ירוק - נורמלי
    if (bmi < 30) return '#f39c12'; // כתום - עודף משקל
    return '#e74c3c'; // אדום - השמנה
  };

  // טיפול בשינוי שדות
  const handleChange = (e) => {
    const { name, value } = e.target;
    setFormData(prevState => ({
      ...prevState,
      [name]: value
    }));
  };

  // פונקציית שליחה לשרת
  const handleSubmit = async (e) => {
    e.preventDefault();
    setIsSubmitting(true);
    setSubmitStatus('');

    try {
      // בדיקה שהמשתמש מחובר
      if (!auth.isAuthenticated()) {
        throw new Error('נדרשת התחברות למערכת');
      }

      // קבלת הטוקן
      const token = auth.getToken();
      if (!token) {
        throw new Error('לא נמצא טוקן גישה');
      }

      // ולידציה בסיסית
      if (!formData.weight || !formData.height) {
        throw new Error('אנא מלא שדות גובה ומשקל');
      }

      // הכנת הנתונים לשליחה - כל הערכים כבר בפורמט הנכון של ENUM
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

      console.log('שולח נתונים:', dataToSend);

      const response = await fetch('/api/user/risk-factors', {
        method: 'POST',
        headers: {
          'Content-Type': 'application/json',
          'Authorization': `Bearer ${token}` // הוספת טוקן ההרשאה
        },
        body: JSON.stringify(dataToSend)
      });

      if (!response.ok) {
        if (response.status === 401) {
          // טוקן לא תקין - הוצא את המשתמש
          auth.logout();
          throw new Error('הסשן פג תוקף, אנא התחבר מחדש');
        }
        const errorData = await response.json().catch(() => ({}));
        throw new Error(errorData.message || `שגיאה בשרת: ${response.status}`);
      }

      const result = await response.json();
      setSubmitStatus('הנתונים נשמרו בהצלחה!');
      console.log('תגובת השרת:', result);

      // הצגת מידע נוסף מהתגובה
      if (result.overallRiskScore !== undefined) {
        setSubmitStatus(`הנתונים נשמרו בהצלחה! ציון סיכון כולל: ${result.overallRiskScore.toFixed(2)} (${result.riskLevel})`);
      }

    } catch (error) {
      console.error('שגיאה בשליחת הנתונים:', error);
      setSubmitStatus(`שגיאה: ${error.message}`);
    } finally {
      setIsSubmitting(false);
    }
  };

  // בדיקה אם המשתמש מחובר
  useEffect(() => {
    if (!auth.isAuthenticated()) {
      setSubmitStatus('נדרשת התחברות למערכת');
    }
  }, []);

  // קומפוננטה לשדה בחירה (dropdown)
  const renderSelectField = (name, label, options) => (
    <div className="form-field">
      <label htmlFor={name}>{label}</label>
      <select name={name} value={formData[name]} onChange={handleChange}>
        <option value="">Select...</option>
        {options.map(opt => (
          <option key={opt.value} value={opt.value}>{opt.label}</option>
        ))}
      </select>
    </div>
  );

  // קומפוננטה לשדה מספרי (כמו משקל או גובה)
  const renderNumberField = (name, label, placeholder, unit) => (
    <div className="form-field">
      <label htmlFor={name}>{label}</label>
      <div className="input-with-unit">
        <input
          type="number"
          name={name}
          value={formData[name]}
          onChange={handleChange}
          placeholder={placeholder}
          step="0.1"
          min="0"
        />
        <span className="unit">{unit}</span>
      </div>
    </div>
  );

  // רינדור של הקומפוננטה
  return (
    <div className="risk-factor-form">
      <h2 className="form-title">Medical Risk Factor Details</h2>

      <form onSubmit={handleSubmit}>
        <div className="form-container">
          {/* מידע בסיסי */}
          <div className="form-section">
            <h3 className="section-title">Basic Information</h3>
            <div className="form-row">
              {renderSelectField('ageGroup', 'Age Group', riskFactorOptions.ageGroup)}
              {renderNumberField('height', 'Height', 'e.g. 170', 'cm')}
              {renderNumberField('weight', 'Weight', 'e.g. 70', 'kg')}
            </div>
            
            {/* הצגת BMI */}
            {formData.bmi && (
              <div className="bmi-display">
                <h4>BMI: <span style={{color: getBmiColor(formData.bmi)}}>{formData.bmi}</span></h4>
                <p className="bmi-category" style={{color: getBmiColor(formData.bmi)}}>
                  Category: {getBmiCategory(formData.bmi)}
                </p>
              </div>
            )}
          </div>

          {/* אורח חיים */}
          <div className="form-section">
            <h3 className="section-title">Lifestyle</h3>
            <div className="form-row">
              {renderSelectField('smokingStatus', 'Smoking Status', riskFactorOptions.smokingStatus)}
              {renderSelectField('alcoholConsumption', 'Alcohol Consumption', riskFactorOptions.alcoholConsumption)}
              {renderSelectField('physicalActivity', 'Physical Activity', riskFactorOptions.physicalActivity)}
            </div>
          </div>

          {/* מצב רפואי */}
          <div className="form-section">
            <h3 className="section-title">Medical Status</h3>
            <div className="form-row">
              {renderSelectField('bloodPressure', 'Blood Pressure', riskFactorOptions.bloodPressure)}
              {renderSelectField('stressLevel', 'Stress Level', riskFactorOptions.stressLevel)}
            </div>
          </div>

          {/* היסטוריה משפחתית */}
          <div className="form-section">
            <h3 className="section-title">Family History</h3>
            <div className="form-row">
              {renderSelectField('familyHeartDisease', 'Family History of Heart Disease', riskFactorOptions.familyHeartDisease)}
              {renderSelectField('familyCancer', 'Family History of Cancer', riskFactorOptions.familyCancer)}
            </div>
          </div>
        </div>

        {/* כפתור שליחה ומסר סטטוס */}
        <div className="submit-section">
          <button type="submit" className="submit-button" disabled={isSubmitting || !auth.isAuthenticated()}>
            {isSubmitting ? 'שולח...' : 'שמור נתונים'}
          </button>
          {submitStatus && (
            <div className={`status-message ${submitStatus.includes('שגיאה') ? 'error' : 'success'}`}>
              {submitStatus}
            </div>
          )}
        </div>
      </form>

      {/* סגנון פנימי לקומפוננטה */}
      <style jsx>{`
        .risk-factor-form {
          font-family: Arial, sans-serif;
          padding: 20px;
          background: #f9f9f9;
          border-radius: 10px;
          max-width: 900px;
          margin: auto;
        }

        .form-title {
          text-align: center;
          font-size: 24px;
          margin-bottom: 20px;
          color: #2c3e50;
        }

        .form-section {
          margin-bottom: 30px;
          background: white;
          padding: 20px;
          border-radius: 8px;
          box-shadow: 0 2px 4px rgba(0,0,0,0.1);
        }

        .section-title {
          font-size: 18px;
          margin-bottom: 15px;
          color: #34495e;
          border-bottom: 2px solid #3498db;
          padding-bottom: 5px;
        }

        .form-row {
          display: flex;
          flex-wrap: wrap;
          gap: 20px;
        }

        .form-field {
          flex: 1 1 250px;
          display: flex;
          flex-direction: column;
        }

        label {
          font-weight: bold;
          margin-bottom: 5px;
          color: #2c3e50;
        }

        select, input {
          padding: 10px;
          border-radius: 5px;
          border: 2px solid #ddd;
          font-size: 14px;
          transition: border-color 0.3s;
        }

        select:focus, input:focus {
          outline: none;
          border-color: #3498db;
        }

        .input-with-unit {
          display: flex;
          align-items: center;
        }

        .input-with-unit input {
          flex: 1;
        }

        .unit {
          margin-left: 8px;
          font-size: 14px;
          color: #666;
          font-weight: bold;
        }

        .bmi-display {
          background: #ecf0f1;
          padding: 15px;
          border-radius: 8px;
          margin-top: 15px;
          text-align: center;
        }

        .bmi-display h4 {
          margin: 0 0 5px 0;
          font-size: 18px;
        }

        .bmi-category {
          margin: 0;
          font-weight: bold;
        }

        .submit-section {
          text-align: center;
          margin-top: 30px;
        }

        .submit-button {
          background: #3498db;
          color: white;
          padding: 12px 30px;
          border: none;
          border-radius: 6px;
          font-size: 16px;
          font-weight: bold;
          cursor: pointer;
          transition: background-color 0.3s;
        }

        .submit-button:hover:not(:disabled) {
          background: #2980b9;
        }

        .submit-button:disabled {
          background: #bdc3c7;
          cursor: not-allowed;
        }

        .status-message {
          margin-top: 15px;
          padding: 10px;
          border-radius: 5px;
          font-weight: bold;
        }

        .status-message.success {
          background: #d4edda;
          color: #155724;
          border: 1px solid #c3e6cb;
        }

        .status-message.error {
          background: #f8d7da;
          color: #721c24;
          border: 1px solid #f5c6cb;
        }

        @media (max-width: 768px) {
          .form-row {
            flex-direction: column;
          }
          
          .form-field {
            flex: 1 1 auto;
          }
        }
      `}</style>
    </div>
  );
};

export default RiskFactorForm;