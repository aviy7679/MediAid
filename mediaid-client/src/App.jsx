import { useState } from 'react'
import { BrowserRouter } from 'react-router-dom';
import './index.css'
import LoginScreen from './components/LoginScreen'
import AppRoutes from './AppRoutes';

function App() {
  const [count, setCount] = useState(0)

  return (
    <>
     <BrowserRouter>
     <AppRoutes />
    </BrowserRouter>
    </>
  )
}

export default App
