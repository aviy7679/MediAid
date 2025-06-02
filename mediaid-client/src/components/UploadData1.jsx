// // import { useState, useEffect } from "react";

// // export default function UploadData() {
// //     // מידע בסיסי
// //     const [data, setData] = useState({ text: "", image: null, audio: null });
// //     const [isRecording, setIsRecording] = useState(false);
    
// //     // מצב המצלמה
// //     const [cameraActive, setCameraActive] = useState(false);
// //     const [videoElement, setVideoElement] = useState(null);
// //     const [cameraStream, setCameraStream] = useState(null);
// //     const [capturedImage, setCapturedImage] = useState(null);
    
// //     // מצב הקלטת האודיו
// //     const [audioRecorder, setAudioRecorder] = useState(null);
// //     const [audioChunks, setAudioChunks] = useState([]);
// //     const [audioURL, setAudioURL] = useState(null);

// //     // ניהול שגיאות
// //     const [cameraError, setCameraError] = useState("");
// //     const [audioError, setAudioError] = useState("");
// //     const [uploadStatus, setUploadStatus] = useState("");

// //     // מופעל כשהקומפוננטה עולה - לקבל הפניה לאלמנט הוידאו
// //     useEffect(() => {
// //         // מאתחל הפניה לאלמנט הוידאו אחרי שהקומפוננטה מוצגת
// //         setVideoElement(document.getElementById('camera-view'));
        
// //         // ניקוי משאבים כשהקומפוננטה יורדת
// //         return () => {
// //             // עוצר את המצלמה
// //             if (cameraStream) {
// //                 //מעבר על כל הערוצים של הזרם: וידיאו, אודיו ועצירת כולם.
// //                 cameraStream.getTracks().forEach(track => track.stop());
// //             }
            
// //             // עוצר את מקליט האודיו
// //             if (audioRecorder && audioRecorder.state !== 'inactive') {
// //                 audioRecorder.stop();
// //             }
            
// //             // ניקוי URL של האודיו
// //             if (audioURL) {
// //                 URL.revokeObjectURL(audioURL);
// //             }
            
// //             // ניקוי URL של התמונה
// //             if (capturedImage) {
// //                 URL.revokeObjectURL(capturedImage);
// //             }
// //         };
// //     }, []);

// // //עדכון הערך של הטקסט באוביקט הdata בעת שינוי
// //     const handleTextChange = (event) => {
// //         setData(prevData => ({ ...prevData, text: event.target.value }));
// //     };

// //     // טיפול בהעלאת תמונה
// //     const handleImageUpload = (event) => {
// //         const file = event.target.files[0];
// //         if (file) {
// //             setData(prevData => ({ ...prevData, image: file }));
            
// //             //ניקוי הכתובת שנשמרה לתמונה כדי למנוע בזבוז של הזכרון
// //             if (capturedImage) {
// //                 URL.revokeObjectURL(capturedImage);
// //             }
// //             setCapturedImage(null);
// //             //כיבוי
// //             stopCamera();
// //         }
// //     };

// //     // הפעלת המצלמה
// //     const startCamera = async () => {
// //         setCameraError("");//איפוס שגיאות קודמות
        
// //         // אם יש תמונה שנלכדה קודם, מסתירים אותה
// //         if (capturedImage) {
// //             setCapturedImage(null);
// //         }
        
// //         if (!videoElement) {
// //             setCameraError("Video element not found. Try refreshing the page.");
// //             return;
// //         }
        
// //         try {//בקשת גישה מהמשתמש לשימוש במצלמה
// //             const stream = await navigator.mediaDevices.getUserMedia({ 
// //                 video: true,
// //                 audio: false
// //             });
            
// //             videoElement.srcObject = stream;//מחברים את הזרם לוידיאו
// //             setCameraStream(stream);//שומרים את הזרם
// //             setCameraActive(true);//המצלמה פועלת
            
// //             // הבטחה שהאלמנט יהיה גלוי
// //             videoElement.style.display = 'block';
            
// //         } catch (error) {
// //             console.error("Camera error:", error);
// //             setCameraError(`Could not access camera: ${error.message}`);
// //         }
// //     };

// //     // כיבוי המצלמה
// //     const stopCamera = () => {
// //         if (cameraStream) {
// //             cameraStream.getTracks().forEach(track => track.stop());
            
// //             if (videoElement) {
// //                 videoElement.srcObject = null;
// //                 videoElement.style.display = 'none';
// //             }
            
// //             setCameraStream(null);
// //             setCameraActive(false);
// //         }
// //     };

// //     // צילום תמונה מהמצלמה
// //     const captureImage = () => {
// //         if (!videoElement || !cameraActive) {
// //             setCameraError("Camera is not active");
// //             return;
// //         }

// //         try {
// //             const canvas = document.createElement('canvas');
// //             canvas.width = videoElement.videoWidth;
// //             canvas.height = videoElement.videoHeight;
// //             //מדפיס את התמונה על הקנבס
// //             const ctx = canvas.getContext('2d');
// //             ctx.drawImage(videoElement, 0, 0, canvas.width, canvas.height);
            
// //             canvas.toBlob((blob) => {
// //                 if (blob) {
// //                     const file = new File([blob], "camera-capture.jpg", { type: "image/jpeg" });
// //                     setData(prevData => ({ ...prevData, image: file }));
                    
// //                     // יצירת URL לתמונה
// //                     const imageURL = URL.createObjectURL(blob);
// //                     setCapturedImage(imageURL);
                    
// //                     // כיבוי המצלמה לאחר לכידת התמונה
// //                     stopCamera();
// //                 } else {
// //                     setCameraError("Failed to capture image");
// //                 }
// //             }, 'image/jpeg', 0.9);
// //         } catch (error) {
// //             setCameraError(`Error capturing image: ${error.message}`);
// //         }
// //     };

// //     // התחלת הקלטת אודיו
// //     const startRecording = async () => {
// //         setAudioError("");
// //         setAudioChunks([]);
        
// //         // אם יש URL קודם של אודיו, נשחרר את המשאב
// //         if (audioURL) {
// //             URL.revokeObjectURL(audioURL);
// //             setAudioURL(null);
// //         }
        
// //         try {
// //             const stream = await navigator.mediaDevices.getUserMedia({ audio: true });
// //             const recorder = new MediaRecorder(stream);
            
// //             recorder.ondataavailable = (event) => {
// //                 if (event.data.size > 0) {
// //                     setAudioChunks(chunks => [...chunks, event.data]);
// //                 }
// //             };
            
// //             recorder.onstop = () => {
// //                 // איסוף כל חלקי האודיו
// //                 const audioBlob = new Blob(audioChunks, { type: 'audio/mp3' });
// //                 const audioFile = new File([audioBlob], "audio-recording.mp3", { type: "audio/mp3" });
                
// //                 // שמירת הקובץ בנתונים
// //                 setData(prevData => ({ ...prevData, audio: audioFile }));
                
// //                 // יצירת URL חדש לנגן האודיו
// //                 const url = URL.createObjectURL(audioBlob);
// //                 setAudioURL(url);
                
// //                 // עצירת הזרם
// //                 stream.getTracks().forEach(track => track.stop());
// //             };
            
// //             // התחלת ההקלטה
// //             setAudioChunks([]);
// //             recorder.start();
// //             setAudioRecorder(recorder);
// //             setIsRecording(true);
// //         } catch (error) {
// //             setAudioError(`Could not access microphone: ${error.message}`);
// //         }
// //     };

// //     // עצירת הקלטת אודיו
// //     const stopRecording = () => {
// //         if (audioRecorder && audioRecorder.state !== 'inactive') {
// //             audioRecorder.stop();
// //             setIsRecording(false);
// //         }
// //     };

// //     // שליחת הנתונים לשרת
// //     const handleSubmit = async () => {
// //         setUploadStatus("");
    
// //         const formData = new FormData();
// //         if (data.text) formData.append("text", data.text);
// //         if (data.image) formData.append("image", data.image);
// //         if (data.audio) formData.append("audio", data.audio);
    
// //         try {
// //             setUploadStatus("Uploading...");
    
// //             const response = await fetch('http://localhost:8080/uploadData', {
// //                 method: "POST",
// //                 body: formData
// //             });
    
// //             if (response.ok) {
// //                 const responseText = await response.text();
// //                 setUploadStatus(responseText);
// //                 // ניקוי הטופס
// //                 setData({ text: "", image: null, audio: null });
    
// //                 // ניקוי משאבים
// //                 if (capturedImage) {
// //                     URL.revokeObjectURL(capturedImage);
// //                     setCapturedImage(null);
// //                 }
// //                 if (audioURL) {
// //                     URL.revokeObjectURL(audioURL);
// //                     setAudioURL(null);
// //                 }
// //             } else {
// //                 const errorText = await response.text();
// //                 setUploadStatus(`Upload failed: ${errorText}`);
// //             }
// //         } catch (error) {
// //             setUploadStatus(`Error during upload: ${error.message}`);
// //         }
// //     };

// //     return (
// //         <div style={{ padding: "20px", maxWidth: "600px", margin: "auto" }}>
// //             <h1 style={{ textAlign: "center", color: "blue" }}>Medical Diagnosis</h1>
            
// //             {/* Text Input Section */}
// //             <div style={{ textAlign: "center", marginBottom: "20px" }}>
// //                 <h3>By Text Description</h3>
// //                 <textarea 
// //                     placeholder="Enter a textual description"
// //                     style={{ width: "80%", height: "60px", padding: "10px", border: "1px solid black" }} 
// //                     onChange={handleTextChange} 
// //                     value={data.text}
// //                 />
// //             </div>
// //             <hr />
            
// //             {/* Image Upload & Camera Section */}
// //             <div style={{ textAlign: "center", marginBottom: "20px" }}>
// //                 <h3>By Image</h3>
                
// //                 {/* File Upload */}
// //                 <div style={{ marginBottom: "15px" }}>
// //                     <label htmlFor="myfile">Select a file: </label>
// //                     <input 
// //                         type="file" 
// //                         id="myfile" 
// //                         name="myfile" 
// //                         accept="image/*" 
// //                         onChange={handleImageUpload} 
// //                     />
// //                 </div>
                
// //                 {/* Camera Controls */}
// //                 <div style={{ marginBottom: "15px" }}>
// //                     {!cameraActive ? (
// //                         <button 
// //                             onClick={startCamera} 
// //                             style={{ padding: "8px 15px", backgroundColor: "#4CAF50", color: "white", border: "none", borderRadius: "4px", cursor: "pointer" }}
// //                         >
// //                             Open Camera
// //                         </button>
// //                     ) : (
// //                         <div>
// //                             <button 
// //                                 onClick={captureImage} 
// //                                 style={{ padding: "8px 15px", backgroundColor: "#2196F3", color: "white", border: "none", borderRadius: "4px", cursor: "pointer", marginRight: "10px" }}
// //                             >
// //                                 Take Picture
// //                             </button>
// //                             <button 
// //                                 onClick={stopCamera} 
// //                                 style={{ padding: "8px 15px", backgroundColor: "#f44336", color: "white", border: "none", borderRadius: "4px", cursor: "pointer" }}
// //                             >
// //                                 Close Camera
// //                             </button>
// //                         </div>
// //                     )}
// //                 </div>
                
// //                 {/* Camera Error Message */}
// //                 {cameraError && (
// //                     <div style={{ color: "red", margin: "10px 0", fontWeight: "bold" }}>
// //                         {cameraError}
// //                     </div>
// //                 )}
                
// //                 {/* Camera View - מוצג רק כאשר המצלמה פעילה */}
// //                 <div style={{ margin: "15px auto", maxWidth: "500px" }}>
// //                     <video 
// //                         id="camera-view"
// //                         autoPlay 
// //                         playsInline
// //                         style={{ 
// //                             width: "100%", 
// //                             border: "2px solid #666", 
// //                             borderRadius: "4px",
// //                             display: cameraActive ? "block" : "none"
// //                         }}
// //                     />
// //                 </div>
                
// //                 {/* Display Captured Image - מוצג רק כאשר יש תמונה שנלכדה וכשהמצלמה לא פעילה */}
// //                 {capturedImage && (
// //                     <div style={{ margin: "15px auto", maxWidth: "500px" }}>
// //                         <h4>Captured Image:</h4>
// //                         <img 
// //                             src={capturedImage} 
// //                             alt="Captured" 
// //                             style={{ 
// //                                 width: "100%", 
// //                                 border: "2px solid #4CAF50", 
// //                                 borderRadius: "4px" 
// //                             }} 
// //                         />
// //                     </div>
// //                 )}
                
// //                 {/* Show selected file name - מוצג רק כאשר נבחר קובץ ואין תמונה מהמצלמה */}
// //                 {data.image && !capturedImage && (
// //                     <div style={{ margin: "10px 0", fontWeight: "bold" }}>
// //                         Selected: {data.image.name}
// //                     </div>
// //                 )}
// //             </div>
// //             <hr />
            
// //             {/* Audio Recording Section */}
// //             <div style={{ textAlign: "center", marginBottom: "20px" }}>
// //                 <h3>By Voice Recording</h3>
                
// //                 {/* Recording Controls */}
// //                 {!isRecording ? (
// //                     <button 
// //                         onClick={startRecording} 
// //                         style={{ 
// //                             padding: "8px 15px", 
// //                             backgroundColor: "#4CAF50", 
// //                             color: "white", 
// //                             border: "none", 
// //                             borderRadius: "4px", 
// //                             cursor: "pointer" 
// //                         }}
// //                     >
// //                         Start Recording
// //                     </button>
// //                 ) : (
// //                     <button 
// //                         onClick={stopRecording} 
// //                         style={{ 
// //                             padding: "8px 15px", 
// //                             backgroundColor: "#f44336", 
// //                             color: "white", 
// //                             border: "none", 
// //                             borderRadius: "4px", 
// //                             cursor: "pointer" 
// //                         }}
// //                     >
// //                         Stop Recording
// //                     </button>
// //                 )}
                
// //                 {/* Audio Error Message */}
// //                 {audioError && (
// //                     <div style={{ color: "red", margin: "10px 0", fontWeight: "bold" }}>
// //                         {audioError}
// //                     </div>
// //                 )}
                
// //                 {/* Audio Player - משתמש ב-audioURL במקום לייצר URL חדש בכל פעם */}
// //                 {audioURL && (
// //                     <div style={{ margin: "15px auto", maxWidth: "500px" }}>
// //                         <h4>Recorded Audio</h4>
// //                         <audio 
// //                             controls 
// //                             src={audioURL}
// //                             style={{ 
// //                                 width: "100%", 
// //                                 borderRadius: "4px",
// //                                 margin: "10px 0"
// //                             }}
// //                         />
// //                     </div>
// //                 )}
// //             </div>
// //             <hr />
            
// //             {/* Submit Button */}
// //             <div style={{ textAlign: "center", marginBottom: "20px" }}>
// //                 <button 
// //                     onClick={handleSubmit} 
// //                     style={{ 
// //                         padding: "12px 25px", 
// //                         fontSize: "16px", 
// //                         backgroundColor: "#4CAF50", 
// //                         color: "white", 
// //                         border: "none", 
// //                         borderRadius: "4px", 
// //                         cursor: "pointer",
// //                         fontWeight: "bold"
// //                     }}
// //                 >
// //                     Submit Diagnosis Data
// //                 </button>
                
// //                 {/* Upload Status */}
// //                 {uploadStatus && (
// //                     <div style={{ 
// //                         margin: "15px 0", 
// //                         padding: "10px", 
// //                         borderRadius: "4px", 
// //                         backgroundColor: uploadStatus.includes("success") ? "#dff0d8" : "#f2dede",
// //                         color: uploadStatus.includes("success") ? "#3c763d" : "#a94442"
// //                     }}>
// //                         {uploadStatus}
// //                     </div>
// //                 )}
// //                 {/* OCR Result Section */}
// //                 {uploadStatus.includes("OCR Result") && (
// //                     <div style={{ 
// //                         margin: "15px 0", 
// //                         padding: "10px", 
// //                         borderRadius: "4px", 
// //                         backgroundColor: "#eef5ff",
// //                         color: "#0056b3"
// //                     }}>
// //                         <h4>OCR Result:</h4>
// //                         <p>{uploadStatus.split("OCR Result: ")[1]}</p>
// //                     </div>
// //                 )}
// //             </div>
// //         </div>
// //     );
// // }
// import { useState, useEffect } from "react";
// import MedicalRecommendations from './MedicalGuidelines';
// export default function UploadData1() {
// // מידע בסיסי
// const [data, setData] = useState({ text: "", image: null, audio: null });
// const [isRecording, setIsRecording] = useState(false);
// // מצב המצלמה
// const [cameraActive, setCameraActive] = useState(false);
// const [videoElement, setVideoElement] = useState(null);
// const [cameraStream, setCameraStream] = useState(null);
// const [capturedImage, setCapturedImage] = useState(null);

// // מצב הקלטת האודיו
// const [audioRecorder, setAudioRecorder] = useState(null);
// const [audioChunks, setAudioChunks] = useState([]);
// const [audioURL, setAudioURL] = useState(null);

// // ניהול שגיאות ומצב העלאה
// const [cameraError, setCameraError] = useState("");
// const [audioError, setAudioError] = useState("");
// const [uploadStatus, setUploadStatus] = useState("");
// const [isUploading, setIsUploading] = useState(false);

// // מצב הצגת הנחיות טיפול
// const [showRecommendations, setShowRecommendations] = useState(false);
// const [analysisResult, setAnalysisResult] = useState(null);

// // מופעל כשהקומפוננטה עולה - לקבל הפניה לאלמנט הוידאו
// useEffect(() => {
//     setVideoElement(document.getElementById('camera-view'));
    
//     // ניקוי משאבים כשהקומפוננטה יורדת
//     return () => {
//         if (cameraStream) {
//             cameraStream.getTracks().forEach(track => track.stop());
//         }
        
//         if (audioRecorder && audioRecorder.state !== 'inactive') {
//             audioRecorder.stop();
//         }
        
//         if (audioURL) {
//             URL.revokeObjectURL(audioURL);
//         }
        
//         if (capturedImage) {
//             URL.revokeObjectURL(capturedImage);
//         }
//     };
// }, []);

// const handleTextChange = (event) => {
//     setData(prevData => ({ ...prevData, text: event.target.value }));
// };

// // טיפול בהעלאת תמונה
// const handleImageUpload = (event) => {
//     const file = event.target.files[0];
//     if (file) {
//         // בדיקת גודל הקובץ (מקסימום 10MB)
//         if (file.size > 10 * 1024 * 1024) {
//             setUploadStatus("הקובץ גדול מדי. גודל מקסימלי: 10MB");
//             return;
//         }
        
//         // בדיקת סוג הקובץ
//         if (!file.type.startsWith('image/')) {
//             setUploadStatus("יש לבחור קובץ תמונה תקין");
//             return;
//         }
        
//         setData(prevData => ({ ...prevData, image: file }));
        
//         if (capturedImage) {
//             URL.revokeObjectURL(capturedImage);
//         }
//         setCapturedImage(null);
//         stopCamera();
//         setUploadStatus(""); // איפוס הודעות שגיאה
//     }
// };

// // הפעלת המצלמה
// const startCamera = async () => {
//     setCameraError("");
    
//     if (capturedImage) {
//         setCapturedImage(null);
//     }
    
//     if (!videoElement) {
//         setCameraError("לא ניתן לגשת לאלמנט הווידאו. נסה לרענן את הדף.");
//         return;
//     }
    
//     try {
//         // בקשת הרשאות מתקדמות
//         const stream = await navigator.mediaDevices.getUserMedia({ 
//             video: { 
//                 width: { ideal: 1280 },
//                 height: { ideal: 720 },
//                 facingMode: 'environment' // מצלמה אחורית במכשירים ניידים
//             },
//             audio: false
//         });
        
//         videoElement.srcObject = stream;
//         setCameraStream(stream);
//         setCameraActive(true);
//         videoElement.style.display = 'block';
        
//     } catch (error) {
//         console.error("Camera error:", error);
//         if (error.name === 'NotAllowedError') {
//             setCameraError("גישה למצלמה נדחתה. אנא אפשר גישה למצלמה בהגדרות הדפדפן.");
//         } else if (error.name === 'NotFoundError') {
//             setCameraError("לא נמצאה מצלמה במכשיר.");
//         } else {
//             setCameraError(`לא ניתן לגשת למצלמה: ${error.message}`);
//         }
//     }
// };

// // כיבוי המצלמה
// const stopCamera = () => {
//     if (cameraStream) {
//         cameraStream.getTracks().forEach(track => track.stop());
        
//         if (videoElement) {
//             videoElement.srcObject = null;
//             videoElement.style.display = 'none';
//         }
        
//         setCameraStream(null);
//         setCameraActive(false);
//     }
// };

// // צילום תמונה מהמצלמה
// const captureImage = () => {
//     if (!videoElement || !cameraActive) {
//         setCameraError("המצלמה אינה פעילה");
//         return;
//     }

//     try {
//         const canvas = document.createElement('canvas');
//         canvas.width = videoElement.videoWidth;
//         canvas.height = videoElement.videoHeight;
        
//         const ctx = canvas.getContext('2d');
//         ctx.drawImage(videoElement, 0, 0, canvas.width, canvas.height);
        
//         canvas.toBlob((blob) => {
//             if (blob) {
//                 const file = new File([blob], `camera-capture-${Date.now()}.jpg`, { type: "image/jpeg" });
//                 setData(prevData => ({ ...prevData, image: file }));
                
//                 const imageURL = URL.createObjectURL(blob);
//                 setCapturedImage(imageURL);
                
//                 stopCamera();
//                 setUploadStatus("תמונה נלכדה בהצלחה!");
//             } else {
//                 setCameraError("נכשל בלכידת התמונה");
//             }
//         }, 'image/jpeg', 0.9);
//     } catch (error) {
//         setCameraError(`שגיאה בלכידת התמונה: ${error.message}`);
//     }
// };

// // התחלת הקלטת אודיו
// const startRecording = async () => {
//     setAudioError("");
//     setAudioChunks([]);
    
//     if (audioURL) {
//         URL.revokeObjectURL(audioURL);
//         setAudioURL(null);
//     }
    
//     try {
//         const stream = await navigator.mediaDevices.getUserMedia({ 
//             audio: {
//                 echoCancellation: true,
//                 noiseSuppression: true,
//                 sampleRate: 44100
//             }
//         });
        
//         const recorder = new MediaRecorder(stream, {
//             mimeType: 'audio/webm;codecs=opus'
//         });
        
//         recorder.ondataavailable = (event) => {
//             if (event.data.size > 0) {
//                 setAudioChunks(chunks => [...chunks, event.data]);
//             }
//         };
        
//         recorder.onstop = () => {
//             const audioBlob = new Blob(audioChunks, { type: 'audio/webm' });
//             const audioFile = new File([audioBlob], `audio-recording-${Date.now()}.webm`, { type: "audio/webm" });
            
//             setData(prevData => ({ ...prevData, audio: audioFile }));
            
//             const url = URL.createObjectURL(audioBlob);
//             setAudioURL(url);
            
//             stream.getTracks().forEach(track => track.stop());
//             setUploadStatus("הקלטה הושלמה בהצלחה!");
//         };
        
//         setAudioChunks([]);
//         recorder.start(1000); // חלק הנתונים כל שנייה
//         setAudioRecorder(recorder);
//         setIsRecording(true);
        
//     } catch (error) {
//         if (error.name === 'NotAllowedError') {
//             setAudioError("גישה למיקרופון נדחתה. אנא אפשר גישה למיקרופון בהגדרות הדפדפן.");
//         } else {
//             setAudioError(`לא ניתן לגשת למיקרופון: ${error.message}`);
//         }
//     }
// };

// // עצירת הקלטת אודיו
// const stopRecording = () => {
//     if (audioRecorder && audioRecorder.state !== 'inactive') {
//         audioRecorder.stop();
//         setIsRecording(false);
//     }
// };

// // ניקוי הטופס
// const clearForm = () => {
//     setData({ text: "", image: null, audio: null });
    
//     if (capturedImage) {
//         URL.revokeObjectURL(capturedImage);
//         setCapturedImage(null);
//     }
    
//     if (audioURL) {
//         URL.revokeObjectURL(audioURL);
//         setAudioURL(null);
//     }
    
//     setUploadStatus("");
//     setAnalysisResult(null);
//     setShowRecommendations(false);
    
//     // איפוס אלמנט קלט הקבצים
//     const fileInput = document.getElementById('myfile');
//     if (fileInput) {
//         fileInput.value = '';
//     }
// };

// // שליחת הנתונים לשרת
// const handleSubmit = async () => {
//     setUploadStatus("");
//     setIsUploading(true);
    
//     // בדיקת ולידציה
//     if (!data.text && !data.image && !data.audio) {
//         setUploadStatus("אנא הזן לפחות סוג אחד של מידע (טקסט, תמונה או אודיו)");
//         setIsUploading(false);
//         return;
//     }

//     const formData = new FormData();
//     if (data.text?.trim()) formData.append("text", data.text.trim());
//     if (data.image) formData.append("image", data.image);
//     if (data.audio) formData.append("audio", data.audio);

//     try {
//         setUploadStatus("מעלה נתונים...");

//         const response = await fetch('http://localhost:8080/uploadData', {
//             method: "POST",
//             body: formData
//         });

//         if (response.ok) {
//             const responseText = await response.text();
//             setUploadStatus("העלאה הושלמה בהצלחה! ✅");
            
//             // אם יש תוצאת OCR, מציג אותה בנפרד
//             if (responseText.includes("OCR Result:")) {
//                 const ocrResult = responseText.split("OCR Result: ")[1];
//                 if (ocrResult) {
//                     setAnalysisResult({ ocrText: ocrResult.trim() });
//                     setShowRecommendations(true);
//                 }
//             }
            
//             // המתנה קצרה לפני ניקוי הטופס
//             setTimeout(() => {
//                 clearForm();
//             }, 3000);

//         } else {
//             const errorText = await response.text();
//             setUploadStatus(`העלאה נכשלה: ${errorText} ❌`);
//         }
//     } catch (error) {
//         console.error('Upload error:', error);
//         setUploadStatus(`שגיאה בעת העלאה: ${error.message} ❌`);
//     } finally {
//         setIsUploading(false);
//     }
// };

// // פונקציה לקבלת אייקון מצב
// const getStatusIcon = (status) => {
//     if (status.includes('מעלה') || status.includes('מחפש')) return '⏳';
//     if (status.includes('בהצלחה') || status.includes('✅')) return '✅';
//     if (status.includes('נכשל') || status.includes('❌')) return '❌';
//     if (status.includes('שגיאה')) return '⚠️';
//     return 'ℹ️';
// };

// return (
//     <div style={{ padding: "20px", maxWidth: "800px", margin: "auto", fontFamily: "Arial, sans-serif" }}>
//         <h1 style={{ textAlign: "center", color: "#2c3e50", marginBottom: "30px" }}>
//             🏥 מערכת אבחון רפואי
//         </h1>
        
//         {/* Text Input Section */}
//         <div style={{ marginBottom: "30px", padding: "20px", backgroundColor: "#f8f9fa", borderRadius: "8px", boxShadow: "0 2px 4px rgba(0,0,0,0.1)" }}>
//             <h3 style={{ marginTop: 0, color: "#495057" }}>📝 תיאור טקסטואלי</h3>
//             <textarea 
//                 placeholder="הזן תיאור של הסימפטומים או הבעיה הרפואית..."
//                 style={{ 
//                     width: "100%", 
//                     height: "80px", 
//                     padding: "12px", 
//                     border: "2px solid #dee2e6",
//                     borderRadius: "6px",
//                     fontSize: "14px",
//                     fontFamily: "inherit",
//                     resize: "vertical",
//                     direction: "rtl"
//                 }} 
//                 onChange={handleTextChange} 
//                 value={data.text}
//             />
//             {data.text && (
//                 <small style={{ color: "#6c757d" }}>
//                     תווים: {data.text.length}
//                 </small>
//             )}
//         </div>
        
//         <hr style={{ margin: "30px 0", border: "none", height: "1px", backgroundColor: "#dee2e6" }} />
        
//         {/* Image Upload & Camera Section */}
//         <div style={{ marginBottom: "30px", padding: "20px", backgroundColor: "#f8f9fa", borderRadius: "8px", boxShadow: "0 2px 4px rgba(0,0,0,0.1)" }}>
//             <h3 style={{ marginTop: 0, color: "#495057" }}>📸 העלאת תמונה</h3>
            
//             {/* File Upload */}
//             <div style={{ marginBottom: "20px" }}>
//                 <label htmlFor="myfile" style={{ display: "block", marginBottom: "8px", fontWeight: "bold" }}>
//                     בחר קובץ תמונה:
//                 </label>
//                 <input 
//                     type="file" 
//                     id="myfile" 
//                     name="myfile" 
//                     accept="image/*" 
//                     onChange={handleImageUpload}
//                     style={{ 
//                         padding: "8px",
//                         border: "2px solid #dee2e6",
//                         borderRadius: "6px",
//                         width: "100%"
//                     }}
//                 />
//             </div>
            
//             {/* Camera Controls */}
//             <div style={{ marginBottom: "20px" }}>
//                 <h4 style={{ margin: "0 0 10px 0", color: "#6c757d" }}>או צלם תמונה חדשה:</h4>
//                 {!cameraActive ? (
//                     <button 
//                         onClick={startCamera} 
//                         style={{ 
//                             padding: "12px 20px", 
//                             backgroundColor: "#28a745", 
//                             color: "white", 
//                             border: "none", 
//                             borderRadius: "6px", 
//                             cursor: "pointer",
//                             fontSize: "14px",
//                             fontWeight: "bold"
//                         }}
//                     >
//                         📷 פתח מצלמה
//                     </button>
//                 ) : (
//                     <div style={{ display: "flex", gap: "10px", flexWrap: "wrap" }}>
//                         <button 
//                             onClick={captureImage} 
//                             style={{ 
//                                 padding: "12px 20px", 
//                                 backgroundColor: "#007bff", 
//                                 color: "white", 
//                                 border: "none", 
//                                 borderRadius: "6px", 
//                                 cursor: "pointer",
//                                 fontSize: "14px",
//                                 fontWeight: "bold"
//                             }}
//                         >
//                             📸 צלם תמונה
//                         </button>
//                         <button 
//                             onClick={stopCamera} 
//                             style={{ 
//                                 padding: "12px 20px", 
//                                 backgroundColor: "#dc3545", 
//                                 color: "white", 
//                                 border: "none", 
//                                 borderRadius: "6px", 
//                                 cursor: "pointer",
//                                 fontSize: "14px",
//                                 fontWeight: "bold"
//                             }}
//                         >
//                             ❌ סגור מצלמה
//                         </button>
//                     </div>
//                 )}
//             </div>
            
//             {/* Camera Error Message */}
//             {cameraError && (
//                 <div style={{ 
//                     color: "#721c24", 
//                     backgroundColor: "#f8d7da",
//                     border: "1px solid #f5c6cb",
//                     padding: "12px",
//                     borderRadius: "6px",
//                     margin: "10px 0"
//                 }}>
//                     ⚠️ {cameraError}
//                 </div>
//             )}
            
//             {/* Camera View */}
//             <div style={{ margin: "20px 0", textAlign: "center" }}>
//                 <video 
//                     id="camera-view"
//                     autoPlay 
//                     playsInline
//                     style={{ 
//                         maxWidth: "100%", 
//                         border: "3px solid #007bff", 
//                         borderRadius: "8px",
//                         display: "none"
//                     }}
//                 />
//             </div>
            
//             {/* Display Captured Image */}
//             {capturedImage && (
//                 <div style={{ margin: "20px 0", textAlign: "center" }}>
//                     <h4 style={{ color: "#28a745" }}>✅ תמונה נלכדה:</h4>
//                     <img 
//                         src={capturedImage} 
//                         alt="תמונה שנלכדה" 
//                         style={{ 
//                             maxWidth: "100%", 
//                             border: "3px solid #28a745", 
//                             borderRadius: "8px",
//                             maxHeight: "400px"
//                         }} 
//                     />
//                 </div>
//             )}
            
//             {/* Show selected file name */}
//             {data.image && !capturedImage && (
//                 <div style={{ 
//                     margin: "15px 0", 
//                     padding: "10px",
//                     backgroundColor: "#d1ecf1",
//                     border: "1px solid #bee5eb",
//                     borderRadius: "6px",
//                     color: "#0c5460"
//                 }}>
//                     📎 קובץ נבחר: {data.image.name}
//                 </div>
//             )}
//         </div>
        
//         <hr style={{ margin: "30px 0", border: "none", height: "1px", backgroundColor: "#dee2e6" }} />
        
//         {/* Audio Recording Section */}
//         <div style={{ marginBottom: "30px", padding: "20px", backgroundColor: "#f8f9fa", borderRadius: "8px", boxShadow: "0 2px 4px rgba(0,0,0,0.1)" }}>
//             <h3 style={{ marginTop: 0, color: "#495057" }}>🎤 הקלטת קול</h3>
            
//             {/* Recording Controls */}
//             <div style={{ marginBottom: "15px" }}>
//                 {!isRecording ? (
//                     <button 
//                         onClick={startRecording} 
//                         style={{ 
//                             padding: "12px 20px", 
//                             backgroundColor: "#dc3545", 
//                             color: "white", 
//                             border: "none", 
//                             borderRadius: "6px", 
//                             cursor: "pointer",
//                             fontSize: "14px",
//                             fontWeight: "bold"
//                         }}
//                     >
//                         🎙️ התחל הקלטה
//                     </button>
//                 ) : (
//                     <div style={{ display: "flex", alignItems: "center", gap: "15px" }}>
//                         <button 
//                             onClick={stopRecording} 
//                             style={{ 
//                                 padding: "12px 20px", 
//                                 backgroundColor: "#6c757d", 
//                                 color: "white", 
//                                 border: "none", 
//                                 borderRadius: "6px", 
//                                 cursor: "pointer",
//                                 fontSize: "14px",
//                                 fontWeight: "bold"
//                             }}
//                         >
//                             ⏹️ עצור הקלטה
//                         </button>
//                         <div style={{ 
//                             display: "flex", 
//                             alignItems: "center", 
//                             color: "#dc3545",
//                             fontWeight: "bold"
//                         }}>
//                             <div style={{
//                                 width: "10px",
//                                 height: "10px",
//                                 backgroundColor: "#dc3545",
//                                 borderRadius: "50%",
//                                 marginLeft: "8px",
//                                 animation: "blink 1s infinite"
//                             }}></div>
//                             מקליט...
//                         </div>
//                     </div>
//                 )}
//             </div>
            
//             {/* Audio Error Message */}
//             {audioError && (
//                 <div style={{ 
//                     color: "#721c24", 
//                     backgroundColor: "#f8d7da",
//                     border: "1px solid #f5c6cb",
//                     padding: "12px",
//                     borderRadius: "6px",
//                     margin: "10px 0"
//                 }}>
//                     ⚠️ {audioError}
//                 </div>
//             )}
            
//             {/* Audio Player */}
//             {audioURL && (
//                 <div style={{ margin: "20px 0" }}>
//                     <h4 style={{ color: "#28a745" }}>✅ הקלטה הושלמה:</h4>
//                     <audio 
//                         controls 
//                         src={audioURL}
//                         style={{ 
//                             width: "100%", 
//                             borderRadius: "6px",
//                             margin: "10px 0"
//                         }}
//                     />
//                 </div>
//             )}
//         </div>
        
//         <hr style={{ margin: "30px 0", border: "none", height: "1px", backgroundColor: "#dee2e6" }} />
        
//         {/* Control Buttons */}
//         <div style={{ textAlign: "center", marginBottom: "30px" }}>
//             <div style={{ display: "flex", gap: "15px", justifyContent: "center", flexWrap: "wrap" }}>
//                 <button 
//                     onClick={handleSubmit} 
//                     disabled={isUploading || (!data.text && !data.image && !data.audio)}
//                     style={{ 
//                         padding: "15px 30px", 
//                         fontSize: "16px", 
//                         backgroundColor: isUploading ? "#6c757d" : "#007bff", 
//                         color: "white", 
//                         border: "none", 
//                         borderRadius: "8px", 
//                         cursor: isUploading ? "not-allowed" : "pointer",
//                         fontWeight: "bold",
//                         minWidth: "200px"
//                     }}
//                 >
//                     {isUploading ? "🔄 מעלה..." : "📤 שלח לאבחון"}
//                 </button>
                
//                 <button 
//                     onClick={clearForm}
//                     disabled={isUploading}
//                     style={{ 
//                         padding: "15px 30px", 
//                         fontSize: "16px", 
//                         backgroundColor: "#6c757d", 
//                         color: "white", 
//                         border: "none", 
//                         borderRadius: "8px", 
//                         cursor: isUploading ? "not-allowed" : "pointer",
//                         fontWeight: "bold"
//                     }}
//                 >
//                     🗑️ נקה טופס
//                 </button>
//             </div>
//         </div>
        
//         {/* Upload Status */}
//         {uploadStatus && (
//             <div style={{ 
//                 margin: "20px 0", 
//                 padding: "15px", 
//                 borderRadius: "8px", 
//                 textAlign: "center",
//                 fontWeight: "bold",
//                 backgroundColor: uploadStatus.includes("בהצלחה") || uploadStatus.includes("✅") ? "#d4edda" : 
//                                uploadStatus.includes("נכשל") || uploadStatus.includes("❌") ? "#f8d7da" : "#d1ecf1",
//                 color: uploadStatus.includes("בהצלחה") || uploadStatus.includes("✅") ? "#155724" : 
//                        uploadStatus.includes("נכשל") || uploadStatus.includes("❌") ? "#721c24" : "#0c5460",
//                 border: `1px solid ${uploadStatus.includes("בהצלחה") || uploadStatus.includes("✅") ? "#c3e6cb" : 
//                                    uploadStatus.includes("נכשל") || uploadStatus.includes("❌") ? "#f5c6cb" : "#bee5eb"}`
//             }}>
//                 {getStatusIcon(uploadStatus)} {uploadStatus}
//             </div>
//         )}
        
//         {/* Medical Recommendations */}
//         {showRecommendations && (
//             <div style={{ marginTop: "30px" }}>
//                 <MedicalRecommendations 
//                     analysisResult={analysisResult}
//                     onClose={() => setShowRecommendations(false)}
//                 />
//             </div>
//         )}

//         {/* CSS Animation for recording indicator */}
//         <style jsx>{`
//             @keyframes blink {
//                 0%, 50% { opacity: 1; }
//                 51%, 100% { opacity: 0; }
//             }
//         `}</style>
//     </div>
// );
// };
