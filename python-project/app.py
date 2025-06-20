# """
# שרת API משולב לניתוח סימפטומים מטקסט ותמונות
# משתמש במחלקות ייעודיות נפרדות לכל סוג ניתוח
# """
# from flask import Flask, request, jsonify
# from text_analyzer import TextAnalyzer
# from image_analyzer import ImageAnalyzer
# import logging
# import os
# import time
#
# # הגדרת logging
# logging.basicConfig(level=logging.INFO)
# logger = logging.getLogger(__name__)
#
# app = Flask(__name__)
#
# # הגדרות
# TEXT_MODEL_PATH = "D:\\MediAid\\umls-2024AB-full\\umls_self_train_model_pt2ch_3760d588371755d0.zip"
# IMAGE_MIN_CONFIDENCE = 0.1
#
# # המנתחים הייעודיים
# text_analyzer = None
# image_analyzer = None
#
# def initialize_analyzers():
#     """אתחול שני המודלים בעת הפעלת השרת"""
#     global text_analyzer, image_analyzer
#
#     try:
#         start_time = time.time()
#         logger.info("Starting to initialize analyzers...")
#         print("="*60)
#
#         # בדיקת קיום קובץ המודל לטקסט
#         if not os.path.exists(TEXT_MODEL_PATH):
#             raise FileNotFoundError(f"Text model file not found: {TEXT_MODEL_PATH}")
#
#         # יצירת המנתחים
#         print("Initializing Text Analyzer...")
#         text_analyzer = TextAnalyzer(model_path=TEXT_MODEL_PATH)
#         text_analyzer.load_model()
#
#         print("Initializing Image Analyzer...")
#         image_analyzer = ImageAnalyzer(min_confidence=IMAGE_MIN_CONFIDENCE)
#         image_analyzer.load_model()
#
#         end_time = time.time()
#         print("="*60)
#         logger.info(f"Both analyzers initialized successfully in {end_time - start_time:.2f} seconds")
#
#     except Exception as e:
#         logger.error(f"Failed to initialize analyzers: {e}")
#         raise
#
# @app.route('/health', methods=['GET'])
# def health_check():
#     """בדיקת תקינות השרת"""
#     text_ready = text_analyzer and text_analyzer.is_ready()
#     image_ready = image_analyzer and image_analyzer.is_ready()
#
#     return jsonify({
#         "status": "healthy" if (text_ready and image_ready) else "partial",
#         "service": "combined-symptom-analyzer",
#         "analyzers": {
#             "text_analyzer_ready": text_ready,
#             "image_analyzer_ready": image_ready
#         },
#         "ready": text_ready and image_ready
#     })
#
# @app.route('/status', methods=['GET'])
# def get_detailed_status():
#     """מידע מפורט על סטטוס המערכת"""
#     response = {
#         "success": True,
#         "text_analyzer": text_analyzer.get_status() if text_analyzer else {"error": "Not initialized"},
#         "image_analyzer": image_analyzer.get_status() if image_analyzer else {"error": "Not initialized"}
#     }
#     return jsonify(response)
#
# # =============================================================================
# # TEXT ANALYSIS ENDPOINTS
# # =============================================================================
#
# @app.route('/text/analyze', methods=['POST'])
# def analyze_text_full():
#     """
#     ניתוח טקסט מלא לחילוץ סימפטומים
#
#     Expected JSON:
#     {
#         "text": "I have headache and fever"
#     }
#     """
#     try:
#         if not text_analyzer or not text_analyzer.is_ready():
#             return jsonify({
#                 "success": False,
#                 "error": "Text analyzer not ready"
#             }), 500
#
#         data = request.get_json()
#
#         if not data or 'text' not in data:
#             return jsonify({
#                 "success": False,
#                 "error": "Missing 'text' field in request"
#             }), 400
#
#         text = data['text'].strip()
#
#         if not text:
#             return jsonify({
#                 "success": False,
#                 "error": "Text cannot be empty"
#             }), 400
#
#         logger.info(f"Analyzing text: {text[:100]}...")
#
#         # ביצוע הניתוח
#         result = text_analyzer.analyze_text(text)
#
#         response = {
#             "success": True,
#             "data": result
#         }
#
#         logger.info(f"Text analysis completed. Found {result['symptoms_found']} symptoms")
#         return jsonify(response)
#
#     except Exception as e:
#         logger.error(f"Error in text analysis: {e}")
#         return jsonify({
#             "success": False,
#             "error": str(e)
#         }), 500
#
# @app.route('/text/symptoms', methods=['POST'])
# def extract_text_symptoms_only():
#     """חילוץ סימפטומים בלבד מטקסט (ללא מידע נוסף)"""
#     try:
#         if not text_analyzer or not text_analyzer.is_ready():
#             return jsonify({
#                 "success": False,
#                 "error": "Text analyzer not ready"
#             }), 500
#
#         data = request.get_json()
#
#         if not data or 'text' not in data:
#             return jsonify({
#                 "success": False,
#                 "error": "Missing 'text' field in request"
#             }), 400
#
#         text = data['text'].strip()
#
#         if not text:
#             return jsonify({
#                 "success": False,
#                 "error": "Text cannot be empty"
#             }), 400
#
#         # ביצוע הניתוח
#         symptoms = text_analyzer.extract_symptoms(text)
#
#         return jsonify({
#             "success": True,
#             "symptoms": symptoms
#         })
#
#     except Exception as e:
#         logger.error(f"Error in text symptom extraction: {e}")
#         return jsonify({
#             "success": False,
#             "error": str(e)
#         }), 500
#
# # =============================================================================
# # IMAGE ANALYSIS ENDPOINTS
# # =============================================================================
#
# @app.route('/image/analyze', methods=['POST'])
# def analyze_image_full():
#     """
#     ניתוח תמונה מלא לחילוץ סימפטומים
#
#     Expected JSON:
#     {
#         "image": "base64_encoded_image_data",
#         "min_confidence": 0.1  // אופציונלי
#     }
#
#     או קובץ תמונה ב-multipart/form-data
#     """
#     try:
#         if not image_analyzer or not image_analyzer.is_ready():
#             return jsonify({
#                 "success": False,
#                 "error": "Image analyzer not ready"
#             }), 500
#
#         image_data = None
#         min_confidence = None
#
#         # בדיקה אם זה JSON או form-data
#         if request.is_json:
#             data = request.get_json()
#
#             if not data or 'image' not in data:
#                 return jsonify({
#                     "success": False,
#                     "error": "Missing 'image' field in request"
#                 }), 400
#
#             image_data = data['image']
#             min_confidence = data.get('min_confidence')
#
#         else:
#             # form-data עם קובץ
#             if 'image' not in request.files:
#                 return jsonify({
#                     "success": False,
#                     "error": "No image file provided"
#                 }), 400
#
#             file = request.files['image']
#             if file.filename == '':
#                 return jsonify({
#                     "success": False,
#                     "error": "No image file selected"
#                 }), 400
#
#             # קריאת הקובץ
#             image_data = file.read()
#             min_confidence_str = request.form.get('min_confidence')
#             if min_confidence_str:
#                 try:
#                     min_confidence = float(min_confidence_str)
#                 except ValueError:
#                     return jsonify({
#                         "success": False,
#                         "error": "Invalid min_confidence value"
#                     }), 400
#
#         logger.info("Analyzing image...")
#
#         # ביצוע הניתוח
#         result = image_analyzer.analyze_image(image_data, min_confidence)
#
#         response = {
#             "success": True,
#             "data": result
#         }
#
#         logger.info(f"Image analysis completed. Found {result['symptoms_found']} symptoms")
#         return jsonify(response)
#
#     except Exception as e:
#         logger.error(f"Error in image analysis: {e}")
#         return jsonify({
#             "success": False,
#             "error": str(e)
#         }), 500
#
# @app.route('/image/symptoms', methods=['POST'])
# def extract_image_symptoms_only():
#     """חילוץ סימפטומים בלבד מתמונה (ללא מידע נוסף)"""
#     try:
#         if not image_analyzer or not image_analyzer.is_ready():
#             return jsonify({
#                 "success": False,
#                 "error": "Image analyzer not ready"
#             }), 500
#
#         # לוגיקה דומה לפונקציה הקודמת
#         image_data = None
#         min_confidence = None
#
#         if request.is_json:
#             data = request.get_json()
#
#             if not data or 'image' not in data:
#                 return jsonify({
#                     "success": False,
#                     "error": "Missing 'image' field in request"
#                 }), 400
#
#             image_data = data['image']
#             min_confidence = data.get('min_confidence')
#
#         else:
#             if 'image' not in request.files:
#                 return jsonify({
#                     "success": False,
#                     "error": "No image file provided"
#                 }), 400
#
#             file = request.files['image']
#             if file.filename == '':
#                 return jsonify({
#                     "success": False,
#                     "error": "No image file selected"
#                 }), 400
#
#             image_data = file.read()
#             min_confidence_str = request.form.get('min_confidence')
#             if min_confidence_str:
#                 try:
#                     min_confidence = float(min_confidence_str)
#                 except ValueError:
#                     return jsonify({
#                         "success": False,
#                         "error": "Invalid min_confidence value"
#                     }), 400
#
#         # ביצוע הניתוח
#         symptoms = image_analyzer.extract_symptoms(image_data, min_confidence)
#
#         return jsonify({
#             "success": True,
#             "symptoms": symptoms
#         })
#
#     except Exception as e:
#         logger.error(f"Error in image symptom extraction: {e}")
#         return jsonify({
#             "success": False,
#             "error": str(e)
#         }), 500
#
# @app.route('/image/confidence', methods=['POST'])
# def set_image_confidence_threshold():
#     """
#     עדכון סף הביטחון המינימום לתמונות
#
#     Expected JSON:
#     {
#         "threshold": 0.2
#     }
#     """
#     try:
#         if not image_analyzer:
#             return jsonify({
#                 "success": False,
#                 "error": "Image analyzer not initialized"
#             }), 500
#
#         data = request.get_json()
#
#         if not data or 'threshold' not in data:
#             return jsonify({
#                 "success": False,
#                 "error": "Missing 'threshold' field in request"
#             }), 400
#
#         try:
#             threshold = float(data['threshold'])
#             image_analyzer.set_confidence_threshold(threshold)
#
#             return jsonify({
#                 "success": True,
#                 "message": f"Image confidence threshold updated to {threshold}"
#             })
#
#         except ValueError as e:
#             return jsonify({
#                 "success": False,
#                 "error": str(e)
#             }), 400
#
#     except Exception as e:
#         logger.error(f"Error setting confidence threshold: {e}")
#         return jsonify({
#             "success": False,
#             "error": str(e)
#         }), 500
#
# # =============================================================================
# # COMBINED ANALYSIS ENDPOINTS
# # =============================================================================
#
# @app.route('/analyze/combined', methods=['POST'])
# def analyze_combined():
#     """
#     ניתוח משולב של טקסט ותמונה יחד
#
#     Expected JSON:
#     {
#         "text": "Patient description...",
#         "image": "base64_encoded_image_data",
#         "min_confidence": 0.1
#     }
#     """
#     try:
#         data = request.get_json()
#
#         if not data:
#             return jsonify({
#                 "success": False,
#                 "error": "No data provided"
#             }), 400
#
#         results = {}
#
#         # ניתוח טקסט אם סופק
#         if 'text' in data and data['text'].strip():
#             if text_analyzer and text_analyzer.is_ready():
#                 try:
#                     text_result = text_analyzer.analyze_text(data['text'])
#                     results['text_analysis'] = text_result
#                 except Exception as e:
#                     results['text_analysis'] = {"error": str(e)}
#             else:
#                 results['text_analysis'] = {"error": "Text analyzer not ready"}
#
#         # ניתוח תמונה אם סופק
#         if 'image' in data and data['image']:
#             if image_analyzer and image_analyzer.is_ready():
#                 try:
#                     min_confidence = data.get('min_confidence')
#                     image_result = image_analyzer.analyze_image(data['image'], min_confidence)
#                     results['image_analysis'] = image_result
#                 except Exception as e:
#                     results['image_analysis'] = {"error": str(e)}
#             else:
#                 results['image_analysis'] = {"error": "Image analyzer not ready"}
#
#         if not results:
#             return jsonify({
#                 "success": False,
#                 "error": "No valid text or image provided"
#             }), 400
#
#         return jsonify({
#             "success": True,
#             "data": results
#         })
#
#     except Exception as e:
#         logger.error(f"Error in combined analysis: {e}")
#         return jsonify({
#             "success": False,
#             "error": str(e)
#         }), 500
#
# # =============================================================================
# # ERROR HANDLERS
# # =============================================================================
#
# @app.errorhandler(404)
# def not_found(error):
#     return jsonify({
#         "success": False,
#         "error": "Endpoint not found"
#     }), 404
#
# @app.errorhandler(500)
# def internal_error(error):
#     return jsonify({
#         "success": False,
#         "error": "Internal server error"
#     }), 500
#
# @app.errorhandler(413)
# def too_large(error):
#     return jsonify({
#         "success": False,
#         "error": "File too large"
#     }), 413
#
# if __name__ == '__main__':
#     try:
#         print("Starting Combined Symptom Analysis Server...")
#         print("="*60)
#         # אתחול המנתחים
#         initialize_analyzers()
#
#         print("\nServer Ready!")
#
#         # הגדרת גודל קובץ מקסימלי
#         app.config['MAX_CONTENT_LENGTH'] = 16 * 1024 * 1024
#
#         app.run(host='0.0.0.0', port=5000, debug=False)
#
#     except Exception as e:
#         logger.error(f"Failed to start server: {e}")
#         print(f"Failed to start server: {e}")
#         exit(1)
'''
"""
שרת API משולב לניתוח סימפטומים מטקסט ותמונות
משתמש במחלקות ייעודיות נפרדות לכל סוג ניתוח
"""
from flask import Flask, request, jsonify
from text_analyzer1 import TextAnalyzer
from image_analyzer1 import ImageAnalyzer
import logging
import os
import time
import threading
import gc

# הגדרת logging
logging.basicConfig(
    level=logging.INFO,
    format='%(asctime)s - %(name)s - %(levelname)s - %(message)s'
)
logger = logging.getLogger(__name__)

app = Flask(__name__)

# הגדרות
TEXT_MODEL_PATH = "D:\\MediAid\\umls-2024AB-full\\umls_self_train_model_pt2ch_3760d588371755d0.zip"
IMAGE_MIN_CONFIDENCE = 0.1

# המנתחים הייעודיים
text_analyzer = None
image_analyzer = None
analyzers_ready = {"text": False, "image": False}
initialization_lock = threading.Lock()


def initialize_analyzer_async(analyzer_type):
    """אתחול אסינכרוני של מנתח ספציפי"""
    global text_analyzer, image_analyzer, analyzers_ready

    try:
        if analyzer_type == "text":
            logger.info("מתחיל אתחול Text Analyzer...")
            if not os.path.exists(TEXT_MODEL_PATH):
                raise FileNotFoundError(f"Text model file not found: {TEXT_MODEL_PATH}")

            text_analyzer = TextAnalyzer(model_path=TEXT_MODEL_PATH)
            text_analyzer.load_model()
            analyzers_ready["text"] = True
            logger.info("Text Analyzer מוכן לשימוש")

        elif analyzer_type == "image":
            logger.info("מתחיל אתחול Image Analyzer...")
            image_analyzer = ImageAnalyzer(min_confidence=IMAGE_MIN_CONFIDENCE)
            image_analyzer.load_model()
            analyzers_ready["image"] = True
            logger.info("Image Analyzer מוכן לשימוש")

    except Exception as e:
        logger.error(f"שגיאה באתחול {analyzer_type} analyzer: {e}")
        analyzers_ready[analyzer_type] = False


def initialize_analyzers():
    """אתחול שני המודלים במקביל"""
    global text_analyzer, image_analyzer

    try:
        logger.info("מתחיל אתחול המנתחים...")
        print("=" * 60)

        # אתחול במקביל
        text_thread = threading.Thread(target=initialize_analyzer_async, args=("text",))
        image_thread = threading.Thread(target=initialize_analyzer_async, args=("image",))

        text_thread.start()
        image_thread.start()

        # המתנה לסיום
        text_thread.join()
        image_thread.join()

        print("=" * 60)
        logger.info(f"אתחול הושלם - Text: {analyzers_ready['text']}, Image: {analyzers_ready['image']}")

        # ניקוי זיכרון אחרי אתחול
        gc.collect()

    except Exception as e:
        logger.error(f"שגיאה באתחול המנתחים: {e}")
        raise


@app.route('/health', methods=['GET'])
def health_check():
    """בדיקת תקינות השרת"""
    text_ready = analyzers_ready.get("text", False)
    image_ready = analyzers_ready.get("image", False)

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
        "image_analyzer": image_analyzer.get_status() if image_analyzer else {"error": "Not initialized"},
        "ready_status": analyzers_ready
    }
    return jsonify(response)


# =============================================================================
# TEXT ANALYSIS ENDPOINTS
# =============================================================================

@app.route('/text/analyze', methods=['POST'])
def analyze_text_full():
    """
    ניתוח טקסט מלא לחילוץ ישויות רפואיות

    Expected JSON:
    {
        "text": "I have headache and fever"
    }
    """
    try:
        if not analyzers_ready.get("text", False):
            return jsonify({
                "success": False,
                "error": "Text analyzer not ready"
            }), 503

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

        logger.info(f"מנתח טקסט: {text[:100]}...")

        # ביצוע הניתוח
        result = text_analyzer.analyze_text(text)

        logger.info(f"ניתוח הושלם. נמצאו {result.get('entities_found', 0)} ישויות רפואיות")

        return jsonify({
            "success": True,
            "data": result
        })

    except Exception as e:
        logger.error(f"שגיאה בניתוח טקסט: {e}")
        return jsonify({
            "success": False,
            "error": str(e)
        }), 500


@app.route('/text/debug', methods=['POST'])
def debug_text_analysis():
    """endpoint לדיבוג - מציג את כל ה-entities שנמצאו"""
    try:
        if not analyzers_ready.get("text", False):
            return jsonify({
                "success": False,
                "error": "Text analyzer not ready"
            }), 503

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

        # ביצוע דיבוג
        entities = text_analyzer.debug_entities(text)

        return jsonify({
            "success": True,
            "text": text,
            "raw_entities": entities,
            "total_entities": len(entities.get("entities", {}))
        })

    except Exception as e:
        logger.error(f"שגיאה בדיבוג טקסט: {e}")
        return jsonify({
            "success": False,
            "error": str(e)
        }), 500


@app.route('/text/entities', methods=['POST'])
def extract_medical_entities_only():
    """חילוץ ישויות רפואיות בלבד מטקסט (ללא מידע נוסף)"""
    try:
        if not analyzers_ready.get("text", False):
            return jsonify({
                "success": False,
                "error": "Text analyzer not ready"
            }), 503

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
        medical_entities = text_analyzer.extract_symptoms(text)

        return jsonify({
            "success": True,
            "medical_entities": medical_entities,
            "count": len(medical_entities)
        })

    except Exception as e:
        logger.error(f"Error in medical entity extraction: {e}")
        return jsonify({
            "success": False,
            "error": str(e)
        }), 500


@app.route('/text/all_entities', methods=['POST'])
def extract_all_entities():
    """חילוץ כל הישויות מטקסט (לדיבוג)"""
    try:
        if not analyzers_ready.get("text", False):
            return jsonify({
                "success": False,
                "error": "Text analyzer not ready"
            }), 503

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
        all_entities = text_analyzer.extract_all_entities(text)

        return jsonify({
            "success": True,
            "text": text,
            "all_entities": all_entities,
            "count": len(all_entities)
        })

    except Exception as e:
        logger.error(f"Error in all entities extraction: {e}")
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
        if not analyzers_ready.get("image", False):
            return jsonify({
                "success": False,
                "error": "Image analyzer not ready"
            }), 503

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

        logger.info("מנתח תמונה...")

        # ביצוע הניתוח
        result = image_analyzer.analyze_image(image_data, min_confidence)

        logger.info(f"ניתוח תמונה הושלם. נמצאו {result['symptoms_found']} סימפטומים")

        return jsonify({
            "success": True,
            "data": result
        })

    except Exception as e:
        logger.error(f"שגיאה בניתוח תמונה: {e}")
        return jsonify({
            "success": False,
            "error": str(e)
        }), 500


@app.route('/text/symptoms', methods=['POST'])
def extract_symptoms_only():
    """חילוץ סימפטומים בלבד מטקסט (לתאימות לאחור)"""
    try:
        if not analyzers_ready.get("text", False):
            return jsonify({
                "success": False,
                "error": "Text analyzer not ready"
            }), 503

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

        # ביצוע הניתוח וסינון רק סימפטומים
        medical_entities = text_analyzer.extract_symptoms(text)

        # סינון רק סימפטומים ו-findings
        symptoms = [entity for entity in medical_entities
                    if entity.get("entity_category") in ["symptom"]]

        return jsonify({
            "success": True,
            "symptoms": symptoms,
            "count": len(symptoms)
        })

    except Exception as e:
        logger.error(f"Error in symptom extraction: {e}")
        return jsonify({
            "success": False,
            "error": str(e)
        }), 500


# הוסף את שני ה-endpoints החדשים אחרי זה...

@app.route('/image/symptoms', methods=['POST'])
def extract_image_symptoms_only():
    """חילוץ סימפטומים בלבד מתמונה (ללא מידע נוסף)"""
    try:
        if not analyzers_ready.get("image", False):
            return jsonify({
                "success": False,
                "error": "Image analyzer not ready"
            }), 503

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
        print("מפעיל שרת ניתוח סימפטומים משולב...")
        print("=" * 60)

        # אתחול המנתחים
        initialize_analyzers()

        print("\nשרת מוכן!")

        # הגדרת גודל קובץ מקסימלי
        app.config['MAX_CONTENT_LENGTH'] = 16 * 1024 * 1024

        app.run(host='0.0.0.0', port=5000, debug=False, threaded=True)

    except Exception as e:
        logger.error(f"שגיאה בהפעלת השרת: {e}")
        print(f"שגיאה בהפעלת השרת: {e}")
        exit(1)
'''
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

# הגדרת logging
logging.basicConfig(level=logging.INFO, format='%(asctime)s - %(levelname)s - %(message)s')
logger = logging.getLogger(__name__)

app = Flask(__name__)
app.config['MAX_CONTENT_LENGTH'] = 16 * 1024 * 1024  # 16MB max file size

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
            return jsonify({"error": "Text analyzer not ready"}), 503

        data = request.get_json()
        if not data or 'text' not in data:
            return jsonify({"error": "Missing 'text' field"}), 400

        text = data['text'].strip()
        if not text:
            return jsonify({"error": "Text cannot be empty"}), 400

        # ניתוח
        symptoms = text_analyzer.extract_symptoms(text)

        return jsonify({
            "success": True,
            "symptoms": symptoms,
            "count": len(symptoms)
        })

    except Exception as e:
        logger.error(f"Error in text analysis: {e}")
        return jsonify({"error": str(e)}), 500


@app.route('/text/entities', methods=['POST'])
def get_all_text_entities():
    """
    קבלת כל הישויות מטקסט (לא רק סימפטומים)

    Body: {"text": "I have headache and fever"}
    """
    try:
        if not text_analyzer or not text_analyzer.is_loaded:
            return jsonify({"error": "Text analyzer not ready"}), 503

        data = request.get_json()
        if not data or 'text' not in data:
            return jsonify({"error": "Missing 'text' field"}), 400

        text = data['text'].strip()
        if not text:
            return jsonify({"error": "Text cannot be empty"}), 400

        # קבלת כל הישויות
        all_entities = text_analyzer.get_all_entities(text)

        return jsonify({
            "success": True,
            "entities": all_entities
        })

    except Exception as e:
        logger.error(f"Error getting all entities: {e}")
        return jsonify({"error": str(e)}), 500


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
            return jsonify({"error": "Image analyzer not ready"}), 503

        image_data = None

        # בדיקה אם זה JSON או form-data
        if request.is_json:
            data = request.get_json()
            if not data or 'image' not in data:
                return jsonify({"error": "Missing 'image' field"}), 400
            image_data = data['image']

        else:
            # form-data עם קובץ
            if 'image' not in request.files:
                return jsonify({"error": "No image file provided"}), 400

            file = request.files['image']
            if file.filename == '':
                return jsonify({"error": "No image selected"}), 400

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
        return jsonify({"error": str(e)}), 500


@app.route('/image/top_predictions', methods=['POST'])
def get_top_image_predictions():
    """
    קבלת המצבים הסבירים ביותר מתמונה

    Body: {"image": "base64_data", "top_k": 5}
    """
    try:
        if not image_analyzer or not image_analyzer.is_loaded:
            return jsonify({"error": "Image analyzer not ready"}), 503

        data = request.get_json()
        if not data or 'image' not in data:
            return jsonify({"error": "Missing 'image' field"}), 400

        top_k = data.get('top_k', 5)

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
        return jsonify({"error": str(e)}), 500


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
            return jsonify({"error": "No data provided"}), 400

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
                    results['image_predictions'] = image_results[:5]
                    results['image_count'] = len(image_results[:5])
                except Exception as e:
                    results['image_error'] = f"Error processing image: {str(e)}"
            else:
                results['image_error'] = "Image analyzer not ready"

        return jsonify(results)

    except Exception as e:
        logger.error(f"Error in combined analysis: {e}")
        return jsonify({"error": str(e)}), 500


# =============================================================================
# ERROR HANDLERS
# =============================================================================

@app.errorhandler(404)
def not_found(error):
    return jsonify({"error": "Endpoint not found"}), 404


@app.errorhandler(413)
def too_large(error):
    return jsonify({"error": "File too large (max 16MB)"}), 413


@app.errorhandler(500)
def internal_error(error):
    return jsonify({"error": "Internal server error"}), 500


# =============================================================================
# MAIN
# =============================================================================

if __name__ == '__main__':
    try:
        print("מפעיל שרת ניתוח סימפטומים...")
        print("=" * 50)

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
        print("=" * 50)

        app.run(host='0.0.0.0', port=5000, debug=False, threaded=True)

    except Exception as e:
        logger.error(f"שגיאה בהפעלת השרת: {e}")
        print(f"שגיאה: {e}")
        exit(1)