import { useState, useEffect } from "react";

export default function UploadData() {
    // מידע בסיסי
    const [data, setData] = useState({ text: "", image: null, audio: null });
    const [isRecording, setIsRecording] = useState(false);
    
    // מצב המצלמה
    const [cameraActive, setCameraActive] = useState(false);
    const [videoElement, setVideoElement] = useState(null);
    const [cameraStream, setCameraStream] = useState(null);
    const [capturedImage, setCapturedImage] = useState(null);
    
    // מצב הקלטת האודיו
    const [audioRecorder, setAudioRecorder] = useState(null);
    const [audioChunks, setAudioChunks] = useState([]);
    const [audioURL, setAudioURL] = useState(null);

    // ניהול שגיאות
    const [cameraError, setCameraError] = useState("");
    const [audioError, setAudioError] = useState("");
    const [uploadStatus, setUploadStatus] = useState("");

    // מופעל כשהקומפוננטה עולה - לקבל הפניה לאלמנט הוידאו
    useEffect(() => {
        // מאתחל הפניה לאלמנט הוידאו אחרי שהקומפוננטה מוצגת
        setVideoElement(document.getElementById('camera-view'));
        
        // ניקוי משאבים כשהקומפוננטה יורדת
        return () => {
            // עוצר את המצלמה
            if (cameraStream) {
                cameraStream.getTracks().forEach(track => track.stop());
            }
            
            // עוצר את מקליט האודיו
            if (audioRecorder && audioRecorder.state !== 'inactive') {
                audioRecorder.stop();
            }
            
            // ניקוי URL של האודיו
            if (audioURL) {
                URL.revokeObjectURL(audioURL);
            }
            
            // ניקוי URL של התמונה
            if (capturedImage) {
                URL.revokeObjectURL(capturedImage);
            }
        };
    }, []);

//עדכון הערך של הטקסט באוביקט הdata בעת שינוי
    const handleTextChange = (event) => {
        setData(prevData => ({ ...prevData, text: event.target.value }));
    };

    // טיפול בהעלאת תמונה
    const handleImageUpload = (event) => {
        const file = event.target.files[0];
        if (file) {
            setData(prevData => ({ ...prevData, image: file }));
            
            //ניקוי הכתובת שנשמרה לתמונה כדי למנוע בזבוז של הזכרון
            if (capturedImage) {
                URL.revokeObjectURL(capturedImage);
            }
            setCapturedImage(null);
            //כיבוי
            stopCamera();
        }
    };

    // הפעלת המצלמה
    const startCamera = async () => {
        setCameraError("");//איפוס שגיאות קודמות
        
        // אם יש תמונה שנלכדה קודם, מסתירים אותה
        if (capturedImage) {
            setCapturedImage(null);
        }
        
        if (!videoElement) {
            setCameraError("Video element not found. Try refreshing the page.");
            return;
        }
        
        try {//בקשת גישה מהמשתמש לשימוש במצלמה
            const stream = await navigator.mediaDevices.getUserMedia({ 
                video: true,
                audio: false
            });
            
            videoElement.srcObject = stream;//מחברים את הזרם לוידיאו
            setCameraStream(stream);//שומרים את הזרם
            setCameraActive(true);//המצלמה פועלת
            
            // הבטחה שהאלמנט יהיה גלוי
            videoElement.style.display = 'block';
            
        } catch (error) {
            console.error("Camera error:", error);
            setCameraError(`Could not access camera: ${error.message}`);
        }
    };

    // כיבוי המצלמה
    const stopCamera = () => {
        if (cameraStream) {
            cameraStream.getTracks().forEach(track => track.stop());
            
            if (videoElement) {
                videoElement.srcObject = null;
                videoElement.style.display = 'none';
            }
            
            setCameraStream(null);
            setCameraActive(false);
        }
    };

    // צילום תמונה מהמצלמה
    const captureImage = () => {
        if (!videoElement || !cameraActive) {
            setCameraError("Camera is not active");
            return;
        }

        try {
            const canvas = document.createElement('canvas');
            canvas.width = videoElement.videoWidth;
            canvas.height = videoElement.videoHeight;
            //מדפיס את התמונה על הקנבס
            const ctx = canvas.getContext('2d');
            ctx.drawImage(videoElement, 0, 0, canvas.width, canvas.height);
            
            canvas.toBlob((blob) => {
                if (blob) {
                    const file = new File([blob], "camera-capture.jpg", { type: "image/jpeg" });
                    setData(prevData => ({ ...prevData, image: file }));
                    
                    // יצירת URL לתמונה
                    const imageURL = URL.createObjectURL(blob);
                    setCapturedImage(imageURL);
                    
                    // כיבוי המצלמה לאחר לכידת התמונה
                    stopCamera();
                } else {
                    setCameraError("Failed to capture image");
                }
            }, 'image/jpeg', 0.9);
        } catch (error) {
            setCameraError(`Error capturing image: ${error.message}`);
        }
    };

    // התחלת הקלטת אודיו
    const startRecording = async () => {
        setAudioError("");
        setAudioChunks([]);
        
        // אם יש URL קודם של אודיו, נשחרר את המשאב
        if (audioURL) {
            URL.revokeObjectURL(audioURL);
            setAudioURL(null);
        }
        
        try {
            const stream = await navigator.mediaDevices.getUserMedia({ audio: true });
            const recorder = new MediaRecorder(stream);
            
            recorder.ondataavailable = (event) => {
                if (event.data.size > 0) {
                    setAudioChunks(chunks => [...chunks, event.data]);
                }
            };
            
            recorder.onstop = () => {
                // איסוף כל חלקי האודיו
                const audioBlob = new Blob(audioChunks, { type: 'audio/mp3' });
                const audioFile = new File([audioBlob], "audio-recording.mp3", { type: "audio/mp3" });
                
                // שמירת הקובץ בנתונים
                setData(prevData => ({ ...prevData, audio: audioFile }));
                
                // יצירת URL חדש לנגן האודיו
                const url = URL.createObjectURL(audioBlob);
                setAudioURL(url);
                
                // עצירת הזרם
                stream.getTracks().forEach(track => track.stop());
            };
            
            // התחלת ההקלטה
            setAudioChunks([]);
            recorder.start();
            setAudioRecorder(recorder);
            setIsRecording(true);
        } catch (error) {
            setAudioError(`Could not access microphone: ${error.message}`);
        }
    };

    // עצירת הקלטת אודיו
    const stopRecording = () => {
        if (audioRecorder && audioRecorder.state !== 'inactive') {
            audioRecorder.stop();
            setIsRecording(false);
        }
    };

    // שליחת הנתונים לשרת
    const handleSubmit = async () => {
        setUploadStatus("");
        
        const formData = new FormData();
        if (data.text) formData.append("text", data.text);
        if (data.image) formData.append("image", data.image);
        if (data.audio) formData.append("audio", data.audio);
        
        try {
            setUploadStatus("Uploading...");
            
            const response = await fetch('http://localhost:8080/uploadData', {
                method: "POST",
                body: formData
            });
            
            if (response.ok) {
                setUploadStatus("Data uploaded successfully!");
                // ניקוי הטופס
                setData({ text: "", image: null, audio: null });
                
                // ניקוי משאבים
                if (capturedImage) {
                    URL.revokeObjectURL(capturedImage);
                    setCapturedImage(null);
                }
                if (audioURL) {
                    URL.revokeObjectURL(audioURL);
                    setAudioURL(null);
                }
            } else {
                const errorText = await response.text();
                setUploadStatus(`Upload failed: ${errorText}`);
            }
        } catch (error) {
            setUploadStatus(`Error during upload: ${error.message}`);
        }
    };

    return (
        <div style={{ padding: "20px", maxWidth: "600px", margin: "auto" }}>
            <h1 style={{ textAlign: "center", color: "blue" }}>Medical Diagnosis</h1>
            
            {/* Text Input Section */}
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
            
            {/* Image Upload & Camera Section */}
            <div style={{ textAlign: "center", marginBottom: "20px" }}>
                <h3>By Image</h3>
                
                {/* File Upload */}
                <div style={{ marginBottom: "15px" }}>
                    <label htmlFor="myfile">Select a file: </label>
                    <input 
                        type="file" 
                        id="myfile" 
                        name="myfile" 
                        accept="image/*" 
                        onChange={handleImageUpload} 
                    />
                </div>
                
                {/* Camera Controls */}
                <div style={{ marginBottom: "15px" }}>
                    {!cameraActive ? (
                        <button 
                            onClick={startCamera} 
                            style={{ padding: "8px 15px", backgroundColor: "#4CAF50", color: "white", border: "none", borderRadius: "4px", cursor: "pointer" }}
                        >
                            Open Camera
                        </button>
                    ) : (
                        <div>
                            <button 
                                onClick={captureImage} 
                                style={{ padding: "8px 15px", backgroundColor: "#2196F3", color: "white", border: "none", borderRadius: "4px", cursor: "pointer", marginRight: "10px" }}
                            >
                                Take Picture
                            </button>
                            <button 
                                onClick={stopCamera} 
                                style={{ padding: "8px 15px", backgroundColor: "#f44336", color: "white", border: "none", borderRadius: "4px", cursor: "pointer" }}
                            >
                                Close Camera
                            </button>
                        </div>
                    )}
                </div>
                
                {/* Camera Error Message */}
                {cameraError && (
                    <div style={{ color: "red", margin: "10px 0", fontWeight: "bold" }}>
                        {cameraError}
                    </div>
                )}
                
                {/* Camera View - מוצג רק כאשר המצלמה פעילה */}
                <div style={{ margin: "15px auto", maxWidth: "500px" }}>
                    <video 
                        id="camera-view"
                        autoPlay 
                        playsInline
                        style={{ 
                            width: "100%", 
                            border: "2px solid #666", 
                            borderRadius: "4px",
                            display: cameraActive ? "block" : "none"
                        }}
                    />
                </div>
                
                {/* Display Captured Image - מוצג רק כאשר יש תמונה שנלכדה וכשהמצלמה לא פעילה */}
                {capturedImage && (
                    <div style={{ margin: "15px auto", maxWidth: "500px" }}>
                        <h4>Captured Image:</h4>
                        <img 
                            src={capturedImage} 
                            alt="Captured" 
                            style={{ 
                                width: "100%", 
                                border: "2px solid #4CAF50", 
                                borderRadius: "4px" 
                            }} 
                        />
                    </div>
                )}
                
                {/* Show selected file name - מוצג רק כאשר נבחר קובץ ואין תמונה מהמצלמה */}
                {data.image && !capturedImage && (
                    <div style={{ margin: "10px 0", fontWeight: "bold" }}>
                        Selected: {data.image.name}
                    </div>
                )}
            </div>
            <hr />
            
            {/* Audio Recording Section */}
            <div style={{ textAlign: "center", marginBottom: "20px" }}>
                <h3>By Voice Recording</h3>
                
                {/* Recording Controls */}
                {!isRecording ? (
                    <button 
                        onClick={startRecording} 
                        style={{ 
                            padding: "8px 15px", 
                            backgroundColor: "#4CAF50", 
                            color: "white", 
                            border: "none", 
                            borderRadius: "4px", 
                            cursor: "pointer" 
                        }}
                    >
                        Start Recording
                    </button>
                ) : (
                    <button 
                        onClick={stopRecording} 
                        style={{ 
                            padding: "8px 15px", 
                            backgroundColor: "#f44336", 
                            color: "white", 
                            border: "none", 
                            borderRadius: "4px", 
                            cursor: "pointer" 
                        }}
                    >
                        Stop Recording
                    </button>
                )}
                
                {/* Audio Error Message */}
                {audioError && (
                    <div style={{ color: "red", margin: "10px 0", fontWeight: "bold" }}>
                        {audioError}
                    </div>
                )}
                
                {/* Audio Player - משתמש ב-audioURL במקום לייצר URL חדש בכל פעם */}
                {audioURL && (
                    <div style={{ margin: "15px auto", maxWidth: "500px" }}>
                        <h4>Recorded Audio</h4>
                        <audio 
                            controls 
                            src={audioURL}
                            style={{ 
                                width: "100%", 
                                borderRadius: "4px",
                                margin: "10px 0"
                            }}
                        />
                    </div>
                )}
            </div>
            <hr />
            
            {/* Submit Button */}
            <div style={{ textAlign: "center", marginBottom: "20px" }}>
                <button 
                    onClick={handleSubmit} 
                    style={{ 
                        padding: "12px 25px", 
                        fontSize: "16px", 
                        backgroundColor: "#4CAF50", 
                        color: "white", 
                        border: "none", 
                        borderRadius: "4px", 
                        cursor: "pointer",
                        fontWeight: "bold"
                    }}
                >
                    Submit Diagnosis Data
                </button>
                
                {/* Upload Status */}
                {uploadStatus && (
                    <div style={{ 
                        margin: "15px 0", 
                        padding: "10px", 
                        borderRadius: "4px", 
                        backgroundColor: uploadStatus.includes("success") ? "#dff0d8" : "#f2dede",
                        color: uploadStatus.includes("success") ? "#3c763d" : "#a94442"
                    }}>
                        {uploadStatus}
                    </div>
                )}
            </div>
        </div>
    );
}