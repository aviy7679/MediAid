"""
שרת API מינימלי לניתוח סימפטומים מטקסט ותמונות
"""
from flask import Flask, request, jsonify
from text_analyzer import TextAnalyzer
from image_analyzer import ImageAnalyzer
import logging
import base64
import io
from PIL import Image
from config import (
    SERVER_PORT_MAIN, SERVER_HOST, MAX_FILE_SIZE_BYTES,
    TEXT_PREVIEW_LENGTH, HTTP_OK, HTTP_BAD_REQUEST, HTTP_NOT_FOUND,
    HTTP_PAYLOAD_TOO_LARGE, HTTP_INTERNAL_ERROR, HTTP_SERVICE_UNAVAILABLE,
    DEBUG_SEPARATOR_LONG, DEBUG_SEPARATOR_SHORT
)

# הגדרת logging
logging.basicConfig(level=logging.INFO, format='%(asctime)s - %(levelname)s - %(message)s')
logger = logging.getLogger(__name__)

app = Flask(__name__)
app.config['MAX_CONTENT_LENGTH'] = MAX_FILE_SIZE_BYTES

# מנתחים גלובליים
text_analyzer = None
image_analyzer = None


def initialize_models():
    """אתחול המודלים פעם אחת בהפעלת השרת"""
    global text_analyzer, image_analyzer

    try:
        logger.info("מתחיל אתחול המודלים...")

        # אתחול Text Analyzer
        text_analyzer = TextAnalyzer()
        text_analyzer.load_model()
        logger.info("Text Analyzer נטען בהצלחה")

        # אתחול Image Analyzer
        image_analyzer = ImageAnalyzer()
        image_analyzer.load_model()
        logger.info("Image Analyzer נטען בהצלחה")

        logger.info("כל המודלים נטענו בהצלחה!")

    except Exception as e:
        logger.error(f"שגיאה באתחול המודלים: {e}")
        raise


# =============================================================================
# HEALTH & STATUS
# =============================================================================

@app.route('/health', methods=['GET'])
def health_check():
    """בדיקת תקינות השרת"""
    text_ready = text_analyzer is not None and text_analyzer.is_loaded
    image_ready = image_analyzer is not None and image_analyzer.is_loaded

    return jsonify({
        "status": "healthy" if (text_ready and image_ready) else "partial",
        "text_analyzer": text_ready,
        "image_analyzer": image_ready
    })


# =============================================================================
# TEXT ANALYSIS
# =============================================================================

@app.route('/text/analyze', methods=['POST'])
def analyze_text():
    """
    ניתוח טקסט לחילוץ סימפטומים

    Body: {"text": "I have headache and fever"}
    """
    try:
        if not text_analyzer or not text_analyzer.is_loaded:
            return jsonify({"error": "Text analyzer not ready"}), HTTP_SERVICE_UNAVAILABLE

        data = request.get_json()
        if not data or 'text' not in data:
            return jsonify({"error": "Missing 'text' field"}), HTTP_BAD_REQUEST

        text = data['text'].strip()
        if not text:
            return jsonify({"error": "Text cannot be empty"}), HTTP_BAD_REQUEST

        # ניתוח
        symptoms = text_analyzer.extract_symptoms(text)

        return jsonify({
            "success": True,
            "symptoms": symptoms,
            "count": len(symptoms)
        })

    except Exception as e:
        logger.error(f"Error in text analysis: {e}")
        return jsonify({"error": str(e)}), HTTP_INTERNAL_ERROR


@app.route('/text/entities', methods=['POST'])
def get_all_text_entities():
    """
    קבלת כל הישויות מטקסט (לא רק סימפטומים)

    Body: {"text": "I have headache and fever"}
    """
    try:
        if not text_analyzer or not text_analyzer.is_loaded:
            return jsonify({"error": "Text analyzer not ready"}), HTTP_SERVICE_UNAVAILABLE

        data = request.get_json()
        if not data or 'text' not in data:
            return jsonify({"error": "Missing 'text' field"}), HTTP_BAD_REQUEST

        text = data['text'].strip()
        if not text:
            return jsonify({"error": "Text cannot be empty"}), HTTP_BAD_REQUEST

        # קבלת כל הישויות
        all_entities = text_analyzer.get_all_entities(text)

        return jsonify({
            "success": True,
            "entities": all_entities
        })

    except Exception as e:
        logger.error(f"Error getting all entities: {e}")
        return jsonify({"error": str(e)}), HTTP_INTERNAL_ERROR


# =============================================================================
# IMAGE ANALYSIS
# =============================================================================

def process_image_input(image_data):
    """עיבוד קלט תמונה - base64 או bytes"""
    try:
        if isinstance(image_data, str):
            # אם זה base64 string
            image_bytes = base64.b64decode(image_data)
        else:
            # אם זה כבר bytes
            image_bytes = image_data

        # המרה ל-PIL Image
        image = Image.open(io.BytesIO(image_bytes))
        return image

    except Exception as e:
        raise Exception(f"Error processing image: {e}")


@app.route('/image/analyze', methods=['POST'])
def analyze_image():
    """
    ניתוח תמונה לזיהוי מצבי עור

    JSON Body: {"image": "base64_data"}
    או Form-data: files={'image': file}
    """
    try:
        if not image_analyzer or not image_analyzer.is_loaded:
            return jsonify({"error": "Image analyzer not ready"}), HTTP_SERVICE_UNAVAILABLE

        image_data = None

        # בדיקה אם זה JSON או form-data
        if request.is_json:
            data = request.get_json()
            if not data or 'image' not in data:
                return jsonify({"error": "Missing 'image' field"}), HTTP_BAD_REQUEST
            image_data = data['image']

        else:
            # form-data עם קובץ
            if 'image' not in request.files:
                return jsonify({"error": "No image file provided"}), HTTP_BAD_REQUEST

            file = request.files['image']
            if file.filename == '':
                return jsonify({"error": "No image selected"}), HTTP_BAD_REQUEST

            image_data = file.read()

        # עיבוד התמונה
        pil_image = process_image_input(image_data)

        # ניתוח
        results = image_analyzer.analyze_image(pil_image)

        return jsonify({
            "success": True,
            "predictions": results,
            "count": len(results)
        })

    except Exception as e:
        logger.error(f"Error in image analysis: {e}")
        return jsonify({"error": str(e)}), HTTP_INTERNAL_ERROR


@app.route('/image/top_predictions', methods=['POST'])
def get_top_image_predictions():
    """
    קבלת המצבים הסבירים ביותר מתמונה

    Body: {"image": "base64_data", "top_k": 5}
    """
    try:
        if not image_analyzer or not image_analyzer.is_loaded:
            return jsonify({"error": "Image analyzer not ready"}), HTTP_SERVICE_UNAVAILABLE

        data = request.get_json()
        if not data or 'image' not in data:
            return jsonify({"error": "Missing 'image' field"}), HTTP_BAD_REQUEST

        from config import TOP_PREDICTIONS_DEFAULT
        top_k = data.get('top_k', TOP_PREDICTIONS_DEFAULT)

        # עיבוד התמונה
        pil_image = process_image_input(data['image'])

        # ניתוח
        results = image_analyzer.analyze_image(pil_image)

        # החזרת top_k תוצאות
        top_results = results[:top_k]

        return jsonify({
            "success": True,
            "top_predictions": top_results,
            "requested_count": top_k,
            "returned_count": len(top_results)
        })

    except Exception as e:
        logger.error(f"Error getting top predictions: {e}")
        return jsonify({"error": str(e)}), HTTP_INTERNAL_ERROR


# =============================================================================
# COMBINED ANALYSIS
# =============================================================================

@app.route('/analyze', methods=['POST'])
def analyze_combined():
    """
    ניתוח משולב של טקסט ותמונה

    Body: {
        "text": "I have headache",
        "image": "base64_data"  // אופציונלי
    }
    """
    try:
        data = request.get_json()
        if not data:
            return jsonify({"error": "No data provided"}), HTTP_BAD_REQUEST

        results = {"success": True}

        # ניתוח טקסט
        if 'text' in data and data['text'].strip():
            if text_analyzer and text_analyzer.is_loaded:
                text_symptoms = text_analyzer.extract_symptoms(data['text'].strip())
                results['text_symptoms'] = text_symptoms
                results['text_count'] = len(text_symptoms)
            else:
                results['text_error'] = "Text analyzer not ready"

        # ניתוח תמונה
        if 'image' in data and data['image']:
            if image_analyzer and image_analyzer.is_loaded:
                try:
                    pil_image = process_image_input(data['image'])
                    image_results = image_analyzer.analyze_image(pil_image)
                    # לקחת רק את ה-5 הראשונים
                    from config import TOP_PREDICTIONS_DEFAULT
                    results['image_predictions'] = image_results[:TOP_PREDICTIONS_DEFAULT]
                    results['image_count'] = len(image_results[:TOP_PREDICTIONS_DEFAULT])
                except Exception as e:
                    results['image_error'] = f"Error processing image: {str(e)}"
            else:
                results['image_error'] = "Image analyzer not ready"

        return jsonify(results)

    except Exception as e:
        logger.error(f"Error in combined analysis: {e}")
        return jsonify({"error": str(e)}), HTTP_INTERNAL_ERROR


# =============================================================================
# ERROR HANDLERS
# =============================================================================

@app.errorhandler(HTTP_NOT_FOUND)
def not_found(error):
    return jsonify({"error": "Endpoint not found"}), HTTP_NOT_FOUND


@app.errorhandler(HTTP_PAYLOAD_TOO_LARGE)
def too_large(error):
    from config import MAX_FILE_SIZE_MB
    return jsonify({"error": f"File too large (max {MAX_FILE_SIZE_MB}MB)"}), HTTP_PAYLOAD_TOO_LARGE


@app.errorhandler(HTTP_INTERNAL_ERROR)
def internal_error(error):
    return jsonify({"error": "Internal server error"}), HTTP_INTERNAL_ERROR


# =============================================================================
# MAIN
# =============================================================================

if __name__ == '__main__':
    try:
        print("מפעיל שרת ניתוח סימפטומים...")
        print(DEBUG_SEPARATOR_LONG)

        # אתחול המודלים
        initialize_models()

        print("שרת מוכן!")
        print("Endpoints:")
        print("  GET  /health - בדיקת תקינות")
        print("  POST /text/analyze - ניתוח טקסט (סימפטומים)")
        print("  POST /text/entities - כל הישויות מטקסט")
        print("  POST /image/analyze - ניתוח תמונה מלא")
        print("  POST /image/top_predictions - top predictions מתמונה")
        print("  POST /analyze - ניתוח משולב")
        print(DEBUG_SEPARATOR_LONG)

        app.run(host=SERVER_HOST, port=SERVER_PORT_MAIN, debug=False, threaded=True)

    except Exception as e:
        logger.error(f"שגיאה בהפעלת השרת: {e}")
        print(f"שגיאה: {e}")
        exit(1)