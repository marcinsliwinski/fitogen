package com.egen.fitogen.database;

import com.egen.fitogen.config.DatabaseConfig;

import java.sql.Connection;
import java.sql.SQLException;
import java.sql.Statement;

public class DatabaseInitializer {

    public static void initDatabase() {

        try (Connection conn = DatabaseConfig.getConnection();
             Statement stmt = conn.createStatement()) {

            // PLANTS
            stmt.execute("""
                CREATE TABLE IF NOT EXISTS plants (
                    id INTEGER PRIMARY KEY AUTOINCREMENT,
                    species TEXT,
                    variety TEXT,
                    rootstock TEXT,
                    latin_species_name TEXT,
                    visibility_status TEXT
                )
            """);

            // CONTRAHENTS
            stmt.execute("""
                CREATE TABLE IF NOT EXISTS contrahents (
                    id INTEGER PRIMARY KEY AUTOINCREMENT,
                    name TEXT NOT NULL,
                    country TEXT,
                    country_code TEXT,
                    postal_code TEXT,
                    city TEXT,
                    street TEXT,
                    phytosanitary_number TEXT
                )
            """);

            // PLANT BATCH
            stmt.execute("""
                CREATE TABLE IF NOT EXISTS plant_batches (
                    id INTEGER PRIMARY KEY AUTOINCREMENT,
                    interior_batch_no TEXT,
                    exterior_batch_no TEXT,
                    plant_id INTEGER,
                    qty INTEGER,
                    creation_date TEXT,
                    manufacturer_country_code TEXT,
                    fito_qualification_category TEXT,
                    eppo_code TEXT,
                    zp_zone TEXT,
                    contrahent_id INTEGER,
                    comments TEXT
                )
            """);

            // DOCUMENT
            stmt.execute("""
                CREATE TABLE IF NOT EXISTS documents (
                    id INTEGER PRIMARY KEY AUTOINCREMENT,
                    document_number TEXT,
                    document_type TEXT,
                    issue_date TEXT,
                    contrahent_id INTEGER,
                    created_by TEXT,
                    comments TEXT
                )
            """);

            // DOCUMENT ITEMS
            stmt.execute("""
                CREATE TABLE IF NOT EXISTS document_items (
                    id INTEGER PRIMARY KEY AUTOINCREMENT,
                    document_id INTEGER,
                    plant_batch_id INTEGER,
                    qty INTEGER,
                    passport_required INTEGER
                )
            """);

            System.out.println("Database initialized ✅");

        } catch (SQLException e) {
            e.printStackTrace();
        }
    }
}