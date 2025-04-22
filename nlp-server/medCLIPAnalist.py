
from transformers import AutoProcessor, AutoModelForZeroShotImageClassification
from PIL import Image
import torch

symptom_descriptions = [
    "redness of skin",
    "blister",
    "rash",
    "dry skin",
    "ulcer",
    "crusting",
    "swelling",
    "bleeding",
    "scaling",
    "discoloration",
    "no visible abnormality"
]

image=Image.open('Finger_cut.jpg').convert("RGB")
"""
model_name = "UCSF-AI/MedCLIP-ViT"
processor = AutoProcessor.from_pretrained(model_name)
model = AutoModelForZeroShotImageClassification.from_pretrained(model_name)

model_name = "openai/clip-vit-base-patch32"
processor = AutoProcessor.from_pretrained(model_name)
model = AutoModelForZeroShotImageClassification.from_pretrained(model_name)
"""
model_name = "kaushalya/medclip"

processor = AutoProcessor.from_pretrained(model_name)
model = AutoModelForZeroShotImageClassification.from_pretrained(model_name)


inputs = processor(images=image, candidate_labels=symptom_descriptions, return_tensors="pt")
with torch.no_grad():
    outputs = model(**inputs)

# חישוב סקור לכל תיאור
scores = torch.nn.functional.softmax(outputs.logits, dim=1)[0]

# הצגת תיאורים עם דירוג
results = list(zip(symptom_descriptions, scores.tolist()))
results.sort(key=lambda x: x[1], reverse=True)
for label, score in results:
    print(f"{label}: {score:.2f}")
