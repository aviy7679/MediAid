import React, { useState } from 'react';
import { 
  AlertCircle, CheckCircle, Clock, Stethoscope, Pill, TestTube, 
  UserCheck, Info, X, Download, Share2, ChevronDown, ChevronUp 
} from 'lucide-react';

const TreatmentPlanDisplay = ({ treatmentPlan, extractedSymptoms, onClose }) => {
  const [expandedSections, setExpandedSections] = useState({
    symptoms: true,
    actions: true,
    tests: false,
    visits: false,
    connections: false
  });

  const toggleSection = (section) => {
    setExpandedSections(prev => ({
      ...prev,
      [section]: !prev[section]
    }));
  };

  const getUrgencyColor = (urgency) => {
    switch (urgency?.toLowerCase()) {
      case 'emergency': return 'bg-red-500 text-white';
      case 'high': return 'bg-orange-500 text-white';
      case 'medium': return 'bg-yellow-500 text-white';
      case 'low': return 'bg-green-500 text-white';
      default: return 'bg-gray-500 text-white';
    }
  };

  const getActionTypeIcon = (type) => {
    switch (type) {
      case 'CALL_EMERGENCY': return <AlertCircle className="w-5 h-5 text-red-600" />;
      case 'STOP_MEDICATION': return <Pills className="w-5 h-5 text-orange-600" />;
      case 'MONITOR_SYMPTOMS': return <Clock className="w-5 h-5 text-blue-600" />;
      case 'SEEK_IMMEDIATE_CARE': return <Stethoscope className="w-5 h-5 text-purple-600" />;
      default: return <Info className="w-5 h-5 text-gray-600" />;
    }
  };

  if (!treatmentPlan) {
    return null;
  }

  return (
    <div className="fixed inset-0 bg-black bg-opacity-50 flex items-center justify-center p-4 z-50">
      <div className="bg-white rounded-xl max-w-4xl w-full max-h-[90vh] overflow-y-auto">
        {/* Header */}
        <div className="p-8 border-b border-gray-200 bg-gradient-to-r from-blue-50 to-indigo-50">
          <div className="flex justify-between items-start">
            <div>
              <h2 className="text-3xl font-bold text-gray-900 mb-2">Medical Analysis Results</h2>
              <p className="text-gray-600 text-lg">Generated at {new Date().toLocaleString()}</p>
            </div>
            <div className="flex items-center gap-3">
              <span className={`px-4 py-2 rounded-full font-medium ${getUrgencyColor(treatmentPlan.urgencyLevel)}`}>
                {treatmentPlan.urgencyLevel} Priority
              </span>
              <button
                onClick={onClose}
                className="p-2 text-gray-400 hover:text-gray-600 rounded-lg hover:bg-gray-100"
              >
                <X className="w-6 h-6" />
              </button>
            </div>
          </div>
        </div>

        <div className="p-8 space-y-8">
          {/* Main Concern */}
          <div className="bg-blue-50 border-l-4 border-blue-400 p-6 rounded-r-lg">
            <h3 className="font-semibold text-blue-900 text-xl mb-2">Main Medical Concern</h3>
            <p className="text-blue-800 text-lg">{treatmentPlan.mainConcern}</p>
            {treatmentPlan.reasoning && (
              <div className="mt-4 pt-4 border-t border-blue-200">
                <p className="text-blue-700">{treatmentPlan.reasoning}</p>
              </div>
            )}
          </div>

          {/* Extracted Symptoms */}
          {extractedSymptoms && extractedSymptoms.length > 0 && (
            <div className="bg-white border-2 border-gray-200 rounded-xl">
              <button
                onClick={() => toggleSection('symptoms')}
                className="w-full p-6 flex justify-between items-center hover:bg-gray-50"
              >
                <h3 className="font-semibold text-gray-900 text-xl flex items-center">
                  <Stethoscope className="w-6 h-6 mr-3 text-purple-600" />
                  Detected Symptoms ({extractedSymptoms.length})
                </h3>
                {expandedSections.symptoms ? <ChevronUp /> : <ChevronDown />}
              </button>
              
              {expandedSections.symptoms && (
                <div className="px-6 pb-6 border-t border-gray-200">
                  <div className="grid grid-cols-1 md:grid-cols-2 gap-4">
                    {extractedSymptoms.map((symptom, index) => (
                      <div key={index} className="p-4 bg-gray-50 rounded-lg">
                        <div className="font-medium text-gray-900">{symptom.name}</div>
                        <div className="text-sm text-gray-600">
                          Source: {symptom.source} | Confidence: {(symptom.confidence * 100).toFixed(1)}%
                        </div>
                        {symptom.status && (
                          <div className="text-sm text-gray-500">Status: {symptom.status}</div>
                        )}
                      </div>
                    ))}
                  </div>
                </div>
              )}
            </div>
          )}

          {/* Immediate Actions */}
          {treatmentPlan.immediateActions && treatmentPlan.immediateActions.length > 0 && (
            <div className="bg-white border-2 border-gray-200 rounded-xl">
              <button
                onClick={() => toggleSection('actions')}
                className="w-full p-6 flex justify-between items-center hover:bg-gray-50"
              >
                <h3 className="font-semibold text-gray-900 text-xl flex items-center">
                  <AlertCircle className="w-6 h-6 mr-3 text-red-600" />
                  Immediate Actions ({treatmentPlan.immediateActions.length})
                </h3>
                {expandedSections.actions ? <ChevronUp /> : <ChevronDown />}
              </button>
              
              {expandedSections.actions && (
                <div className="px-6 pb-6 border-t border-gray-200">
                  <div className="space-y-4">
                    {treatmentPlan.immediateActions.map((action, index) => (
                      <div key={index} className="flex items-start p-4 bg-red-50 border border-red-200 rounded-lg">
                        <div className="mr-4 mt-1">
                          {getActionTypeIcon(action.type)}
                        </div>
                        <div className="flex-1">
                          <div className="font-medium text-gray-900 mb-2">{action.description}</div>
                          <div className="text-gray-600 text-sm">{action.reason}</div>
                          <div className="text-xs text-gray-500 mt-2">Priority: {action.priority}</div>
                        </div>
                      </div>
                    ))}
                  </div>
                </div>
              )}
            </div>
          )}

          {/* Recommended Tests */}
          {treatmentPlan.recommendedTests && treatmentPlan.recommendedTests.length > 0 && (
            <div className="bg-white border-2 border-gray-200 rounded-xl">
              <button
                onClick={() => toggleSection('tests')}
                className="w-full p-6 flex justify-between items-center hover:bg-gray-50"
              >
                <h3 className="font-semibold text-gray-900 text-xl flex items-center">
                  <TestTube className="w-6 h-6 mr-3 text-green-600" />
                  Recommended Tests ({treatmentPlan.recommendedTests.length})
                </h3>
                {expandedSections.tests ? <ChevronUp /> : <ChevronDown />}
              </button>
              
              {expandedSections.tests && (
                <div className="px-6 pb-6 border-t border-gray-200">
                  <div className="space-y-4">
                    {treatmentPlan.recommendedTests.map((test, index) => (
                      <div key={index} className="p-4 bg-green-50 border border-green-200 rounded-lg">
                        <div className="font-medium text-gray-900">{test.type || test.description}</div>
                        <div className="text-gray-600 text-sm mt-1">{test.reason}</div>
                        <div className="text-sm text-green-700 mt-2">Urgency: {test.urgency}</div>
                      </div>
                    ))}
                  </div>
                </div>
              )}
            </div>
          )}

          {/* Doctor Visits */}
          {treatmentPlan.doctorVisits && treatmentPlan.doctorVisits.length > 0 && (
            <div className="bg-white border-2 border-gray-200 rounded-xl">
              <button
                onClick={() => toggleSection('visits')}
                className="w-full p-6 flex justify-between items-center hover:bg-gray-50"
              >
                <h3 className="font-semibold text-gray-900 text-xl flex items-center">
                  <UserCheck className="w-6 h-6 mr-3 text-blue-600" />
                  Recommended Doctor Visits ({treatmentPlan.doctorVisits.length})
                </h3>
                {expandedSections.visits ? <ChevronUp /> : <ChevronDown />}
              </button>
              
              {expandedSections.visits && (
                <div className="px-6 pb-6 border-t border-gray-200">
                  <div className="space-y-4">
                    {treatmentPlan.doctorVisits.map((visit, index) => (
                      <div key={index} className="p-4 bg-blue-50 border border-blue-200 rounded-lg">
                        <div className="font-medium text-gray-900">{visit.type}</div>
                        <div className="text-gray-600 text-sm mt-1">{visit.reason}</div>
                        <div className="text-sm text-blue-700 mt-2">Timeframe: {visit.urgency}</div>
                      </div>
                    ))}
                  </div>
                </div>
              )}
            </div>
          )}

          {/* Medical Connections */}
          {treatmentPlan.foundConnections && treatmentPlan.foundConnections.length > 0 && (
            <div className="bg-white border-2 border-gray-200 rounded-xl">
              <button
                onClick={() => toggleSection('connections')}
                className="w-full p-6 flex justify-between items-center hover:bg-gray-50"
              >
                <h3 className="font-semibold text-gray-900 text-xl flex items-center">
                  <Info className="w-6 h-6 mr-3 text-purple-600" />
                  Medical Connections ({treatmentPlan.foundConnections.length})
                </h3>
                {expandedSections.connections ? <ChevronUp /> : <ChevronDown />}
              </button>
              
              {expandedSections.connections && (
                <div className="px-6 pb-6 border-t border-gray-200">
                  <div className="space-y-4">
                    {treatmentPlan.foundConnections.map((connection, index) => (
                      <div key={index} className="p-4 bg-purple-50 border border-purple-200 rounded-lg">
                        <div className="font-medium text-gray-900">{connection.explanation}</div>
                        <div className="text-sm text-gray-600 mt-2">
                          {connection.fromEntity} → {connection.toEntity}
                        </div>
                        <div className="text-sm text-purple-700 mt-1">
                          Confidence: {(connection.confidence * 100).toFixed(1)}%
                        </div>
                      </div>
                    ))}
                  </div>
                </div>
              )}
            </div>
          )}

          {/* Disclaimer */}
          <div className="bg-yellow-50 border border-yellow-200 rounded-lg p-6">
            <div className="flex items-start">
              <AlertCircle className="w-6 h-6 text-yellow-600 mr-3 mt-1" />
              <div>
                <h4 className="font-semibold text-yellow-800 mb-2">Important Disclaimer</h4>
                <p className="text-yellow-700">
                  This analysis is for informational purposes only and should not replace professional medical advice. 
                  Always consult with a qualified healthcare provider for proper diagnosis and treatment.
                </p>
              </div>
            </div>
          </div>

          {/* Action Buttons */}
          <div className="flex justify-between items-center pt-6 border-t border-gray-200">
            <div className="flex gap-4">
              <button className="px-6 py-3 bg-blue-600 text-white rounded-xl hover:bg-blue-700 transition-colors flex items-center">
                <Download className="w-5 h-5 mr-2" />
                Download Report
              </button>
              <button className="px-6 py-3 border-2 border-gray-300 text-gray-700 rounded-xl hover:bg-gray-50 transition-colors flex items-center">
                <Share2 className="w-5 h-5 mr-2" />
                Share with Doctor
              </button>
            </div>
            <button
              onClick={onClose}
              className="px-6 py-3 bg-gray-600 text-white rounded-xl hover:bg-gray-700 transition-colors"
            >
              Close
            </button>
          </div>
        </div>
      </div>
    </div>
  );
};

export default TreatmentPlanDisplay;