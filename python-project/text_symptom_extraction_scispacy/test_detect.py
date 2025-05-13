import spacy

# טוענים את המודל
nlp = spacy.load("en_ner_bionlp13cg_md")

# בדיקה אם המודל נטען נכון
print(nlp.meta)
