package com.egen.fitogen.util;

import java.time.LocalDate;
import java.time.format.DateTimeFormatter;

public class NumeratorService {

    public static String generateDocumentNumber(String documentType) {

        String prefix;

        switch (documentType) {

            case "SUPPLIER_DOCUMENT":
                prefix = "DD";
                break;

            case "NURSERY_DOCUMENT":
                prefix = "SDD";
                break;

            default:
                prefix = "DOC";
        }

        String date = LocalDate.now()
                .format(DateTimeFormatter.ofPattern("yyyyMMdd"));

        long random = System.currentTimeMillis() % 10000;

        return prefix + "-" + date + "-" + random;
    }
}