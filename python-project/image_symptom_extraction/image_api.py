"""
API שרת לניתוח סימפטומים מתמונות
"""
from flask import Flask, request, jsonify
from image_symptom_analyzer import ImageSymptomAnalyzer
import logging
import base64
import tempfile
import os

# הגדרת logging
logging.basicConfig(level=logging.INFO)
logger = logging.getLogger(__name__)

app = Flask(__name__)

# אתחול המנתח
analyzer = None


def initialize_analyzer():
    """אתחול המנתח בעת הפעלת השרת"""
    global analyzer
    try:
        analyzer = ImageSymptomAnalyzer(min_confidence=0.1)  # סף ברירת מחדל של 10%
        logger.info("Image analyzer initialized successfully")
    except Exception as e:
        logger.error(f"Failed to initialize analyzer: {e}")
        raise


@app.route('/health', methods=['GET'])
def health_check():
    """בדיקת תקינות השרת"""
    return jsonify({
        "status": "healthy",
        "service": "image-symptom-analyzer",
        "analyzer_ready": analyzer is not None
    })


@app.route('/analyze/image', methods=['POST'])
def analyze_image():
    """
    ניתוח תמונה לחילוץ סימפטומים

    Expected JSON:
    {
        "image": "base64_encoded_image_data",
        "min_confidence": 0.1  // אופציונלי
    }

    או קובץ תמונה ב-multipart/form-data

    Returns:
    {
        "success": true,
        "data": {
            "symptoms_found": 2,
            "symptoms": [...],
            "min_confidence_threshold": 0.1
        }
    }
    """
    try:
        if not analyzer:
            return jsonify({
                "success": False,
                "error": "Analyzer not initialized"
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
            min_confidence = request.form.get('min_confidence')

        # עדכון סף הביטחון אם צוין
        if min_confidence is not None:
            try:
                min_confidence = float(min_confidence)
                analyzer.set_confidence_threshold(min_confidence)
            except ValueError:
                return jsonify({
                    "success": False,
                    "error": "Invalid min_confidence value"
                }), 400

        logger.info("Analyzing image...")

        # ביצוע הניתוח
        result = analyzer.get_full_analysis(image_data)

        response = {
            "success": True,
            "data": result
        }

        logger.info(f"Analysis completed. Found {result['symptoms_found']} symptoms")
        return jsonify(response)

    except Exception as e:
        logger.error(f"Error in image analysis: {e}")
        return jsonify({
            "success": False,
            "error": str(e)
        }), 500


@app.route('/analyze/symptoms', methods=['POST'])
def extract_symptoms_only():
    """
    חילוץ סימפטומים בלבד (ללא מידע נוסף)

    Expected JSON:
    {
        "image": "base64_encoded_image_data",
        "min_confidence": 0.1  // אופציונלי
    }

    Returns:
    {
        "success": true,
        "symptoms": [...]
    }
    """
    try:
        if not analyzer:
            return jsonify({
                "success": False,
                "error": "Analyzer not initialized"
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
            min_confidence = request.form.get('min_confidence')

        if min_confidence is not None:
            try:
                min_confidence = float(min_confidence)
                analyzer.set_confidence_threshold(min_confidence)
            except ValueError:
                return jsonify({
                    "success": False,
                    "error": "Invalid min_confidence value"
                }), 400

        # חילוץ סימפטומים בלבד
        symptoms = analyzer.analyze_image(image_data)

        return jsonify({
            "success": True,
            "symptoms": symptoms
        })

    except Exception as e:
        logger.error(f"Error in symptom extraction: {e}")
        return jsonify({
            "success": False,
            "error": str(e)
        }), 500


@app.route('/set_confidence', methods=['POST'])
def set_confidence_threshold():
    """
    עדכון סף הביטחון המינימום

    Expected JSON:
    {
        "threshold": 0.2
    }
    """
    try:
        if not analyzer:
            return jsonify({
                "success": False,
                "error": "Analyzer not initialized"
            }), 500

        data = request.get_json()

        if not data or 'threshold' not in data:
            return jsonify({
                "success": False,
                "error": "Missing 'threshold' field in request"
            }), 400

        try:
            threshold = float(data['threshold'])
            analyzer.set_confidence_threshold(threshold)

            return jsonify({
                "success": True,
                "message": f"Confidence threshold updated to {threshold}"
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


if __name__ == '__main__':
    try:
        initialize_analyzer()
        print("Starting Image Symptom Analysis API Server...")
        print("Available endpoints:")
        print("  GET  /health - Health check")
        print("  POST /analyze/image - Full image analysis")
        print("  POST /analyze/symptoms - Extract symptoms only")
        print("  POST /set_confidence - Set confidence threshold")

        app.run(host='0.0.0.0', port=5002, debug=False)

    except Exception as e:
        logger.error(f"Failed to start server: {e}")
        exit(1)