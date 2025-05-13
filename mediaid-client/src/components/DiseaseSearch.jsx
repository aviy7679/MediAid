import React, { useState, useEffect } from 'react';

const DiseaseSearch = () => {
  const [diseases, setDiseases] = useState([]);
  const [query, setQuery] = useState('');
  const [filtered, setFiltered] = useState([]);

  useEffect(() => {
    fetch('/diseases.json')
      .then(res => res.json())
      .then(data => setDiseases(data));
  }, []);

  useEffect(() => {
    if (!query) {
      setFiltered([]);
      return;
    }
    const lowerQuery = query.toLowerCase();
    const results = diseases
      .filter(d => d.name.toLowerCase().includes(lowerQuery))
      .slice(0, 10);
    setFiltered(results);
  }, [query, diseases]);

  return (
    <div className="max-w-md mx-auto p-4 relative">
      <label className="block mb-2 font-medium text-gray-700">Search Disease:</label>
      <input
      type="text"
      value={query}
      onChange={e => setQuery(e.target.value)}
      onKeyDown={e => {
        if (e.key === 'Enter') {
          if (filtered.length > 0) {
            setQuery(filtered[0].name); // בוחר את האפשרות הראשונה
          }
          setFiltered([]); // סוגר את הרשימה
        }
      }}
      placeholder="Type to search..."
      className="w-full border border-gray-300 rounded-lg px-3 py-2 shadow-sm focus:outline-none focus:ring focus:ring-blue-300"
    />

      {filtered.length > 0 && (
        <ul className="absolute w-full bg-white border border-gray-300 rounded-lg mt-1 shadow-lg z-10">
          {filtered.map(d => (
            <li
              key={d.CUI}
              className="px-4 py-2 hover:bg-gray-100 cursor-pointer flex items-center"
              onClick={() => setQuery(d.name)}
            >
              <svg className="w-4 h-4 text-gray-500 mr-2" fill="currentColor" viewBox="0 0 20 20">
                <path fillRule="evenodd" d="M12.9 14.32a8 8 0 111.414-1.414l4.387 4.387a1 1 0 01-1.414 1.414l-4.387-4.387zM14 8a6 6 0 11-12 0 6 6 0 0112 0z" clipRule="evenodd" />
              </svg>
              {d.name}
            </li>
          ))}
        </ul>
      )}
    </div>
  );
};

export default DiseaseSearch;
