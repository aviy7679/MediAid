"""
בדיקה מהירה של המודל המעודכן
בדיוק כמו הנוטבוק שלך, אבל דרך השרת
"""
import requests
import json


def quick_test():
    """בדיקה מהירה של המודל"""
    base_url = "http://localhost:5000"

    # דוגמאות מהנוטבוק שלך
    test_cases = [
        "Patient complains of headache and fever",
        "I have been suffering from a high fever since yesterday and I also have a headache.",
        "I have headache and high fever.",
        "My stomach hurts",
        "I feel depressed and anxious",
        "Everything is fine"
    ]

    print("🚀 בדיקה מהירה של המודל המעודכן")
    print("=" * 50)

    for i, text in enumerate(test_cases, 1):
        print(f"\n{i}. טקסט: '{text}'")
        print("-" * 40)

        try:
            # בדיקת ניתוח מלא
            response = requests.post(f"{base_url}/text/analyze",
                                     json={"text": text},
                                     timeout=10)

            if response.status_code == 200:
                result = response.json()
                if result.get("success"):
                    data = result["data"]
                    entities = data.get("medical_entities", [])
                    categories = data.get("categories", {})

                    print(f"📊 נמצאו {len(entities)} ישויות רפואיות")

                    # הצגה לפי סוגים
                    for cat_name, cat_entities in categories.items():
                        emoji = {"symptom": "🏥", "disease": "🦠", "mental_health": "🧠", "injury": "🩹"}.get(cat_name, "📋")
                        print(f"{emoji} {cat_name}: {len(cat_entities)} פריטים")

                        for entity in cat_entities:
                            name = entity.get("name", "Unknown")
                            cui = entity.get("cui", "N/A")
                            acc = entity.get("accuracy", 0)
                            detected = entity.get("detected_name", "")
                            print(f"   - {name} (זוהה: '{detected}', CUI: {cui}, דיוק: {acc:.3f})")

                    if not entities:
                        print("   🚫 לא נמצאו ישויות רפואיות")

                else:
                    print(f"❌ שגיאה: {result.get('error', 'Unknown')}")
            else:
                print(f"🔴 HTTP שגיאה: {response.status_code}")

        except Exception as e:
            print(f"💥 שגיאה: {e}")


def compare_endpoints():
    """השוואה בין endpoints שונים"""
    base_url = "http://localhost:5000"
    text = "I have been suffering from a high fever and headache"

    print(f"\n🔍 השוואת endpoints עבור: '{text}'")
    print("=" * 60)

    endpoints = [
        ("/text/entities", "ישויות רפואיות בלבד"),
        ("/text/symptoms", "סימפטומים בלבד"),
        ("/text/all_entities", "כל הישויות")
    ]

    for endpoint, description in endpoints:
        print(f"\n📍 {description} ({endpoint}):")
        try:
            response = requests.post(f"{base_url}{endpoint}",
                                     json={"text": text},
                                     timeout=10)

            if response.status_code == 200:
                result = response.json()
                if result.get("success"):
                    # קבלת המידע הרלוונטי
                    if "medical_entities" in result:
                        items = result["medical_entities"]
                        key = "medical_entities"
                    elif "symptoms" in result:
                        items = result["symptoms"]
                        key = "symptoms"
                    elif "all_entities" in result:
                        items = result["all_entities"]
                        key = "all_entities"
                    else:
                        items = []
                        key = "unknown"

                    print(f"   📊 מספר פריטים: {len(items)}")

                    # הצגת הפריטים
                    for item in items[:3]:  # הראה רק 3 ראשונים
                        name = item.get("name", item.get("pretty_name", "Unknown"))
                        cui = item.get("cui", "N/A")
                        acc = item.get("accuracy", item.get("acc", 0))
                        types = item.get("type_descriptions", item.get("types", []))
                        print(f"   - {name} (CUI: {cui}, דיוק: {acc:.3f}, סוגים: {types})")

                else:
                    print(f"   ❌ שגיאה: {result.get('error')}")
            else:
                print(f"   🔴 HTTP {response.status_code}")

        except Exception as e:
            print(f"   💥 שגיאה: {e}")


def check_server_status():
    """בדיקת מצב השרת"""
    base_url = "http://localhost:5000"

    print("🔄 בודק מצב השרת...")
    try:
        response = requests.get(f"{base_url}/health", timeout=5)
        if response.status_code == 200:
            health = response.json()
            status = health.get("status", "unknown")
            text_ready = health.get("analyzers", {}).get("text_analyzer_ready", False)

            if status == "healthy" and text_ready:
                print("✅ השרת מוכן ופועל!")
                return True
            else:
                print(f"⚠️  השרת חלקית מוכן: status={status}, text_ready={text_ready}")
                return False
        else:
            print(f"🔴 השרת לא עונה: HTTP {response.status_code}")
            return False
    except Exception as e:
        print(f"💥 לא ניתן להתחבר לשרת: {e}")
        return False


if __name__ == "__main__":
    print("🧪 בדיקה מהירה של המנתח הרפואי המעודכן")
    print("📝 מבוסס על הנוטבוק שלך - עכשיו דרך השרת!")
    print()

    if check_server_status():
        quick_test()
        compare_endpoints()
    else:
        print("❌ השרת לא זמין. ודא שהשרת רץ על http://localhost:5000")

    print("\n🎯 סיום בדיקה!")