package com.egen.fitogen.repository;

import com.egen.fitogen.model.Contrahent;

import java.util.List;

public interface ContrahentRepository {

    void save(Contrahent contrahent);

    void update(Contrahent contrahent);

    void deleteById(int id);

    Contrahent findById(int id);

    List<Contrahent> findAll();

    List<Contrahent> findAllClients();
}