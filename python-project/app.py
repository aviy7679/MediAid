"""
שרת API משולב לניתוח סימפטומים מטקסט ותמונות
משתמש במחלקות ייעודיות נפרדות לכל סוג ניתוח
"""
from flask import Flask, request, jsonify
from text_analyzer import TextAnalyzer
from image_analyzer import ImageAnalyzer
import logging
import os
import time

# הגדרת logging
logging.basicConfig(level=logging.INFO)
logger = logging.getLogger(__name__)

app = Flask(__name__)

# הגדרות
TEXT_MODEL_PATH = "D:\\MediAid\\umls-2024AB-full\\umls_self_train_model_pt2ch_3760d588371755d0.zip"
IMAGE_MIN_CONFIDENCE = 0.1

# המנתחים הייעודיים
text_analyzer = None
image_analyzer = None

def initialize_analyzers():
    """אתחול שני המודלים בעת הפעלת השרת"""
    global text_analyzer, image_analyzer

    try:
        start_time = time.time()
        logger.info("Starting to initialize analyzers...")
        print("="*60)

        # בדיקת קיום קובץ המודל לטקסט
        if not os.path.exists(TEXT_MODEL_PATH):
            raise FileNotFoundError(f"Text model file not found: {TEXT_MODEL_PATH}")

        # יצירת המנתחים
        print("Initializing Text Analyzer...")
        text_analyzer = TextAnalyzer(model_path=TEXT_MODEL_PATH)
        text_analyzer.load_model()

        print("Initializing Image Analyzer...")
        image_analyzer = ImageAnalyzer(min_confidence=IMAGE_MIN_CONFIDENCE)
        image_analyzer.load_model()

        end_time = time.time()
        print("="*60)
        logger.info(f"Both analyzers initialized successfully in {end_time - start_time:.2f} seconds")

    except Exception as e:
        logger.error(f"Failed to initialize analyzers: {e}")
        raise

@app.route('/health', methods=['GET'])
def health_check():
    """בדיקת תקינות השרת"""
    text_ready = text_analyzer and text_analyzer.is_ready()
    image_ready = image_analyzer and image_analyzer.is_ready()

    return jsonify({
        "status": "healthy" if (text_ready and image_ready) else "partial",
        "service": "combined-symptom-analyzer",
        "analyzers": {
            "text_analyzer_ready": text_ready,
            "image_analyzer_ready": image_ready
        },
        "ready": text_ready and image_ready
    })

@app.route('/status', methods=['GET'])
def get_detailed_status():
    """מידע מפורט על סטטוס המערכת"""
    response = {
        "success": True,
        "text_analyzer": text_analyzer.get_status() if text_analyzer else {"error": "Not initialized"},
        "image_analyzer": image_analyzer.get_status() if image_analyzer else {"error": "Not initialized"}
    }
    return jsonify(response)

# =============================================================================
# TEXT ANALYSIS ENDPOINTS
# =============================================================================

@app.route('/text/analyze', methods=['POST'])
def analyze_text_full():
    """
    ניתוח טקסט מלא לחילוץ סימפטומים

    Expected JSON:
    {
        "text": "I have headache and fever"
    }
    """
    try:
        if not text_analyzer or not text_analyzer.is_ready():
            return jsonify({
                "success": False,
                "error": "Text analyzer not ready"
            }), 500

        data = request.get_json()

        if not data or 'text' not in data:
            return jsonify({
                "success": False,
                "error": "Missing 'text' field in request"
            }), 400

        text = data['text'].strip()

        if not text:
            return jsonify({
                "success": False,
                "error": "Text cannot be empty"
            }), 400

        logger.info(f"Analyzing text: {text[:100]}...")

        # ביצוע הניתוח
        result = text_analyzer.analyze_text(text)

        response = {
            "success": True,
            "data": result
        }

        logger.info(f"Text analysis completed. Found {result['symptoms_found']} symptoms")
        return jsonify(response)

    except Exception as e:
        logger.error(f"Error in text analysis: {e}")
        return jsonify({
            "success": False,
            "error": str(e)
        }), 500

@app.route('/text/symptoms', methods=['POST'])
def extract_text_symptoms_only():
    """חילוץ סימפטומים בלבד מטקסט (ללא מידע נוסף)"""
    try:
        if not text_analyzer or not text_analyzer.is_ready():
            return jsonify({
                "success": False,
                "error": "Text analyzer not ready"
            }), 500

        data = request.get_json()

        if not data or 'text' not in data:
            return jsonify({
                "success": False,
                "error": "Missing 'text' field in request"
            }), 400

        text = data['text'].strip()

        if not text:
            return jsonify({
                "success": False,
                "error": "Text cannot be empty"
            }), 400

        # ביצוע הניתוח
        symptoms = text_analyzer.extract_symptoms(text)

        return jsonify({
            "success": True,
            "symptoms": symptoms
        })

    except Exception as e:
        logger.error(f"Error in text symptom extraction: {e}")
        return jsonify({
            "success": False,
            "error": str(e)
        }), 500

# =============================================================================
# IMAGE ANALYSIS ENDPOINTS
# =============================================================================

@app.route('/image/analyze', methods=['POST'])
def analyze_image_full():
    """
    ניתוח תמונה מלא לחילוץ סימפטומים

    Expected JSON:
    {
        "image": "base64_encoded_image_data",
        "min_confidence": 0.1  // אופציונלי
    }

    או קובץ תמונה ב-multipart/form-data
    """
    try:
        if not image_analyzer or not image_analyzer.is_ready():
            return jsonify({
                "success": False,
                "error": "Image analyzer not ready"
            }), 500

        image_data = None
        min_confidence = None

        # בדיקה אם זה JSON או form-data
        if request.is_json:
            data = request.get_json()

            if not data or 'image' not in data:
                return jsonify({
                    "success": False,
                    "error": "Missing 'image' field in request"
                }), 400

            image_data = data['image']
            min_confidence = data.get('min_confidence')

        else:
            # form-data עם קובץ
            if 'image' not in request.files:
                return jsonify({
                    "success": False,
                    "error": "No image file provided"
                }), 400

            file = request.files['image']
            if file.filename == '':
                return jsonify({
                    "success": False,
                    "error": "No image file selected"
                }), 400

            # קריאת הקובץ
            image_data = file.read()
            min_confidence_str = request.form.get('min_confidence')
            if min_confidence_str:
                try:
                    min_confidence = float(min_confidence_str)
                except ValueError:
                    return jsonify({
                        "success": False,
                        "error": "Invalid min_confidence value"
                    }), 400

        logger.info("Analyzing image...")

        # ביצוע הניתוח
        result = image_analyzer.analyze_image(image_data, min_confidence)

        response = {
            "success": True,
            "data": result
        }

        logger.info(f"Image analysis completed. Found {result['symptoms_found']} symptoms")
        return jsonify(response)

    except Exception as e:
        logger.error(f"Error in image analysis: {e}")
        return jsonify({
            "success": False,
            "error": str(e)
        }), 500

@app.route('/image/symptoms', methods=['POST'])
def extract_image_symptoms_only():
    """חילוץ סימפטומים בלבד מתמונה (ללא מידע נוסף)"""
    try:
        if not image_analyzer or not image_analyzer.is_ready():
            return jsonify({
                "success": False,
                "error": "Image analyzer not ready"
            }), 500

        # לוגיקה דומה לפונקציה הקודמת
        image_data = None
        min_confidence = None

        if request.is_json:
            data = request.get_json()

            if not data or 'image' not in data:
                return jsonify({
                    "success": False,
                    "error": "Missing 'image' field in request"
                }), 400

            image_data = data['image']
            min_confidence = data.get('min_confidence')

        else:
            if 'image' not in request.files:
                return jsonify({
                    "success": False,
                    "error": "No image file provided"
                }), 400

            file = request.files['image']
            if file.filename == '':
                return jsonify({
                    "success": False,
                    "error": "No image file selected"
                }), 400

            image_data = file.read()
            min_confidence_str = request.form.get('min_confidence')
            if min_confidence_str:
                try:
                    min_confidence = float(min_confidence_str)
                except ValueError:
                    return jsonify({
                        "success": False,
                        "error": "Invalid min_confidence value"
                    }), 400

        # ביצוע הניתוח
        symptoms = image_analyzer.extract_symptoms(image_data, min_confidence)

        return jsonify({
            "success": True,
            "symptoms": symptoms
        })

    except Exception as e:
        logger.error(f"Error in image symptom extraction: {e}")
        return jsonify({
            "success": False,
            "error": str(e)
        }), 500

@app.route('/image/confidence', methods=['POST'])
def set_image_confidence_threshold():
    """
    עדכון סף הביטחון המינימום לתמונות

    Expected JSON:
    {
        "threshold": 0.2
    }
    """
    try:
        if not image_analyzer:
            return jsonify({
                "success": False,
                "error": "Image analyzer not initialized"
            }), 500

        data = request.get_json()

        if not data or 'threshold' not in data:
            return jsonify({
                "success": False,
                "error": "Missing 'threshold' field in request"
            }), 400

        try:
            threshold = float(data['threshold'])
            image_analyzer.set_confidence_threshold(threshold)

            return jsonify({
                "success": True,
                "message": f"Image confidence threshold updated to {threshold}"
            })

        except ValueError as e:
            return jsonify({
                "success": False,
                "error": str(e)
            }), 400

    except Exception as e:
        logger.error(f"Error setting confidence threshold: {e}")
        return jsonify({
            "success": False,
            "error": str(e)
        }), 500

# =============================================================================
# COMBINED ANALYSIS ENDPOINTS
# =============================================================================

@app.route('/analyze/combined', methods=['POST'])
def analyze_combined():
    """
    ניתוח משולב של טקסט ותמונה יחד

    Expected JSON:
    {
        "text": "Patient description...",
        "image": "base64_encoded_image_data",
        "min_confidence": 0.1
    }
    """
    try:
        data = request.get_json()

        if not data:
            return jsonify({
                "success": False,
                "error": "No data provided"
            }), 400

        results = {}

        # ניתוח טקסט אם סופק
        if 'text' in data and data['text'].strip():
            if text_analyzer and text_analyzer.is_ready():
                try:
                    text_result = text_analyzer.analyze_text(data['text'])
                    results['text_analysis'] = text_result
                except Exception as e:
                    results['text_analysis'] = {"error": str(e)}
            else:
                results['text_analysis'] = {"error": "Text analyzer not ready"}

        # ניתוח תמונה אם סופק
        if 'image' in data and data['image']:
            if image_analyzer and image_analyzer.is_ready():
                try:
                    min_confidence = data.get('min_confidence')
                    image_result = image_analyzer.analyze_image(data['image'], min_confidence)
                    results['image_analysis'] = image_result
                except Exception as e:
                    results['image_analysis'] = {"error": str(e)}
            else:
                results['image_analysis'] = {"error": "Image analyzer not ready"}

        if not results:
            return jsonify({
                "success": False,
                "error": "No valid text or image provided"
            }), 400

        return jsonify({
            "success": True,
            "data": results
        })

    except Exception as e:
        logger.error(f"Error in combined analysis: {e}")
        return jsonify({
            "success": False,
            "error": str(e)
        }), 500

# =============================================================================
# ERROR HANDLERS
# =============================================================================

@app.errorhandler(404)
def not_found(error):
    return jsonify({
        "success": False,
        "error": "Endpoint not found"
    }), 404

@app.errorhandler(500)
def internal_error(error):
    return jsonify({
        "success": False,
        "error": "Internal server error"
    }), 500

@app.errorhandler(413)
def too_large(error):
    return jsonify({
        "success": False,
        "error": "File too large"
    }), 413

if __name__ == '__main__':
    try:
        print("Starting Combined Symptom Analysis Server...")
        print("="*60)
        # אתחול המנתחים
        initialize_analyzers()

        print("\nServer Ready!")

        # הגדרת גודל קובץ מקסימלי
        app.config['MAX_CONTENT_LENGTH'] = 16 * 1024 * 1024

        app.run(host='0.0.0.0', port=5000, debug=False)

    except Exception as e:
        logger.error(f"Failed to start server: {e}")
        print(f"Failed to start server: {e}")
        exit(1)