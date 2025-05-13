'''from medclip import MedCLIPModel,MedCLIPProcessor, MedCLIPVisionModelViT
from PIL import Image
from .medclip_model.src.medclip.modeling_hybrid_clip import FlaxHybridCLIP

import torch

def load_model():
    model = MedCLIPModel(vision_cls=MedCLIPVisionModelViT)
    model.load_ckpt("./medclip_model/medclip_model-vit-pretrained.pt")
    processor=MedCLIPProcessor.from_pretrained("./medclip_model")
    model.eval()
    return model, processor

def encode_image(image_path, model, processor):
    image = Image.open(image_path).convert("RGB")
    inputs=processor(images=image, return_tensors="pt")
    return  model.get_image_features(**inputs)

def encode_text(text, model, processor):
    inputs= processor(text=text, return_tensors="pt", padding=True, truncation=True)
    return model.get_text_features(**inputs)
'''
from medclip import MedCLIPModel, MedCLIPProcessor, MedCLIPVisionModelViT
from PIL import Image
from .medclip_model.src.medclip.modeling_hybrid_clip import FlaxHybridCLIP

import torch
import os


def load_model():
    model = MedCLIPModel(vision_cls=MedCLIPVisionModelViT)

    # טעינת המודל באמצעות load_state_dict במקום load_ckpt
    checkpoint_path = "./medclip_model/medclip_model-vit-pretrained.pt"
    if os.path.exists(checkpoint_path):
        state_dict = torch.load(checkpoint_path, map_location=torch.device('cpu'))
        model.load_state_dict(state_dict)
    else:
        raise FileNotFoundError(f"the model file did not found un path: {checkpoint_path}")

    processor = MedCLIPProcessor.from_pretrained("./medclip_model")
    model.eval()
    return model, processor


def encode_image(image_path, model, processor):
    image = Image.open(image_path).convert("RGB")
    inputs = processor(images=image, return_tensors="pt")
    return model.get_image_features(**inputs)


def encode_text(text, model, processor):
    inputs = processor(text=text, return_tensors="pt", padding=True, truncation=True)
    return model.get_text_features(**inputs)