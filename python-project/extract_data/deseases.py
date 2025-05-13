import csv
import json

# להעלות את הגודל המקסימלי לשורה ארוכה
csv.field_size_limit(10 ** 7)

# קובץ הקלטים
input_file = "D:\\MediAid\\umls-2024AB-full\\2024AB-full\\2024AB\\META\\MRCONSO.RRF"
stypes_file = "D:\\MediAid\\umls-2024AB-full\\2024AB-full\\2024AB\\META\\MRSTY.RRF"
output_file = 'diseases.json'

# קטגוריות Semantic Type של מחלות
disease_categories = {'T046', 'T047', 'T191'}

# שלב 1: טען את כל ה־CUI שהם מחלות
disease_cuis = set()
with open(stypes_file, encoding='utf-8') as sty_f:
    reader = csv.reader(sty_f, delimiter='|')
    for row in reader:
        cui = row[0]
        tui = row[1]
        if tui in disease_categories:
            disease_cuis.add(cui)


# פונקציית תיעדוף מונח מועדף
def choose_best_term(umls_terms):
    preferred_sabs = ['SNOMEDCT_US', 'MSH', 'ICD10CM', 'LNC', 'MEDDRA']

    def sab_score(term):
        return preferred_sabs.index(term['SAB']) if term['SAB'] in preferred_sabs else len(preferred_sabs)

    def term_score(term):
        return (
            sab_score(term),
            0 if term.get('ISPREF') == 'Y' else 1,
            0 if term.get('TTY') == 'PT' else 1,
            len(term['name']),
            sum(c in term['name'] for c in [';', '.', ','])  # פחות סימנים = טוב יותר
        )

    sorted_terms = sorted(umls_terms, key=term_score)
    return sorted_terms[0]['name'] if sorted_terms else None


# שלב 2: טען את המונחים האנגליים עבור אותם CUI, ובחר את המועדף
all_terms = {}
with open(input_file, encoding='utf-8') as f:
    reader = csv.reader(f, delimiter='|')
    for row in reader:
        cui = row[0]
        lang = row[1]
        sab = row[11]
        tty = row[12]
        ispref = row[16]
        name = row[14]

        if cui in disease_cuis and lang == 'ENG':
            term = {
                'name': name,
                'SAB': sab,
                'TTY': tty,
                'ISPREF': ispref
            }
            all_terms.setdefault(cui, []).append(term)

# שלב 3: בחר מונח מועדף לכל CUI וכתוב לקובץ JSON
disease_terms = {}
for cui, terms in all_terms.items():
    best_name = choose_best_term(terms)
    if best_name:
        disease_terms[cui] = best_name

# כתיבה לקובץ
with open(output_file, 'w', encoding='utf-8') as out_f:
    json.dump(
        [{"CUI": cui, "name": name} for cui, name in sorted(disease_terms.items())],
        out_f,
        indent=2,
        ensure_ascii=False
    )

print(f"✔ נוצר קובץ {output_file} עם {len(disease_terms)} מחלות.")