"""
מחלקה לניתוח סימפטומים מתמונות באמצעות מודל BiomedCLIP
"""
import torch
from PIL import Image
from open_clip import create_model_from_pretrained, get_tokenizer
import numpy as np
import logging
import os
import io
import base64

# יבוא רשימת הסימפטומים (צריך להיות בקובץ נפרד)
try:
    from skin_umls_codes import verified_skin_conditions_umls
except ImportError:
    # אם הקובץ לא קיים, נשתמש ברשימה בסיסית
    verified_skin_conditions_umls = {
        "C0011389": "Dermatitis",
        "C0015230": "Rash",
        "C0333355": "Skin discoloration",
        "C0018681": "Headache",
        "C0015967": "Fever"
    }


class ImageSymptomAnalyzer:
    def __init__(self, min_confidence=0.1):
        """
        אתחול המחלקה עם טעינת מודל BiomedCLIP

        Args:
            min_confidence (float): סף מינימום לביטחון בתוצאה
        """
        self.min_confidence = min_confidence
        self.model = None
        self.preprocess = None
        self.tokenizer = None
        self.device = None
        self.labels = list(verified_skin_conditions_umls.values())
        self.cui_to_label = verified_skin_conditions_umls
        self._load_model()

    def _load_model(self):
        """טעינת מודל BiomedCLIP"""
        try:
            print("מתחיל לטעון את מודל BiomedCLIP...")

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

            print(f"המודל נטען בהצלחה על {self.device}!")

        except Exception as e:
            logging.error(f"שגיאה בטעינת המודל: {e}")
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
            logging.error(f"שגיאה בעיבוד התמונה: {e}")
            raise Exception(f"Failed to preprocess image: {e}")

    def analyze_image(self, image_input):
        """
        ניתוח תמונה לזיהוי סימפטומים

        Args:
            image_input: התמונה לניתוח (נתיב, bytes, או base64)

        Returns:
            list: רשימת סימפטומים שזוהו
        """
        if not self.model:
            raise Exception("Model not loaded")

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
                if prob >= self.min_confidence:
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
            logging.error(f"שגיאה בניתוח התמונה: {e}")
            raise Exception(f"Failed to analyze image: {e}")

    def get_full_analysis(self, image_input):
        """
        ניתוח מלא עם מידע נוסף

        Args:
            image_input: התמונה לניתוח

        Returns:
            dict: תוצאות הניתוח המלאות
        """
        symptoms = self.analyze_image(image_input)

        return {
            "symptoms_found": len(symptoms),
            "symptoms": symptoms,
            "min_confidence_threshold": self.min_confidence,
            "total_labels_checked": len(self.labels),
            "analysis_status": "success"
        }

    def set_confidence_threshold(self, threshold):
        """עדכון סף הביטחון המינימום"""
        if 0 <= threshold <= 1:
            self.min_confidence = threshold
        else:
            raise ValueError("Confidence threshold must be between 0 and 1")