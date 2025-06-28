#!/usr/bin/env python3
"""
Debug Script for Medical Text Analysis Server
מסייע באבחון בעיות זיהוי סימפטומים
"""

import requests
import json
import re


def test_server_connection(base_url="http://127.0.0.1:5001"):
    """בדיקת חיבור לשרת"""
    try:
        response = requests.get(f"{base_url}/health", timeout=5)
        print(f"✅ שרת מגיב: {response.status_code}")
        return True
    except requests.exceptions.RequestException as e:
        print(f"❌ שרת לא מגיב: {e}")
        return False


def test_text_analysis(text, base_url="http://127.0.0.1:5001"):
    """בדיקת ניתוח טקסט עם פלט מפורט"""

    print(f"\n🔍 בודק טקסט: '{text}'")
    print("-" * 50)

    # בדיקת עיבוד בסיסי של הטקסט
    print(f"📝 טקסט מקורי: {text}")
    print(f"📝 אורך: {len(text)} תווים")
    print(f"📝 מילים: {text.split()}")
    print(f"📝 lowercase: {text.lower()}")

    # חיפוש מילות מפתח ידוע
    common_symptoms = [
        'headache', 'pain', 'dizzy', 'dizziness', 'nausea', 'fever',
        'fatigue', 'cough', 'sore throat', 'chest pain', 'shortness of breath'
    ]

    found_keywords = []
    text_lower = text.lower()
    for symptom in common_symptoms:
        if symptom in text_lower:
            found_keywords.append(symptom)

    print(f"🔍 מילות מפתח שנמצאו ידנית: {found_keywords}")

    # שליחת בקשה לשרת
    try:
        payload = {"text": text}
        response = requests.post(
            f"{base_url}/text/analyze",
            json=payload,
            headers={'Content-Type': 'application/json'},
            timeout=10
        )

        print(f"\n📡 תגובת שרת:")
        print(f"   Status Code: {response.status_code}")

        if response.status_code == 200:
            result = response.json()
            print(f"   ✅ הצלחה: {result.get('success', False)}")
            print(f"   📊 סימפטומים שזוהו: {len(result.get('symptoms', []))}")
            print(f"   🏥 רשימת סימפטומים: {result.get('symptoms', [])}")
            print(f"   ⏱️ זמן עיבוד: {result.get('processing_time', 0)}ms")
            print(f"   🧠 זיכרון: {result.get('memory_usage', 'לא ידוע')}MB")
            print(f"   🔧 מודל: {result.get('model', 'לא ידוע')}")

            # השוואה בין הציפיות למציאות
            expected_count = len(found_keywords)
            actual_count = len(result.get('symptoms', []))

            if expected_count > 0 and actual_count == 0:
                print(f"\n⚠️  בעיה זוהתה!")
                print(f"   צפוי: {expected_count} סימפטומים")
                print(f"   נמצא: {actual_count} סימפטומים")
                print(f"   מילות מפתח שהוחמצו: {found_keywords}")

                return False
            else:
                print(f"\n✅ נראה תקין")
                return True

        else:
            print(f"   ❌ שגיאה: {response.status_code}")
            print(f"   📝 תגובה: {response.text}")
            return False

    except requests.exceptions.RequestException as e:
        print(f"❌ שגיאה בשליחת בקשה: {e}")
        return False


def test_multiple_cases(base_url="http://127.0.0.1:5001"):
    """בדיקת מקרי בוחן מרובים"""

    test_cases = [
        "I suffer from a terrible headache and also diziness.",
        "I have a headache",
        "My head hurts",
        "I feel dizzy",
        "I am experiencing dizziness",
        "I have chest pain and shortness of breath",
        "I feel nauseous and have a fever",
        "No symptoms here, just regular text"
    ]

    print("\n🧪 בדיקת מקרי בוחן מרובים")
    print("=" * 60)

    results = []
    for i, test_text in enumerate(test_cases, 1):
        print(f"\n🔬 מקרה בוחן #{i}")
        result = test_text_analysis(test_text, base_url)
        results.append((test_text, result))

    print(f"\n📊 סיכום תוצאות:")
    print("-" * 40)
    passed = sum(1 for _, result in results if result)
    total = len(results)
    print(f"עבר: {passed}/{total} מקרי בוחן")

    if passed < total:
        print(f"\n❌ נכשלו:")
        for text, result in results:
            if not result:
                print(f"   • '{text[:50]}...'")


def debug_keyword_matching():
    """אבחון תהליך התאמת מילות מפתח"""

    print("\n🔧 אבחון התאמת מילות מפתח")
    print("=" * 40)

    test_text = "I suffer from a terrible headache and also diziness."

    # ניתוח מקדים של הטקסט
    print(f"📝 טקסט מקורי: '{test_text}'")

    # נרמול טקסט כמו שצריך להתבצע בשרת
    normalized = test_text.lower().strip()
    print(f"📝 נרמול: '{normalized}'")

    # הסרת סימני פיסוק
    cleaned = re.sub(r'[^\w\s]', ' ', normalized)
    print(f"📝 ללא פיסוק: '{cleaned}'")

    # פיצול למילים
    words = cleaned.split()
    print(f"📝 מילים: {words}")

    # חיפוש סימפטומים ידועים
    known_symptoms = {
        'headache': 'כאב ראש',
        'head': 'ראש',
        'pain': 'כאב',
        'dizzy': 'סחרחורת',
        'dizziness': 'סחרחורת',
        'diziness': 'סחרחורת (שגיאת כתיב)',  # שים לב לשגיאת הכתיב!
        'nausea': 'בחילה',
        'fever': 'חום'
    }

    found_symptoms = []
    for word in words:
        if word in known_symptoms:
            found_symptoms.append((word, known_symptoms[word]))

    print(f"\n🎯 סימפטומים שנמצאו:")
    for symptom, hebrew in found_symptoms:
        print(f"   • {symptom} = {hebrew}")

    if not found_symptoms:
        print("   ❌ לא נמצאו סימפטומים!")
        print("   💡 האם המילון במילות המפתח לא עדכני?")

    # בדיקת שגיאות כתיב נפוצות
    print(f"\n🔍 בדיקת שגיאות כתיב:")
    if 'diziness' in words:
        print("   ⚠️  זוהתה שגיאת כתיב: 'diziness' במקום 'dizziness'")
        print("   💡 השרת צריך לטפל בשגיאות כתיב נפוצות")


def suggest_fixes():
    """הצעות לתיקון השרת"""

    print("\n🛠️  הצעות לתיקון השרת")
    print("=" * 40)

    fixes = [
        "1. וודא שמילון מילות המפתח כולל 'headache' ו-'dizziness'",
        "2. הוסף טיפול בשגיאות כתיב נפוצות (diziness → dizziness)",
        "3. בדוק שהנרמול של הטקסט עובד נכון (lowercase, ניקוי סימני פיסוק)",
        "4. הוסף לוג מפורט לשרת כדי לראות מה קורה בתהליך הניתוח",
        "5. בדוק שמילות המפתח לא דורשות התאמה מדויקת (partial matching)",
        "6. וודא שהמיפוי ל-CUI codes עובד נכון",
        "7. הוסף בדיקת תקינות למילון הסימפטומים בהפעלת השרת"
    ]

    for fix in fixes:
        print(f"   {fix}")


def main():
    """פונקציה ראשית"""

    print("🏥 Medical Text Analysis - Debug Tool")
    print("=" * 50)

    base_url = "http://127.0.0.1:5001"

    # בדיקת חיבור
    if not test_server_connection(base_url):
        print("❌ לא ניתן להתחבר לשרת. וודא שהשרת רץ על פורט 5001")
        return

    # בדיקת המקרה הספציפי
    problematic_text = "I suffer from a terrible headache and also diziness."
    test_text_analysis(problematic_text, base_url)

    # אבחון מפורט
    debug_keyword_matching()

    # בדיקת מקרים נוספים
    test_multiple_cases(base_url)

    # הצעות לתיקון
    suggest_fixes()


if __name__ == "__main__":
    main()