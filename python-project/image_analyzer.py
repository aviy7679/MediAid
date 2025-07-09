import torch
import logging
import time
from PIL import Image
from open_clip import create_model_from_pretrained, get_tokenizer
from skin_umls_codes import verified_skin_conditions_umls
from config import BIOMEDCLIP_MODEL_NAME, CONTEXT_LENGTH_DEFAULT


class ImageAnalyzer:
    def __init__(self, auto_load=False):
        self.model = None
        self.preprocess = None
        self.tokenizer = None
        self.device = None
        self.labels = None
        self.is_loaded = False
        self.logger = logging.getLogger(__name__)

        if auto_load:
            self.load_model()

    def load_model(self):
        if self.is_loaded:
            self.logger.info("Model already loaded, skipping...")
            return

        try:
            start_time = time.time()
            self.logger.info("Loading BiomedCLIP model...")

            self.model, self.preprocess = create_model_from_pretrained(BIOMEDCLIP_MODEL_NAME)
            self.tokenizer = get_tokenizer(BIOMEDCLIP_MODEL_NAME)
            self.device = torch.device('cuda' if torch.cuda.is_available() else 'cpu')
            self.model.to(self.device).eval()
            self.labels = list(verified_skin_conditions_umls.values())
            self.is_loaded = True

            end_time = time.time()
            self.logger.info(f"Model loaded in {end_time - start_time:.2f} seconds")
        except Exception as e:
            self.logger.error(f"Error loading model: {e}")
            raise Exception(f"Error loading model: {e}")

    def _ensure_model_loaded(self):
        if not self.is_loaded:
            self.load_model()

    def analyze_image(self, image_input, template='this is a photo of ', context_length=None):
        """
        ניתוח תמונה עם BiomedCLIP

        Args:
            image_input: PIL Image או נתיב לתמונה
            template: תבנית הטקסט לזיהוי
            context_length: אורך הקונטקסט (ברירת מחדל מהקבועים)
        """
        self._ensure_model_loaded()

        if context_length is None:
            context_length = CONTEXT_LENGTH_DEFAULT

        try:
            # טעינת תמונה
            if isinstance(image_input, str):
                image = self.preprocess(Image.open(image_input)).unsqueeze(0).to(self.device)
            else:  # PIL Image
                image = self.preprocess(image_input).unsqueeze(0).to(self.device)

            # ניתוח
            text_inputs = self.tokenizer([template + l for l in self.labels], context_length=context_length).to(
                self.device)

            with torch.no_grad():
                image_features, text_features, logit_scale = self.model(image, text_inputs)
                logits = (logit_scale * image_features @ text_features.T).softmax(dim=-1)
                probs = logits[0].cpu().numpy()

            # תוצאות
            results = []
            for i in probs.argsort()[::-1]:
                results.append({
                    'condition': self.labels[i],
                    'probability': float(probs[i])
                })

            return results

        except Exception as e:
            self.logger.error(f"Error analyzing image: {e}")
            raise Exception(f"Error analyzing image: {e}")