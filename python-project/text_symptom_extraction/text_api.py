"""
API שרת לניתוח סימפטומים מטקסט
"""
from flask import Flask, request, jsonify
from text_analyzer import TextSymptomAnalyzer
import logging
import os

# הגדרת logging
logging.basicConfig(level=logging.INFO)
logger = logging.getLogger(__name__)

app = Flask(__name__)

# אתחול המנתח
MODEL_PATH = "D:\\MediAid\\umls-2024AB-full\\umls_sm_pt2ch_533bab5115c6c2d6.zip"
analyzer = None


def initialize_analyzer():
    """אתחול המנתח בעת הפעלת השרת"""
    global analyzer
    try:
        if os.path.exists(MODEL_PATH):
            analyzer = TextSymptomAnalyzer(MODEL_PATH)
            logger.info("Text analyzer initialized successfully")
        else:
            logger.error(f"Model file not found: {MODEL_PATH}")
            raise FileNotFoundError(f"Model file not found: {MODEL_PATH}")
    except Exception as e:
        logger.error(f"Failed to initialize analyzer: {e}")
        raise


@app.route('/health', methods=['GET'])
def health_check():
    """בדיקת תקינות השרת"""
    return jsonify({
        "status": "healthy",
        "service": "text-symptom-analyzer",
        "analyzer_ready": analyzer is not None
    })


@app.route('/analyze/text', methods=['POST'])
def analyze_text():
    """
    ניתוח טקסט לחילוץ סימפטומים

    Expected JSON:
    {
        "text": "I have headache and fever"
    }

    Returns:
    {
        "success": true,
        "data": {
            "original_text": "...",
            "symptoms_found": 2,
            "symptoms": [...]
        }
    }
    """
    try:
        if not analyzer:
            return jsonify({
                "success": False,
                "error": "Analyzer not initialized"
            }), 500

        # קבלת הנתונים מה-request
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
        result = analyzer.analyze_text(text)

        response = {
            "success": True,
            "data": result
        }

        logger.info(f"Analysis completed. Found {result['symptoms_found']} symptoms")
        return jsonify(response)

    except Exception as e:
        logger.error(f"Error in text analysis: {e}")
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
        "text": "I have headache and fever"
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

        # חילוץ סימפטומים בלבד
        symptoms = analyzer.extract_symptoms(text)

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
        print("Starting Text Symptom Analysis API Server...")
        print("Available endpoints:")
        print("  GET  /health - Health check")
        print("  POST /analyze/text - Full text analysis")
        print("  POST /analyze/symptoms - Extract symptoms only")

        app.run(host='0.0.0.0', port=5001, debug=False)

    except Exception as e:
        logger.error(f"Failed to start server: {e}")
        exit(1)