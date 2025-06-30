import { useState, useEffect } from "react";
import { Camera, Mic, Upload, FileText, ArrowLeft, CheckCircle, AlertCircle, X } from 'lucide-react';
import { API_ENDPOINTS } from '../apiConfig';
import TreatmentPlanDisplay from './TreatmentPlanDisplay';

export default function UploadData() {
    // Basic information
    const [data, setData] = useState({ text: "", image: null, audio: null });
    const [isRecording, setIsRecording] = useState(false);
    
    // Camera state
    const [cameraActive, setCameraActive] = useState(false);
    const [videoElement, setVideoElement] = useState(null);
    const [cameraStream, setCameraStream] = useState(null);
    const [capturedImage, setCapturedImage] = useState(null);
    
    // Audio recording state
    const [audioRecorder, setAudioRecorder] = useState(null);
    const [audioChunks, setAudioChunks] = useState([]);
    const [audioURL, setAudioURL] = useState(null);

    // Error and status management
    const [cameraError, setCameraError] = useState("");
    const [audioError, setAudioError] = useState("");
    const [uploadStatus, setUploadStatus] = useState("");
    const [isUploading, setIsUploading] = useState(false);
    const [uploadSuccess, setUploadSuccess] = useState(false);
    
    // Server response data
    const [serverResponse, setServerResponse] = useState(null);
    const [showTreatmentPlan, setShowTreatmentPlan] = useState(false);

    // Navigation function
    const navigate = (path) => {
        window.location.href = path;
    };

    // Initialize when component mounts
    useEffect(() => {
        setVideoElement(document.getElementById('camera-view'));
        
        // Cleanup resources when component unmounts
        return () => {
            if (cameraStream) {
                cameraStream.getTracks().forEach(track => track.stop());
            }
            
            if (audioRecorder && audioRecorder.state !== 'inactive') {
                audioRecorder.stop();
            }
            
            if (audioURL) {
                URL.revokeObjectURL(audioURL);
            }
            
            if (capturedImage) {
                URL.revokeObjectURL(capturedImage);
            }
        };
    }, []);

    const handleTextChange = (event) => {
        setData(prevData => ({ ...prevData, text: event.target.value }));
    };

    // Handle image upload
    const handleImageUpload = (event) => {
        const file = event.target.files[0];
        if (file) {
            // Check file size (maximum 10MB)
            if (file.size > 10 * 1024 * 1024) {
                alert('File size too large. Please select a file smaller than 10MB.');
                return;
            }

            setData(prevData => ({ ...prevData, image: file }));
            
            if (capturedImage) {
                URL.revokeObjectURL(capturedImage);
            }
            setCapturedImage(null);
            stopCamera();
        }
    };

    // Start camera
    const startCamera = async () => {
        setCameraError("");
        
        if (capturedImage) {
            setCapturedImage(null);
        }
        
        if (!videoElement) {
            setCameraError("Video element not found. Try refreshing the page.");
            return;
        }
        
        try {
            const stream = await navigator.mediaDevices.getUserMedia({ 
                video: { width: 1280, height: 720 },
                audio: false
            });
            
            videoElement.srcObject = stream;
            setCameraStream(stream);
            setCameraActive(true);
            videoElement.style.display = 'block';
            
        } catch (error) {
            console.error("Camera error:", error);
            setCameraError(`Could not access camera: ${error.message}`);
        }
    };

    // Stop camera
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

    // Capture image from camera
    const captureImage = () => {
        if (!videoElement || !cameraActive) {
            setCameraError("Camera is not active");
            return;
        }

        try {
            const canvas = document.createElement('canvas');
            canvas.width = videoElement.videoWidth;
            canvas.height = videoElement.videoHeight;
            
            const ctx = canvas.getContext('2d');
            ctx.drawImage(videoElement, 0, 0, canvas.width, canvas.height);
            
            canvas.toBlob((blob) => {
                if (blob) {
                    const file = new File([blob], "camera-capture.jpg", { type: "image/jpeg" });
                    setData(prevData => ({ ...prevData, image: file }));
                    
                    const imageURL = URL.createObjectURL(blob);
                    setCapturedImage(imageURL);
                    
                    stopCamera();
                } else {
                    setCameraError("Failed to capture image");
                }
            }, 'image/jpeg', 0.9);
        } catch (error) {
            setCameraError(`Error capturing image: ${error.message}`);
        }
    };

    // Start audio recording
    const startRecording = async () => {
        setAudioError("");
        setAudioChunks([]);
        
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
                const audioBlob = new Blob(audioChunks, { type: 'audio/mp3' });
                const audioFile = new File([audioBlob], "audio-recording.mp3", { type: "audio/mp3" });
                
                setData(prevData => ({ ...prevData, audio: audioFile }));
                
                const url = URL.createObjectURL(audioBlob);
                setAudioURL(url);
                
                stream.getTracks().forEach(track => track.stop());
            };
            
            setAudioChunks([]);
            recorder.start();
            setAudioRecorder(recorder);
            setIsRecording(true);
        } catch (error) {
            setAudioError(`Could not access microphone: ${error.message}`);
        }
    };

    // Stop audio recording
    const stopRecording = () => {
        if (audioRecorder && audioRecorder.state !== 'inactive') {
            audioRecorder.stop();
            setIsRecording(false);
        }
    };

    // Submit data to server
    const handleSubmit = async () => {
        setUploadStatus("");
        setUploadSuccess(false);
        setServerResponse(null);
        setShowTreatmentPlan(false);
        
        if (!data.text && !data.image && !data.audio) {
            setUploadStatus("Please provide at least one form of medical data (text, image, or audio).");
            return;
        }

        // בדיקת אימות
        const token = localStorage.getItem('mediaid_token');
        if (!token) {
            setUploadStatus("Authentication required. Please log in again.");
            navigate('/login');
            return;
        }

        const formData = new FormData();
        if (data.text) formData.append("text", data.text);
        if (data.image) formData.append("image", data.image);
        if (data.audio) formData.append("audio", data.audio);

        try {
            setIsUploading(true);
            setUploadStatus("Uploading and analyzing your medical data...");

            const response = await fetch(API_ENDPOINTS.UPLOAD_DATA, {
                method: "POST",
                headers: {
                    'Authorization': `Bearer ${token}`
                },
                body: formData
            });

            if (response.ok) {
                const responseData = await response.json();
                console.log('Server response:', responseData);
                
                setServerResponse(responseData);
                
                if (responseData.success) {
                    setUploadStatus("Analysis completed successfully!");
                    setUploadSuccess(true);
                    
                    // אם יש תכנית טיפול, הצג אותה
                    if (responseData.treatmentPlan) {
                        setTimeout(() => {
                            setShowTreatmentPlan(true);
                        }, 1000);
                    } else {
                        setUploadStatus("Data processed successfully, but no specific treatment plan was generated.");
                    }
                } else {
                    setUploadStatus(`Analysis failed: ${responseData.message || 'Unknown error'}`);
                }
                
            } else if (response.status === 401) {
                localStorage.removeItem('mediaid_token');
                setUploadStatus("Session expired. Please log in again.");
                navigate('/login');
            } else if (response.status === 403) {
                setUploadStatus("Access denied. Please ensure you are logged in properly.");
            } else {
                const errorText = await response.text();
                setUploadStatus(`Upload failed (${response.status}): ${errorText}`);
            }
        } catch (error) {
            console.error('Upload error:', error);
            setUploadStatus(`Error during upload: ${error.message}`);
        } finally {
            setIsUploading(false);
        }
    };

    // Clear all data
    const clearAllData = () => {
        setData({ text: "", image: null, audio: null });
        setUploadStatus("");
        setUploadSuccess(false);
        setServerResponse(null);
        setShowTreatmentPlan(false);
        
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
            {/* Treatment Plan Modal */}
            {showTreatmentPlan && serverResponse && (
                <TreatmentPlanDisplay
                    treatmentPlan={serverResponse.treatmentPlan}
                    extractedSymptoms={serverResponse.extractedSymptoms}
                    onClose={() => setShowTreatmentPlan(false)}
                />
            )}

            {/* Header */}
            <div className="bg-white shadow-sm border-b border-gray-200">
                <div className="max-w-7xl mx-auto px-8 py-6">
                    <div className="flex items-center justify-between">
                        <div className="flex items-center">
                            <button
                                onClick={() => navigate('/homePage')}
                                className="mr-6 p-3 text-gray-500 hover:text-gray-700 rounded-lg hover:bg-gray-100 transition-colors"
                            >
                                <ArrowLeft className="w-6 h-6" />
                            </button>
                            <div>
                                <h1 className="text-3xl font-bold text-blue-600">Medical Data Upload & Analysis</h1>
                                <p className="text-xl text-gray-600 mt-2">Upload medical documents, images, or recordings for AI-powered analysis</p>
                            </div>
                        </div>
                        {(data.text || data.image || data.audio) && (
                            <button
                                onClick={clearAllData}
                                className="px-6 py-3 text-red-600 border-2 border-red-300 rounded-xl hover:bg-red-50 transition-colors font-medium"
                            >
                                Clear All
                            </button>
                        )}
                    </div>
                </div>
            </div>

            <div className="max-w-7xl mx-auto px-8 py-12">
                {/* Success Banner */}
                {uploadSuccess && (
                    <div className="mb-12 p-6 bg-green-50 border-2 border-green-200 rounded-xl flex items-center">
                        <CheckCircle className="w-8 h-8 text-green-600 mr-4" />
                        <div className="flex-1">
                            <p className="text-green-800 font-semibold text-lg">Analysis Complete!</p>
                            <p className="text-green-700">
                                Your medical data has been processed successfully.
                                {serverResponse?.treatmentPlan && " Click below to view your treatment recommendations."}
                            </p>
                        </div>
                        {serverResponse?.treatmentPlan && (
                            <button
                                onClick={() => setShowTreatmentPlan(true)}
                                className="ml-6 px-6 py-3 bg-green-600 text-white rounded-xl hover:bg-green-700 transition-colors font-medium"
                            >
                                View Treatment Plan
                            </button>
                        )}
                    </div>
                )}

                <div className="grid grid-cols-1 xl:grid-cols-2 gap-12">
                    {/* Text Input Section */}
                    <div className="bg-white rounded-xl shadow-lg p-8 border border-gray-200">
                        <div className="flex items-center mb-6">
                            <FileText className="w-8 h-8 text-blue-600 mr-4" />
                            <h3 className="text-2xl font-semibold text-gray-900">Text Description</h3>
                        </div>
                        <textarea 
                            placeholder="Describe your symptoms, medical concerns, or provide any relevant medical information..."
                            className="w-full h-48 p-6 border-2 border-gray-300 rounded-xl focus:ring-2 focus:ring-blue-500 focus:border-transparent resize-none text-lg"
                            onChange={handleTextChange} 
                            value={data.text}
                        />
                        <p className="text-gray-500 mt-4">Provide detailed information about your symptoms or medical situation.</p>
                        {data.text && (
                            <p className="text-sm text-gray-400 mt-2">Characters: {data.text.length}</p>
                        )}
                    </div>
                    
                    {/* Image Upload & Camera Section */}
                    <div className="bg-white rounded-xl shadow-lg p-8 border border-gray-200">
                        <div className="flex items-center mb-6">
                            <Camera className="w-8 h-8 text-blue-600 mr-4" />
                            <h3 className="text-2xl font-semibold text-gray-900">Medical Images</h3>
                        </div>
                        
                        {/* File Upload */}
                        <div className="mb-8">
                            <label htmlFor="myfile" className="block text-lg font-medium text-gray-700 mb-4">
                                Upload Medical Document or Image
                            </label>
                            <input 
                                type="file" 
                                id="myfile" 
                                name="myfile" 
                                accept="image/*,application/pdf" 
                                onChange={handleImageUpload}
                                className="block w-full text-lg text-gray-500 file:mr-6 file:py-3 file:px-6 file:rounded-xl file:border-0 file:font-medium file:bg-blue-50 file:text-blue-700 hover:file:bg-blue-100 transition-colors"
                            />
                            <p className="text-gray-500 mt-2">Supported formats: JPG, PNG, PDF (Max 10MB)</p>
                        </div>
                        
                        {/* Camera Controls */}
                        <div className="mb-6">
                            <p className="text-lg font-medium text-gray-700 mb-4">Or take a photo with your camera:</p>
                            {!cameraActive ? (
                                <button 
                                    onClick={startCamera} 
                                    className="flex items-center px-6 py-3 bg-blue-600 text-white rounded-xl hover:bg-blue-700 transition-colors font-medium text-lg"
                                >
                                    <Camera className="w-6 h-6 mr-3" />
                                    Open Camera
                                </button>
                            ) : (
                                <div className="flex gap-4">
                                    <button 
                                        onClick={captureImage} 
                                        className="flex items-center px-6 py-3 bg-green-600 text-white rounded-xl hover:bg-green-700 transition-colors font-medium text-lg"
                                    >
                                        <Camera className="w-6 h-6 mr-3" />
                                        Take Picture
                                    </button>
                                    <button 
                                        onClick={stopCamera} 
                                        className="px-6 py-3 text-gray-700 border-2 border-gray-300 rounded-xl hover:bg-gray-50 transition-colors font-medium text-lg"
                                    >
                                        Cancel
                                    </button>
                                </div>
                            )}
                        </div>
                        
                        {/* Camera Error Message */}
                        {cameraError && (
                            <div className="mb-6 p-4 bg-red-50 border-2 border-red-200 rounded-xl">
                                <div className="flex items-center">
                                    <AlertCircle className="w-5 h-5 text-red-600 mr-3" />
                                    <p className="text-red-700">{cameraError}</p>
                                </div>
                            </div>
                        )}
                        
                        {/* Camera View */}
                        <div className="mb-6">
                            <video 
                                id="camera-view"
                                autoPlay 
                                playsInline
                                className={`w-full border-4 border-gray-300 rounded-xl ${cameraActive ? 'block' : 'hidden'}`}
                            />
                        </div>
                        
                        {/* Display Captured Image */}
                        {capturedImage && (
                            <div className="mb-6">
                                <p className="text-lg font-medium text-gray-700 mb-4">Captured Image:</p>
                                <img 
                                    src={capturedImage} 
                                    alt="Captured" 
                                    className="w-full border-4 border-green-300 rounded-xl" 
                                />
                            </div>
                        )}
                        
                        {/* Show selected file name */}
                        {data.image && !capturedImage && (
                            <div className="p-4 bg-blue-50 border-2 border-blue-200 rounded-xl">
                                <p className="text-blue-800 font-medium">📎 Selected: {data.image.name}</p>
                            </div>
                        )}
                    </div>
                </div>

                {/* Audio Recording Section */}
                <div className="mt-12 bg-white rounded-xl shadow-lg p-8 border border-gray-200">
                    <div className="flex items-center mb-6">
                        <Mic className="w-8 h-8 text-blue-600 mr-4" />
                        <h3 className="text-2xl font-semibold text-gray-900">Voice Recording</h3>
                    </div>
                    
                    <div className="grid grid-cols-1 lg:grid-cols-2 gap-8">
                        <div>
                            {/* Recording Controls */}
                            <div className="mb-6">
                                {!isRecording ? (
                                    <button 
                                        onClick={startRecording} 
                                        className="flex items-center px-6 py-4 bg-red-600 text-white rounded-xl hover:bg-red-700 transition-colors font-medium text-lg"
                                    >
                                        <Mic className="w-6 h-6 mr-3" />
                                        Start Recording
                                    </button>
                                ) : (
                                    <div className="flex items-center gap-6">
                                        <button 
                                            onClick={stopRecording} 
                                            className="flex items-center px-6 py-4 bg-gray-600 text-white rounded-xl hover:bg-gray-700 transition-colors font-medium text-lg"
                                        >
                                            <div className="w-4 h-4 bg-white rounded-sm mr-3"></div>
                                            Stop Recording
                                        </button>
                                        <div className="flex items-center text-red-600">
                                            <div className="w-4 h-4 bg-red-600 rounded-full animate-pulse mr-3"></div>
                                            <span className="font-medium text-lg">Recording...</span>
                                        </div>
                                    </div>
                                )}
                            </div>
                            
                            {/* Audio Error Message */}
                            {audioError && (
                                <div className="mb-6 p-4 bg-red-50 border-2 border-red-200 rounded-xl">
                                    <div className="flex items-center">
                                        <AlertCircle className="w-5 h-5 text-red-600 mr-3" />
                                        <p className="text-red-700">{audioError}</p>
                                    </div>
                                </div>
                            )}
                        </div>

                        {/* Audio Player */}
                        {audioURL && (
                            <div className="p-6 bg-gray-50 border-2 border-gray-200 rounded-xl">
                                <p className="text-lg font-medium text-gray-700 mb-4">Recorded Audio:</p>
                                <audio 
                                    controls 
                                    src={audioURL}
                                    className="w-full"
                                />
                            </div>
                        )}
                    </div>
                </div>
                
                {/* Submit Button and Status */}
                <div className="mt-12 bg-white rounded-xl shadow-lg p-8 border border-gray-200">
                    <button 
                        onClick={handleSubmit} 
                        disabled={isUploading || (!data.text && !data.image && !data.audio)}
                        className="w-full flex items-center justify-center px-8 py-6 bg-blue-600 text-white font-semibold text-xl rounded-xl hover:bg-blue-700 disabled:bg-gray-400 disabled:cursor-not-allowed transition-colors"
                    >
                        {isUploading ? (
                            <>
                                <div className="animate-spin rounded-full h-8 w-8 border-b-2 border-white mr-4"></div>
                                Analyzing Medical Data...
                            </>
                        ) : (
                            <>
                                <Upload className="w-8 h-8 mr-4" />
                                Upload & Analyze Medical Data
                            </>
                        )}
                    </button>
                    
                    {/* Upload Status */}
                    {uploadStatus && (
                        <div className={`mt-6 p-6 rounded-xl ${
                            uploadStatus.includes("success") || uploadSuccess
                                ? "bg-green-50 border-2 border-green-200" 
                                : uploadStatus.includes("failed") || uploadStatus.includes("Error") || uploadStatus.includes("denied")
                                ? "bg-red-50 border-2 border-red-200"
                                : "bg-blue-50 border-2 border-blue-200"
                        }`}>
                            <p className={`text-lg ${
                                uploadStatus.includes("success") || uploadSuccess
                                    ? "text-green-700" 
                                    : uploadStatus.includes("failed") || uploadStatus.includes("Error") || uploadStatus.includes("denied")
                                    ? "text-red-700"
                                    : "text-blue-700"
                            }`}>
                                {uploadStatus}
                            </p>
                        </div>
                    )}

                    {/* Analysis Summary */}
                    {uploadSuccess && serverResponse && (
                        <div className="mt-6 p-6 bg-blue-50 border-2 border-blue-200 rounded-xl">
                            <h4 className="font-semibold text-blue-900 mb-3 text-lg">Analysis Summary:</h4>
                            <div className="space-y-2 text-blue-800">
                                {serverResponse.extractedSymptoms && (
                                    <p>• Found {serverResponse.extractedSymptoms.length} symptoms</p>
                                )}
                                {serverResponse.treatmentPlan && (
                                    <p>• Generated treatment plan with {serverResponse.treatmentPlan.urgencyLevel} priority</p>
                                )}
                                {serverResponse.processedInputs && (
                                    <p>• Processed: {serverResponse.processedInputs.join(', ')}</p>
                                )}
                            </div>
                        </div>
                    )}

                    {/* Quick Actions after upload */}
                    {uploadSuccess && (
                        <div className="mt-6 flex gap-4">
                            {serverResponse?.treatmentPlan && (
                                <button
                                    onClick={() => setShowTreatmentPlan(true)}
                                    className="flex-1 px-6 py-3 bg-purple-600 text-white rounded-xl hover:bg-purple-700 transition-colors font-medium"
                                >
                                    View Full Treatment Plan
                                </button>
                            )}
                            <button
                                onClick={() => navigate('/treatment-guidelines')}
                                className="flex-1 px-6 py-3 border-2 border-gray-300 text-gray-700 rounded-xl hover:bg-gray-50 transition-colors font-medium"
                            >
                                Browse General Guidelines
                            </button>
                        </div>
                    )}
                </div>
            </div>
        </div>
    );
}