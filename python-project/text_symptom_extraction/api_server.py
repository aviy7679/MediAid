"""
REST API Server for Symptom Extraction
שרת API לחילוץ סימפטומים
"""

from flask import Flask, request, jsonify
from flask_cors import CORS
import json
import traceback
from symptom_extractor import initialize_model, process_text

app = Flask(__name__)
CORS(app)  # מאפשר גישה מ-Java Spring Boot


@app.route('/health', methods=['GET'])
def health_check():
    """בדיקת תקינות השרת"""
    return jsonify({
        "status": "healthy",
        "message": "Symptom extraction service is running"
    })


@app.route('/extract-symptoms', methods=['POST'])
def extract_symptoms_endpoint():
    """
    Endpoint לחילוץ סימפטומים
    מקבל: {"text": "הטקסט לעיבוד"}
    מחזיר: {"original_text": "...", "symptoms_count": 2, "symptoms": [...]}
    """
    try:
        # בדיקת נתונים שהתקבלו
        if not request.is_json:
            return jsonify({
                "error": "Content-Type must be application/json"
            }), 400

        data = request.get_json()

        if not data or 'text' not in data:
            return jsonify({
                "error": "Missing 'text' field in request body"
            }), 400

        text = data['text'].strip()

        if not text:
            return jsonify({
                "error": "Text field cannot be empty"
            }), 400

        # עיבוד הטקסט
        result = process_text(text)

        return jsonify({
            "success": True,
            "data": result
        })

    except RuntimeError as e:
        # שגיאה במודל
        return jsonify({
            "success": False,
            "error": "Model not initialized",
            "message": str(e)
        }), 500

    except Exception as e:
        # שגיאה כללית
        print(f"שגיאה בעיבוד הבקשה: {e}")
        print(traceback.format_exc())

        return jsonify({
            "success": False,
            "error": "Internal server error",
            "message": str(e)
        }), 500


@app.route('/extract-symptoms-batch', methods=['POST'])
def extract_symptoms_batch():
    """
    Endpoint לעיבוד מספר טקסטים בבת אחת
    מקבל: {"texts": ["טקסט 1", "טקסט 2", ...]}
    מחזיר: [{"original_text": "...", "symptoms": [...]}, ...]
    """
    try:
        if not request.is_json:
            return jsonify({
                "error": "Content-Type must be application/json"
            }), 400

        data = request.get_json()

        if not data or 'texts' not in data:
            return jsonify({
                "error": "Missing 'texts' field in request body"
            }), 400

        texts = data['texts']

        if not isinstance(texts, list):
            return jsonify({
                "error": "'texts' must be an array"
            }), 400

        if len(texts) > 50:  # הגבלת כמות
            return jsonify({
                "error": "Maximum 50 texts per batch"
            }), 400

        # עיבוד כל הטקסטים
        results = []
        for text in texts:
            if text and text.strip():
                result = process_text(text.strip())
                results.append(result)
            else:
                results.append({
                    "original_text": text,
                    "symptoms_count": 0,
                    "symptoms": [],
                    "error": "Empty text"
                })

        return jsonify({
            "success": True,
            "data": results,
            "processed_count": len(results)
        })

    except Exception as e:
        print(f"שגיאה בעיבוד batch: {e}")
        print(traceback.format_exc())

        return jsonify({
            "success": False,
            "error": "Internal server error",
            "message": str(e)
        }), 500


@app.errorhandler(404)
def not_found(error):
    return jsonify({
        "error": "Endpoint not found",
        "available_endpoints": [
            "GET /health",
            "POST /extract-symptoms",
            "POST /extract-symptoms-batch"
        ]
    }), 404


@app.errorhandler(405)
def method_not_allowed(error):
    return jsonify({
        "error": "Method not allowed"
    }), 405


def create_app():
    """יצירת האפליקציה"""
    # אתחול המודל
    print("מאתחל את מודל חילוץ הסימפטומים...")
    if not initialize_model():
        print("כישלון באתחול המודל!")
        exit(1)

    print("השרת מוכן לקבלת בקשות")
    return app


if __name__ == '__main__':
    # הרצה מקומית
    app = create_app()

    print("\n" + "=" * 50)
    print("Symptom Extraction API Server")
    print("=" * 50)
    print("Available endpoints:")
    print("  GET  /health - Health check")
    print("  POST /extract-symptoms - Extract symptoms from single text")
    print("  POST /extract-symptoms-batch - Extract symptoms from multiple texts")
    print("\nExample request:")
    print('  curl -X POST http://localhost:5000/extract-symptoms \\')
    print('       -H "Content-Type: application/json" \\')
    print('       -d \'{"text": "I have headache and fever"}\'')
    print("=" * 50)

    app.run(
        host='0.0.0.0',  # מאפשר גישה מחוץ למחשב
        port=5000,
        debug=False  # כבה debug במודל production
    )