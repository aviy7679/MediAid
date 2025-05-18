package com.example.mediaid.bl.build_UMLS_terms;
import com.example.mediaid.dal.UMLS_terms.Disease;
import com.example.mediaid.dal.UMLS_terms.UmlsTerm;

import com.example.mediaid.dal.UMLS_terms.DiseaseRepository;
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
public class UmlsDiseaseProcessor implements CommandLineRunner {

    // הגדרת קבצי קלט
    private static final String MRCONSO_FILE = "D:\\MediAid\\umls-2024AB-full\\2024AB-full\\2024AB\\META\\MRCONSO.RRF";
    private static final String MRSTY_FILE = "D:\\MediAid\\umls-2024AB-full\\2024AB-full\\2024AB\\META\\MRSTY.RRF";

    // קטגוריות Semantic Type של מחלות
    private static final Set<String> DISEASE_CATEGORIES = new HashSet<>(Arrays.asList("T046", "T047", "T191"));

    // מקורות מועדפים לבחירת מונחים
    private static final List<String> PREFERRED_SABS = Arrays.asList("SNOMEDCT_US", "MSH", "ICD10CM", "LNC", "MEDDRA");

    @PersistenceContext
    private EntityManager entityManager;

    @Autowired
    private DiseaseRepository diseaseRepository;

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
            if (diseaseRepository.count() > 0) {
                System.out.println("הנתונים כבר קיימים במסד הנתונים. מדלג על ייבוא.");
                return;
            }

            // שלב 1: טען את כל ה־CUI שהם מחלות
            System.out.println("שלב 1: טוען מחלות מקובץ MRSTY...");
            Set<String> diseaseCuis = loadDiseaseCuis();
            System.out.println("נמצאו " + diseaseCuis.size() + " מחלות (CUIs)");

            // שלב 2+3: טען מונחים ובחר מועדפים
            System.out.println("שלב 2: טוען מונחים מקובץ MRCONSO...");
            Map<String, String> diseaseTerms = loadDiseaseTerms(diseaseCuis);
            System.out.println("נבחרו " + diseaseTerms.size() + " מונחים מועדפים");

            // שלב 4: הכנס למסד נתונים
            System.out.println("שלב 3: מכניס נתונים למסד הנתונים...");
            insertIntoDatabase(diseaseTerms);

            System.out.println("✔ התהליך הושלם בהצלחה.");
        } catch (Exception e) {
            System.err.println("שגיאה בעיבוד: " + e.getMessage());
            e.printStackTrace();
        }
    }

    /**
     * טוען את כל ה-CUIs שמייצגים מחלות לפי הקטגוריות שהוגדרו
     */
    private Set<String> loadDiseaseCuis() throws IOException {
        Set<String> diseaseCuis = new HashSet<>();

        try (BufferedReader reader = new BufferedReader(new FileReader(MRSTY_FILE))) {
            String line;
            while ((line = reader.readLine()) != null) {
                String[] fields = line.split("\\|");
                String cui = fields[0];
                String tui = fields[1];

                if (DISEASE_CATEGORIES.contains(tui)) {
                    diseaseCuis.add(cui);
                }
            }
        }

        return diseaseCuis;
    }

    /**
     * טוען את המונחים האנגליים של המחלות ובוחר את המונח המועדף לכל CUI
     */
    private Map<String, String> loadDiseaseTerms(Set<String> diseaseCuis) throws IOException {
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

                if (diseaseCuis.contains(cui) && "ENG".equals(lang)) {
                    UmlsTerm term = new UmlsTerm(name, sab, tty, ispref);

                    // הוסף את המונח לרשימת המונחים עבור ה-CUI
                    allTerms.computeIfAbsent(cui, k -> new ArrayList<>()).add(term);
                }
            }
        }

        // בחר מונח מועדף לכל CUI
        Map<String, String> diseaseTerms = new HashMap<>();
        for (Map.Entry<String, List<UmlsTerm>> entry : allTerms.entrySet()) {
            String bestName = chooseBestTerm(entry.getValue());
            if (bestName != null) {
                diseaseTerms.put(entry.getKey(), bestName);
            }
        }

        return diseaseTerms;
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

            // מיון לפי סוג מונח
            int ttyCompare = Integer.compare(
                    "PT".equals(term1.getTty()) ? 0 : 1,
                    "PT".equals(term2.getTty()) ? 0 : 1
            );
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
     * מכניס את המחלות למסד הנתונים
     */
    private static final Logger logger = LoggerFactory.getLogger(UmlsDiseaseProcessor.class);
    private static final int MAX_NAME_LENGTH = 250; // קצת מתחת למגבלה של 255 כדי להיות בטוחים

    protected void insertIntoDatabase(Map<String, String> diseaseTerms) {
        final int batchSize = 50;
        final int totalSize = diseaseTerms.size();
        int processedCount = 0;
        int skippedCount = 0;

        List<List<Map.Entry<String, String>>> batches = new ArrayList<>();
        List<Map.Entry<String, String>> currentBatch = new ArrayList<>();

        // הכנת האצוות
        for (Map.Entry<String, String> entry : diseaseTerms.entrySet()) {
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

                            Disease disease = new Disease();
                            disease.setCui(entry.getKey());
                            disease.setName(name);
                            // שים לב שלא משתמשים בעמודה 'description'

                            entityManager.persist(disease);
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
            System.out.println("נשמרו " + processedCount + " מחלות מתוך " + totalSize);
        }

        System.out.println("הוכנסו " + processedCount + " מחלות למסד הנתונים");
        if (skippedCount > 0) {
            System.out.println("הערה: " + skippedCount + " שמות קוצרו בגלל אורך חריג");
        }
    }
}