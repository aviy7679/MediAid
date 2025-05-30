"""
Symptom Extractor using MedCAT
מחלק סימפטומים רפואיים
"""

import json
import time
from medcat.cat import CAT


class SymptomExtractor:
    """מחלק לחילוץ סימפטומים מטקסט"""

    MODEL_PATH = r"D:\MediAid\umls-2024AB-full\umls_sm_pt2ch_533bab5115c6c2d6.zip"

    def __init__(self):
        self.cat = None
        self.is_loaded = False

    def load_model(self):
        """טעינת המודל"""
        if self.is_loaded:
            return True

        try:
            print("מתחיל לטעון את מודל MedCAT...")
            start_time = time.time()

            self.cat = CAT.load_model_pack(self.MODEL_PATH)

            end_time = time.time()
            self.is_loaded = True
            print(f"המודל נטען בהצלחה! זמן טעינה: {end_time - start_time:.2f} שניות")
            return True

        except Exception as e:
            print(f"שגיאה בטעינת המודל: {e}")
            self.is_loaded = False
            return False

    def extract_symptoms(self, text: str):
        """חילוץ סימפטומים מהטקסט"""
        if not self.is_loaded:
            raise RuntimeError("המודל לא נטען. יש לקרוא ל-load_model() תחילה")

        try:
            entities = self.cat.get_entities(text)
            symptoms = []

            for ent_id, data in entities.get("entities", {}).items():
                type_ids = data.get("type_ids", [])
                # T184 = Sign or Symptom לפי UMLS
                if "T184" in type_ids:
                    symptom = {
                        "cui": data.get("cui"),
                        "name": data.get("pretty_name"),
                        "detected_name": data.get("detected_name"),
                        "start": data.get("start"),
                        "end": data.get("end"),
                        "accuracy": data.get("acc"),
                        "status": data.get("meta_anns", {}).get("Status", {}).get("value"),
                        "status_confidence": data.get("meta_anns", {}).get("Status", {}).get("confidence")
                    }
                    symptoms.append(symptom)

            return symptoms

        except Exception as e:
            print(f"שגיאה בחילוץ סימפטומים: {e}")
            return []

    def get_symptoms_summary(self, text: str):
        """קבלת סיכום של הסימפטומים"""
        symptoms = self.extract_symptoms(text)

        return {
            "original_text": text,
            "symptoms_count": len(symptoms),
            "symptoms": symptoms
        }


# יצירת instance גלובלי
extractor = SymptomExtractor()


def initialize_model():
    """אתחול המודל - לקריאה פעם אחת בהרצת השרת"""
    return extractor.load_model()


def process_text(text: str):
    """עיבוד טקסט והחזרת סימפטומים"""
    return extractor.get_symptoms_summary(text)


# דוגמה לשימוש
if __name__ == "__main__":
    if initialize_model():
        text = "I have been suffering from a high fever since yesterday and I also have a headache."
        result = process_text(text)
        print(json.dumps(result, indent=2, ensure_ascii=False))
    else:
        print("כישלון באתחול המודל")