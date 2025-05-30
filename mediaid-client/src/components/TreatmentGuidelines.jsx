import React, { useState, useEffect } from 'react';
import { 
  FileText, Download, Search, Filter, Clock, AlertCircle, 
  CheckCircle, Info, ArrowLeft, Printer, Share2, Star
} from 'lucide-react';

const TreatmentGuidelines = () => {
  const [guidelines, setGuidelines] = useState([]);
  const [loading, setLoading] = useState(true);
  const [error, setError] = useState('');
  const [searchQuery, setSearchQuery] = useState('');
  const [selectedCategory, setSelectedCategory] = useState('all');
  const [selectedGuideline, setSelectedGuideline] = useState(null);
  const [favorites, setFavorites] = useState([]);

  // קטגוריות הנחיות
  const categories = [
    { id: 'all', name: 'All Guidelines', icon: FileText },
    { id: 'emergency', name: 'Emergency Care', icon: AlertCircle },
    { id: 'chronic', name: 'Chronic Conditions', icon: Clock },
    { id: 'preventive', name: 'Preventive Care', icon: CheckCircle },
    { id: 'medication', name: 'Medication Guidelines', icon: Info }
  ];

  useEffect(() => {
    fetchGuidelines();
    loadFavorites();
  }, []);

  const fetchGuidelines = async () => {
    setLoading(true);
    try {
      const token = localStorage.getItem('mediaid_token');
      const response = await fetch('/api/treatment-guidelines', {
        headers: {
          'Authorization': `Bearer ${token}`,
          'Content-Type': 'application/json'
        }
      });

      if (response.ok) {
        const data = await response.json();
        setGuidelines(data);
      } else {
        // Fallback data עד שהשרת יהיה מוכן
        setGuidelines(mockGuidelines);
      }
    } catch (err) {
      console.log('Using mock data until server is ready');
      setGuidelines(mockGuidelines);
    } finally {
      setLoading(false);
    }
  };

  const loadFavorites = () => {
    const savedFavorites = localStorage.getItem('treatment_favorites');
    if (savedFavorites) {
      setFavorites(JSON.parse(savedFavorites));
    }
  };

  const toggleFavorite = (guidelineId) => {
    const newFavorites = favorites.includes(guidelineId)
      ? favorites.filter(id => id !== guidelineId)
      : [...favorites, guidelineId];
    
    setFavorites(newFavorites);
    localStorage.setItem('treatment_favorites', JSON.stringify(newFavorites));
  };

  const filteredGuidelines = guidelines.filter(guideline => {
    const matchesSearch = guideline.title.toLowerCase().includes(searchQuery.toLowerCase()) ||
                         guideline.description.toLowerCase().includes(searchQuery.toLowerCase());
    const matchesCategory = selectedCategory === 'all' || guideline.category === selectedCategory;
    return matchesSearch && matchesCategory;
  });

  const handleDownload = (guideline) => {
    // יצירת PDF או הורדת המסמך
    const element = document.createElement('a');
    const file = new Blob([guideline.content], { type: 'text/plain' });
    element.href = URL.createObjectURL(file);
    element.download = `${guideline.title.replace(/\s+/g, '_')}.txt`;
    document.body.appendChild(element);
    element.click();
    document.body.removeChild(element);
  };

  const handlePrint = () => {
    window.print();
  };

  const handleShare = (guideline) => {
    if (navigator.share) {
      navigator.share({
        title: guideline.title,
        text: guideline.description,
        url: window.location.href
      });
    } else {
      navigator.clipboard.writeText(`${guideline.title}\n${guideline.description}\n${window.location.href}`);
      alert('Link copied to clipboard!');
    }
  };

  if (loading) {
    return (
      <div className="min-h-screen bg-gray-50 flex items-center justify-center">
        <div className="text-center">
          <div className="animate-spin rounded-full h-12 w-12 border-b-2 border-blue-600 mx-auto mb-4"></div>
          <p className="text-gray-600">Loading treatment guidelines...</p>
        </div>
      </div>
    );
  }

  if (selectedGuideline) {
    return (
      <div className="min-h-screen bg-gray-50">
        {/* Header */}
        <div className="bg-white shadow-sm border-b border-gray-200 print:hidden">
          <div className="max-w-7xl mx-auto px-4 py-4">
            <div className="flex items-center justify-between">
              <div className="flex items-center">
                <button
                  onClick={() => setSelectedGuideline(null)}
                  className="mr-4 p-2 text-gray-500 hover:text-gray-700 rounded-lg hover:bg-gray-100"
                >
                  <ArrowLeft className="w-5 h-5" />
                </button>
                <div>
                  <h1 className="text-2xl font-bold text-gray-900">{selectedGuideline.title}</h1>
                  <p className="text-gray-600 mt-1">{selectedGuideline.description}</p>
                </div>
              </div>
              <div className="flex items-center space-x-2">
                <button
                  onClick={() => toggleFavorite(selectedGuideline.id)}
                  className={`p-2 rounded-lg transition-colors ${
                    favorites.includes(selectedGuideline.id)
                      ? 'text-yellow-500 bg-yellow-50 hover:bg-yellow-100'
                      : 'text-gray-500 hover:text-yellow-500 hover:bg-gray-100'
                  }`}
                >
                  <Star className="w-5 h-5" />
                </button>
                <button
                  onClick={handlePrint}
                  className="p-2 text-gray-500 hover:text-gray-700 rounded-lg hover:bg-gray-100"
                >
                  <Printer className="w-5 h-5" />
                </button>
                <button
                  onClick={() => handleShare(selectedGuideline)}
                  className="p-2 text-gray-500 hover:text-gray-700 rounded-lg hover:bg-gray-100"
                >
                  <Share2 className="w-5 h-5" />
                </button>
                <button
                  onClick={() => handleDownload(selectedGuideline)}
                  className="px-4 py-2 bg-blue-600 text-white rounded-lg hover:bg-blue-700 transition-colors flex items-center"
                >
                  <Download className="w-4 h-4 mr-2" />
                  Download
                </button>
              </div>
            </div>
          </div>
        </div>

        {/* Content */}
        <div className="max-w-4xl mx-auto px-4 py-8">
          <div className="bg-white rounded-lg shadow-sm p-8">
            {/* Meta information */}
            <div className="mb-6 p-4 bg-blue-50 rounded-lg">
              <div className="grid grid-cols-1 md:grid-cols-3 gap-4 text-sm">
                <div>
                  <span className="font-medium text-gray-700">Category:</span>
                  <span className="ml-2 text-blue-600 capitalize">{selectedGuideline.category}</span>
                </div>
                <div>
                  <span className="font-medium text-gray-700">Priority:</span>
                  <span className={`ml-2 px-2 py-1 rounded text-xs font-medium ${
                    selectedGuideline.priority === 'high' ? 'bg-red-100 text-red-800' :
                    selectedGuideline.priority === 'medium' ? 'bg-yellow-100 text-yellow-800' :
                    'bg-green-100 text-green-800'
                  }`}>
                    {selectedGuideline.priority}
                  </span>
                </div>
                <div>
                  <span className="font-medium text-gray-700">Last Updated:</span>
                  <span className="ml-2 text-gray-600">{selectedGuideline.lastUpdated}</span>
                </div>
              </div>
            </div>

            {/* Main content */}
            <div className="prose prose-lg max-w-none">
              <div dangerouslySetInnerHTML={{ __html: selectedGuideline.content }} />
            </div>

            {/* Related guidelines */}
            {selectedGuideline.relatedGuidelines && selectedGuideline.relatedGuidelines.length > 0 && (
              <div className="mt-8 pt-6 border-t border-gray-200">
                <h3 className="text-lg font-semibold text-gray-900 mb-4">Related Guidelines</h3>
                <div className="grid grid-cols-1 md:grid-cols-2 gap-4">
                  {selectedGuideline.relatedGuidelines.map((related) => (
                    <div
                      key={related.id}
                      className="p-4 border border-gray-200 rounded-lg hover:border-blue-300 cursor-pointer transition-colors"
                      onClick={() => {
                        const relatedGuideline = guidelines.find(g => g.id === related.id);
                        if (relatedGuideline) setSelectedGuideline(relatedGuideline);
                      }}
                    >
                      <h4 className="font-medium text-gray-900">{related.title}</h4>
                      <p className="text-sm text-gray-600 mt-1">{related.description}</p>
                    </div>
                  ))}
                </div>
              </div>
            )}
          </div>
        </div>
      </div>
    );
  }

  return (
    <div className="min-h-screen bg-gray-50">
      {/* Header */}
      <div className="bg-white shadow-sm border-b border-gray-200">
        <div className="max-w-7xl mx-auto px-4 py-6">
          <div className="flex justify-between items-center">
            <div>
              <h1 className="text-3xl font-bold text-gray-900">Treatment Guidelines</h1>
              <p className="text-gray-600 mt-2">Evidence-based medical treatment recommendations</p>
            </div>
            <button
              onClick={() => window.history.back()}
              className="px-4 py-2 text-gray-600 hover:text-gray-800 border border-gray-300 rounded-lg hover:bg-gray-50 transition-colors"
            >
              Back to Dashboard
            </button>
          </div>
        </div>
      </div>

      <div className="max-w-7xl mx-auto px-4 py-8">
        {/* Search and Filter */}
        <div className="mb-8 bg-white p-6 rounded-lg shadow-sm">
          <div className="flex flex-col md:flex-row gap-4">
            <div className="flex-1 relative">
              <Search className="absolute left-3 top-3 w-5 h-5 text-gray-400" />
              <input
                type="text"
                placeholder="Search guidelines..."
                value={searchQuery}
                onChange={(e) => setSearchQuery(e.target.value)}
                className="w-full pl-10 pr-4 py-3 border border-gray-300 rounded-lg focus:ring-2 focus:ring-blue-500 focus:border-transparent"
              />
            </div>
            <div className="flex items-center">
              <Filter className="w-5 h-5 text-gray-400 mr-2" />
              <select
                value={selectedCategory}
                onChange={(e) => setSelectedCategory(e.target.value)}
                className="px-4 py-3 border border-gray-300 rounded-lg focus:ring-2 focus:ring-blue-500 focus:border-transparent"
              >
                {categories.map((category) => (
                  <option key={category.id} value={category.id}>
                    {category.name}
                  </option>
                ))}
              </select>
            </div>
          </div>
        </div>

        {/* Categories */}
        <div className="mb-8">
          <div className="flex flex-wrap gap-4">
            {categories.map((category) => {
              const Icon = category.icon;
              return (
                <button
                  key={category.id}
                  onClick={() => setSelectedCategory(category.id)}
                  className={`flex items-center px-4 py-2 rounded-lg transition-colors ${
                    selectedCategory === category.id
                      ? 'bg-blue-600 text-white'
                      : 'bg-white text-gray-700 hover:bg-gray-50 border border-gray-200'
                  }`}
                >
                  <Icon className="w-4 h-4 mr-2" />
                  {category.name}
                </button>
              );
            })}
          </div>
        </div>

        {/* Guidelines Grid */}
        <div className="grid grid-cols-1 md:grid-cols-2 lg:grid-cols-3 gap-6">
          {filteredGuidelines.map((guideline) => (
            <div
              key={guideline.id}
              className="bg-white p-6 rounded-lg shadow-sm border border-gray-200 hover:border-blue-300 hover:shadow-md transition-all cursor-pointer"
              onClick={() => setSelectedGuideline(guideline)}
            >
              <div className="flex items-start justify-between mb-4">
                <div className="flex-1">
                  <h3 className="font-semibold text-gray-900 mb-2">{guideline.title}</h3>
                  <p className="text-gray-600 text-sm mb-3 line-clamp-3">{guideline.description}</p>
                </div>
                <button
                  onClick={(e) => {
                    e.stopPropagation();
                    toggleFavorite(guideline.id);
                  }}
                  className={`ml-2 p-1 rounded transition-colors ${
                    favorites.includes(guideline.id)
                      ? 'text-yellow-500 hover:text-yellow-600'
                      : 'text-gray-400 hover:text-yellow-500'
                  }`}
                >
                  <Star className="w-4 h-4" />
                </button>
              </div>

              <div className="flex items-center justify-between">
                <div className="flex items-center space-x-2">
                  <span className={`px-2 py-1 rounded text-xs font-medium ${
                    guideline.priority === 'high' ? 'bg-red-100 text-red-800' :
                    guideline.priority === 'medium' ? 'bg-yellow-100 text-yellow-800' :
                    'bg-green-100 text-green-800'
                  }`}>
                    {guideline.priority}
                  </span>
                  <span className="text-xs text-gray-500 capitalize">{guideline.category}</span>
                </div>
                <div className="flex items-center space-x-1">
                  <button
                    onClick={(e) => {
                      e.stopPropagation();
                      handleDownload(guideline);
                    }}
                    className="p-1 text-gray-400 hover:text-gray-600 rounded transition-colors"
                  >
                    <Download className="w-4 h-4" />
                  </button>
                  <button
                    onClick={(e) => {
                      e.stopPropagation();
                      handleShare(guideline);
                    }}
                    className="p-1 text-gray-400 hover:text-gray-600 rounded transition-colors"
                  >
                    <Share2 className="w-4 h-4" />
                  </button>
                </div>
              </div>

              <div className="mt-3 pt-3 border-t border-gray-100">
                <p className="text-xs text-gray-500">
                  Last updated: {guideline.lastUpdated}
                </p>
              </div>
            </div>
          ))}
        </div>

        {filteredGuidelines.length === 0 && (
          <div className="text-center py-12">
            <FileText className="w-12 h-12 text-gray-300 mx-auto mb-4" />
            <h3 className="text-lg font-medium text-gray-900 mb-2">No guidelines found</h3>
            <p className="text-gray-500">Try adjusting your search or filter criteria.</p>
          </div>
        )}
      </div>
    </div>
  );
};

// Mock data עד שהשרת יהיה מוכן
const mockGuidelines = [
  {
    id: 1,
    title: "Hypertension Management Guidelines",
    description: "Comprehensive guidelines for managing high blood pressure in adults",
    category: "chronic",
    priority: "high",
    lastUpdated: "2024-01-15",
    content: `
      <h2>Hypertension Management Guidelines</h2>
      <h3>Overview</h3>
      <p>Hypertension affects millions of adults worldwide and is a major risk factor for cardiovascular disease.</p>
      
      <h3>Diagnosis</h3>
      <ul>
        <li>Blood pressure ≥140/90 mmHg on repeated measurements</li>
        <li>24-hour ambulatory monitoring recommended</li>
        <li>Home blood pressure monitoring encouraged</li>
      </ul>
      
      <h3>Treatment Approach</h3>
      <ol>
        <li><strong>Lifestyle Modifications:</strong>
          <ul>
            <li>Diet: DASH diet, sodium reduction</li>
            <li>Exercise: 150 minutes moderate activity per week</li>
            <li>Weight management</li>
            <li>Alcohol moderation</li>
          </ul>
        </li>
        <li><strong>Pharmacological Treatment:</strong>
          <ul>
            <li>First-line: ACE inhibitors, ARBs, CCBs, Diuretics</li>
            <li>Combination therapy often required</li>
            <li>Target: <130/80 mmHg for most patients</li>
          </ul>
        </li>
      </ol>
      
      <h3>Monitoring</h3>
      <p>Regular follow-up appointments every 3-6 months until controlled, then every 6-12 months.</p>
    `,
    relatedGuidelines: [
      { id: 2, title: "Diabetes Management", description: "Guidelines for Type 2 diabetes" }
    ]
  },
  {
    id: 2,
    title: "Type 2 Diabetes Management",
    description: "Evidence-based approach to managing Type 2 diabetes mellitus",
    category: "chronic",
    priority: "high",
    lastUpdated: "2024-01-10",
    content: `
      <h2>Type 2 Diabetes Management Guidelines</h2>
      <h3>Diagnostic Criteria</h3>
      <ul>
        <li>HbA1c ≥6.5% (48 mmol/mol)</li>
        <li>Fasting glucose ≥126 mg/dL (7.0 mmol/L)</li>
        <li>2-hour glucose ≥200 mg/dL (11.1 mmol/L) during OGTT</li>
      </ul>
      
      <h3>Treatment Goals</h3>
      <ul>
        <li>HbA1c <7% for most adults</li>
        <li>Individualized targets based on patient factors</li>
        <li>Blood pressure <140/90 mmHg</li>
        <li>LDL cholesterol <100 mg/dL</li>
      </ul>
      
      <h3>Medication Algorithm</h3>
      <ol>
        <li>First-line: Metformin</li>
        <li>Second-line: Add SGLT2 inhibitor, GLP-1 RA, or DPP-4 inhibitor</li>
        <li>Third-line: Triple combination or insulin</li>
      </ol>
    `,
    relatedGuidelines: [
      { id: 1, title: "Hypertension Management", description: "Guidelines for high blood pressure" }
    ]
  },
  {
    id: 3,
    title: "Emergency Chest Pain Protocol",
    description: "Rapid assessment and management of acute chest pain",
    category: "emergency",
    priority: "high",
    lastUpdated: "2024-01-20",
    content: `
      <h2>Emergency Chest Pain Protocol</h2>
      <h3>Initial Assessment (ABCDE)</h3>
      <ul>
        <li>Airway, Breathing, Circulation</li>
        <li>Disability, Exposure</li>
        <li>Vital signs and oxygen saturation</li>
      </ul>
      
      <h3>Immediate Actions</h3>
      <ol>
        <li>12-lead ECG within 10 minutes</li>
        <li>IV access and blood samples</li>
        <li>Oxygen if SpO2 <90%</li>
        <li>Aspirin 325mg chewed</li>
        <li>Nitroglycerin if appropriate</li>
      </ol>
      
      <h3>Risk Stratification</h3>
      <p>Use HEART score or GRACE score for risk assessment.</p>
      
      <h3>Disposition</h3>
      <ul>
        <li>STEMI: Immediate cath lab activation</li>
        <li>NSTEMI: Cardiology consultation</li>
        <li>Low risk: Consider discharge with follow-up</li>
      </ul>
    `
  },
  {
    id: 4,
    title: "Antibiotic Stewardship Guidelines",
    description: "Rational antibiotic prescribing and resistance prevention",
    category: "medication",
    priority: "medium",
    lastUpdated: "2024-01-12",
    content: `
      <h2>Antibiotic Stewardship Guidelines</h2>
      <h3>Principles</h3>
      <ul>
        <li>Right drug, right dose, right duration</li>
        <li>Culture before treatment when possible</li>
        <li>De-escalate based on culture results</li>
        <li>Stop antibiotics when infection resolved</li>
      </ul>
      
      <h3>Common Infections</h3>
      <h4>Community-Acquired Pneumonia</h4>
      <ul>
        <li>Outpatient: Amoxicillin or macrolide</li>
        <li>Inpatient: Beta-lactam + macrolide</li>
        <li>Duration: 5-7 days typically</li>
      </ul>
      
      <h4>Urinary Tract Infection</h4>
      <ul>
        <li>Uncomplicated: Nitrofurantoin or TMP-SMX</li>
        <li>Complicated: Fluoroquinolone</li>
        <li>Duration: 3-7 days</li>
      </ul>
    `
  },
  {
    id: 5,
    title: "Preventive Care Checklist",
    description: "Age-appropriate screening and prevention recommendations",
    category: "preventive",
    priority: "low",
    lastUpdated: "2024-01-05",
    content: `
      <h2>Preventive Care Guidelines</h2>
      <h3>Adults 18-39 years</h3>
      <ul>
        <li>Blood pressure: Every 2 years</li>
        <li>Cholesterol: Every 5 years (or more if risk factors)</li>
        <li>Diabetes screening: If overweight/obese</li>
        <li>Cervical cancer: Every 3 years (Pap smear)</li>
        <li>STI screening: As appropriate</li>
      </ul>
      
      <h3>Adults 40-65 years</h3>
      <ul>
        <li>Blood pressure: Annually</li>
        <li>Cholesterol: Every 5 years</li>
        <li>Diabetes: Every 3 years</li>
        <li>Breast cancer: Mammogram every 1-2 years</li>
        <li>Colorectal cancer: Starting at 45-50</li>
      </ul>
      
      <h3>Adults 65+ years</h3>
      <ul>
        <li>All above screenings</li>
        <li>Osteoporosis screening</li>
        <li>Fall risk assessment</li>
        <li>Cognitive assessment</li>
        <li>Vaccinations: Flu, pneumonia, shingles</li>
      </ul>
    `
  }
];

export default TreatmentGuidelines;