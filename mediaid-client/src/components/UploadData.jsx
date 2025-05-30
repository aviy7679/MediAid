import { useState, useEffect } from "react";
import { Camera, Mic, Upload, FileText, ArrowLeft, CheckCircle, AlertCircle } from 'lucide-react';
import { API_ENDPOINTS } from '../apiConfig';

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

    // ניהול שגיאות וסטטוס
    const [cameraError, setCameraError] = useState("");
    const [audioError, setAudioError] = useState("");
    const [uploadStatus, setUploadStatus] = useState("");
    const [isUploading, setIsUploading] = useState(false);
    const [uploadSuccess, setUploadSuccess] = useState(false);
    const [ocrResult, setOcrResult] = useState("");
    const [treatmentGuidelines, setTreatmentGuidelines] = useState([]);
    const [showGuidelines, setShowGuidelines] = useState(false);

    // Navigation function
    const navigate = (path) => {
        window.location.href = path;
    };

    // מופעל כשהקומפוננטה עולה - לקבל הפניה לאלמנט הוידאו
    useEffect(() => {
        // מאתחל הפניה לאלמנט הוידאו אחרי שהקומפוננטה מוצגת
        setVideoElement(document.getElementById('camera-view'));
        
        // ניקוי משאבים כשהקומפוננטה יורדת
        return () => {
            // עוצר את המצלמה
            if (cameraStream) {
                //מעבר על כל הערוצים של הזרם: וידיאו, אודיו ועצירת כולם.
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
            // בדיקת גודל קובץ (מקסימום 10MB)
            if (file.size > 10 * 1024 * 1024) {
                alert('File size too large. Please select a file smaller than 10MB.');
                return;
            }

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
                video: { width: 1280, height: 720 },
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
            const stream = await navigator.mediaDevices.getUserMedia({ 
                audio: { 
                    echoCancellation: true,
                    noiseSuppression: true,
                    sampleRate: 44100
                } 
            });
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

    // // מידע נוסף למצב
    // const [treatmentGuidelines, setTreatmentGuidelines] = useState([]);
    // const [showGuidelines, setShowGuidelines] = useState(false);

    // שליחת הנתונים לשרת
    const handleSubmit = async () => {
        setUploadStatus("");
        setOcrResult("");
        setUploadSuccess(false);
        setTreatmentGuidelines([]);
        setShowGuidelines(false);
        
        if (!data.text && !data.image && !data.audio) {
            setUploadStatus("Please provide at least one form of medical data (text, image, or audio).");
            return;
        }
    
        const formData = new FormData();
        if (data.text) formData.append("text", data.text);
        if (data.image) formData.append("image", data.image);
        if (data.audio) formData.append("audio", data.audio);
    
        try {
            setIsUploading(true);
            setUploadStatus("Uploading and processing your medical data...");
    
            const response = await fetch(API_ENDPOINTS.UPLOAD_DATA, {
                method: "POST",
                body: formData
            });
    
            if (response.ok) {
                const responseText = await response.text();
                
                try {
                    // ניסיון לפרסר JSON אם השרת מחזיר JSON
                    const responseData = JSON.parse(responseText);
                    
                    if (responseData.guidelines && responseData.guidelines.length > 0) {
                        setTreatmentGuidelines(responseData.guidelines);
                        setShowGuidelines(true);
                        setUploadStatus("Analysis complete! Here are your personalized treatment recommendations:");
                    } else {
                        setUploadStatus("Data processed successfully, but no specific guidelines were generated.");
                    }
                    
                    if (responseData.ocrResult) {
                        setOcrResult(responseData.ocrResult);
                    }
                } catch (jsonError) {
                    // אם השרת לא מחזיר JSON, נטפל בתגובה הישנה
                    setUploadStatus("Data uploaded and processed successfully!");
                    
                    // חילוץ תוצאת OCR אם קיימת
                    if (responseText.includes("OCR Result:")) {
                        const ocrText = responseText.split("OCR Result: ")[1];
                        setOcrResult(ocrText);
                    }
                    
                    // נתונים דמה עד שהשרת יעודכן
                    setTimeout(() => {
                        setTreatmentGuidelines(generateMockGuidelines(data.text, ocrResult));
                        setShowGuidelines(true);
                        setUploadStatus("Analysis complete! Here are your treatment recommendations:");
                    }, 1000);
                }
                
                setUploadSuccess(true);
                
            } else {
                const errorText = await response.text();
                setUploadStatus(`Upload failed: ${errorText}`);
            }
        } catch (error) {
            setUploadStatus(`Error during upload: ${error.message}`);
        } finally {
            setIsUploading(false);
        }
    };

    // פונקציה לייצור הנחיות דמה בהתבסס על הטקסט שהמשתמש הקליד
    const generateMockGuidelines = (text, ocr) => {
        const allSymptoms = (text + " " + ocr).toLowerCase();
        const guidelines = [];

        if (allSymptoms.includes('headache') || allSymptoms.includes('כאב ראש')) {
            guidelines.push({
                id: 1,
                title: "Headache Management",
                priority: "medium",
                category: "Pain Management",
                recommendations: [
                    "Rest in a quiet, dark room",
                    "Apply cold compress to forehead for 15-20 minutes",
                    "Stay hydrated - drink plenty of water",
                    "Over-the-counter pain relief: Ibuprofen 400mg or Paracetamol 1000mg",
                    "Avoid loud noises and bright lights"
                ],
                warnings: ["If headache persists over 24 hours or worsens, seek medical attention"],
                followUp: "Monitor symptoms for 24-48 hours"
            });
        }

        if (allSymptoms.includes('fever') || allSymptoms.includes('חום')) {
            guidelines.push({
                id: 2,
                title: "Fever Management",
                priority: "high",
                category: "General Care",
                recommendations: [
                    "Monitor temperature every 2-4 hours",
                    "Increase fluid intake - water, herbal teas, broths",
                    "Rest and avoid strenuous activities",
                    "Paracetamol 1000mg every 6 hours (max 4g/day)",
                    "Light clothing and cool environment",
                    "Lukewarm sponge bath if temperature >38.5°C"
                ],
                warnings: ["Seek immediate medical attention if temperature >39.5°C or if accompanied by severe symptoms"],
                followUp: "If fever persists >3 days or worsens, consult healthcare provider"
            });
        }

        if (allSymptoms.includes('cough') || allSymptoms.includes('שיעול')) {
            guidelines.push({
                id: 3,
                title: "Cough Treatment",
                priority: "medium",
                category: "Respiratory",
                recommendations: [
                    "Increase fluid intake to thin mucus",
                    "Honey and warm water (1-2 teaspoons honey in warm water)",
                    "Use humidifier or inhale steam from hot shower",
                    "Avoid irritants like smoke and strong odors",
                    "Sleep with head elevated",
                    "Consider cough suppressant if dry cough interferes with sleep"
                ],
                warnings: ["Seek medical attention if cough produces blood, persists >3 weeks, or is accompanied by high fever"],
                followUp: "Monitor for improvement over 7-10 days"
            });
        }

        // הנחיה כללית אם לא זוהו סימפטומים ספציפיים
        if (guidelines.length === 0) {
            guidelines.push({
                id: 4,
                title: "General Health Monitoring",
                priority: "low",
                category: "Preventive Care",
                recommendations: [
                    "Monitor your symptoms closely",
                    "Maintain a healthy diet and stay hydrated",
                    "Get adequate rest (7-9 hours of sleep)",
                    "Light exercise if feeling well",
                    "Practice good hygiene"
                ],
                warnings: ["Contact healthcare provider if symptoms worsen or new symptoms develop"],
                followUp: "Consider follow-up consultation if symptoms persist or change"
            });
        }

        return guidelines;
    };

    // Clear all data
    const clearAllData = () => {
        setData({ text: "", image: null, audio: null });
        setUploadStatus("");
        setOcrResult("");
        setUploadSuccess(false);
        setTreatmentGuidelines([]);
        setShowGuidelines(false);
        
        if (capturedImage) {
            URL.revokeObjectURL(capturedImage);
            setCapturedImage(null);
        }
        if (audioURL) {
            URL.revokeObjectURL(audioURL);
            setAudioURL(null);
        }
        stopCamera();
        if (isRecording) stopRecording();
    };

    return (
        <div className="min-h-screen bg-gray-50">
            {/* Header */}
            <div className="bg-white shadow-sm border-b border-gray-200">
                <div className="max-w-4xl mx-auto px-4 py-4">
                    <div className="flex items-center justify-between">
                        <div className="flex items-center">
                            <button
                                onClick={() => navigate('/homePage')}
                                className="mr-4 p-2 text-gray-500 hover:text-gray-700 rounded-lg hover:bg-gray-100"
                            >
                                <ArrowLeft className="w-5 h-5" />
                            </button>
                            <div>
                                <h1 className="text-2xl font-bold text-blue-600">Medical Data Upload</h1>
                                <p className="text-gray-600">Upload medical documents, images, or recordings for analysis</p>
                            </div>
                        </div>
                        {(data.text || data.image || data.audio) && (
                            <button
                                onClick={clearAllData}
                                className="px-4 py-2 text-red-600 border border-red-300 rounded-lg hover:bg-red-50 transition-colors"
                            >
                                Clear All
                            </button>
                        )}
                    </div>
                </div>
            </div>

            <div className="max-w-4xl mx-auto px-4 py-8">
                {/* Success Banner */}
                {uploadSuccess && (
                    <div className="mb-6 p-4 bg-green-50 border border-green-200 rounded-lg flex items-center">
                        <CheckCircle className="w-5 h-5 text-green-600 mr-2" />
                        <div className="flex-1">
                            <p className="text-green-800 font-medium">Analysis Complete!</p>
                            <p className="text-green-700 text-sm">
                                {showGuidelines 
                                    ? "Your personalized treatment recommendations are displayed below."
                                    : "Your medical data has been processed successfully."
                                }
                            </p>
                        </div>
                        {showGuidelines && (
                            <button
                                onClick={() => {
                                    const guidelinesSection = document.querySelector('[data-guidelines]');
                                    if (guidelinesSection) {
                                        guidelinesSection.scrollIntoView({ behavior: 'smooth' });
                                    }
                                }}
                                className="ml-4 px-3 py-1 bg-green-600 text-white rounded text-sm hover:bg-green-700 transition-colors"
                            >
                                View Recommendations
                            </button>
                        )}
                    </div>
                )}

                {/* Text Input Section */}
                <div className="mb-8 bg-white rounded-lg shadow-sm p-6 border border-gray-200">
                    <div className="flex items-center mb-4">
                        <FileText className="w-5 h-5 text-blue-600 mr-2" />
                        <h3 className="text-lg font-semibold text-gray-900">Text Description</h3>
                    </div>
                    <textarea 
                        placeholder="Describe your symptoms, medical concerns, or provide any relevant medical information..."
                        className="w-full h-32 p-4 border border-gray-300 rounded-lg focus:ring-2 focus:ring-blue-500 focus:border-transparent resize-none"
                        onChange={handleTextChange} 
                        value={data.text}
                    />
                    <p className="text-sm text-gray-500 mt-2">Provide detailed information about your symptoms or medical situation.</p>
                </div>
                
                {/* Image Upload & Camera Section */}
                <div className="mb-8 bg-white rounded-lg shadow-sm p-6 border border-gray-200">
                    <div className="flex items-center mb-4">
                        <Camera className="w-5 h-5 text-blue-600 mr-2" />
                        <h3 className="text-lg font-semibold text-gray-900">Medical Images</h3>
                    </div>
                    
                    {/* File Upload */}
                    <div className="mb-6">
                        <label htmlFor="myfile" className="block text-sm font-medium text-gray-700 mb-2">
                            Upload Medical Document or Image
                        </label>
                        <input 
                            type="file" 
                            id="myfile" 
                            name="myfile" 
                            accept="image/*,application/pdf" 
                            onChange={handleImageUpload}
                            className="block w-full text-sm text-gray-500 file:mr-4 file:py-2 file:px-4 file:rounded-lg file:border-0 file:text-sm file:font-medium file:bg-blue-50 file:text-blue-700 hover:file:bg-blue-100"
                        />
                        <p className="text-sm text-gray-500 mt-1">Supported formats: JPG, PNG, PDF (Max 10MB)</p>
                    </div>
                    
                    {/* Camera Controls */}
                    <div className="mb-4">
                        <p className="text-sm font-medium text-gray-700 mb-2">Or take a photo with your camera:</p>
                        {!cameraActive ? (
                            <button 
                                onClick={startCamera} 
                                className="flex items-center px-4 py-2 bg-blue-600 text-white rounded-lg hover:bg-blue-700 transition-colors"
                            >
                                <Camera className="w-4 h-4 mr-2" />
                                Open Camera
                            </button>
                        ) : (
                            <div className="flex gap-3">
                                <button 
                                    onClick={captureImage} 
                                    className="flex items-center px-4 py-2 bg-green-600 text-white rounded-lg hover:bg-green-700 transition-colors"
                                >
                                    <Camera className="w-4 h-4 mr-2" />
                                    Take Picture
                                </button>
                                <button 
                                    onClick={stopCamera} 
                                    className="px-4 py-2 text-gray-700 border border-gray-300 rounded-lg hover:bg-gray-50 transition-colors"
                                >
                                    Cancel
                                </button>
                            </div>
                        )}
                    </div>
                    
                    {/* Camera Error Message */}
                    {cameraError && (
                        <div className="mb-4 p-3 bg-red-50 border border-red-200 rounded-lg">
                            <div className="flex items-center">
                                <AlertCircle className="w-4 h-4 text-red-600 mr-2" />
                                <p className="text-red-700 text-sm">{cameraError}</p>
                            </div>
                        </div>
                    )}
                    
                    {/* Camera View */}
                    <div className="mb-4">
                        <video 
                            id="camera-view"
                            autoPlay 
                            playsInline
                            className={`w-full max-w-md border-2 border-gray-300 rounded-lg ${cameraActive ? 'block' : 'hidden'}`}
                        />
                    </div>
                    
                    {/* Display Captured Image */}
                    {capturedImage && (
                        <div className="mb-4">
                            <p className="text-sm font-medium text-gray-700 mb-2">Captured Image:</p>
                            <img 
                                src={capturedImage} 
                                alt="Captured" 
                                className="w-full max-w-md border-2 border-green-300 rounded-lg" 
                            />
                        </div>
                    )}
                    
                    {/* Show selected file name */}
                    {data.image && !capturedImage && (
                        <div className="p-3 bg-blue-50 border border-blue-200 rounded-lg">
                            <p className="text-blue-800 text-sm font-medium">Selected: {data.image.name}</p>
                        </div>
                    )}
                </div>
                
                {/* Audio Recording Section */}
                <div className="mb-8 bg-white rounded-lg shadow-sm p-6 border border-gray-200">
                    <div className="flex items-center mb-4">
                        <Mic className="w-5 h-5 text-blue-600 mr-2" />
                        <h3 className="text-lg font-semibold text-gray-900">Voice Recording</h3>
                    </div>
                    
                    {/* Recording Controls */}
                    <div className="mb-4">
                        {!isRecording ? (
                            <button 
                                onClick={startRecording} 
                                className="flex items-center px-4 py-2 bg-red-600 text-white rounded-lg hover:bg-red-700 transition-colors"
                            >
                                <Mic className="w-4 h-4 mr-2" />
                                Start Recording
                            </button>
                        ) : (
                            <div className="flex items-center gap-3">
                                <button 
                                    onClick={stopRecording} 
                                    className="flex items-center px-4 py-2 bg-gray-600 text-white rounded-lg hover:bg-gray-700 transition-colors"
                                >
                                    <div className="w-2 h-2 bg-white rounded-full mr-2"></div>
                                    Stop Recording
                                </button>
                                <div className="flex items-center text-red-600">
                                    <div className="w-2 h-2 bg-red-600 rounded-full animate-pulse mr-2"></div>
                                    <span className="text-sm font-medium">Recording...</span>
                                </div>
                            </div>
                        )}
                    </div>
                    
                    {/* Audio Error Message */}
                    {audioError && (
                        <div className="mb-4 p-3 bg-red-50 border border-red-200 rounded-lg">
                            <div className="flex items-center">
                                <AlertCircle className="w-4 h-4 text-red-600 mr-2" />
                                <p className="text-red-700 text-sm">{audioError}</p>
                            </div>
                        </div>
                    )}
                    
                    {/* Audio Player */}
                    {audioURL && (
                        <div className="mb-4 p-4 bg-gray-50 border border-gray-200 rounded-lg">
                            <p className="text-sm font-medium text-gray-700 mb-2">Recorded Audio:</p>
                            <audio 
                                controls 
                                src={audioURL}
                                className="w-full"
                            />
                        </div>
                    )}
                </div>
                
                {/* Submit Button and Status */}
                <div className="bg-white rounded-lg shadow-sm p-6 border border-gray-200">
                    <button 
                        onClick={handleSubmit} 
                        disabled={isUploading || (!data.text && !data.image && !data.audio)}
                        className="w-full flex items-center justify-center px-6 py-3 bg-blue-600 text-white font-medium rounded-lg hover:bg-blue-700 disabled:bg-gray-400 disabled:cursor-not-allowed transition-colors"
                    >
                        {isUploading ? (
                            <>
                                <div className="animate-spin rounded-full h-5 w-5 border-b-2 border-white mr-2"></div>
                                Processing...
                            </>
                        ) : (
                            <>
                                <Upload className="w-5 h-5 mr-2" />
                                Upload & Analyze Medical Data
                            </>
                        )}
                    </button>
                    
                    {/* Upload Status */}
                    {uploadStatus && (
                        <div className={`mt-4 p-4 rounded-lg ${
                            uploadStatus.includes("success") || uploadSuccess
                                ? "bg-green-50 border border-green-200" 
                                : uploadStatus.includes("failed") || uploadStatus.includes("Error")
                                ? "bg-red-50 border border-red-200"
                                : "bg-blue-50 border border-blue-200"
                        }`}>
                            <p className={`text-sm ${
                                uploadStatus.includes("success") || uploadSuccess
                                    ? "text-green-700" 
                                    : uploadStatus.includes("failed") || uploadStatus.includes("Error")
                                    ? "text-red-700"
                                    : "text-blue-700"
                            }`}>
                                {uploadStatus}
                            </p>
                        </div>
                    )}
                    
                    {/* OCR Result Section */}
                    {ocrResult && (
                        <div className="mt-4 p-4 bg-blue-50 border border-blue-200 rounded-lg">
                            <h4 className="font-medium text-blue-900 mb-2">Extracted Text from Image:</h4>
                            <p className="text-blue-800 text-sm whitespace-pre-wrap">{ocrResult}</p>
                        </div>
                    )}

                    {/* Treatment Guidelines Display */}
                    {showGuidelines && treatmentGuidelines.length > 0 && (
                        <div className="mt-6 space-y-4" data-guidelines>
                            <div className="flex items-center justify-between mb-4">
                                <h3 className="text-lg font-semibold text-gray-900">📋 Treatment Recommendations</h3>
                                <button
                                    onClick={() => setShowGuidelines(false)}
                                    className="text-sm text-gray-500 hover:text-gray-700"
                                >
                                    Hide
                                </button>
                            </div>
                            
                            {treatmentGuidelines.map((guideline) => (
                                <div key={guideline.id} className="border border-gray-200 rounded-lg p-5 bg-white">
                                    <div className="flex items-start justify-between mb-3">
                                        <div>
                                            <h4 className="font-semibold text-gray-900 text-lg">{guideline.title}</h4>
                                            <div className="flex items-center gap-2 mt-1">
                                                <span className={`px-2 py-1 rounded text-xs font-medium ${
                                                    guideline.priority === 'high' ? 'bg-red-100 text-red-800' :
                                                    guideline.priority === 'medium' ? 'bg-yellow-100 text-yellow-800' :
                                                    'bg-green-100 text-green-800'
                                                }`}>
                                                    {guideline.priority} priority
                                                </span>
                                                <span className="text-sm text-gray-500">{guideline.category}</span>
                                            </div>
                                        </div>
                                    </div>
                                    
                                    {/* Recommendations */}
                                    <div className="mb-4">
                                        <h5 className="font-medium text-gray-800 mb-2">✅ Recommended Actions:</h5>
                                        <ul className="space-y-1">
                                            {guideline.recommendations.map((rec, index) => (
                                                <li key={index} className="text-sm text-gray-700 flex items-start">
                                                    <span className="text-green-600 mr-2 mt-1">•</span>
                                                    {rec}
                                                </li>
                                            ))}
                                        </ul>
                                    </div>
                                    
                                    {/* Warnings */}
                                    {guideline.warnings && guideline.warnings.length > 0 && (
                                        <div className="mb-4 p-3 bg-red-50 border border-red-200 rounded">
                                            <h5 className="font-medium text-red-800 mb-2">⚠️ Important Warnings:</h5>
                                            <ul className="space-y-1">
                                                {guideline.warnings.map((warning, index) => (
                                                    <li key={index} className="text-sm text-red-700 flex items-start">
                                                        <span className="text-red-600 mr-2 mt-1">•</span>
                                                        {warning}
                                                    </li>
                                                ))}
                                            </ul>
                                        </div>
                                    )}
                                    
                                    {/* Follow-up */}
                                    {guideline.followUp && (
                                        <div className="p-3 bg-blue-50 border border-blue-200 rounded">
                                            <h5 className="font-medium text-blue-800 mb-1">📅 Follow-up:</h5>
                                            <p className="text-sm text-blue-700">{guideline.followUp}</p>
                                        </div>
                                    )}
                                </div>
                            ))}
                            
                            <div className="mt-6 p-4 bg-gray-50 border border-gray-200 rounded-lg">
                                <p className="text-sm text-gray-600 text-center">
                                    <strong>Disclaimer:</strong> These recommendations are for informational purposes only and should not replace professional medical advice. 
                                    Always consult with a qualified healthcare provider for proper diagnosis and treatment.
                                </p>
                            </div>
                        </div>
                    )}

                    {/* Quick Actions after upload */}
                    {uploadSuccess && !showGuidelines && (
                        <div className="mt-4 flex gap-3">
                            <button
                                onClick={() => navigate('/treatment-guidelines')}
                                className="flex-1 px-4 py-2 bg-purple-600 text-white rounded-lg hover:bg-purple-700 transition-colors text-sm"
                            >
                                View General Guidelines
                            </button>
                            <button
                                onClick={() => navigate('/profile')}
                                className="flex-1 px-4 py-2 border border-gray-300 text-gray-700 rounded-lg hover:bg-gray-50 transition-colors text-sm"
                            >
                                Update Profile
                            </button>
                        </div>
                    )}
                </div>
            </div>
        </div>
    );
}