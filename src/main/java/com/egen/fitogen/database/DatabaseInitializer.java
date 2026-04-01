package com.egen.fitogen.database;

import com.egen.fitogen.config.DatabaseConfig;

import java.sql.Connection;
import java.sql.ResultSet;
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
                    eppo_code TEXT,
                    passport_required INTEGER NOT NULL DEFAULT 0,
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
                    phytosanitary_number TEXT,
                    is_supplier INTEGER NOT NULL DEFAULT 0,
                    is_client INTEGER NOT NULL DEFAULT 0
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
                    is_internal_source INTEGER NOT NULL DEFAULT 0,
                    comments TEXT,
                    status TEXT DEFAULT 'ACTIVE'
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
                    comments TEXT,
                    status TEXT DEFAULT 'ACTIVE'
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

            // NUMBERING CONFIG
            stmt.execute("""
                CREATE TABLE IF NOT EXISTS numbering_config (
                    id INTEGER PRIMARY KEY AUTOINCREMENT,
                    type TEXT NOT NULL UNIQUE,
                    section1_type TEXT,
                    section1_static_value TEXT,
                    section1_separator TEXT,
                    section2_type TEXT,
                    section2_static_value TEXT,
                    section2_separator TEXT,
                    section3_type TEXT,
                    section3_static_value TEXT,
                    section3_separator TEXT,
                    current_counter INTEGER NOT NULL DEFAULT 0
                )
            """);

            // DOCUMENT TYPES
            stmt.execute("""
                CREATE TABLE IF NOT EXISTS document_types (
                    id INTEGER PRIMARY KEY AUTOINCREMENT,
                    name TEXT NOT NULL,
                    code TEXT
                )
            """);

            // APP USERS
            stmt.execute("""
                CREATE TABLE IF NOT EXISTS app_users (
                    id INTEGER PRIMARY KEY AUTOINCREMENT,
                    first_name TEXT NOT NULL,
                    last_name TEXT NOT NULL
                )
            """);

            // APP SETTINGS
            stmt.execute("""
                CREATE TABLE IF NOT EXISTS app_settings (
                    setting_key TEXT PRIMARY KEY,
                    setting_value TEXT
                )
            """);

            // EPPO CODES
            stmt.execute("""
                CREATE TABLE IF NOT EXISTS eppo_codes (
                    id INTEGER PRIMARY KEY AUTOINCREMENT,
                    code TEXT NOT NULL UNIQUE,
                    species_name TEXT,
                    latin_species_name TEXT,
                    scientific_name TEXT,
                    common_name TEXT,
                    passport_required INTEGER NOT NULL DEFAULT 0,
                    status TEXT DEFAULT 'ACTIVE'
                )
            """);

            // EPPO ZONES
            stmt.execute("""
                CREATE TABLE IF NOT EXISTS eppo_zones (
                    id INTEGER PRIMARY KEY AUTOINCREMENT,
                    code TEXT NOT NULL UNIQUE,
                    name TEXT NOT NULL,
                    country_code TEXT,
                    status TEXT DEFAULT 'ACTIVE'
                )
            """);

            // EPPO CODE ↔ ZONE LINKS
            stmt.execute("""
                CREATE TABLE IF NOT EXISTS eppo_code_zone_links (
                    id INTEGER PRIMARY KEY AUTOINCREMENT,
                    eppo_code_id INTEGER NOT NULL,
                    eppo_zone_id INTEGER NOT NULL
                )
            """);

            stmt.execute("""
                CREATE UNIQUE INDEX IF NOT EXISTS ux_eppo_code_zone_links_pair
                ON eppo_code_zone_links (eppo_code_id, eppo_zone_id)
            """);

            stmt.execute("""
                CREATE INDEX IF NOT EXISTS ix_eppo_code_zone_links_code
                ON eppo_code_zone_links (eppo_code_id)
            """);

            stmt.execute("""
                CREATE INDEX IF NOT EXISTS ix_eppo_code_zone_links_zone
                ON eppo_code_zone_links (eppo_zone_id)
            """);

            // EPPO CODE ↔ SPECIES LINKS
            stmt.execute("""
                CREATE TABLE IF NOT EXISTS eppo_code_species_links (
                    id INTEGER PRIMARY KEY AUTOINCREMENT,
                    eppo_code_id INTEGER NOT NULL,
                    species_name TEXT,
                    latin_species_name TEXT
                )
            """);

            stmt.execute("""
                CREATE UNIQUE INDEX IF NOT EXISTS ux_eppo_code_species_links_signature
                ON eppo_code_species_links (eppo_code_id, species_name, latin_species_name)
            """);

            stmt.execute("""
                CREATE INDEX IF NOT EXISTS ix_eppo_code_species_links_code
                ON eppo_code_species_links (eppo_code_id)
            """);

            // EPPO CODE ↔ PLANT LINKS
            stmt.execute("""
                CREATE TABLE IF NOT EXISTS eppo_code_plant_links (
                    id INTEGER PRIMARY KEY AUTOINCREMENT,
                    eppo_code_id INTEGER NOT NULL,
                    plant_id INTEGER NOT NULL
                )
            """);

            stmt.execute("""
                CREATE UNIQUE INDEX IF NOT EXISTS ux_eppo_code_plant_links_pair
                ON eppo_code_plant_links (eppo_code_id, plant_id)
            """);

            stmt.execute("""
                CREATE INDEX IF NOT EXISTS ix_eppo_code_plant_links_code
                ON eppo_code_plant_links (eppo_code_id)
            """);

            stmt.execute("""
                CREATE INDEX IF NOT EXISTS ix_eppo_code_plant_links_plant
                ON eppo_code_plant_links (plant_id)
            """);

            // migrations for older databases
            ensureColumnExists(stmt, "plants", "eppo_code", "TEXT");
            ensureColumnExists(stmt, "plants", "passport_required", "INTEGER NOT NULL DEFAULT 0");
            ensureColumnExists(stmt, "contrahents", "is_supplier", "INTEGER NOT NULL DEFAULT 0");
            ensureColumnExists(stmt, "contrahents", "is_client", "INTEGER NOT NULL DEFAULT 0");
            ensureColumnExists(stmt, "plant_batches", "is_internal_source", "INTEGER NOT NULL DEFAULT 0");
            ensureColumnExists(stmt, "plant_batches", "status", "TEXT DEFAULT 'ACTIVE'");
            ensureColumnExists(stmt, "documents", "status", "TEXT DEFAULT 'ACTIVE'");
            ensureColumnExists(stmt, "eppo_codes", "species_name", "TEXT");
            ensureColumnExists(stmt, "eppo_codes", "latin_species_name", "TEXT");
            ensureColumnExists(stmt, "eppo_codes", "scientific_name", "TEXT");
            ensureColumnExists(stmt, "eppo_codes", "common_name", "TEXT");
            ensureColumnExists(stmt, "eppo_codes", "passport_required", "INTEGER NOT NULL DEFAULT 0");
            ensureColumnExists(stmt, "eppo_codes", "status", "TEXT DEFAULT 'ACTIVE'");
            ensureColumnExists(stmt, "eppo_zones", "country_code", "TEXT");
            ensureColumnExists(stmt, "eppo_zones", "status", "TEXT DEFAULT 'ACTIVE'");

            stmt.executeUpdate("UPDATE plants SET passport_required = COALESCE(passport_required, 0)");
            stmt.executeUpdate("UPDATE contrahents SET is_supplier = COALESCE(is_supplier, 0)");
            stmt.executeUpdate("UPDATE contrahents SET is_client = COALESCE(is_client, 0)");
            stmt.executeUpdate("UPDATE plant_batches SET is_internal_source = COALESCE(is_internal_source, 0)");
            stmt.executeUpdate("UPDATE plant_batches SET status = 'ACTIVE' WHERE status IS NULL OR TRIM(status) = ''");
            stmt.executeUpdate("UPDATE documents SET status = 'ACTIVE' WHERE status IS NULL OR TRIM(status) = ''");
            stmt.executeUpdate("UPDATE eppo_codes SET species_name = COALESCE(NULLIF(TRIM(species_name), ''), common_name)");
            stmt.executeUpdate("UPDATE eppo_codes SET latin_species_name = COALESCE(NULLIF(TRIM(latin_species_name), ''), scientific_name)");
            stmt.executeUpdate("UPDATE eppo_codes SET passport_required = COALESCE(passport_required, 0)");
            stmt.executeUpdate("UPDATE eppo_codes SET status = 'ACTIVE' WHERE status IS NULL OR TRIM(status) = ''");
            stmt.executeUpdate("UPDATE eppo_zones SET status = 'ACTIVE' WHERE status IS NULL OR TRIM(status) = ''");

            stmt.execute("""
                INSERT OR IGNORE INTO eppo_code_species_links (eppo_code_id, species_name, latin_species_name)
                SELECT id,
                       NULLIF(TRIM(COALESCE(species_name, common_name)), ''),
                       NULLIF(TRIM(COALESCE(latin_species_name, scientific_name)), '')
                FROM eppo_codes
                WHERE NULLIF(TRIM(COALESCE(species_name, common_name)), '') IS NOT NULL
                   OR NULLIF(TRIM(COALESCE(latin_species_name, scientific_name)), '') IS NOT NULL
            """);

            // default numbering rows
            stmt.execute("""
                INSERT OR IGNORE INTO numbering_config (
                    type,
                    current_counter
                ) VALUES ('BATCH', 0)
            """);

            stmt.execute("""
                INSERT OR IGNORE INTO numbering_config (
                    type,
                    current_counter
                ) VALUES ('DOCUMENT', 0)
            """);

            // default document types
            stmt.execute("""
                INSERT INTO document_types (name, code)
                SELECT 'Dokument dostawcy', 'DD'
                WHERE NOT EXISTS (
                    SELECT 1 FROM document_types WHERE name = 'Dokument dostawcy' AND code = 'DD'
                )
            """);

            stmt.execute("""
                INSERT INTO document_types (name, code)
                SELECT 'Szkółkarski dokument dostawcy', 'SDD'
                WHERE NOT EXISTS (
                    SELECT 1 FROM document_types WHERE name = 'Szkółkarski dokument dostawcy' AND code = 'SDD'
                )
            """);

            System.out.println("Database initialized ✅");

        } catch (SQLException e) {
            e.printStackTrace();
        }
    }

    private static void ensureColumnExists(Statement stmt, String tableName, String columnName, String definition) throws SQLException {
        if (!columnExists(stmt, tableName, columnName)) {
            stmt.execute("ALTER TABLE " + tableName + " ADD COLUMN " + columnName + " " + definition);
        }
    }

    private static boolean columnExists(Statement stmt, String tableName, String columnName) throws SQLException {
        try (ResultSet rs = stmt.executeQuery("PRAGMA table_info(" + tableName + ")")) {
            while (rs.next()) {
                String existingName = rs.getString("name");
                if (columnName.equalsIgnoreCase(existingName)) {
                    return true;
                }
            }
        }
        return false;
    }
}