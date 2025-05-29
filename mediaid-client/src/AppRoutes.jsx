import React from 'react'
import { Route, Routes } from 'react-router-dom'
import LoginScreen from './components/LoginScreen'
import SignUp from './components/SignUp'
import UploadData from './components/UploadData'
import MainMenu from './components/MainMenue'
import FillUserData from './components/FillUserData'
import ProtectedRoute from './ProtectedRoute'

export default function AppRoutes() {
  return (
    <Routes>
        <Route path='/' element={<LoginScreen />}></Route>
        <Route path='/login' element={<LoginScreen />}></Route>
        <Route path='/signUp' element={<SignUp />}></Route>
        
        {/* Protected Routes - דורשים התחברות */}
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
    </Routes>
  )
}