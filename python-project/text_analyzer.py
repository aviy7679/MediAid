from medcat.cat import CAT
import time
import logging
from config import get_model_path_with_fallback, UMLS_TYPE_SIGN_SYMPTOM, UMLS_TYPE_FINDING


class TextAnalyzer:
    def __init__(self, auto_load=False):
        self.cat = None
        self.is_loaded = False
        self.model_path = None
        self.logger = logging.getLogger(__name__)

        if auto_load:
            self.load_model()

    def load_model(self):
        """טעינת המודל - יכולה להיקרא רק פעם אחת"""
        if self.is_loaded:
            self.logger.info("Model already loaded, skipping...")
            return

        try:
            start_time = time.time()
            self.logger.info("Loading MedCAT model...")

            # קבלת נתיב המודל עם fallback
            self.model_path = get_model_path_with_fallback()
            self.logger.info(f"Using model path: {self.model_path}")

            self.cat = CAT.load_model_pack(self.model_path)
            self.is_loaded = True

            end_time = time.time()
            self.logger.info(f"Model loaded successfully in {end_time - start_time:.2f} seconds")
        except Exception as e:
            self.logger.error(f"Error loading MedCAT model: {e}")
            raise Exception(f"Error loading MedCAT model: {e}")

    def _ensure_model_loaded(self):
        """בדיקה שהמודל נטען, אם לא - טוען אותו אוטומטית"""
        if not self.is_loaded:
            self.logger.info("Model not loaded, loading automatically...")
            self.load_model()

    def extract_symptoms(self, text):
        # וידוא שהמודל נטען
        self._ensure_model_loaded()

        #טקסט ריק או רווחים בלבד
        if not text or not text.strip():
            return []

        try:
            entities = self.cat.get_entities(text)
            symptom_entities = []

            for ent_id, data in entities.get("entities", {}).items():
                type_ids = data.get("type_ids", [])
                # T184 = Sign or Symptom, T033 = Finding
                if UMLS_TYPE_SIGN_SYMPTOM in type_ids or UMLS_TYPE_FINDING in type_ids:
                    symptom = {
                        "cui": data.get("cui"),
                        "name": data.get("pretty_name"),
                        "detected_name": data.get("detected_name"),
                        "start": data.get("start"),
                        "end": data.get("end"),
                        "acc": data.get("acc"),
                        "meta_anns": data.get("meta_anns"),
                    }
                    symptom_entities.append(symptom)

            return symptom_entities

        except Exception as e:
            self.logger.error(f"Error extracting symptoms: {e}")
            raise Exception(f"Error extracting symptoms: {e}")

    def get_all_entities(self, text):
        """קבלת כל הישויות (לא רק סימפטומים)"""
        self._ensure_model_loaded()

        if not text or not text.strip():
            return {}

        try:
            return self.cat.get_entities(text)
        except Exception as e:
            self.logger.error(f"Error getting entities: {e}")
            raise Exception(f"Error getting entities: {e}")