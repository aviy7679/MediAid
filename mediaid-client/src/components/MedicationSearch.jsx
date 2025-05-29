
import React, { useState, useEffect, useRef } from 'react';

const MedicationSearch = () => {
  const [query, setQuery] = useState('');
  const [filtered, setFiltered] = useState([]);
  const [loading, setLoading] = useState(false);
  const [selectedMedication, setSelectedMedication] = useState(null);
  const debounceTimeout = useRef(null);
  
  //כל פעם שהquery משתנה
  useEffect(() => {
    //אם המשתמש לא הקליד כלום או הקליד פחות מ-2 תווים, מבטל את החיפוש ומנקה את התוצאות
    if (!query || query.length < 2) {
      setFiltered([]);
      return;
    }
    
    // מבטל חיפוש קודם אם המשתמש ממשיך להקליד
    if (debounceTimeout.current) {
      clearTimeout(debounceTimeout.current);
    }
    
    // מבצע חיפוש רק אחרי שהמשתמש הפסיק להקליד למשך 300ms
    debounceTimeout.current = setTimeout(() => {
      searchMedications();
    }, 300);
    
    return () => {
      if (debounceTimeout.current) {
        clearTimeout(debounceTimeout.current);
      }
    };
  }, [query]);
  
  const searchMedications = async () => {
    if (!query || query.length < 2) return;
    
    setLoading(true);
    try {
      const BASE_URL = 'http://localhost:8080';
      
      const response = await fetch(`${BASE_URL}/api/medications/search?query=${encodeURIComponent(query)}`);
      if (!response.ok) {
        // הדפסת פרטי השגיאה לדיבאג
        const errorText = await response.text();
        console.error(`Server responded with ${response.status}: ${errorText}`);
        throw new Error(`Server responded with ${response.status}`);
      }
      const data = await response.json();
      setFiltered(data.content || data); // תמיכה גם ב-Page וגם ב-List
    } catch (error) {
      console.error('Error fetching medications:', error);
      setFiltered([]);
    } finally {
      setLoading(false);
    }
  };
  
  const handleSelect = (medication) => {
    setQuery(medication.name);
    setSelectedMedication(medication);
    setFiltered([]);
  };
  
  return (
    <div className="max-w-md mx-auto p-4 relative">
      <label className="block mb-2 font-medium text-gray-700">חיפוש תרופה:</label>
      <div className="relative">
        <input
          type="text"
          value={query}
          onChange={(e) => setQuery(e.target.value)}
          onKeyDown={(e) => {
            if (e.key === 'Enter' && filtered.length > 0) {
              handleSelect(filtered[0]);
            }
          }}
          placeholder="הקלד לחיפוש..."
          className="w-full border border-gray-300 rounded-lg px-3 py-2 shadow-sm focus:outline-none focus:ring focus:ring-blue-300"
          dir="rtl"
        />
        
        {loading && (
          <div className="absolute right-3 top-2">
            <div className="w-5 h-5 border-2 border-gray-300 border-t-blue-500 rounded-full animate-spin"></div>
          </div>
        )}
      </div>
      
      {filtered.length > 0 && (
        <ul className="absolute w-full bg-white border border-gray-300 rounded-lg mt-1 shadow-lg z-10 max-h-64 overflow-y-auto">
          {filtered.map((medication) => (
            <li
              key={medication.cui}
              className="px-4 py-2 hover:bg-gray-100 cursor-pointer flex items-center text-right"
              onClick={() => handleSelect(medication)}
              dir="rtl"
            >
              <svg className="w-4 h-4 text-gray-500 ml-2" fill="currentColor" viewBox="0 0 20 20">
                <path fillRule="evenodd" d="M12.9 14.32a8 8 0 111.414-1.414l4.387 4.387a1 1 0 01-1.414 1.414l-4.387-4.387zM14 8a6 6 0 11-12 0 6 6 0 0112 0z" clipRule="evenodd" />
              </svg>
              {medication.name}
            </li>
          ))}
        </ul>
      )}
      
      {selectedMedication && (
        <div className="mt-4 p-3 bg-blue-50 border border-blue-200 rounded-lg text-right" dir="rtl">
          <p className="font-medium">נבחר:</p>
          <p>{selectedMedication.name} (CUI: {selectedMedication.cui})</p>
        </div>
      )}
    </div>
  );
};

export default MedicationSearch;