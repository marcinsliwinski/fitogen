package com.egen.fitogen;

import com.egen.fitogen.database.DatabaseInitializer;

public class MainApp {

    public static void main(String[] args) {
        DatabaseInitializer.init(); // Tworzy tabele SQLite
        System.out.println("Fitogen database ready!");
    }
}