from flask import Flask, request, jsonify
import logging
import base64
import io
import time
import psutil
from PIL import Image
from typing import Dict
from config import (
    SERVER_PORT_LITE, SERVER_HOST, MAX_FILE_SIZE_BYTES,
    HTTP_BAD_REQUEST, HTTP_INTERNAL_ERROR,
    DEBUG_SEPARATOR_LONG
)

# ייבוא המנתחים
try:
    from text_keywords_system import KeywordTextAnalyzer
except ImportError:
    KeywordTextAnalyzer = None
    logging.warning("KeywordTextAnalyzer לא זמין")

try:
    from image_analyzer import ImageAnalyzer
except ImportError:
    ImageAnalyzer = None
    logging.warning("ImageAnalyzer לא זמין")

# הגדרת logging
logging.basicConfig(level=logging.INFO, format='%(asctime)s - %(levelname)s - %(message)s')
logger = logging.getLogger(__name__)

app = Flask(__name__)
app.config['MAX_CONTENT_LENGTH'] = MAX_FILE_SIZE_BYTES

class LiteMedicalAnalysisSystem:
    """מערכת ניתוח רפואי קלה - Keywords + BiomedCLIP"""

    def __init__(self):
        self.text_analyzer = None
        self.image_analyzer = None

        # סטטיסטיקות
        self.stats = {
            "text_analyses": 0,
            "image_analyses": 0,
            "combined_analyses": 0,
            "total_processing_time": 0,
            "system_start_time": time.time()
        }

        self.initialize_system()

    def initialize_system(self):
        """אתחול המערכת"""

        logger.info("Initializing lite medical analysis system...")
        start_time = time.time()
        memory_before = psutil.virtual_memory().used / (1024**2)

        # Initialize Text Analyzer (Keywords)
        if KeywordTextAnalyzer:
            try:
                self.text_analyzer = KeywordTextAnalyzer()
                logger.info("Keyword text analyzer loaded successfully")
            except Exception as e:
                logger.error(f"Error loading keyword text analyzer: {e}")
                self.text_analyzer = None

        # Initialize Image Analyzer
        if ImageAnalyzer:
            try:
                self.image_analyzer = ImageAnalyzer()
                self.image_analyzer.load_model()
            except Exception as e:
                logger.error(f"Error loading Image Analyzer: {e}")
                self.image_analyzer = None

        # Initialization summary
        init_time = time.time() - start_time
        memory_after = psutil.virtual_memory().used / (1024**2)
        memory_used = memory_after - memory_before

        logger.info(f"Lite system ready in {init_time:.1f}s, memory used: {memory_used:.0f}MB")

    def analyze_text(self, text: str) -> Dict:
        """ניתוח טקסט עם מערכת מילות מפתח"""

        start_time = time.time()

        if not self.text_analyzer:
            return {
                "success": False,
                "error": "Keyword text analyzer not available",
                "processing_time": time.time() - start_time
            }

        try:
            # ניתוח עם מערכת מילות מפתח
            symptoms = self.text_analyzer.analyze_text(text)

            processing_time = time.time() - start_time
            self.stats["text_analyses"] += 1
            self.stats["total_processing_time"] += processing_time

            return {
                "success": True,
                "symptoms": symptoms,
                "count": len(symptoms),
                "processing_time": round(processing_time, 3),
                "model": "Advanced Keywords + CUI",
                "memory_usage": psutil.virtual_memory().percent,
                "advantages": [
                    "Instant response - no model loading",
                    "Minimal memory usage",
                    "Accurate CUI normalization",
                    "Focus on common symptoms"
                ]
            }

        except Exception as e:
            return {
                "success": False,
                "error": str(e),
                "processing_time": time.time() - start_time
            }

    def analyze_image(self, image_data) -> Dict:
        """ניתוח תמונה עם BiomedCLIP"""

        start_time = time.time()

        if not self.image_analyzer:
            return {
                "success": False,
                "error": "Image Analyzer לא זמין",
                "processing_time": time.time() - start_time
            }

        try:
            # עיבוד התמונה
            if isinstance(image_data, str):
                image_bytes = base64.b64decode(image_data)
            else:
                image_bytes = image_data

            pil_image = Image.open(io.BytesIO(image_bytes))

            # ניתוח
            predictions = self.image_analyzer.analyze_image(pil_image)

            processing_time = time.time() - start_time
            self.stats["image_analyses"] += 1
            self.stats["total_processing_time"] += processing_time

            return {
                "success": True,
                "predictions": predictions,
                "count": len(predictions),
                "processing_time": round(processing_time, 3),
                "model": "BiomedCLIP",
                "image_size": pil_image.size
            }

        except Exception as e:
            return {
                "success": False,
                "error": str(e),
                "processing_time": time.time() - start_time
            }

    def analyze_combined(self, text: str = None, image_data=None) -> Dict:
        """ניתוח משולב של טקסט ותמונה"""

        start_time = time.time()
        results = {"success": True, "analysis_type": "combined"}

        # ניתוח טקסט
        if text and text.strip():
            text_result = self.analyze_text(text.strip())
            if text_result["success"]:
                results["text_analysis"] = {
                    "symptoms": text_result["symptoms"],
                    "count": text_result["count"],
                    "processing_time": text_result["processing_time"]
                }
            else:
                results["text_error"] = text_result["error"]

        # ניתוח תמונה
        if image_data:
            image_result = self.analyze_image(image_data)
            if image_result["success"]:
                results["image_analysis"] = {
                    "predictions": image_result["predictions"],
                    "count": image_result["count"],
                    "processing_time": image_result["processing_time"]
                }
            else:
                results["image_error"] = image_result["error"]

        # סיכום
        total_processing_time = time.time() - start_time
        self.stats["combined_analyses"] += 1

        results.update({
            "total_processing_time": round(total_processing_time, 3),
            "system": "Keywords + BiomedCLIP",
            "efficiency": "High - minimal memory, instant response",
            "timestamp": time.time()
        })

        return results

# מערכת גלובלית
analysis_system = LiteMedicalAnalysisSystem()

# =============================================================================
# API ENDPOINTS
# =============================================================================


@app.route('/text/analyze', methods=['POST'])
def analyze_text_endpoint():
    """ניתוח טקסט"""

    try:
        data = request.get_json()
        if not data or 'text' not in data:
            return jsonify({"error": "Missing 'text' field"}), HTTP_BAD_REQUEST

        text = data['text'].strip()
        if not text:
            return jsonify({"error": "Text cannot be empty"}), HTTP_BAD_REQUEST

        result = analysis_system.analyze_text(text)

        if result["success"]:
            return jsonify(result)
        else:
            return jsonify(result), HTTP_INTERNAL_ERROR

    except Exception as e:
        return jsonify({"error": str(e)}), HTTP_INTERNAL_ERROR

@app.route('/image/analyze', methods=['POST'])
def analyze_image_endpoint():
    """ניתוח תמונה"""

    try:
        image_data = None

        if request.is_json:
            data = request.get_json()
            if not data or 'image' not in data:
                return jsonify({"error": "Missing 'image' field"}), HTTP_BAD_REQUEST
            image_data = data['image']
        else:
            if 'image' not in request.files:
                return jsonify({"error": "No image file provided"}), HTTP_BAD_REQUEST
            file = request.files['image']
            if file.filename == '':
                return jsonify({"error": "No image selected"}), HTTP_BAD_REQUEST
            image_data = file.read()

        result = analysis_system.analyze_image(image_data)

        if result["success"]:
            return jsonify(result)
        else:
            return jsonify(result), HTTP_INTERNAL_ERROR

    except Exception as e:
        return jsonify({"error": str(e)}), HTTP_INTERNAL_ERROR

@app.route('/analyze/combined', methods=['POST'])
def analyze_combined_endpoint():
    """ניתוח משולב"""

    try:
        data = request.get_json()
        if not data:
            return jsonify({"error": "No data provided"}), HTTP_BAD_REQUEST

        text = data.get('text', '').strip()
        image_data = data.get('image')

        if not text and not image_data:
            return jsonify({"error": "Provide either text or image"}), HTTP_BAD_REQUEST

        result = analysis_system.analyze_combined(text, image_data)
        return jsonify(result)

    except Exception as e:
        return jsonify({"error": str(e)}), HTTP_INTERNAL_ERROR


if __name__ == '__main__':
    print(f" Endpoints:")
    print(f"   POST /text/analyze - Text analysis")
    print(f"   POST /image/analyze - Image analysis")
    print(f"   POST /analyze/combined - Combined analysis")
    print(DEBUG_SEPARATOR_LONG)

    app.run(host=SERVER_HOST, port=SERVER_PORT_LITE, debug=False, threaded=True)