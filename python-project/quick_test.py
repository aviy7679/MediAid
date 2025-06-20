# '''
"""
סקריפט לבדיקת בעיית טעינת MedCAT
"""
import os
import traceback
from medcat.cat import CAT

# הנתיב למודל
MODEL_PATH = "D:\\MediAid\\umls-2024AB-full\\umls_sm_pt2ch_533bab5115c6c2d6.zip"


def check_model_files():
    """בדיקת קיום קבצי המודל"""
    print("🔍 בודק קבצי מודל...")

    # בדיקת קובץ ZIP
    if os.path.exists(MODEL_PATH):
        size = os.path.getsize(MODEL_PATH) / (1024*1024)  # MB
        print(f"✅ קובץ ZIP קיים: {MODEL_PATH}")
        print(f"📁 גודל: {size:.1f} MB")
    else:
        print(f"❌ קובץ ZIP לא קיים: {MODEL_PATH}")
        return False

    # בדיקת תיקיה מחולצת
    extracted_path = MODEL_PATH.replace('.zip', '')
    if os.path.exists(extracted_path):
        print(f"✅ תיקיה מחולצת קיימת: {extracted_path}")

        # בדיקת קבצים חשובים
        important_files = ['cdb.dat', 'config.json', 'vocab.dat']
        for file in important_files:
            file_path = os.path.join(extracted_path, file)
            if os.path.exists(file_path):
                size = os.path.getsize(file_path) / (1024*1024)  # MB
                print(f"  ✅ {file}: {size:.1f} MB")
            else:
                print(f"  ❌ {file}: חסר!")

        return True
    else:
        print(f"❌ תיקיה מחולצת לא קיימת: {extracted_path}")
        return False


def test_model_loading():
    """ניסיון טעינת המודל עם פירוט שגיאות"""
    print("\n🚀 מנסה לטעון את המודל...")

    try:
        # ניסיון טעינה
        cat = CAT.load_model_pack(MODEL_PATH)
        print("✅ המודל נטען בהצלחה!")

        # בדיקה בסיסית
        test_text = "I have a headache"
        entities = cat.get_entities(test_text)
        print(f"✅ ניתוח עבד! נמצאו {len(entities.get('entities', {}))} ישויות")

        return True

    except Exception as e:
        print(f"❌ שגיאה בטעינת המודל:")
        print(f"   סוג שגיאה: {type(e).__name__}")
        print(f"   הודעה: {str(e)}")
        print(f"\n📋 מסלול שגיאה מלא:")
        traceback.print_exc()
        return False


def check_permissions():
    """בדיקת הרשאות גישה"""
    print("\n🔐 בודק הרשאות...")

    try:
        # בדיקת קריאה
        with open(MODEL_PATH, 'rb') as f:
            f.read(1024)  # קריאת 1KB ראשון
        print("✅ הרשאות קריאה תקינות")

        return True
    except PermissionError:
        print("❌ בעיית הרשאות - הרץ כ-Administrator")
        return False
    except Exception as e:
        print(f"❌ שגיאה בבדיקת הרשאות: {e}")
        return False


def main():
    """הרצת כל הבדיקות"""
    print("=" * 60)
    print("🩺 MedCAT Model Diagnostics")
    print("=" * 60)

    # בדיקת קבצים
    files_ok = check_model_files()

    # בדיקת הרשאות
    permissions_ok = check_permissions()

    # ניסיון טעינה רק אם הכל תקין
    if files_ok and permissions_ok:
        model_ok = test_model_loading()
    else:
        print("\n⚠️  דילוג על טעינת מודל עקב בעיות קודמות")
        model_ok = False

    print("\n" + "=" * 60)
    print("📊 סיכום:")
    print(f"  קבצים: {'✅' if files_ok else '❌'}")
    print(f"  הרשאות: {'✅' if permissions_ok else '❌'}")
    print(f"  טעינת מודל: {'✅' if model_ok else '❌'}")

    if not model_ok:
        print("\n💡 הצעות לפתרון:")
        print("  1. ודא שהקובץ לא פגום - הורד מחדש")
        print("  2. הרץ כ-Administrator")
        print("  3. בדוק שיש מספיק זיכרון RAM")
        print("  4. נסה להעתיק למיקום אחר")
        print("  5. בדוק גרסת MedCAT: pip show medcat")


if __name__ == "__main__":
    main()
# '''
# import psutil
# print(f"Total RAM: {psutil.virtual_memory().total / (1024**3):.1f} GB")
# print(f"Available RAM: {psutil.virtual_memory().available / (1024**3):.1f} GB")