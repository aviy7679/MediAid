from .medclip_utils import *
from .skin_umls_codes import verified_skin_conditions_umls
import torch

def predict(image_path, top_k=3):
    model, processor = load_model()

    image_vec=encode_image(image_path, model, processor)

    descriptions=list(verified_skin_conditions_umls.values())
    text_vecs=encode_text(descriptions, model, processor)

    similarity=torch.nn.functional.cosine_similarity(image_vec, text_vecs)

    top_indices=similarity.topk(top_k).indices
    return [(descriptions[i], float(similarity[i])) for i in top_indices]

if __name__ == "__main__":
    results = predict("../Finger_cut.jpg")
    for label, score in results:
        print(f"{label}: {score:.2f}")
