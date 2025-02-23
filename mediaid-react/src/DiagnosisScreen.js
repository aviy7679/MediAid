import { useState, useRef } from "react";

export default function DiagnosisScreen() {
    const [stream, setStream] = useState(null);
    const videoRef = useRef(null);
    const [isRecording, setIsRecording] = useState(false);
    const [audioURL, setAudioURL] = useState(null);
    const mediaRecorderRef = useRef(null);
    const audioChunksRef = useRef([]);

    // Start Camera
    const startCamera = async () => {
        try {
            //בקשת הרשאה מהמשתמש לפתיחת מצלמה
            const userStream = await navigator.mediaDevices.getUserMedia({ video: true });
            videoRef.current.srcObject = userStream;
            setStream(userStream);
        } catch (error) {
            console.error("Error: ", error);
        }
    };

    // Stop Camera
    const stopCamera = () => {
        if (stream) {
            stream.getTracks().forEach(track => track.stop());
            setStream(null);
        }
    };
     // Start recording
     const startRecording = async () => {
        try {
            //בקשת הרשאה לפתיחת מיקרופון
            const stream = await navigator.mediaDevices.getUserMedia({ audio: true });
            mediaRecorderRef.current = new MediaRecorder(stream);
            audioChunksRef.current = [];

            mediaRecorderRef.current.ondataavailable = (event) => {
                if (event.data.size > 0) {
                    audioChunksRef.current.push(event.data);
                }
            };

            mediaRecorderRef.current.onstop = () => {
                const audioBlob = new Blob(audioChunksRef.current, { type: "audio/wav" });
                const url = URL.createObjectURL(audioBlob);
                setAudioURL(url);
            };

            mediaRecorderRef.current.start();
            setIsRecording(true);
        } catch (error) {
            console.error("Error accessing microphone:", error);
        }
    };

    // Stop recording
    const stopRecording = () => {
        if (mediaRecorderRef.current) {
            mediaRecorderRef.current.stop();
            setIsRecording(false);
        }
    };
    

    return (
        <div style={{  padding: "20px", maxWidth: "600px", margin: "auto" }}>
            <h1 style={{ textAlign: "center", color: "blue" }}>Medical Diagnosis</h1>

            {/* Textual Description */}
            <div style={{ textAlign: "center", marginBottom: "20px" }}>
                <h3>By Text Description</h3>
                <textarea placeholder="Enter a textual description" style={{ width: "80%", height: "60px", padding: "10px", border: "1px solid black" }} />
                <br />
                <button style={{ marginTop: "10px" }}>Diagnose</button>
            </div>

            <hr />

            {/* Diagnosis by Image */}
            <div style={{ textAlign: "center", marginBottom: "20px" }}>
                <h3>By Image</h3>
                <div style={{ display: "flex", justifyContent: "center", gap: "10px" }}>
                    <label htmlFor="myfile">Select a file:</label>
                    <input type="file" id="myfile" name="myfile" accept=".jpg, .jpeg, .png" /><br /><br />        
                    <button onClick={startCamera} disabled={stream}>Open Camera</button>
                </div>
                <video ref={videoRef} autoPlay playsInline style={{ width: "100%", maxWidth: "500px", border: "2px solid black", marginTop: "10px" }} />
                {stream && <button onClick={stopCamera} style={{ marginTop: "10px" }}>Turn Off Camera</button>}
            </div>

            <hr />

            {/* Diagnosis by Voice Recording */}
            <div style={{ textAlign: "center", marginBottom: "20px" }}>
                <h3>By Voice Recording</h3>
                <button onClick={startRecording} disabled={isRecording} style={{ marginRight: "10px" }}>
                Start Recording
            </button>
            <button onClick={stopRecording} disabled={!isRecording}>
                Stop Recording
            </button>

            {audioURL && (
                <div style={{ marginTop: "20px" }}>
                    <h3>Recorded Audio:</h3>
                    <audio controls src={audioURL}></audio>
                </div>
            )}            
            </div>
        </div>
    );
}
