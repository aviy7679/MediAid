"""
מחלקה ייעודית לניתוח סימפטומים מתמונות באמצעות מודל BiomedCLIP
"""
import torch
from PIL import Image
from open_clip import create_model_from_pretrained, get_tokenizer
import numpy as np
import logging
import os
import io
import base64
import time

# יבוא רשימת הסימפטומים
try:
    from skin_umls_codes import verified_skin_conditions_umls
except ImportError:
    # אם הקובץ לא קיים, נשתמש ברשימה בסיסית
    verified_skin_conditions_umls = {
        "C0011389": "Dermatitis",
        "C0015230": "Rash",
        "C0333355": "Skin discoloration",
        "C0018681": "Headache",
        "C0015967": "Fever",
        "C0030193": "Pain",
        "C0037090": "Signs and symptoms",
        "C0038990": "Swelling"
    }

class ImageAnalyzer:
    def __init__(self, min_confidence=0.1):
        """
        אתחול המחלקה עם הגדרות ברירת מחדל

        Args:
            min_confidence (float): סף מינימום לביטחון בתוצאה
        """
        self.min_confidence = min_confidence
        self.model = None
        self.preprocess = None
        self.tokenizer = None
        self.device = None
        self.is_loaded = False

        # נתונים לתמונות
        self.labels = list(verified_skin_conditions_umls.values())
        self.cui_to_label = verified_skin_conditions_umls

        self.logger = logging.getLogger(__name__)

    def load_model(self):
        """טעינת מודל BiomedCLIP"""
        try:
            start_time = time.time()
            self.logger.info("מתחיל לטעון את מודל BiomedCLIP לתמונות...")

            # טעינת המודל
            self.model, self.preprocess = create_model_from_pretrained(
                'hf-hub:microsoft/BiomedCLIP-PubMedBERT_256-vit_base_patch16_224'
            )
            self.tokenizer = get_tokenizer(
                'hf-hub:microsoft/BiomedCLIP-PubMedBERT_256-vit_base_patch16_224'
            )

            # הגדרת device
            self.device = torch.device('cuda' if torch.cuda.is_available() else 'cpu')
            self.model.to(self.device).eval()
            self.is_loaded = True

            end_time = time.time()
            self.logger.info(f"מודל התמונות נטען בהצלחה על {self.device} בזמן: {end_time - start_time:.2f} שניות")

        except Exception as e:
            self.logger.error(f"שגיאה בטעינת מודל התמונות: {e}")
            raise Exception(f"Failed to load BiomedCLIP model: {e}")

    def _preprocess_image(self, image_input):
        """
        עיבוד מקדים של התמונה

        Args:
            image_input: יכול להיות נתיב קובץ, bytes, או base64 string

        Returns:
            torch.Tensor: התמונה המעובדת
        """
        try:
            if isinstance(image_input, str):
                if image_input.startswith('data:image') or len(image_input) > 1000:
                    # Base64 string
                    if image_input.startswith('data:image'):
                        image_input = image_input.split(',')[1]
                    image_bytes = base64.b64decode(image_input)
                    image = Image.open(io.BytesIO(image_bytes)).convert('RGB')
                else:
                    # File path
                    if not os.path.exists(image_input):
                        raise FileNotFoundError(f"Image file not found: {image_input}")
                    image = Image.open(image_input).convert('RGB')
            elif isinstance(image_input, bytes):
                # Raw bytes
                image = Image.open(io.BytesIO(image_input)).convert('RGB')
            elif isinstance(image_input, Image.Image):
                # PIL Image
                image = image_input.convert('RGB')
            else:
                raise ValueError("Unsupported image input type")

            # עיבוד התמונה
            processed_image = self.preprocess(image).unsqueeze(0).to(self.device)
            return processed_image

        except Exception as e:
            self.logger.error(f"שגיאה בעיבוד התמונה: {e}")
            raise Exception(f"Failed to preprocess image: {e}")

    def extract_symptoms(self, image_input, min_confidence=None):
        """
        חילוץ סימפטומים מתמונה

        Args:
            image_input: התמונה לניתוח (נתיב, bytes, או base64)
            min_confidence: סף ביטחון (אופציונלי)

        Returns:
            list: רשימת סימפטומים שזוהו
        """
        if not self.is_loaded:
            raise Exception("Image model not loaded. Call load_model() first.")

        # שימוש בסף מותאם או ברירת מחדל
        confidence_threshold = min_confidence if min_confidence is not None else self.min_confidence

        try:
            # עיבוד התמונה
            image = self._preprocess_image(image_input)

            # הכנת הטקסטים לניתוח
            template = 'this is a photo of '
            context_length = 256
            text_inputs = self.tokenizer(
                [template + label for label in self.labels],
                context_length=context_length
            ).to(self.device)

            # ביצוע הניתוח
            with torch.no_grad():
                image_features, text_features, logit_scale = self.model(image, text_inputs)
                logits = (logit_scale * image_features @ text_features.T).softmax(dim=-1)
                probs = logits[0].cpu().numpy()

            # עיבוד התוצאות
            symptoms = []
            for i, prob in enumerate(probs):
                if prob >= confidence_threshold:
                    # חיפוש ה-CUI המתאים
                    label = self.labels[i]
                    cui = None
                    for c, l in self.cui_to_label.items():
                        if l == label:
                            cui = c
                            break

                    symptom = {
                        "cui": cui,
                        "name": label,
                        "confidence": round(float(prob), 4),
                        "probability": round(float(prob), 4)
                    }
                    symptoms.append(symptom)

            # מיון לפי רמת ביטחון
            symptoms.sort(key=lambda x: x['confidence'], reverse=True)

            return symptoms

        except Exception as e:
            self.logger.error(f"שגיאה בחילוץ סימפטומים מתמונה: {e}")
            raise Exception(f"Failed to extract symptoms from image: {e}")

    def analyze_image(self, image_input, min_confidence=None):
        """
        ניתוח מלא של תמונה עם מידע נוסף

        Args:
            image_input: התמונה לניתוח
            min_confidence: סף ביטחון (אופציונלי)

        Returns:
            dict: תוצאות הניתוח המלאות
        """
        # שימוש בסף מותאם או ברירת מחדל
        confidence_threshold = min_confidence if min_confidence is not None else self.min_confidence

        symptoms = self.extract_symptoms(image_input, confidence_threshold)

        return {
            "symptoms_found": len(symptoms),
            "symptoms": symptoms,
            "min_confidence_threshold": confidence_threshold,
            "total_labels_checked": len(self.labels),
            "analysis_status": "success",
            "analyzer_type": "BiomedCLIP",
            "device": str(self.device)
        }

    def set_confidence_threshold(self, threshold):
        """עדכון סף הביטחון המינימום"""
        if 0 <= threshold <= 1:
            self.min_confidence = threshold
            self.logger.info(f"Image confidence threshold updated to {threshold}")
        else:
            raise ValueError("Confidence threshold must be between 0 and 1")

    def get_status(self):
        """קבלת סטטוס המודל"""
        return {
            "model_loaded": self.is_loaded,
            "device": str(self.device) if self.device else None,
            "confidence_threshold": self.min_confidence,
            "total_labels": len(self.labels),
            "analyzer_type": "BiomedCLIP Image Analyzer"
        }

    def is_ready(self):
        """בדיקה האם המודל מוכן לשימוש"""
        return self.is_loaded and self.model is not None