import spacy

_nlp_ner=None

def get_ner_model():
    global _nlp_ner
    if _nlp_ner is None:
        _nlp_ner=spacy.load("en_ner_bionlp13cg_md")
    return _nlp_ner

def detect_symptoms(text):
    nlp=get_ner_model()
    doc = _nlp_ner(text)
    symptoms = [ent.text for ent in doc.ents if ent.label_ == 'SIGN_OR_SYMPTOM']
    return symptoms

