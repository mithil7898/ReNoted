// Home.jsx - Home Page Component
// 
// Purpose: Landing page that displays backend status
// Tests connection between frontend and backend

import { useState, useEffect } from 'react';
import { healthCheck, getWelcome } from '../services/api';

function Home() {
  // React State - stores data that can change
  // When state changes, component re-renders
  
  const [healthStatus, setHealthStatus] = useState(null);  // Backend health data
  const [welcomeMessage, setWelcomeMessage] = useState(''); // Welcome message
  const [loading, setLoading] = useState(true);   // Loading indicator
  const [error, setError] = useState(null);       // Error messages

  // useEffect - Runs when component mounts (loads)
  // Like componentDidMount in class components
  useEffect(() => {
    // Async function to fetch data from backend
    const fetchData = async () => {
      try {
        setLoading(true);
        
        // Call both API endpoints
        const health = await healthCheck();
        const welcome = await getWelcome();
        
        // Update state with responses
        setHealthStatus(health);
        setWelcomeMessage(welcome);
        setError(null);  // Clear any previous errors
        
      } catch (err) {
        // If API call fails, set error message
        setError('Failed to connect to backend. Is the server running?');
        console.error('API Error:', err);
      } finally {
        // Always set loading to false (success or failure)
        setLoading(false);
      }
    };

    // Call the async function
    fetchData();
  }, []); // Empty array = run once when component mounts

  // Render loading state
  if (loading) {
    return (
      <div className="min-h-screen bg-gradient-to-br from-blue-50 to-indigo-100 flex items-center justify-center">
        <div className="text-center">
          <div className="animate-spin rounded-full h-16 w-16 border-b-4 border-indigo-600 mx-auto"></div>
          <p className="mt-4 text-gray-600 text-lg">Connecting to backend...</p>
        </div>
      </div>
    );
  }

  // Render error state
  if (error) {
    return (
      <div className="min-h-screen bg-gradient-to-br from-red-50 to-pink-100 flex items-center justify-center p-4">
        <div className="bg-white rounded-lg shadow-xl p-8 max-w-md w-full">
          <div className="text-center">
            <div className="text-6xl mb-4">❌</div>
            <h2 className="text-2xl font-bold text-red-600 mb-4">Connection Failed</h2>
            <p className="text-gray-700 mb-6">{error}</p>
            <div className="bg-red-50 border border-red-200 rounded-lg p-4 text-left">
              <p className="text-sm text-red-800 font-semibold mb-2">Troubleshooting:</p>
              <ul className="text-sm text-red-700 space-y-1">
                <li>• Is the Spring Boot backend running?</li>
                <li>• Check: http://localhost:8080/api/health</li>
                <li>• Look for errors in IntelliJ console</li>
              </ul>
            </div>
            <button 
              onClick={() => window.location.reload()} 
              className="mt-6 bg-red-600 text-white px-6 py-2 rounded-lg hover:bg-red-700 transition"
            >
              Retry Connection
            </button>
          </div>
        </div>
      </div>
    );
  }

  // Render success state
  return (
    <div className="min-h-screen bg-gradient-to-br from-blue-50 to-indigo-100">
      {/* Header */}
      <header className="bg-white shadow-sm">
        <div className="max-w-7xl mx-auto px-4 py-6">
          <h1 className="text-3xl font-bold text-indigo-600">ReNoted</h1>
          <p className="text-gray-600">Your Notion-like Note Taking App</p>
        </div>
      </header>

      {/* Main Content */}
      <main className="max-w-7xl mx-auto px-4 py-12">
        <div className="grid md:grid-cols-2 gap-6">
          
          {/* Welcome Card */}
          <div className="bg-white rounded-lg shadow-lg p-6 transform hover:scale-105 transition">
            <div className="text-4xl mb-4">👋</div>
            <h2 className="text-2xl font-bold text-gray-800 mb-4">Welcome</h2>
            <p className="text-gray-600 text-lg">{welcomeMessage}</p>
          </div>

          {/* Backend Status Card */}
          <div className="bg-white rounded-lg shadow-lg p-6 transform hover:scale-105 transition">
            <div className="text-4xl mb-4">✅</div>
            <h2 className="text-2xl font-bold text-gray-800 mb-4">Backend Status</h2>
            
            {healthStatus && (
              <div className="space-y-3">
                <div className="flex items-center">
                  <span className="font-semibold text-gray-700 w-24">Status:</span>
                  <span className="px-3 py-1 bg-green-100 text-green-800 rounded-full text-sm font-semibold">
                    {healthStatus.status}
                  </span>
                </div>
                
                <div className="flex items-center">
                  <span className="font-semibold text-gray-700 w-24">Version:</span>
                  <span className="text-gray-600">{healthStatus.version}</span>
                </div>
                
                <div className="flex items-start">
                  <span className="font-semibold text-gray-700 w-24">Message:</span>
                  <span className="text-gray-600 flex-1">{healthStatus.message}</span>
                </div>
                
                <div className="flex items-start">
                  <span className="font-semibold text-gray-700 w-24">Time:</span>
                  <span className="text-gray-600 text-sm flex-1">{healthStatus.timestamp}</span>
                </div>
              </div>
            )}
          </div>

        </div>

        {/* Connection Success Message */}
        <div className="mt-12 bg-green-50 border border-green-200 rounded-lg p-6">
          <div className="flex items-center">
            <div className="text-3xl mr-4">🎉</div>
            <div>
              <h3 className="text-xl font-bold text-green-800 mb-2">
                Frontend-Backend Connection Successful!
              </h3>
              <p className="text-green-700">
                Your React frontend is successfully communicating with your Spring Boot backend.
                <strong> v0.1 is complete!</strong>
              </p>
            </div>
          </div>
        </div>

      </main>
    </div>
  );
}

export default Home;