package com.example.mediaid.bl.build_UMLS_terms;

import com.example.mediaid.dal.UMLS_terms.UmlsTerm;
import com.example.mediaid.constants.DatabaseConstants;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.BufferedReader;
import java.io.FileReader;
import java.io.IOException;
import java.util.*;

public class UmlsTermHelper {
    private static final Logger logger = LoggerFactory.getLogger(UmlsTermHelper.class);

    /**
     * טוען CUIs מסוג סמנטי מסוים מקובץ MRSTY
     */
    public static Set<String> loadCuisBySemanticTypes(Set<String> semanticTypes) throws IOException {
        Set<String> resultCuis = new HashSet<String>();
        int lineNumber = 0;
        int validLines = 0;
        int skippedLines = 0;
        try (BufferedReader reader = new BufferedReader(new FileReader(DatabaseConstants.MRSTY_FILE_PATH))) {
            String line;
            while ((line = reader.readLine()) != null) {
                lineNumber++;

                // דלג על שורות ריקות
                if (line.trim().isEmpty()) {
                    skippedLines++;
                    continue;
                }

                String[] fields = line.split("\\|");

                // בדיקת תקינות - וודא שיש לפחות 2 שדות
                if (fields.length < 2) {
                    logger.warn("שורה {} בקובץ MRSTY לא תקינה: {}", lineNumber, line.substring(0, Math.min(50, line.length())));
                    skippedLines++;
                    continue;
                }

                String cui = fields[0].trim();
                String tui = fields[1].trim();

                if (semanticTypes.contains(tui)) {
                    resultCuis.add(cui);
                    validLines++;
                }
            }
        }
        logger.info("Finished reading MRSTY: {} valid CUIs loaded, {} lines skipped (from total {}).",
                validLines, skippedLines, lineNumber);
        return resultCuis;

    }

    /**
     * טוען את המונחים באנגלית של רשימת הCUI
     */
    public static Map<String, List<UmlsTerm>> loadTermsForCuis(Set<String> cuis) throws IOException {
        Map<String,List<UmlsTerm>> allTerms = new HashMap<>();
        try(BufferedReader reader = new BufferedReader(new FileReader(DatabaseConstants.MRCONSO_FILE_PATH))) {
            String line;
            while ((line = reader.readLine()) != null) {
                String[] fields = line.split("\\|");
                String cui = fields[0];
                String lang = fields[1];
                String sab = fields[11]; // מקור המידע (UMLS source) לדוגמה: SNOMEDCT_US
                String tty = fields[12]; // סוג מונח: PT = Preferred Term
                String ispref = fields[16]; // האם מונח מועדף
                String name = fields[14]; // שם המונח

                if(cuis.contains(cui) && "ENG".equals(lang)) {
                    UmlsTerm term = new UmlsTerm(name, sab, tty, ispref);
                    allTerms.computeIfAbsent(cui, k -> new ArrayList<>()).add(term);
                }
            }

        }
        return allTerms;
    }

    /**
     * בוחר את המונח המועדף לפי המקורות המועדפים
     */
    public static String chooseBestTerm(List<UmlsTerm> terms, List<String> preferredSources) {
        if(terms == null || terms.isEmpty()) {
            return null;
        }
        terms.sort((term1, term2)->{
            //לפי מקור מידע מועדף
            int sabCompare = Integer.compare(
                    preferredSources.contains(term1.getSab())?preferredSources.indexOf(term1.getSab()):preferredSources.size(),
                    preferredSources.contains(term2.getSab())?preferredSources.indexOf(term1.getSab()):preferredSources.size()
            );
            if (sabCompare != 0) {return sabCompare;}


            // מיון לפי האם מועדף
            int prefCompare = Integer.compare(
                    "Y".equals(term1.getIspref()) ? 0 : 1,
                    "Y".equals(term2.getIspref()) ? 0 : 1
            );
            if (prefCompare != 0) return prefCompare;

            // מיון לפי סוג מונח
            List<String> preferredTypes = Arrays.asList("PT", "PV", "SY");
            int term1Rank = preferredTypes.contains(term1.getTty()) ? preferredTypes.indexOf(term1.getTty()) : preferredTypes.size();
            int term2Rank = preferredTypes.contains(term2.getTty()) ? preferredTypes.indexOf(term2.getTty()) : preferredTypes.size();
            int ttyCompare = Integer.compare(term1Rank, term2Rank);
            if (ttyCompare != 0) return ttyCompare;

            //מיון לפי אורך מונח
            int lengthCompare = Integer.compare(term1.getName().length(), term2.getName().length());
            if (lengthCompare != 0) return lengthCompare;

            //מיון לפי מספר סימני הפיסוק
            return Integer.compare(countPunctuation(term1.getName()), countPunctuation(term2.getName()));
        });
        return terms.get(0).getName();
    }

    /**
     * ספירת מספר סימני פיסוק
     */
    private static int countPunctuation(String text) {
        int count = 0;
        for(char c : text.toCharArray()) {
            if(c == ';' || c == ',' || c == '.') {
                count++;
            }
        }
        return count;
    }

}