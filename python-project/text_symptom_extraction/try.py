from medcat.cat import CAT
import time
import json

#
print("מתחיל לטעון את מודל UMLS Small...")
start_time = time.time()

# שנה את הנתיב לנתיב שבו שמרת את המודל הקטן
model_path_small = "D:\\MediAid\\umls-2024AB-full\\umls_sm_pt2ch_533bab5115c6c2d6.zip"
cat = CAT.load_model_pack(model_path_small)

end_time = time.time()
print(f"המודל נטען בהצלחה! זמן טעינה: {end_time - start_time:.2f} שניות")

# בדיקה מהירה שהמודל עובד
test_text = "Patient complains of headache and fever"
entities = cat.get_entities(test_text)
print(f"נמצאו {len(entities)} ישויות בטקסט הבדיקה:")

# בוא נדפיס את המבנה של הישויות כדי להבין את הפורמט
print("מבנה הישויות:")

for i, entity in enumerate(entities):
    print(f"ישות {i + 1}:")
    # נבדוק מה המבנה של כל ישות
    for key in entity.keys():
        print(f"  {key}: {type(entity[key])}")

    # נדפיס את הערכים העיקריים אם הם קיימים
    if 'detected_name' in entity:
        print(f"  שם שזוהה: {entity['detected_name']}")
    if 'source_value' in entity:
        print(f"  ערך מקורי: {entity['source_value']}")
    if 'cui' in entity:
        print(f"  קוד CUI: {entity['cui']}")

    print("----")
    # אחרי טעינת המודל וקריאה ל-get_entities
print("מבנה התוצאה:")
# נסה להדפיס את 10 המפתחות הראשונים של הישות הראשונה
if entities and len(entities) > 0:
    entity = entities[0]
    if hasattr(entity, "__dict__"):
        print(json.dumps(entity.__dict__, indent=2, default=str)[:500] + "...")
    elif isinstance(entity, dict):
        print(json.dumps({k: str(v) for k, v in list(entity.items())[:10]}, indent=2))
    else:
        print(f"סוג הישות: {type(entity)}")
        print(f"ייצוג כמחרוזת: {str(entity)[:200]}")