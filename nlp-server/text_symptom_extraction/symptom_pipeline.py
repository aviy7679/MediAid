# from .detect_symptoms import detect_symptoms
# from .normalize_symptoms import normalize_symptoms
#
#
#
# def extract_and_normalize_symptoms(text):
#     """
#     מקבל טקסט חופשי, מזהה מתוכו סימפטומים, ומנרמל אותם לייצוג אחיד לפי UMLS.
#
#     :param text: טקסט חופשי מהמשתמש.
#     :return: רשימה של מילונים עם שדות: original, canonical_name, cui
#     """
#     detected = detect_symptoms(text)
#     normalized = []
#
#     for symptom in detected:
#         result = normalize_symptoms(symptom)
#         normalized.append(result)
#
#     return normalized
#
#
# if __name__ == "__main__":
#     user_text = "יש לי חום גבוה, לחץ בחזה ואני מזיע הרבה"
#     result = extract_and_normalize_symptoms(user_text)
#
#     for entry in result:
#         print(f"🔍 טקסט: {entry['original']}")
#         print(f"✅ סימפטום מנורמל: {entry['canonical_name']} (CUI: {entry['cui']})\n")
