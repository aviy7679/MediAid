import React from 'react'
import { Route, Routes } from 'react-router-dom'
import LoginScreen from './components/LoginScreen'
import SignUp from './components/SignUp'
import UploadData from './components/UploadData'
import MainMenu from './components/MainMenue'
import FillUserData from './components/FillUserData'


export default function AppRoutes() {
  return (
    <Routes>
        <Route path='/' element={<LoginScreen />}></Route>
        <Route path='/login' element={<LoginScreen />}></Route>
        <Route path='/signUp' element={<SignUp />}></Route>
        <Route path='/uploadUserData' element={<UploadData />}></Route>
        <Route path='/homePage' element={<MainMenu />}></Route>
        <Route path='/fillUserData' element={<FillUserData />}></Route>
    </Routes>
  )
}