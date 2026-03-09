package com.egen.fitogen.database;

import com.egen.fitogen.config.DatabaseConfig;

import java.sql.Connection;
import java.sql.SQLException;
import java.sql.Statement;

public class DatabaseInitializer {

    public static void init() {
        try (Connection conn = DatabaseConfig.getConnection();
             Statement stmt = conn.createStatement()) {

            // Tabela Plants
            stmt.executeUpdate("""
                CREATE TABLE IF NOT EXISTS plants (
                    id INTEGER PRIMARY KEY AUTOINCREMENT,
                    species TEXT NOT NULL,
                    variety TEXT,
                    rootstock TEXT,
                    latin_species_name TEXT,
                    visibility_status TEXT
                );
            """);

            // Tabela PlantBatch
            stmt.executeUpdate("""
                CREATE TABLE IF NOT EXISTS plant_batches (
                    id INTEGER PRIMARY KEY AUTOINCREMENT,
                    plant_id INTEGER NOT NULL,
                    interior_batch_no TEXT NOT NULL,
                    exterior_batch_no TEXT,
                    qty INTEGER,
                    creation_date TEXT,
                    manufacturer_country_code TEXT,
                    fito_qualification_category TEXT,
                    eppo_code TEXT,
                    zp_zone TEXT,
                    contrahent_id INTEGER,
                    comments TEXT,
                    FOREIGN KEY(plant_id) REFERENCES plants(id)
                );
            """);

            // Tabela Contrahent
            stmt.executeUpdate("""
                CREATE TABLE IF NOT EXISTS contrahents (
                    id INTEGER PRIMARY KEY AUTOINCREMENT,
                    name TEXT NOT NULL,
                    country TEXT,
                    city TEXT,
                    street TEXT,
                    postal_code TEXT,
                    fitosanitary_number TEXT
                );
            """);

            // Tabela Document
            stmt.executeUpdate("""
                CREATE TABLE IF NOT EXISTS documents (
                    id INTEGER PRIMARY KEY AUTOINCREMENT,
                    contrahent_id INTEGER,
                    plant_batch_id INTEGER,
                    issue_date TEXT,
                    passport_required INTEGER,
                    comments TEXT,
                    issued_by TEXT,
                    FOREIGN KEY(contrahent_id) REFERENCES contrahents(id),
                    FOREIGN KEY(plant_batch_id) REFERENCES plant_batches(id)
                );
            """);

            System.out.println("Database initialized ✅");

        } catch (SQLException e) {
            e.printStackTrace();
        }
    }
}