import { BrowserRouter, Routes, Route, Navigate } from "react-router-dom";
import NotesPage from "./pages/NotesPage";
import Login from "./pages/Login";
import Signup from "./pages/Signup";
import OAuth2Success from "./pages/OAuth2Success";

/**
 * Protected Route Wrapper
 * Only allows access if user is authenticated
 */
const ProtectedRoute = ({ children }) => {
  const token = localStorage.getItem("accessToken");

  return token ? children : <Navigate to="/login" />;
};

/**
 * Home Redirect
 * Decides where "/" should go
 */
const HomeRedirect = () => {
  const token = localStorage.getItem("accessToken");

  return token ? <Navigate to="/notes" /> : <Navigate to="/login" />;
};

/**
 * Prevent logged-in users from accessing login/signup
 */
const PublicRoute = ({ children }) => {
  const token = localStorage.getItem("accessToken");

  return token ? <Navigate to="/notes" /> : children;
};

function App() {
  return (
    <BrowserRouter>
      <Routes>

        {/* Smart Entry Point */}
        <Route path="/" element={<HomeRedirect />} />

        {/* Login */}
        <Route
          path="/login"
          element={
            <PublicRoute>
              <Login />
            </PublicRoute>
          }
        />

        {/* Signup */}
        <Route
          path="/signup"
          element={
            <PublicRoute>
              <Signup />
            </PublicRoute>
          }
        />

        {/* OAuth Success */}
        <Route path="/oauth2/success" element={<OAuth2Success />} />

        {/* Protected Notes Page */}
        <Route
          path="/notes"
          element={
            <ProtectedRoute>
              <NotesPage />
            </ProtectedRoute>
          }
        />

      </Routes>
    </BrowserRouter>
  );
}

export default App;