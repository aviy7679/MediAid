"""
קבועי המערכת - Configuration Constants
מכיל את כל הקבועים במערכת לצורך בדיקה קלה ותחזוקה
"""
import os

# =============================================================================
# NETWORK & SERVER CONFIGURATION
# =============================================================================

# Server Ports
SERVER_PORT_MAIN = 5000  # Port עיקרי - Flask default port, נפוץ לפיתוח
SERVER_PORT_LITE = 5001  # Port למערכת קלה - +1 מהעיקרי להפרדה
SERVER_HOST = '0.0.0.0'  # כל הממשקים - לגישה חיצונית

# Request Timeouts
REQUEST_TIMEOUT_SHORT = 5  # שניות - בדיקת חיבור מהירה
REQUEST_TIMEOUT_LONG = 10  # שניות - בקשות ניתוח מורכבות

# URLs
BASE_URL_LOCALHOST = "http://127.0.0.1"
TEST_SERVER_URL = f"{BASE_URL_LOCALHOST}:{SERVER_PORT_LITE}"

# =============================================================================
# MEMORY & PERFORMANCE
# =============================================================================

# File Size Limits
MAX_FILE_SIZE_MB = 16  # MB - גבול סביר לתמונות רפואיות
MAX_FILE_SIZE_BYTES = MAX_FILE_SIZE_MB * 1024 * 1024  # Conversion to bytes

# Memory Thresholds
MEMORY_CHECK_PROXIMITY_CHARS = 40  # תווים - לבדיקת קרבה בטקסט
MEMORY_CHECK_TEXT_PREVIEW = 20  # תווים - לבדיקת negation לפני סימפטום
MEMORY_CHECK_TEXT_CONTEXT = 100  # תווים - לוג תצוגה קצרה של טקסט

# Performance Constants
CONTEXT_LENGTH_DEFAULT = 256  # Tokens - מאוזן בין דיוק למהירות
TEXT_PREVIEW_LENGTH = 100  # תווים - לוג debugging
TEXT_PREVIEW_SHORT = 50  # תווים - הודעות קצרות

# =============================================================================
# MODEL PATHS & CONFIGURATIONS
# =============================================================================

# Model Paths (מותאמים למערכת הנוכחית)
BASE_MODEL_DIR = "D:\\MediAid\\umls-2024AB-full"
TEXT_MODEL_FILENAME = "umls_self_train_model_pt2ch_3760d588371755d0.zip"
TEXT_MODEL_FILENAME_ALT = "umls_sm_pt2ch_533bab5115c6c2d6.zip"  # גרסה חלופית
TEXT_MODEL_PATH_MAIN = os.path.join(BASE_MODEL_DIR, TEXT_MODEL_FILENAME)
TEXT_MODEL_PATH_ALT = os.path.join(BASE_MODEL_DIR, TEXT_MODEL_FILENAME_ALT)

# Model Names
BIOMEDCLIP_MODEL_NAME = 'hf-hub:microsoft/BiomedCLIP-PubMedBERT_256-vit_base_patch16_224'

# =============================================================================
# CONFIDENCE & ACCURACY THRESHOLDS
# =============================================================================

# Image Analysis Confidence
IMAGE_MIN_CONFIDENCE_DEFAULT = 0.1  # נמוך - כדי לקבל יותר תוצאות
IMAGE_CONFIDENCE_THRESHOLD = 0.2  # בינוני - איזון דיוק/כמות

# Text Analysis Confidence
TEXT_CONFIDENCE_HIGH = 0.95  # גבוה - סימפטומים בסיסיים ברורים
TEXT_CONFIDENCE_MEDIUM = 0.9  # בינוני - סימפטומים רגילים
TEXT_CONFIDENCE_LOW = 0.85  # נמוך - סימפטומים כלליים

# Phrase Enhancement Factors
INTENSITY_FACTOR_HIGH = 1.3  # "extreme" - הגברה חזקה
INTENSITY_FACTOR_MEDIUM = 1.2  # "severe", "intense" - הגברה בינונית
INTENSITY_FACTOR_LOW = 1.1  # "chronic", "persistent" - הגברה קלה
INTENSITY_FACTOR_MILD = 0.8  # "mild" - הנמכה קלה
INTENSITY_FACTOR_SLIGHT = 0.7  # "slight", "minor" - הנמכה בינונית

# =============================================================================
# MEDICAL ENTITY TYPES (UMLS Semantic Types)
# =============================================================================

# UMLS Semantic Type IDs
UMLS_TYPE_SIGN_SYMPTOM = "T184"  # Sign or Symptom
UMLS_TYPE_FINDING = "T033"  # Finding
UMLS_TYPE_DISEASE = "T047"  # Disease or Syndrome

# =============================================================================
# LOGGING & DEBUG
# =============================================================================

# Log Levels
LOG_LEVEL_INFO = "INFO"
LOG_LEVEL_DEBUG = "DEBUG"
LOG_LEVEL_ERROR = "ERROR"

# Debug Output
DEBUG_ENTITY_DISPLAY_LIMIT = 10  # מספר entities להציג בדיבוג
DEBUG_SEPARATOR_SHORT = "-" * 40
DEBUG_SEPARATOR_LONG = "=" * 60
DEBUG_SEPARATOR_HEADER = "=" * 50

# =============================================================================
# MEDICAL CATEGORIES
# =============================================================================

# Medical System Categories
CATEGORY_CARDIOVASCULAR = "cardiovascular"
CATEGORY_RESPIRATORY = "respiratory"
CATEGORY_NEUROLOGICAL = "neurological"
CATEGORY_GASTROINTESTINAL = "gastrointestinal"
CATEGORY_MUSCULOSKELETAL = "musculoskeletal"
CATEGORY_DERMATOLOGICAL = "dermatological"
CATEGORY_PSYCHOLOGICAL = "psychological"
CATEGORY_SYSTEMIC = "systemic"
CATEGORY_VISUAL = "visual"
CATEGORY_SLEEP = "sleep"
CATEGORY_PAIN = "pain"
CATEGORY_GENERAL = "general"

# =============================================================================
# API RESPONSE CODES
# =============================================================================

# HTTP Status Codes
HTTP_OK = 200
HTTP_BAD_REQUEST = 400
HTTP_NOT_FOUND = 404
HTTP_PAYLOAD_TOO_LARGE = 413
HTTP_INTERNAL_ERROR = 500
HTTP_SERVICE_UNAVAILABLE = 503

# =============================================================================
# SYSTEM COMPARISON CONSTANTS
# =============================================================================

# Memory Usage Estimates (MB)
LITE_SYSTEM_MEMORY_MIN = 50  # MB - מינימום לסיסטם קלה
LITE_SYSTEM_MEMORY_MAX = 100  # MB - מקסימום לסיסטם קלה
FULL_SYSTEM_MEMORY_MIN = 3000  # MB - מינימום לסיסטם מלאה
FULL_SYSTEM_MEMORY_MAX = 4000  # MB - מקסימום לסיסטם מלאה

# Performance Multipliers
LITE_MEMORY_FACTOR = 40  # פי כמה פחות זיכרון מהמערכת המלאה
LITE_SPEED_LOAD_FACTOR = 10  # פי כמה מהירות טעינה
LITE_SPEED_RESPONSE_FACTOR = 5  # פי כמה מהירות תגובה

# Accuracy Estimates
LITE_ACCURACY_PERCENT = 85  # % דיוק למערכת קלה
FULL_ACCURACY_PERCENT = 95  # % דיוק למערכת מלאה
ACCURACY_DIFFERENCE = FULL_ACCURACY_PERCENT - LITE_ACCURACY_PERCENT

# =============================================================================
# ANALYSIS PARAMETERS
# =============================================================================

# Image Analysis
TOP_PREDICTIONS_DEFAULT = 5  # כמות תחזיות עליונות להציג

# Text Processing
NEGATION_CHECK_DISTANCE = 20  # תווים לבדיקת שלילה לפני סימפטום
WORD_BOUNDARY_CONTEXT = 40  # תווים לבדיקת קרבה בין מילים


# =============================================================================
# HELPER FUNCTIONS
# =============================================================================

def get_model_path_with_fallback():
    """מחזיר נתיב מודל טקסט עם fallback"""
    if os.path.exists(TEXT_MODEL_PATH_MAIN):
        return TEXT_MODEL_PATH_MAIN
    elif os.path.exists(TEXT_MODEL_PATH_ALT):
        return TEXT_MODEL_PATH_ALT
    else:
        raise FileNotFoundError(f"לא נמצא קובץ מודל בנתיבים: {TEXT_MODEL_PATH_MAIN} או {TEXT_MODEL_PATH_ALT}")


def get_server_url(port=None):
    """מחזיר URL שרת מלא"""
    port = port or SERVER_PORT_MAIN
    return f"{BASE_URL_LOCALHOST}:{port}"


def get_memory_usage_display(usage_mb):
    """מחזיר תצוגה יפה של שימוש זיכרון"""
    if usage_mb < 1024:
        return f"{usage_mb:.0f} MB"
    else:
        return f"{usage_mb / 1024:.1f} GB"


# =============================================================================
# VALIDATION
# =============================================================================

def validate_constants():
    """בדיקת תקינות הקבועים"""
    errors = []

    # בדיקת ports
    if SERVER_PORT_MAIN == SERVER_PORT_LITE:
        errors.append("SERVER_PORT_MAIN ו-SERVER_PORT_LITE זהים")

    # בדיקת memory factors
    if LITE_MEMORY_FACTOR <= 1:
        errors.append("LITE_MEMORY_FACTOR צריך להיות גדול מ-1")

    # בדיקת confidence thresholds
    if not (0 <= IMAGE_MIN_CONFIDENCE_DEFAULT <= 1):
        errors.append("IMAGE_MIN_CONFIDENCE_DEFAULT צריך להיות בין 0 ל-1")

    if TEXT_CONFIDENCE_LOW >= TEXT_CONFIDENCE_MEDIUM >= TEXT_CONFIDENCE_HIGH:
        errors.append("סדר confidence thresholds לא תקין")

    if errors:
        raise ValueError(f"שגיאות בקבועים: {'; '.join(errors)}")

    return True


# Run validation on import
if __name__ != "__main__":
    validate_constants()

# =============================================================================
# EXPORT SUMMARY (לקריאה קלה)
# =============================================================================

__all__ = [
    # Server
    'SERVER_PORT_MAIN', 'SERVER_PORT_LITE', 'SERVER_HOST',

    # Timeouts
    'REQUEST_TIMEOUT_SHORT', 'REQUEST_TIMEOUT_LONG',

    # Memory
    'MAX_FILE_SIZE_MB', 'MAX_FILE_SIZE_BYTES',

    # Models
    'TEXT_MODEL_PATH_MAIN', 'TEXT_MODEL_PATH_ALT', 'BIOMEDCLIP_MODEL_NAME',

    # Confidence
    'IMAGE_MIN_CONFIDENCE_DEFAULT', 'TEXT_CONFIDENCE_HIGH', 'TEXT_CONFIDENCE_MEDIUM',

    # Categories
    'CATEGORY_CARDIOVASCULAR', 'CATEGORY_RESPIRATORY', 'CATEGORY_NEUROLOGICAL',

    # HTTP
    'HTTP_OK', 'HTTP_BAD_REQUEST', 'HTTP_INTERNAL_ERROR',

    # Helpers
    'get_model_path_with_fallback', 'get_server_url', 'validate_constants'
]