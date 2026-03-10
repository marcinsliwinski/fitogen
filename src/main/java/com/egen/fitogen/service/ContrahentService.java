package com.egen.fitogen.service;

import com.egen.fitogen.domain.Contrahent;
import com.egen.fitogen.repository.ContrahentRepository;

import java.util.List;

public class ContrahentService {

    private final ContrahentRepository repository;

    public ContrahentService(ContrahentRepository repository) {
        this.repository = repository;
    }

    public void addContrahent(Contrahent c) {

        if (c.getName() == null || c.getName().isBlank()) {
            throw new IllegalArgumentException("Contrahent name required");
        }

        repository.save(c);
    }

    public List<Contrahent> getAllContrahents() {
        return repository.findAll();
    }
}