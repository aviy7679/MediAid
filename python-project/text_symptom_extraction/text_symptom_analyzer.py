"""
מחלקה לניתוח סימפטומים מטקסט באמצעות מודל MedCAT
"""
from medcat.cat import CAT
import json
import logging


class TextSymptomAnalyzer:
    def __init__(self, model_path):
        """
        אתחול המחלקה עם טעינת מודל MedCAT

        Args:
            model_path (str): נתיב למודל UMLS
        """
        self.model_path = model_path
        self.cat = None
        self._load_model()

    def _load_model(self):
        """טעינת מודל MedCAT"""
        try:
            print("מתחיל לטעון את מודל UMLS...")
            self.cat = CAT.load_model_pack(self.model_path)
            print("המודל נטען בהצלחה!")
        except Exception as e:
            logging.error(f"שגיאה בטעינת המודל: {e}")
            raise Exception(f"Failed to load MedCAT model: {e}")

    def extract_symptoms(self, text):
        """
        חילוץ סימפטומים מטקסט

        Args:
            text (str): הטקסט לניתוח

        Returns:
            list: רשימת סימפטומים שזוהו
        """
        if not self.cat:
            raise Exception("Model not loaded")

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
            logging.error(f"שגיאה בחילוץ סימפטומים: {e}")
            raise Exception(f"Failed to extract symptoms: {e}")

    def analyze_text(self, text):
        """
        ניתוח מלא של טקסט עם החזרת מידע מפורט

        Args:
            text (str): הטקסט לניתוח

        Returns:
            dict: תוצאות הניתוח
        """
        symptoms = self.extract_symptoms(text)

        return {
            "original_text": text,
            "symptoms_found": len(symptoms),
            "symptoms": symptoms,
            "analysis_status": "success"
        }