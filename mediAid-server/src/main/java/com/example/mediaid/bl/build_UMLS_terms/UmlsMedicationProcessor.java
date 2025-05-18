package com.example.mediaid.bl.build_UMLS_terms;

import com.example.mediaid.dal.UMLS_terms.Medication;
import com.example.mediaid.dal.UMLS_terms.MedicationRepository;
import com.example.mediaid.dal.UMLS_terms.UmlsTerm;

import jakarta.persistence.EntityManager;
import jakarta.persistence.PersistenceContext;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.CommandLineRunner;
import org.springframework.stereotype.Component;
import org.springframework.transaction.PlatformTransactionManager;
import org.springframework.transaction.TransactionStatus;
import org.springframework.transaction.support.TransactionCallback;
import org.springframework.transaction.support.TransactionTemplate;

import java.io.BufferedReader;
import java.io.FileReader;
import java.io.IOException;
import java.util.*;

@Component
public class UmlsMedicationProcessor implements CommandLineRunner {

    // הגדרת קבצי קלט
    private static final String MRCONSO_FILE = "D:\\MediAid\\umls-2024AB-full\\2024AB-full\\2024AB\\META\\MRCONSO.RRF";
    private static final String MRSTY_FILE = "D:\\MediAid\\umls-2024AB-full\\2024AB-full\\2024AB\\META\\MRSTY.RRF";

    // קטגוריות Semantic Type של תרופות
    private static final Set<String> MEDICATION_CATEGORIES = new HashSet<>(Arrays.asList("T121", "T200", "T195", "T125"));

    // מקורות מועדפים לבחירת מונחים
    private static final List<String> PREFERRED_SABS = Arrays.asList("RXNORM", "SNOMEDCT_US", "MSH", "ATC", "MEDDRA", "NDFRT");

    @PersistenceContext
    private EntityManager entityManager;

    @Autowired
    private MedicationRepository medicationRepository;

    @Autowired
    private PlatformTransactionManager transactionManager;

    private TransactionTemplate transactionTemplate;

    @Autowired
    public void setTransactionManager(PlatformTransactionManager transactionManager) {
        this.transactionManager = transactionManager;
        this.transactionTemplate = new TransactionTemplate(transactionManager);
    }

    @Override
    public void run(String... args) throws Exception {
        try {
            // בדיקה האם יש צורך לייבא נתונים
            if (medicationRepository.count() > 0) {
                System.out.println("הנתונים של התרופות כבר קיימים במסד הנתונים. מדלג על ייבוא.");
                return;
            }

            // שלב 1: טען את כל ה־CUI שהם תרופות
            System.out.println("שלב 1: טוען תרופות מקובץ MRSTY...");
            Set<String> medicationCuis = loadMedicationCuis();
            System.out.println("נמצאו " + medicationCuis.size() + " תרופות (CUIs)");

            // שלב 2+3: טען מונחים ובחר מועדפים
            System.out.println("שלב 2: טוען מונחים מקובץ MRCONSO...");
            Map<String, String> medicationTerms = loadMedicationTerms(medicationCuis);
            System.out.println("נבחרו " + medicationTerms.size() + " מונחים מועדפים");

            // שלב 4: הכנס למסד נתונים
            System.out.println("שלב 3: מכניס נתונים למסד הנתונים...");
            insertIntoDatabase(medicationTerms);

            System.out.println("✔ התהליך הושלם בהצלחה.");
        } catch (Exception e) {
            System.err.println("שגיאה בעיבוד: " + e.getMessage());
            e.printStackTrace();
        }
    }

    /**
     * טוען את כל ה-CUIs שמייצגים תרופות לפי הקטגוריות שהוגדרו
     */
    private Set<String> loadMedicationCuis() throws IOException {
        Set<String> medicationCuis = new HashSet<>();

        try (BufferedReader reader = new BufferedReader(new FileReader(MRSTY_FILE))) {
            String line;
            while ((line = reader.readLine()) != null) {
                String[] fields = line.split("\\|");
                String cui = fields[0];
                String tui = fields[1];

                if (MEDICATION_CATEGORIES.contains(tui)) {
                    medicationCuis.add(cui);
                }
            }
        }

        return medicationCuis;
    }

    /**
     * טוען את המונחים האנגליים של התרופות ובוחר את המונח המועדף לכל CUI
     */
    private Map<String, String> loadMedicationTerms(Set<String> medicationCuis) throws IOException {
        // מבנה לשמירת כל המונחים עבור כל CUI
        Map<String, List<UmlsTerm>> allTerms = new HashMap<>();

        try (BufferedReader reader = new BufferedReader(new FileReader(MRCONSO_FILE))) {
            String line;
            while ((line = reader.readLine()) != null) {
                String[] fields = line.split("\\|");
                String cui = fields[0];
                String lang = fields[1];
                String sab = fields[11];
                String tty = fields[12];
                String ispref = fields[16];
                String name = fields[14];

                if (medicationCuis.contains(cui) && "ENG".equals(lang)) {
                    UmlsTerm term = new UmlsTerm(name, sab, tty, ispref);

                    // הוסף את המונח לרשימת המונחים עבור ה-CUI
                    allTerms.computeIfAbsent(cui, k -> new ArrayList<>()).add(term);
                }
            }
        }

        // בחר מונח מועדף לכל CUI
        Map<String, String> medicationTerms = new HashMap<>();
        for (Map.Entry<String, List<UmlsTerm>> entry : allTerms.entrySet()) {
            String bestName = chooseBestTerm(entry.getValue());
            if (bestName != null) {
                medicationTerms.put(entry.getKey(), bestName);
            }
        }

        return medicationTerms;
    }

    /**
     * בוחר את המונח המועדף מרשימת מונחים לפי קריטריונים שונים
     */
    private String chooseBestTerm(List<UmlsTerm> terms) {
        // אנחנו מבטיחים שהטיפוס יהיה UmlsTerm באופן מפורש
        terms.sort((term1, term2) -> {
            // מיון לפי מקור מועדף
            int sabCompare = Integer.compare(
                    PREFERRED_SABS.contains(term1.getSab()) ? PREFERRED_SABS.indexOf(term1.getSab()) : PREFERRED_SABS.size(),
                    PREFERRED_SABS.contains(term2.getSab()) ? PREFERRED_SABS.indexOf(term2.getSab()) : PREFERRED_SABS.size()
            );
            if (sabCompare != 0) return sabCompare;

            // מיון לפי האם מועדף
            int prefCompare = Integer.compare(
                    "Y".equals(term1.getIspref()) ? 0 : 1,
                    "Y".equals(term2.getIspref()) ? 0 : 1
            );
            if (prefCompare != 0) return prefCompare;

            // מיון לפי סוג מונח - עדיפות ל-TTYs של תרופות
            int ttyCompare = compareTtyForMedications(term1.getTty(), term2.getTty());
            if (ttyCompare != 0) return ttyCompare;

            // מיון לפי אורך המונח
            int lengthCompare = Integer.compare(term1.getName().length(), term2.getName().length());
            if (lengthCompare != 0) return lengthCompare;

            // מיון לפי כמות סימני פיסוק
            return Integer.compare(countPunctuation(term1.getName()), countPunctuation(term2.getName()));
        });

        return !terms.isEmpty() ? terms.get(0).getName() : null;
    }

    /**
     * השוואה מיוחדת לסוגי מונחים של תרופות
     * RxNorm: IN (Ingredient), MIN (Multiple Ingredients), PIN (Precise Ingredient), BN (Brand Name)
     * SNOMED: PT (Preferred Term)
     */
    private int compareTtyForMedications(String tty1, String tty2) {
        // סדר עדיפות לסוגי מונחים של תרופות
        List<String> preferredTypes = Arrays.asList("IN", "PIN", "BN", "MIN", "PT", "PV", "SY");

        int rank1 = preferredTypes.contains(tty1) ? preferredTypes.indexOf(tty1) : preferredTypes.size();
        int rank2 = preferredTypes.contains(tty2) ? preferredTypes.indexOf(tty2) : preferredTypes.size();

        return Integer.compare(rank1, rank2);
    }

    /**
     * סופר סימני פיסוק במחרוזת (פחות סימנים = עדיף)
     */
    private int countPunctuation(String text) {
        int count = 0;
        for (char c : text.toCharArray()) {
            if (c == ';' || c == '.' || c == ',') {
                count++;
            }
        }
        return count;
    }

    /**
     * מכניס את התרופות למסד הנתונים
     */
    private static final Logger logger = LoggerFactory.getLogger(UmlsMedicationProcessor.class);
    private static final int MAX_NAME_LENGTH = 250; // קצת מתחת למגבלה של 255 כדי להיות בטוחים

    protected void insertIntoDatabase(Map<String, String> medicationTerms) {
        final int batchSize = 50;
        final int totalSize = medicationTerms.size();
        int processedCount = 0;
        int skippedCount = 0;

        List<List<Map.Entry<String, String>>> batches = new ArrayList<>();
        List<Map.Entry<String, String>> currentBatch = new ArrayList<>();

        // הכנת האצוות
        for (Map.Entry<String, String> entry : medicationTerms.entrySet()) {
            currentBatch.add(entry);

            if (currentBatch.size() >= batchSize) {
                batches.add(new ArrayList<>(currentBatch));
                currentBatch.clear();
            }
        }

        // הוספת האצווה האחרונה אם נשארו פריטים
        if (!currentBatch.isEmpty()) {
            batches.add(currentBatch);
        }

        // עיבוד כל אצווה בתוך טרנזקציה נפרדת
        for (List<Map.Entry<String, String>> batch : batches) {
            final List<Map.Entry<String, String>> batchToProcess = batch;

            Integer batchResult = transactionTemplate.execute(new TransactionCallback<Integer>() {
                @Override
                public Integer doInTransaction(TransactionStatus status) {
                    int localSkipped = 0;
                    try {
                        for (Map.Entry<String, String> entry : batchToProcess) {
                            String name = entry.getValue();

                            // בדיקה אם שם ארוך מדי וקיצורו במידת הצורך
                            if (name != null && name.length() > MAX_NAME_LENGTH) {
                                logger.warn("קיצור שם ארוך: {}", name);
                                name = name.substring(0, MAX_NAME_LENGTH);
                                localSkipped++;
                            }

                            Medication medication = new Medication();
                            medication.setCui(entry.getKey());
                            medication.setName(name);
                            // שים לב שלא משתמשים בעמודה 'description'

                            entityManager.persist(medication);
                        }
                        return localSkipped;
                    } catch (Exception e) {
                        logger.error("שגיאה בעת הכנסת נתונים: {}", e.getMessage());
                        status.setRollbackOnly();
                        throw e;
                    }
                }
            });

            if (batchResult != null) {
                skippedCount += batchResult;
            }

            processedCount += batch.size();
            System.out.println("נשמרו " + processedCount + " תרופות מתוך " + totalSize);
        }

        System.out.println("הוכנסו " + processedCount + " תרופות למסד הנתונים");
        if (skippedCount > 0) {
            System.out.println("הערה: " + skippedCount + " שמות קוצרו בגלל אורך חריג");
        }
    }
}