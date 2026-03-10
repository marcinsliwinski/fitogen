package com.egen.fitogen;

import com.egen.fitogen.database.*;
import com.egen.fitogen.domain.*;
import com.egen.fitogen.repository.*;
import com.egen.fitogen.database.DatabaseInitializer;

import java.time.LocalDate;

public class MainApp {

    public static void main(String[] args) {

        DatabaseInitializer.initDatabase();

        System.out.println("Fitogen database ready!");

        ContrahentRepository contrahentRepo = new SqliteContrahentRepository();

        Contrahent c = new Contrahent();
        c.setName("Szkółka Zielona");
        c.setCountry("Polska");
        c.setCountryCode("PL");
        c.setCity("Warszawa");
        c.setPostalCode("00-001");
        c.setStreet("Ogrodowa 1");
        c.setPhytosanitaryNumber("PL-12345");

        contrahentRepo.save(c);

        System.out.println("Test contrahent inserted");

    }
}