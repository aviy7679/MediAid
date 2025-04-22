import React from 'react'
import { Route, Routes } from 'react-router-dom'
import LoginScreen from './components/LoginScreen'
import SignIn from './components/SignIn'
import App from './App'


export default function AppRoutes() {
  return (
    <Routes>
        <Route path='/' element={<LoginScreen />}></Route>
        <Route path='/login' element={<LoginScreen />}></Route>
        <Route path='/signIn' element={<SignIn />}></Route>
    </Routes>
  )
}