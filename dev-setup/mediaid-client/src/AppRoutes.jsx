import React from 'react'
import { Route, Routes } from 'react-router-dom'
import LoginScreen from './components/LoginScreen'
import SignUp from './components/SignUp'
import UploadData from './components/UploadData'
import MainMenu from './components/MainMenue'
import FillUserData from './components/FillUserData'
import UserDataWizard from './components/UserDataWizard'
import UserProfile from './components/UserProfile'
import TreatmentGuidelines from './components/TreatmentGuidelines'
import ProtectedRoute from './ProtectedRoute'

export default function AppRoutes() {
  return (
    <Routes>
        <Route path='/' element={<LoginScreen />}></Route>
        <Route path='/login' element={<LoginScreen />}></Route>
        <Route path='/signUp' element={<SignUp />}></Route>
        
        {/* New User Setup Wizard - accessible without login for new users */}
        <Route path='/setup' element={<UserDataWizard />}></Route>
        
        {/* Protected Routes - require authentication */}
        <Route path='/uploadUserData' element={
            <ProtectedRoute>
                <UploadData />
            </ProtectedRoute>
        }></Route>
        
        <Route path='/homePage' element={
            <ProtectedRoute>
                <MainMenu />
            </ProtectedRoute>
        }></Route>
        
        <Route path='/fillUserData' element={
            <ProtectedRoute>
                <FillUserData />
            </ProtectedRoute>
        }></Route>

        <Route path='/profile' element={
            <ProtectedRoute>
                <UserProfile />
            </ProtectedRoute>
        }></Route>

        {/* Treatment Guidelines Route */}
        <Route path='/treatment-guidelines' element={
            <ProtectedRoute>
                <TreatmentGuidelines />
            </ProtectedRoute>
        }></Route>

        {/* Wizard can also be accessed by authenticated users to update their data */}
        <Route path='/update-profile' element={
            <ProtectedRoute>
                <UserDataWizard isUpdate={true} />
            </ProtectedRoute>
        }></Route>

        {/* Catch all route - redirect to login */}
        <Route path='*' element={<LoginScreen />}></Route>
    </Routes>
  )
}