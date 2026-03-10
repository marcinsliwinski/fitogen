package com.egen.fitogen;

import com.egen.fitogen.database.*;
import com.egen.fitogen.domain.Contrahent;
import com.egen.fitogen.repository.ContrahentRepository;
import com.egen.fitogen.service.ContrahentService;

public class MainApp {

    public static void main(String[] args) {

        DatabaseInitializer.initDatabase();

        System.out.println("Fitogen database ready!");

        ContrahentRepository repo = new SqliteContrahentRepository();

        ContrahentService contrahentService =
                new ContrahentService(repo);

        Contrahent c = new Contrahent();

        c.setName("Szkółka Zielona");
        c.setCountry("Polska");
        c.setCountryCode("PL");
        c.setCity("Warszawa");
        c.setPostalCode("00-001");
        c.setStreet("Ogrodowa 1");
        c.setPhytosanitaryNumber("PL12345");

        contrahentService.addContrahent(c);

        contrahentService.getAllContrahents()
                .forEach(System.out::println);
    }
}