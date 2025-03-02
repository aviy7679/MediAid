import { useState, useRef } from "react";

export default function DiagnosisScreen() {
    const [data, setData] = useState({ text: "", image: null, audio: null });
    const videoRef = useRef(null);
    const mediaRecorderRef = useRef(null);
    const audioChunksRef = useRef([]);
    const [isRecording, setIsRecording] = useState(false);
    const [stream, setStream] = useState(null);
    const [capturedImage, setCapturedImage] = useState(null);

    const handleTextChange = (event) => {
        setData(prevData => ({ ...prevData, text: event.target.value }));
    };

    const handleImageUpload = (event) => {
        const file = event.target.files[0];
        if (file) {
            setData(prevData => ({ ...prevData, image: file }));
        }
    };

    const startCamera = async () => {
        try {
            const userStream = await navigator.mediaDevices.getUserMedia({ video: true });
            videoRef.current.srcObject = userStream;
            setStream(userStream);
        } catch (error) {
            console.error("Error accessing camera:", error);
        }
    };

    const stopCamera = () => {
        if (stream) {
            stream.getTracks().forEach(track => track.stop());
            setStream(null);
        }
    };

    const captureImage = () => {
        if (videoRef.current && stream) {
            const canvas = document.createElement('canvas');
            canvas.width = videoRef.current.videoWidth;
            canvas.height = videoRef.current.videoHeight;
            const ctx = canvas.getContext('2d');
            ctx.drawImage(videoRef.current, 0, 0);
            
            // Convert to a file
            canvas.toBlob((blob) => {
                const file = new File([blob], "camera-capture.jpg", { type: "image/jpeg" });
                setData(prevData => ({ ...prevData, image: file }));
                setCapturedImage(URL.createObjectURL(blob));
            }, 'image/jpeg');
        }
    };

    const startRecording = async () => {
        try {
            const stream = await navigator.mediaDevices.getUserMedia({ audio: true });
            mediaRecorderRef.current = new MediaRecorder(stream);
            audioChunksRef.current = [];
            mediaRecorderRef.current.ondataavailable = (event) => {
                if (event.data.size > 0) {
                    audioChunksRef.current.push(event.data);
                }
            };
            mediaRecorderRef.current.onstop = () => {
                const audioBlob = new Blob(audioChunksRef.current, { type: "audio/mpeg" });
                const audioFile = new File([audioBlob], "audio-recording.mp3", { type: "audio/mpeg" });
                setData(prevData => ({ ...prevData, audio: audioFile }));
            };
            mediaRecorderRef.current.start();
            setIsRecording(true);
        } catch (error) {
            console.error("Error accessing microphone:", error);
        }
    };

    const stopRecording = () => {
        if (mediaRecorderRef.current && mediaRecorderRef.current.state !== "inactive") {
            mediaRecorderRef.current.stop();
            mediaRecorderRef.current.stream.getTracks().forEach(track => track.stop());
            setIsRecording(false);
        }
    };

    const handleSubmit = async () => {
        const formData = new FormData();
        if (data.text) formData.append("text", data.text);
        if (data.image) formData.append("image", data.image);
        if (data.audio) formData.append("audio", data.audio);
        
        try {
            console.log("Sending form data:", {
                text: data.text,
                image: data.image ? data.image.name : "No image",
                audio: data.audio ? data.audio.name : "No audio"
            });
            
            const response = await fetch('http://localhost:8080/uploadData', {
                method: "POST",
                body: formData,
                // לא להוסיף 'Content-Type' header כדי שהדפדפן יוסיף אותו עם boundary
            });
            
            if (response.ok) {
                alert("Data uploaded successfully");
                console.log("Data uploaded successfully");
                // ניקוי הטופס
                setData({ text: "", image: null, audio: null });
                setCapturedImage(null);
            } else {
                const errorText = await response.text();
                console.error("Failed to upload data:", errorText);
                alert("Failed to upload data: " + errorText);
            }
        } catch (error) {
            console.error("Error uploading data:", error);
            alert("Error uploading data: " + error.message);
        }
    };

    return (
        <div style={{ padding: "20px", maxWidth: "600px", margin: "auto" }}>
            <h1 style={{ textAlign: "center", color: "blue" }}>Medical Diagnosis</h1>
            <div style={{ textAlign: "center", marginBottom: "20px" }}>
                <h3>By Text Description</h3>
                <textarea 
                    placeholder="Enter a textual description"
                    style={{ width: "80%", height: "60px", padding: "10px", border: "1px solid black" }} 
                    onChange={handleTextChange} 
                    value={data.text}
                />
            </div>
            <hr />
            <div style={{ textAlign: "center", marginBottom: "20px" }}>
                <h3>By Image</h3>
                <div style={{ display: "flex", justifyContent: "center", gap: "10px", flexWrap: "wrap" }}>
                    <div>
                        <label htmlFor="myfile">Select a file:</label>
                        <input type="file" id="myfile" name="myfile" accept=".jpg, .jpeg, .png" onChange={handleImageUpload} />
                    </div>
                    <div>
                        <button onClick={startCamera} disabled={stream}>Open Camera</button>
                        {stream && (
                            <>
                                <button onClick={captureImage} style={{ marginLeft: "10px" }}>Capture Image</button>
                                <button onClick={stopCamera} style={{ marginLeft: "10px" }}>Turn Off Camera</button>
                            </>
                        )}
                    </div>
                </div>
                {stream && (
                    <video 
                        ref={videoRef} 
                        autoPlay 
                        playsInline 
                        style={{ width: "100%", maxWidth: "500px", border: "2px solid black", marginTop: "10px" }} 
                    />
                )}
                {capturedImage && (
                    <div style={{ marginTop: "10px" }}>
                        <h4>Captured Image:</h4>
                        <img 
                            src={capturedImage} 
                            alt="Captured" 
                            style={{ width: "100%", maxWidth: "500px", border: "2px solid green" }} 
                        />
                    </div>
                )}
                {data.image && !capturedImage && (
                    <div style={{ marginTop: "10px" }}>
                        <h4>Selected Image: {data.image.name}</h4>
                    </div>
                )}
            </div>
            <hr />
            <div style={{ textAlign: "center", marginBottom: "20px" }}>
                <h3>By Voice Recording</h3>
                <button onClick={startRecording} disabled={isRecording} style={{ marginRight: "10px" }}>
                    Start Recording
                </button>
                <button onClick={stopRecording} disabled={!isRecording}>
                    Stop Recording
                </button>
                {data.audio && (
                    <div style={{ marginTop: "10px" }}>
                        <h4>Recorded Audio</h4>
                        <audio controls>
                            <source src={URL.createObjectURL(data.audio)} type="audio/mpeg" />
                            Your browser does not support the audio element.
                        </audio>
                    </div>
                )}
            </div>
            <hr />
            <div style={{ textAlign: "center", marginBottom: "20px" }}>
                <button 
                    onClick={handleSubmit} 
                    style={{ 
                        padding: "10px 20px", 
                        fontSize: "16px", 
                        background: "green", 
                        color: "white", 
                        border: "none", 
                        cursor: "pointer" 
                    }}
                >
                    Submit Diagnosis Data
                </button>
            </div>
        </div>
    );
}