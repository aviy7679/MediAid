import React, { useState, useEffect, useRef } from 'react';
import { Search, Pill, Check, X } from 'lucide-react';
import { API_ENDPOINTS, buildSearchUrl } from '../apiConfig';

const MedicationSearch = () => {
  const [query, setQuery] = useState('');
  const [filtered, setFiltered] = useState([]);
  const [loading, setLoading] = useState(false);
  const [selectedMedication, setSelectedMedication] = useState(null);
  const [error, setError] = useState('');
  const debounceTimeout = useRef(null);
  
  // Search whenever query changes
  useEffect(() => {
    // If user didn't type anything or typed less than 2 characters, clear results
    if (!query || query.length < 2) {
      setFiltered([]);
      setError('');
      return;
    }
    
    // Cancel previous search if user continues typing
    if (debounceTimeout.current) {
      clearTimeout(debounceTimeout.current);
    }
    
    // Perform search only after user stops typing for 300ms
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
    setError('');
    try {
      const response = await fetch(buildSearchUrl(API_ENDPOINTS.SEARCH_MEDICATIONS, query, 30));
      if (!response.ok) {
        const errorText = await response.text();
        console.error(`Server responded with ${response.status}: ${errorText}`);
        throw new Error(`Server responded with ${response.status}`);
      }
      const data = await response.json();
      setFiltered(data.content || data);
    } catch (error) {
      console.error('Error fetching medications:', error);
      setError('Error searching medications. Please try again.');
      setFiltered([]);
    } finally {
      setLoading(false);
    }
  };
  
  const handleSelect = (medication) => {
    setQuery(medication.name);
    setSelectedMedication(medication);
    setFiltered([]);
    setError('');
  };

  const clearSelection = () => {
    setSelectedMedication(null);
    setQuery('');
    setFiltered([]);
    setError('');
  };
  
  return (
    <div className="max-w-4xl mx-auto p-8">
      <div className="bg-white rounded-xl shadow-lg border border-gray-200 p-8">
        <div className="mb-8">
          <h2 className="text-3xl font-bold text-gray-900 mb-4 flex items-center">
            <Pill className="w-8 h-8 mr-3 text-blue-600" />
            Medication Search
          </h2>
          <p className="text-gray-600 text-lg">Search for medications and get detailed information</p>
        </div>

        <div className="space-y-6">
          <div>
            <label className="block mb-3 font-semibold text-gray-700 text-lg">
              Search Medication:
            </label>
            <div className="relative">
              <div className="absolute inset-y-0 left-0 pl-4 flex items-center">
                <Search className="h-6 w-6 text-gray-400" />
              </div>
              <input
                type="text"
                value={query}
                onChange={(e) => setQuery(e.target.value)}
                onKeyDown={(e) => {
                  if (e.key === 'Enter' && filtered.length > 0) {
                    handleSelect(filtered[0]);
                  }
                }}
                placeholder="Type to search for medications..."
                className="w-full pl-12 pr-12 py-4 border-2 border-gray-300 rounded-xl shadow-sm focus:outline-none focus:ring-2 focus:ring-blue-500 focus:border-transparent text-lg"
              />
              
              {loading && (
                <div className="absolute inset-y-0 right-0 pr-4 flex items-center">
                  <div className="w-6 h-6 border-2 border-gray-300 border-t-blue-500 rounded-full animate-spin"></div>
                </div>
              )}

              {selectedMedication && (
                <button
                  onClick={clearSelection}
                  className="absolute inset-y-0 right-0 pr-4 flex items-center text-gray-400 hover:text-red-500 transition-colors"
                >
                  <X className="w-6 h-6" />
                </button>
              )}
            </div>
          </div>
          
          {error && (
            <div className="p-4 bg-red-50 border border-red-200 rounded-lg">
              <p className="text-red-700">{error}</p>
            </div>
          )}

          {filtered.length > 0 && (
            <div className="border border-gray-200 rounded-xl shadow-lg bg-white max-h-80 overflow-y-auto">
              {filtered.map((medication, index) => (
                <div
                  key={medication.cui}
                  className={`px-6 py-4 hover:bg-blue-50 cursor-pointer flex items-center transition-colors ${
                    index !== filtered.length - 1 ? 'border-b border-gray-100' : ''
                  }`}
                  onClick={() => handleSelect(medication)}
                >
                  <Pill className="w-5 h-5 text-gray-500 mr-4 flex-shrink-0" />
                  <div className="flex-1">
                    <div className="font-medium text-gray-900 text-lg">{medication.name}</div>
                    <div className="text-sm text-gray-500">CUI: {medication.cui}</div>
                  </div>
                  <div className="ml-4 opacity-0 group-hover:opacity-100 transition-opacity">
                    <Check className="w-5 h-5 text-green-500" />
                  </div>
                </div>
              ))}
            </div>
          )}
          
          {selectedMedication && (
            <div className="mt-8 p-8 bg-gradient-to-r from-blue-50 to-indigo-50 border-2 border-blue-200 rounded-xl">
              <div className="flex items-start justify-between">
                <div className="flex-1">
                  <h3 className="font-bold text-xl text-gray-900 mb-2 flex items-center">
                    <Check className="w-6 h-6 text-green-500 mr-2" />
                    Selected Medication:
                  </h3>
                  <div className="space-y-3">
                    <div>
                      <span className="font-semibold text-gray-700">Name:</span>
                      <span className="ml-2 text-gray-900 text-lg">{selectedMedication.name}</span>
                    </div>
                    <div>
                      <span className="font-semibold text-gray-700">CUI:</span>
                      <span className="ml-2 text-gray-600 font-mono">{selectedMedication.cui}</span>
                    </div>
                  </div>
                </div>
                <button
                  onClick={clearSelection}
                  className="ml-4 p-2 text-gray-400 hover:text-red-500 hover:bg-white rounded-lg transition-colors"
                  title="Clear selection"
                >
                  <X className="w-5 h-5" />
                </button>
              </div>
            </div>
          )}

          {query.length > 0 && query.length < 2 && (
            <div className="text-center py-8 text-gray-500">
              <Pill className="w-16 h-16 mx-auto mb-4 text-gray-300" />
              <p className="text-lg">Please enter at least 2 characters to search</p>
            </div>
          )}

          {query.length >= 2 && !loading && filtered.length === 0 && !selectedMedication && !error && (
            <div className="text-center py-8 text-gray-500">
              <Search className="w-16 h-16 mx-auto mb-4 text-gray-300" />
              <p className="text-lg">No medications found for "{query}"</p>
              <p className="text-gray-400 mt-2">Try a different search term</p>
            </div>
          )}
        </div>
      </div>
    </div>
  );
};

export default MedicationSearch;