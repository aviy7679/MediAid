#!/usr/bin/env python3
"""
קובץ בדיקה לזיהוי בעיות במערכת ניתוח מילות מפתח
"""


def test_imports():
    """בדיקת ייבוא הקבועים"""
    print("🔍 בודק ייבוא קבועים...")

    try:
        from config import (
            TEXT_CONFIDENCE_HIGH, TEXT_CONFIDENCE_MEDIUM, TEXT_CONFIDENCE_LOW,
            INTENSITY_FACTOR_HIGH, INTENSITY_FACTOR_MEDIUM, INTENSITY_FACTOR_LOW,
            INTENSITY_FACTOR_MILD, INTENSITY_FACTOR_SLIGHT,
            NEGATION_CHECK_DISTANCE, WORD_BOUNDARY_CONTEXT,
            UMLS_TYPE_SIGN_SYMPTOM,
            CATEGORY_CARDIOVASCULAR, CATEGORY_RESPIRATORY, CATEGORY_NEUROLOGICAL,
            CATEGORY_GASTROINTESTINAL, CATEGORY_MUSCULOSKELETAL, CATEGORY_DERMATOLOGICAL,
            CATEGORY_PSYCHOLOGICAL, CATEGORY_SYSTEMIC, CATEGORY_VISUAL, CATEGORY_SLEEP,
            CATEGORY_PAIN, CATEGORY_GENERAL,
            DEBUG_SEPARATOR_LONG
        )

        print("✅ כל הקבועים יובאו בהצלחה!")

        # הדפסת ערכי הקבועים החשובים
        print(f"   TEXT_CONFIDENCE_HIGH: {TEXT_CONFIDENCE_HIGH}")
        print(f"   TEXT_CONFIDENCE_MEDIUM: {TEXT_CONFIDENCE_MEDIUM}")
        print(f"   TEXT_CONFIDENCE_LOW: {TEXT_CONFIDENCE_LOW}")
        print(f"   CATEGORY_CARDIOVASCULAR: {CATEGORY_CARDIOVASCULAR}")
        print(f"   UMLS_TYPE_SIGN_SYMPTOM: {UMLS_TYPE_SIGN_SYMPTOM}")

        return True

    except ImportError as e:
        print(f"❌ שגיאת ייבוא: {e}")
        return False
    except Exception as e:
        print(f"❌ שגיאה כללית: {e}")
        return False


def test_keyword_analyzer():
    """בדיקת מערכת ניתוח מילות מפתח"""
    print("\n🔍 בודק מערכת ניתוח מילות מפתח...")

    try:
        from text_keywords_system import KeywordTextAnalyzer

        # יצירת המנתח
        analyzer = KeywordTextAnalyzer()

        print(f"✅ KeywordTextAnalyzer נוצר בהצלחה!")
        print(f"   is_loaded: {analyzer.is_loaded}")
        print(f"   מספר CUI במאגר: {len(analyzer.cui_database)}")
        print(f"   מספר מילות מפתח באינדקס: {len(analyzer.keyword_index)}")

        # בדיקת המאגר
        print("\n📊 דוגמאות מהמאגר:")
        count = 0
        for cui, data in analyzer.cui_database.items():
            if count < 3:  # הצג רק 3 ראשונות
                print(f"   {cui}: {data['name']} ({len(data.get('keywords', []))} מילות מפתח)")
                count += 1

        return analyzer

    except ImportError as e:
        print(f"❌ שגיאת ייבוא KeywordTextAnalyzer: {e}")
        return None
    except Exception as e:
        print(f"❌ שגיאה ביצירת KeywordTextAnalyzer: {e}")
        return None


def test_symptom_detection(analyzer):
    """בדיקת זיהוי סימפטומים"""
    if not analyzer:
        return

    print("\n🧪 בודק זיהוי סימפטומים...")

    test_text = "I'm experiencing severe chest pain that started an hour ago. The pain radiates to my left arm and I feel short of breath. I'm also feeling nauseous and dizzy. The pain is crushing and feels like someone is sitting on my chest."

    print(f"🔤 טקסט לבדיקה: {test_text}")

    try:
        # ניתוח הטקסט
        symptoms = analyzer.analyze_text(test_text)

        print(f"📋 תוצאות: {len(symptoms)} סימפטומים נמצאו")

        if len(symptoms) == 0:
            print("❌ לא נמצאו סימפטומים - צריך לבדוק!")

            # בדיקה ידנית של מילות מפתח
            print("\n🔍 בדיקה ידנית של מילות מפתח:")
            text_lower = test_text.lower()

            for keyword, cuis in analyzer.keyword_index.items():
                if keyword in text_lower:
                    print(f"   ✓ נמצא: '{keyword}' -> {cuis}")

        else:
            print("✅ נמצאו סימפטומים:")
            for symptom in symptoms:
                print(f"   • {symptom['name']} (CUI: {symptom['cui']}, Confidence: {symptom['confidence']:.2f})")

    except Exception as e:
        print(f"❌ שגיאה בניתוח: {e}")


def test_specific_keywords(analyzer):
    """בדיקת מילות מפתח ספציפיות"""
    if not analyzer:
        return

    print("\n🎯 בודק מילות מפתח ספציפיות...")

    # מילות מפתח שצריכות להיות במאגר
    expected_keywords = [
        "chest pain",
        "shortness of breath",
        "nauseous",
        "dizzy",
        "severe"
    ]

    for keyword in expected_keywords:
        if keyword.lower() in analyzer.keyword_index:
            cuis = analyzer.keyword_index[keyword.lower()]
            print(f"   ✓ '{keyword}' -> {cuis}")
        else:
            print(f"   ❌ '{keyword}' לא נמצא!")


def main():
    """ביצוע כל הבדיקות"""
    print("🚀 מתחיל בדיקת מערכת ניתוח מילות מפתח...")
    print("=" * 60)

    # שלב 1: בדיקת ייבוא
    if not test_imports():
        print("❌ נכשל בייבוא הקבועים - בדוק את קובץ config.py")
        return

    # שלב 2: בדיקת יצירת המנתח
    analyzer = test_keyword_analyzer()
    if not analyzer:
        print("❌ נכשל ביצירת המנתח")
        return

    # שלב 3: בדיקת מילות מפתח ספציפיות
    test_specific_keywords(analyzer)

    # שלב 4: בדיקת זיהוי סימפטומים
    test_symptom_detection(analyzer)

    print("\n" + "=" * 60)
    print("✅ בדיקה הושלמה!")


if __name__ == "__main__":
    main()