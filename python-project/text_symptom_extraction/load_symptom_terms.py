from medcat.utils.vocab_utils import Vocab

#סוגי המונחים
mrsty_path = "D:\\MediAid\\umls-2024AB-full\\2024AB-full\\2024AB\\META\\MRSTY.RRF"
#המונחים עצמם
mrconso_path = "D:\\MediAid\\umls-2024AB-full\\2024AB-full\\2024AB\\META\\MRCONSO.RRF"
#קובץ הפלט
vocab_output_path = "D:\\MediAid\\symptom_vocab.dat"

symptom_cuis = set()

# סינון הסימפטומים
with open(mrsty_path, 'r', encoding='utf-8') as f:
    for line in f:
        columns = line.strip().split('|')
        cui = columns[0]  # CUI
        semantic_type = columns[3]  # Semantic Type
        if "Sign or Symptom" in semantic_type:
            symptom_cuis.add(cui)

print(f"Found {len(symptom_cuis)} symptom CUIs.")

# יצירת אובייקט Vocab
vocab = Vocab()

# טעינת הסימפטומים
with open(mrconso_path, 'r', encoding='utf-8') as f:
    for line in f:
        columns = line.strip().split('|')
        cui = columns[0]
        term = columns[14]
        if cui in symptom_cuis:  # בדיקה אם ה-CUI הוא של סימפטום
            vocab.add_word(term)  # הוספת המונח ל-Vocabulary

# שמירת ה-Vocab
vocab.save(vocab_output_path)
print("Symptom Vocabulary created and saved successfully!")