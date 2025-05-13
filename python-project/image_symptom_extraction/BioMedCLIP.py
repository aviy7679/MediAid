import torch
from PIL import Image
from open_clip import create_model_from_pretrained, get_tokenizer
from skin_umls_codes import verified_skin_conditions_umls


# טען את המודל
model, preprocess = create_model_from_pretrained('hf-hub:microsoft/BiomedCLIP-PubMedBERT_256-vit_base_patch16_224')
tokenizer = get_tokenizer('hf-hub:microsoft/BiomedCLIP-PubMedBERT_256-vit_base_patch16_224')

device = torch.device('cuda' if torch.cuda.is_available() else 'cpu')
model.to(device).eval()

# רשימת תוויות (labels) שתיבדק מול התמונה
labels=list(verified_skin_conditions_umls.values())

# טען תמונה מקומית
#image_path = "C:\\Users\\Student5\\Downloads\\Finger_cut.jpg"
#image_path="C:\\Users\\Student5\\Downloads\\normal-mole5-56a8808a5f9b58b7d0f2e9e6.jpg"
#image_path="C:\\Users\\Student5\\Downloads\\Pus_with_blood_coming_out_of_ring_finger,_photographed_in_India,_July_10,_2024.jpg"
image_path="C:\\Users\\Student5\\Downloads\\shutterstock_2041694828_copy.original.width-320.jpg"
image = preprocess(Image.open(image_path)).unsqueeze(0).to(device)

# המר את התוויות לטקסטים בפורמט של zero-shot
template = 'this is a photo of '
context_length = 256
text_inputs = tokenizer([template + l for l in labels], context_length=context_length).to(device)

# ניתוח
with torch.no_grad():
    image_features, text_features, logit_scale = model(image, text_inputs)
    logits = (logit_scale * image_features @ text_features.T).softmax(dim=-1)
    probs = logits[0].cpu().numpy()

# תוצאה
for i in probs.argsort()[::-1]:  # ממוין מהסביר ביותר
    print(f"{labels[i]}: {probs[i]:.4f}")
