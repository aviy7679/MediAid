import { Navigate } from "react-router-dom"
import { auth } from "./authUtils"

const ProtectedRoute = ({ children }) => {
    if(!auth.isAuthenticated()) {
        return  <Navigate to="/" replace />
    }
    return children;
}

export default ProtectedRoute;