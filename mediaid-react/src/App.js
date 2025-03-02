import './App.css';
import { BrowserRouter as Router, Routes, Route } from "react-router-dom";
import LoginScreen from './loginScreen';
import MainMenu from './mainMenu';
import DiagnosisScreen from './DiagnosisScreen';
import SignIn from './signIn';

function App() {
  return (
    <Router>
      <Routes>
        <Route path="/" element={<LoginScreen />} />
        <Route path="/signIn" element={<SignIn />} />
        <Route path="/login" element={<LoginScreen />} />
        <Route path="/uploadData" element={<DiagnosisScreen />} />
      </Routes>
    </Router>
  );
}

export default App;
