import React, { useState, useEffect } from 'react';
import { 
  Search, Filter, BookOpen, Heart, Pill, AlertCircle, 
  CheckCircle, Info, Calendar, User, Stethoscope,
  ChevronDown, ChevronRight, RefreshCw, Download
} from 'lucide-react';

const MedicalGuidelines = () => {
  const [guidelines, setGuidelines] = useState([]);
  const [filteredGuidelines, setFilteredGuidelines] = useState([]);
  const [loading, setLoading] = useState(true);
  const [error, setError] = useState('');
  const [searchTerm, setSearchTerm] = useState('');
  const [selectedCategory, setSelectedCategory] = useState('all');
  const [selectedPriority, setSelectedPriority] = useState('all');
  const [expandedCards, setExpandedCards] = useState(new Set());

  // Categories for filtering
  const categories = [
    { value: 'all', label: 'All Guidelines', icon: BookOpen },
    { value: 'medication', label: 'Medication', icon: Pill },
    { value: 'lifestyle', label: 'Lifestyle', icon: Heart },
    { value: 'screening', label: 'Screening', icon: Stethoscope },
    { value: 'emergency', label: 'Emergency', icon: AlertCircle },
    { value: 'prevention', label: 'Prevention', icon: CheckCircle }
  ];

  const priorities = [
    { value: 'all', label: 'All Priorities' },
    { value: 'high', label: 'High Priority', color: 'text-red-600 bg-red-100' },
    { value: 'medium', label: 'Medium Priority', color: 'text-yellow-600 bg-yellow-100' },
    { value: 'low', label: 'Low Priority', color: 'text-green-600 bg-green-100' }
  ];

  useEffect(() => {
    fetchGuidelines();
  }, []);

  useEffect(() => {
    filterGuidelines();
  }, [guidelines, searchTerm, selectedCategory, selectedPriority]);

  const fetchGuidelines = async () => {
    setLoading(true);
    setError('');
    
    try {
      const token = localStorage.getItem('mediaid_token');
      
      // For now, we'll use mock data since the server endpoint doesn't exist yet
      // Replace this with actual API call when server is ready
      const mockGuidelines = generateMockGuidelines();
      
      // Uncomment this when server endpoint is ready:
      /*
      const response = await fetch('http://localhost:8080/api/medical/guidelines', {
        headers: {
          'Authorization': `Bearer ${token}`,
          'Content-Type': 'application/json'
        }
      });

      if (response.ok) {
        const data = await response.json();
        setGuidelines(data.guidelines || []);
      } else {
        throw new Error('Failed to fetch guidelines');
      }
      */
      
      // Using mock data for now
      setTimeout(() => {
        setGuidelines(mockGuidelines);
        setLoading(false);
      }, 1000);
      
    } catch (err) {
      setError(`Error loading guidelines: ${err.message}`);
      setLoading(false);
    }
  };

  const generateMockGuidelines = () => {
    return [
      {
        id: 1,
        title: 'Blood Pressure Management',
        category: 'medication',
        priority: 'high',
        summary: 'Guidelines for managing high blood pressure with medication and lifestyle changes.',
        content: `
          **Medication Guidelines:**
          • Take prescribed antihypertensive medications as directed
          • Monitor blood pressure regularly (daily if possible)
          • Report any side effects to your healthcare provider
          
          **Lifestyle Modifications:**
          • Reduce sodium intake to less than 2,300mg per day
          • Engage in regular physical activity (150 minutes/week)
          • Maintain healthy weight (BMI 18.5-24.9)
          • Limit alcohol consumption
          
          **Monitoring:**
          • Check blood pressure at same time daily
          • Keep a log of readings
          • Schedule regular follow-ups with your doctor
        `,
        lastUpdated: '2024-01-15',
        applicableConditions: ['Hypertension', 'Pre-hypertension'],
        tags: ['blood pressure', 'hypertension', 'cardiovascular']
      },
      {
        id: 2,
        title: 'Diabetes Management Protocol',
        category: 'lifestyle',
        priority: 'high',
        summary: 'Comprehensive guidelines for managing diabetes through diet, exercise, and monitoring.',
        content: `
          **Blood Sugar Monitoring:**
          • Check blood glucose as recommended by your doctor
          • Record readings in a logbook or app
          • Know your target ranges
          
          **Dietary Guidelines:**
          • Follow a balanced, consistent meal plan
          • Count carbohydrates if advised
          • Limit sugary drinks and processed foods
          • Include fiber-rich foods
          
          **Physical Activity:**
          • Aim for 150 minutes of moderate exercise weekly
          • Include both aerobic and strength training
          • Monitor blood sugar before and after exercise
        `,
        lastUpdated: '2024-01-20',
        applicableConditions: ['Type 2 Diabetes', 'Pre-diabetes'],
        tags: ['diabetes', 'blood sugar', 'diet', 'exercise']
      },
      {
        id: 3,
        title: 'Medication Interaction Warnings',
        category: 'medication',
        priority: 'high',
        summary: 'Important information about potential drug interactions to avoid.',
        content: `
          **High-Risk Combinations:**
          • Blood thinners + NSAIDs (increased bleeding risk)
          • ACE inhibitors + Potassium supplements
          • Diabetes medications + Alcohol
          
          **General Guidelines:**
          • Always inform healthcare providers of all medications
          • Include over-the-counter drugs and supplements
          • Use one pharmacy when possible
          • Keep an updated medication list
          
          **Warning Signs:**
          • Unusual bleeding or bruising
          • Dizziness or fainting
          • Nausea or stomach upset
          • Changes in heart rate
        `,
        lastUpdated: '2024-01-10',
        applicableConditions: ['Multiple Medications', 'Polypharmacy'],
        tags: ['drug interactions', 'safety', 'medications']
      },
      {
        id: 4,
        title: 'Heart Health Screening',
        category: 'screening',
        priority: 'medium',
        summary: 'Regular screening recommendations for cardiovascular health.',
        content: `
          **Recommended Screenings:**
          
          **Blood Pressure:**
          • Every 2 years if normal (less than 120/80)
          • Annually if elevated (120-129 systolic)
          • More frequently if high
          
          **Cholesterol:**
          • Every 5 years starting at age 20
          • More frequently if abnormal or high risk
          
          **Blood Sugar:**
          • Every 3 years starting at age 45
          • Earlier and more frequently if risk factors present
          
          **EKG/Stress Test:**
          • As recommended by healthcare provider
          • Consider if chest pain or risk factors present
        `,
        lastUpdated: '2024-01-18',
        applicableConditions: ['Cardiovascular Risk', 'Preventive Care'],
        tags: ['screening', 'heart health', 'prevention']
      },
      {
        id: 5,
        title: 'Emergency Medication Protocol',
        category: 'emergency',
        priority: 'high',
        summary: 'What to do in medical emergencies related to your medications.',
        content: `
          **Severe Allergic Reaction:**
          • Use EpiPen if prescribed
          • Call 911 immediately
          • Go to nearest emergency room
          
          **Missed Critical Medications:**
          • Blood thinners: Contact doctor immediately
          • Diabetes medications: Monitor blood sugar closely
          • Heart medications: Do not double dose
          
          **Overdose Symptoms:**
          • Confusion or drowsiness
          • Difficulty breathing
          • Rapid or slow heart rate
          • Call Poison Control: 1-800-222-1222
          
          **Emergency Kit:**
          • Keep updated medication list
          • Include emergency contacts
          • Carry medical alert information
        `,
        lastUpdated: '2024-01-12',
        applicableConditions: ['All Patients'],
        tags: ['emergency', 'safety', 'allergic reaction']
      },
      {
        id: 6,
        title: 'Healthy Lifestyle Guidelines',
        category: 'prevention',
        priority: 'medium',
        summary: 'General guidelines for maintaining good health through lifestyle choices.',
        content: `
          **Nutrition:**
          • Eat 5-9 servings of fruits and vegetables daily
          • Choose whole grains over refined grains
          • Limit saturated and trans fats
          • Stay hydrated (8 glasses of water daily)
          
          **Physical Activity:**
          • 150 minutes moderate exercise weekly
          • Include strength training 2x per week
          • Take breaks from sitting every hour
          
          **Sleep:**
          • Aim for 7-9 hours per night
          • Maintain consistent sleep schedule
          • Create comfortable sleep environment
          
          **Stress Management:**
          • Practice relaxation techniques
          • Maintain social connections
          • Consider meditation or yoga
        `,
        lastUpdated: '2024-01-25',
        applicableConditions: ['General Health', 'Prevention'],
        tags: ['lifestyle', 'nutrition', 'exercise', 'prevention']
      }
    ];
  };

  const filterGuidelines = () => {
    let filtered = guidelines;

    // Filter by search term
    if (searchTerm) {
      filtered = filtered.filter(guideline => 
        guideline.title.toLowerCase().includes(searchTerm.toLowerCase()) ||
        guideline.summary.toLowerCase().includes(searchTerm.toLowerCase()) ||
        guideline.tags.some(tag => tag.toLowerCase().includes(searchTerm.toLowerCase()))
      );
    }

    // Filter by category
    if (selectedCategory !== 'all') {
      filtered = filtered.filter(guideline => guideline.category === selectedCategory);
    }

    // Filter by priority
    if (selectedPriority !== 'all') {
      filtered = filtered.filter(guideline => guideline.priority === selectedPriority);
    }

    setFilteredGuidelines(filtered);
  };

  const toggleCardExpansion = (id) => {
    const newExpanded = new Set(expandedCards);
    if (newExpanded.has(id)) {
      newExpanded.delete(id);
    } else {
      newExpanded.add(id);
    }
    setExpandedCards(newExpanded);
  };

  const getPriorityColor = (priority) => {
    switch (priority) {
      case 'high': return 'text-red-600 bg-red-100';
      case 'medium': return 'text-yellow-600 bg-yellow-100';
      case 'low': return 'text-green-600 bg-green-100';
      default: return 'text-gray-600 bg-gray-100';
    }
  };

  const getCategoryIcon = (category) => {
    const categoryData = categories.find(cat => cat.value === category);
    const Icon = categoryData?.icon || BookOpen;
    return <Icon className="w-4 h-4" />;
  };

  const formatContent = (content) => {
    return content.split('\n').map((line, index) => {
      if (line.startsWith('**') && line.endsWith('**')) {
        return (
          <h4 key={index} className="font-semibold text-gray-900 mt-4 mb-2">
            {line.replace(/\*\*/g, '')}
          </h4>
        );
      } else if (line.startsWith('•')) {
        return (
          <li key={index} className="ml-4 text-gray-700">
            {line.substring(1).trim()}
          </li>
        );
      } else if (line.trim()) {
        return (
          <p key={index} className="text-gray-700 mb-2">
            {line}
          </p>
        );
      }
      return null;
    });
  };

  if (loading) {
    return (
      <div className="min-h-screen bg-gray-50 flex items-center justify-center">
        <div className="text-center">
          <RefreshCw className="w-8 h-8 text-blue-600 animate-spin mx-auto mb-4" />
          <p className="text-gray-600">Loading medical guidelines...</p>
        </div>
      </div>
    );
  }

  if (error) {
    return (
      <div className="min-h-screen bg-gray-50 flex items-center justify-center">
        <div className="text-center">
          <AlertCircle className="w-12 h-12 text-red-500 mx-auto mb-4" />
          <p className="text-red-600 mb-4">{error}</p>
          <button 
            onClick={fetchGuidelines}
            className="px-4 py-2 bg-blue-600 text-white rounded-lg hover:bg-blue-700 transition-colors"
          >
            Try Again
          </button>
        </div>
      </div>
    );
  }

  return (
    <div className="min-h-screen bg-gray-50">
      <div className="max-w-7xl mx-auto px-4 py-8">
        {/* Header */}
        <div className="mb-8">
          <h1 className="text-3xl font-bold text-gray-900 mb-2 flex items-center">
            <BookOpen className="w-8 h-8 mr-3 text-blue-600" />
            Medical Guidelines
          </h1>
          <p className="text-gray-600">
            Personalized medical guidelines and recommendations based on your health profile
          </p>
        </div>

        {/* Filters */}
        <div className="bg-white rounded-lg shadow-sm border border-gray-200 p-6 mb-6">
          <div className="flex flex-col lg:flex-row gap-4">
            {/* Search */}
            <div className="flex-1">
              <div className="relative">
                <Search className="absolute left-3 top-1/2 transform -translate-y-1/2 w-5 h-5 text-gray-400" />
                <input
                  type="text"
                  placeholder="Search guidelines..."
                  value={searchTerm}
                  onChange={(e) => setSearchTerm(e.target.value)}
                  className="w-full pl-10 pr-4 py-2 border border-gray-300 rounded-lg focus:ring-2 focus:ring-blue-500 focus:border-transparent"
                />
              </div>
            </div>

            {/* Category Filter */}
            <div className="lg:w-48">
              <select
                value={selectedCategory}
                onChange={(e) => setSelectedCategory(e.target.value)}
                className="w-full px-3 py-2 border border-gray-300 rounded-lg focus:ring-2 focus:ring-blue-500 focus:border-transparent"
              >
                {categories.map(category => (
                  <option key={category.value} value={category.value}>
                    {category.label}
                  </option>
                ))}
              </select>
            </div>

            {/* Priority Filter */}
            <div className="lg:w-48">
              <select
                value={selectedPriority}
                onChange={(e) => setSelectedPriority(e.target.value)}
                className="w-full px-3 py-2 border border-gray-300 rounded-lg focus:ring-2 focus:ring-blue-500 focus:border-transparent"
              >
                {priorities.map(priority => (
                  <option key={priority.value} value={priority.value}>
                    {priority.label}
                  </option>
                ))}
              </select>
            </div>

            {/* Refresh Button */}
            <button
              onClick={fetchGuidelines}
              className="px-4 py-2 bg-blue-600 text-white rounded-lg hover:bg-blue-700 transition-colors flex items-center"
            >
              <RefreshCw className="w-4 h-4 mr-2" />
              Refresh
            </button>
          </div>
        </div>

        {/* Results Counter */}
        <div className="mb-4">
          <p className="text-gray-600">
            Showing {filteredGuidelines.length} of {guidelines.length} guidelines
          </p>
        </div>

        {/* Guidelines List */}
        <div className="space-y-4">
          {filteredGuidelines.length === 0 ? (
            <div className="bg-white rounded-lg shadow-sm border border-gray-200 p-8 text-center">
              <BookOpen className="w-12 h-12 text-gray-300 mx-auto mb-4" />
              <p className="text-gray-500">No guidelines found matching your criteria.</p>
            </div>
          ) : (
            filteredGuidelines.map((guideline) => (
              <div key={guideline.id} className="bg-white rounded-lg shadow-sm border border-gray-200">
                {/* Card Header */}
                <div className="p-6">
                  <div className="flex items-start justify-between mb-4">
                    <div className="flex-1">
                      <div className="flex items-center mb-2">
                        {getCategoryIcon(guideline.category)}
                        <h3 className="text-xl font-semibold text-gray-900 ml-2">
                          {guideline.title}
                        </h3>
                      </div>
                      <p className="text-gray-600 mb-3">{guideline.summary}</p>
                    </div>
                    <div className={`px-3 py-1 rounded-full text-sm font-medium ${getPriorityColor(guideline.priority)}`}>
                      {guideline.priority.charAt(0).toUpperCase() + guideline.priority.slice(1)} Priority
                    </div>
                  </div>

                  {/* Tags */}
                  <div className="flex flex-wrap gap-2 mb-4">
                    {guideline.tags.map((tag, index) => (
                      <span
                        key={index}
                        className="px-2 py-1 bg-blue-100 text-blue-800 text-xs rounded-full"
                      >
                        {tag}
                      </span>
                    ))}
                  </div>

                  {/* Meta Info */}
                  <div className="flex items-center justify-between text-sm text-gray-500">
                    <div className="flex items-center">
                      <Calendar className="w-4 h-4 mr-1" />
                      Last updated: {new Date(guideline.lastUpdated).toLocaleDateString()}
                    </div>
                    <div className="flex items-center">
                      <User className="w-4 h-4 mr-1" />
                      Applies to: {guideline.applicableConditions.join(', ')}
                    </div>
                  </div>

                  {/* Expand/Collapse Button */}
                  <button
                    onClick={() => toggleCardExpansion(guideline.id)}
                    className="mt-4 flex items-center text-blue-600 hover:text-blue-700 font-medium"
                  >
                    {expandedCards.has(guideline.id) ? (
                      <>
                        <ChevronDown className="w-4 h-4 mr-1" />
                        Hide Details
                      </>
                    ) : (
                      <>
                        <ChevronRight className="w-4 h-4 mr-1" />
                        Show Details
                      </>
                    )}
                  </button>
                </div>

                {/* Expanded Content */}
                {expandedCards.has(guideline.id) && (
                  <div className="border-t border-gray-200 p-6">
                    <div className="prose max-w-none">
                      {formatContent(guideline.content)}
                    </div>
                    
                    {/* Action Buttons */}
                    <div className="mt-6 flex gap-3">
                      <button className="px-4 py-2 bg-blue-600 text-white rounded-lg hover:bg-blue-700 transition-colors flex items-center">
                        <Download className="w-4 h-4 mr-2" />
                        Save PDF
                      </button>
                      <button className="px-4 py-2 border border-gray-300 text-gray-700 rounded-lg hover:bg-gray-50 transition-colors">
                        Share with Doctor
                      </button>
                    </div>
                  </div>
                )}
              </div>
            ))
          )}
        </div>
      </div>
    </div>
  );
};

export default MedicalGuidelines;