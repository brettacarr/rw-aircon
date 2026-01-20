import axios from 'axios'

// Create axios instance with default config
const apiClient = axios.create({
  baseURL: '/api',
  headers: {
    'Content-Type': 'application/json',
  },
  timeout: 10000,
})

// Response interceptor for error handling
apiClient.interceptors.response.use(
  (response) => response,
  (error) => {
    // Extract error message from response if available
    const message = error.response?.data?.message || error.message || 'An unexpected error occurred'
    console.error('API Error:', message)
    return Promise.reject(error)
  }
)

export default apiClient
