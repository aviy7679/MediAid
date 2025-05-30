"""
מחלקה ייעודית לניתוח סימפטומים מטקסט באמצעות מודל MedCAT
"""
from medcat.cat import CAT
import logging
import time

class TextAnalyzer:
    def __init__(self, model_path):
        """
        אתחול המחלקה עם טעינת מודל MedCAT

        Args:
            model_path (str): נתיב למודל UMLS
        """
        self.model_path = model_path
        self.cat = None
        self.is_loaded = False
        self.logger = logging.getLogger(__name__)

    def load_model(self):
        """טעינת מודל MedCAT"""
        try:
            start_time = time.time()
            self.logger.info("מתחיל לטעון את מודל MedCAT לטקסט...")

            self.cat = CAT.load_model_pack(self.model_path)
            self.is_loaded = True

            end_time = time.time()
            self.logger.info(f"מודל הטקסט נטען בהצלחה בזמן: {end_time - start_time:.2f} שניות")

        except Exception as e:
            self.logger.error(f"שגיאה בטעינת מודל הטקסט: {e}")
            raise Exception(f"Failed to load MedCAT model: {e}")

    def extract_symptoms(self, text):
        """
        חילוץ סימפטומים מטקסט

        Args:
            text (str): הטקסט לניתוח

        Returns:
            list: רשימת סימפטומים שזוהו
        """
        if not self.is_loaded:
            raise Exception("Text model not loaded. Call load_model() first.")

        try:
            # קבלת entities מהטקסט
            entities = self.cat.get_entities(text)
            symptom_entities = []

            # סינון רק symptoms (T184 = Sign or Symptom)
            for ent_id, data in entities.get("entities", {}).items():
                type_ids = data.get("type_ids", [])

                if "T184" in type_ids:  # T184 = Sign or Symptom לפי UMLS
                    symptom = {
                        "cui": data.get("cui"),
                        "name": data.get("pretty_name"),
                        "detected_name": data.get("detected_name"),
                        "source_value": data.get("source_value"),
                        "start_position": data.get("start"),
                        "end_position": data.get("end"),
                        "accuracy": round(data.get("acc", 0), 4),
                        "context_similarity": round(data.get("context_similarity", 0), 4),
                        "status": data.get("meta_anns", {}).get("Status", {}).get("value", "Unknown"),
                        "confidence": round(data.get("meta_anns", {}).get("Status", {}).get("confidence", 0), 4)
                    }
                    symptom_entities.append(symptom)

            return symptom_entities

        except Exception as e:
            self.logger.error(f"שגיאה בחילוץ סימפטומים מטקסט: {e}")
            raise Exception(f"Failed to extract symptoms from text: {e}")

    def analyze_text(self, text):
        """
        ניתוח מלא של טקסט עם החזרת מידע מפורט

        Args:
            text (str): הטקסט לניתוח

        Returns:
            dict: תוצאות הניתוח המלאות
        """
        symptoms = self.extract_symptoms(text)

        return {
            "original_text": text,
            "symptoms_found": len(symptoms),
            "symptoms": symptoms,
            "analysis_status": "success",
            "analyzer_type": "MedCAT"
        }

    def get_status(self):
        """קבלת סטטוס המודל"""
        return {
            "model_loaded": self.is_loaded,
            "model_path": self.model_path,
            "analyzer_type": "MedCAT Text Analyzer"
        }

    def is_ready(self):
        """בדיקה האם המודל מוכן לשימוש"""
        return self.is_loaded and self.cat is not None