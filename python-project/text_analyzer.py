# """
# מחלקה ייעודית לניתוח סימפטומים מטקסט באמצעות מודל MedCAT
# """
# from medcat.cat import CAT
# import logging
# import time
#
# class TextAnalyzer:
#     def __init__(self, model_path):
#         """
#         אתחול המחלקה עם טעינת מודל MedCAT
#
#         Args:
#             model_path (str): נתיב למודל UMLS
#         """
#         self.model_path = model_path
#         self.cat = None
#         self.is_loaded = False
#         self.logger = logging.getLogger(__name__)
#
#     def load_model(self):
#         """טעינת מודל MedCAT"""
#         try:
#             start_time = time.time()
#             self.logger.info("מתחיל לטעון את מודל MedCAT לטקסט...")
#
#             self.cat = CAT.load_model_pack(self.model_path)
#             self.is_loaded = True
#
#             end_time = time.time()
#             self.logger.info(f"מודל הטקסט נטען בהצלחה בזמן: {end_time - start_time:.2f} שניות")
#
#         except Exception as e:
#             self.logger.error(f"שגיאה בטעינת מודל הטקסט: {e}")
#             raise Exception(f"Failed to load MedCAT model: {e}")
#
#     def extract_symptoms(self, text):
#         """
#         חילוץ סימפטומים מטקסט
#
#         Args:
#             text (str): הטקסט לניתוח
#
#         Returns:
#             list: רשימת סימפטומים שזוהו
#         """
#         if not self.is_loaded:
#             raise Exception("Text model not loaded. Call load_model() first.")
#
#         try:
#             # קבלת entities מהטקסט
#             entities = self.cat.get_entities(text)
#             symptom_entities = []
#
#             # סינון רק symptoms (T184 = Sign or Symptom)
#             for ent_id, data in entities.get("entities", {}).items():
#                 type_ids = data.get("type_ids", [])
#
#                 if "T184" in type_ids:  # T184 = Sign or Symptom לפי UMLS
#                     symptom = {
#                         "cui": data.get("cui"),
#                         "name": data.get("pretty_name"),
#                         "detected_name": data.get("detected_name"),
#                         "source_value": data.get("source_value"),
#                         "start_position": data.get("start"),
#                         "end_position": data.get("end"),
#                         "accuracy": round(data.get("acc", 0), 4),
#                         "context_similarity": round(data.get("context_similarity", 0), 4),
#                         "status": data.get("meta_anns", {}).get("Status", {}).get("value", "Unknown"),
#                         "confidence": round(data.get("meta_anns", {}).get("Status", {}).get("confidence", 0), 4)
#                     }
#                     symptom_entities.append(symptom)
#
#             return symptom_entities
#
#         except Exception as e:
#             self.logger.error(f"שגיאה בחילוץ סימפטומים מטקסט: {e}")
#             raise Exception(f"Failed to extract symptoms from text: {e}")
#
#     def analyze_text(self, text):
#         """
#         ניתוח מלא של טקסט עם החזרת מידע מפורט
#
#         Args:
#             text (str): הטקסט לניתוח
#
#         Returns:
#             dict: תוצאות הניתוח המלאות
#         """
#         symptoms = self.extract_symptoms(text)
#
#         return {
#             "original_text": text,
#             "symptoms_found": len(symptoms),
#             "symptoms": symptoms,
#             "analysis_status": "success",
#             "analyzer_type": "MedCAT"
#         }
#
#     def get_status(self):
#         """קבלת סטטוס המודל"""
#         return {
#             "model_loaded": self.is_loaded,
#             "model_path": self.model_path,
#             "analyzer_type": "MedCAT Text Analyzer"
#         }
#
#     def is_ready(self):
#         """בדיקה האם המודל מוכן לשימוש"""
#         return self.is_loaded and self.cat is not None
"""
מחלקה ייעודית לניתוח סימפטומים מטקסט באמצעות מודל MedCAT
מחזיר את כל הישויות הרפואיות שהמודל מוצא
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

        # רשימת Type IDs רלוונטיים לבריאות (מורחבת)
        self.medical_type_ids = {
            "T184": "Sign or Symptom",
            "T047": "Disease or Syndrome",
            "T048": "Mental or Behavioral Dysfunction",
            "T046": "Pathologic Function",
            "T037": "Injury or Poisoning",
            "T019": "Congenital Abnormality",
            "T020": "Acquired Abnormality",
            "T033": "Finding",
            "T034": "Laboratory or Test Result",
            "T049": "Cell or Molecular Dysfunction",
            "T050": "Experimental Model of Disease",
            "T191": "Neoplastic Process"
        }

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
        חילוץ כל הישויות הרפואיות מטקסט

        Args:
            text (str): הטקסט לניתוח

        Returns:
            list: רשימת ישויות רפואיות שזוהו
        """
        if not self.is_loaded:
            raise Exception("Text model not loaded. Call load_model() first.")

        try:
            # קבלת entities מהטקסט - בדיוק כמו בנוטבוק שלך
            entities_result = self.cat.get_entities(text)
            medical_entities = []

            self.logger.info(f"נמצאו {len(entities_result.get('entities', {}))} entities בטקסט: '{text}'")

            # עיבוד לפי המבנה הנכון שראיתי בנוטבוק
            for ent_id, data in entities_result.get("entities", {}).items():
                type_ids = data.get("type_ids", [])
                cui = data.get("cui")
                pretty_name = data.get("pretty_name", "")
                detected_name = data.get("detected_name", "")
                accuracy = data.get("acc", 0)

                # רישום לדיבוג
                self.logger.info(f"Entity: {pretty_name}, CUI: {cui}, Types: {type_ids}, Accuracy: {accuracy:.3f}")

                # בדיקה אם זה ישות רפואית רלוונטית
                relevant_types = [t for t in type_ids if t in self.medical_type_ids]

                if relevant_types and accuracy >= 0.1:  # סף נמוך לדיוק
                    # הוספת מידע על סוג הישות
                    type_descriptions = [self.medical_type_ids.get(t, t) for t in relevant_types]

                    entity = {
                        "cui": cui,
                        "name": pretty_name,
                        "detected_name": detected_name,
                        "source_value": data.get("source_value"),
                        "start_position": data.get("start"),
                        "end_position": data.get("end"),
                        "accuracy": round(accuracy, 4),
                        "context_similarity": round(data.get("context_similarity", 0), 4),
                        "status": data.get("meta_anns", {}).get("Status", {}).get("value", "Unknown"),
                        "confidence": round(data.get("meta_anns", {}).get("Status", {}).get("confidence", 0), 4),
                        "type_ids": type_ids,
                        "type_descriptions": type_descriptions,
                        "entity_category": self._categorize_entity(relevant_types[0] if relevant_types else "")
                    }
                    medical_entities.append(entity)

            # מיון לפי דיוק
            medical_entities.sort(key=lambda x: x['accuracy'], reverse=True)

            self.logger.info(f"סונן {len(medical_entities)} ישויות רפואיות מתוך הטקסט")
            return medical_entities

        except Exception as e:
            self.logger.error(f"שגיאה בחילוץ ישויות רפואיות מטקסט: {e}")
            raise Exception(f"Failed to extract medical entities from text: {e}")

    def _categorize_entity(self, type_id):
        """קטגוריזציה של הישות לפי Type ID"""
        symptom_types = ["T184", "T033"]  # Signs/Symptoms + Findings
        disease_types = ["T047", "T046", "T191"]  # Diseases
        mental_types = ["T048"]  # Mental/Behavioral
        injury_types = ["T037", "T019", "T020"]  # Injuries/Abnormalities

        if type_id in symptom_types:
            return "symptom"
        elif type_id in disease_types:
            return "disease"
        elif type_id in mental_types:
            return "mental_health"
        elif type_id in injury_types:
            return "injury"
        else:
            return "other_medical"

    def extract_all_entities(self, text):
        """חילוץ כל הישויות (לא רק רפואיות) - לדיבוג"""
        if not self.is_loaded:
            raise Exception("Text model not loaded. Call load_model() first.")

        entities_result = self.cat.get_entities(text)
        all_entities = []

        for ent_id, data in entities_result.get("entities", {}).items():
            entity = {
                "id": ent_id,
                "cui": data.get("cui"),
                "name": data.get("pretty_name"),
                "detected_name": data.get("detected_name"),
                "source_value": data.get("source_value"),
                "start": data.get("start"),
                "end": data.get("end"),
                "accuracy": round(data.get("acc", 0), 4),
                "type_ids": data.get("type_ids", []),
                "types": data.get("types", []),
                "meta_anns": data.get("meta_anns", {})
            }
            all_entities.append(entity)

        return all_entities

    def analyze_text(self, text):
        """
        ניתוח מלא של טקסט עם החזרת מידע מפורט

        Args:
            text (str): הטקסט לניתוח

        Returns:
            dict: תוצאות הניתוח המלאות
        """
        # הוספת validation לטקסט
        if not text or len(text.strip()) == 0:
            return {
                "original_text": text,
                "entities_found": 0,
                "medical_entities": [],
                "analysis_status": "error",
                "error": "Empty text provided",
                "analyzer_type": "MedCAT"
            }

        try:
            medical_entities = self.extract_symptoms(text)

            # חלוקה לקטגוריות
            categories = {}
            for entity in medical_entities:
                cat = entity.get("entity_category", "other")
                if cat not in categories:
                    categories[cat] = []
                categories[cat].append(entity)

            return {
                "original_text": text,
                "entities_found": len(medical_entities),
                "medical_entities": medical_entities,
                "categories": categories,
                "category_counts": {k: len(v) for k, v in categories.items()},
                "analysis_status": "success",
                "analyzer_type": "MedCAT"
            }
        except Exception as e:
            return {
                "original_text": text,
                "entities_found": 0,
                "medical_entities": [],
                "analysis_status": "error",
                "error": str(e),
                "analyzer_type": "MedCAT"
            }

    def get_status(self):
        """קבלת סטטוס המודל"""
        return {
            "model_loaded": self.is_loaded,
            "model_path": self.model_path,
            "analyzer_type": "MedCAT Medical Entity Analyzer",
            "supported_medical_types": len(self.medical_type_ids),
            "medical_type_ids": self.medical_type_ids
        }

    def is_ready(self):
        """בדיקה האם המודל מוכן לשימוש"""
        return self.is_loaded and self.cat is not None

    def debug_entities(self, text):
        """פונקציה לדיבוג - מציגה את כל ה-entities שנמצאו"""
        if not self.is_loaded:
            raise Exception("Text model not loaded. Call load_model() first.")

        entities = self.cat.get_entities(text)

        print(f"\n=== DEBUG: All entities found in '{text}' ===")
        for ent_id, data in entities.get("entities", {}).items():
            print(f"ID: {ent_id}")
            print(f"  CUI: {data.get('cui')}")
            print(f"  Pretty Name: {data.get('pretty_name')}")
            print(f"  Detected Name: {data.get('detected_name')}")
            print(f"  Type IDs: {data.get('type_ids', [])}")
            print(f"  Types: {data.get('types', [])}")
            print(f"  Accuracy: {data.get('acc', 0):.4f}")
            print(f"  Start-End: {data.get('start')}-{data.get('end')}")
            print(f"  Status: {data.get('meta_anns', {}).get('Status', {}).get('value', 'Unknown')}")
            print("  ---")

        return entities