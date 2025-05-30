const API_BASE_URL = 'http://localhost:8081';

export const API_ENDPOINTS = {
  // Auth endpoints
  LOGIN: `${API_BASE_URL}/logIn`,
  SIGNUP: `${API_BASE_URL}/signUp`,
  VALIDATE_TOKEN: `${API_BASE_URL}/validateToken`,
  
  // User management endpoints
  USER_PROFILE: `${API_BASE_URL}/api/user/profile`,
  CREATE_ACCOUNT: `${API_BASE_URL}/api/user/create-account`,
  RISK_FACTORS: `${API_BASE_URL}/api/user/risk/risk-factors`,
  USER_MEDICATIONS: `${API_BASE_URL}/api/user/medications`,
  USER_DISEASES: `${API_BASE_URL}/api/user/diseases`,
  
  // Search endpoints
  SEARCH_MEDICATIONS: `${API_BASE_URL}/api/medications/search`,
  SEARCH_DISEASES: `${API_BASE_URL}/api/diseases/search`,
  
  // Data upload
  UPLOAD_DATA: `${API_BASE_URL}/uploadData`
};

// Helper functions for building URLs with parameters
export const buildSearchUrl = (endpoint, query, limit = 10) => {
  return `${endpoint}?query=${encodeURIComponent(query)}&limit=${limit}`;
};