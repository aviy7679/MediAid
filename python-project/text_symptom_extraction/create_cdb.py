from medcat.preprocessing.cleaners import BasicTextCleaner
from medcat.utils.cdb_maker import UmlsCDBMaker
from medcat.config import Config

# הגדרות בסיס
config = Config()
cleaner = BasicTextCleaner(config=config)

# יצירת אובייקט המחולל
cdb_maker = UmlsCDBMaker(
    config=config,
    cleaner=cleaner,
    cui_filter=None  # אפשר להכניס כאן סט של CUIs של סימפטומים בלבד אם רוצים
)

# נתיבים
umls_dir = "D:/MediAid/umls-2024AB-full/2024AB-full/2024AB/META"
output_cdb_path = "D:/MediAid/symptom_cdb.dat"

# יצירת ה־CDB מתוך UMLS
cdb = cdb_maker.create_cdb(umls_dir)
cdb.save(output_cdb_path)
