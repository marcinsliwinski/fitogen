package com.egen.fitogen.repository;

import com.egen.fitogen.domain.Contrahent;
import java.util.List;

public interface ContrahentRepository {

    List<Contrahent> findAll();

    Contrahent findById(int id);

    void save(Contrahent contrahent);

    void update(Contrahent contrahent);

    void delete(int id);

    // Opcjonalnie: wyszukiwanie po nazwie
    List<Contrahent> findByName(String name);
}