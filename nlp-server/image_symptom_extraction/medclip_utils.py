from medclip import MedCLIPModel,MedCLIPProcessor, MedCLIPVisionModelViT
from PIL import Image
from .medclip.src.medclip.modeling_hybrid_clip import FlaxHybridCLIP

import torch

def load_model():
    model = MedCLIPModel(vision_cls=MedCLIPVisionModelViT)
    model.load_ckpt("./medclip/medclip-vit-pretrained.pt")
    processor=MedCLIPProcessor.from_pretrained("./medclip")
    model.eval()
    return model, processor

def encode_image(image_path, model, processor):
    image = Image.open(image_path).convert("RGB")
    inputs=processor(images=image, return_tensors="pt")
    return  model.get_image_features(**inputs)

def encode_text(text, model, processor):
    inputs= processor(text=text, return_tensors="pt", padding=True, truncation=True)
    return model.get_text_features(**inputs)