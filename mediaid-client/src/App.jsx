import { BrowserRouter } from 'react-router-dom';
import './index.css'
import AppRoutes from './AppRoutes';

function App() {

  return (
    <>
     <BrowserRouter>
     <AppRoutes />
    </BrowserRouter>
    </>

  )
}

export default App
