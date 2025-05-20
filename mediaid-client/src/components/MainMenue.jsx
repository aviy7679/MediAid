import { useNavigate } from "react-router-dom";

export default function MainMenu() {
    const navigate = useNavigate();
    return (
        <div>
            <h1>How Can We Assist You Today?</h1>
            <br></br>
            <button>Emergency Diagnosis</button>
            <br />
            <button onClick={()=>navigate("/uploadUserData")}>Update and Modify Medical Information</button>
            <br />
            <button>Log Out</button>
            <br />
            <button onClick={()=>navigate("/fillUserData")}>Disease Lookup</button>
        </div>
    );
}