package com.egen.fitogen.service;

import com.egen.fitogen.model.Contrahent;
import com.egen.fitogen.repository.ContrahentRepository;

import java.util.List;

public class ContrahentService {

    private final ContrahentRepository repository;

    public ContrahentService(ContrahentRepository repository) {
        this.repository = repository;
    }

    public void addContrahent(Contrahent contrahent) {
        validate(contrahent);
        repository.save(contrahent);
    }

    public void updateContrahent(Contrahent contrahent) {
        validate(contrahent);
        repository.update(contrahent);
    }

    public void deleteContrahent(int id) {
        repository.deleteById(id);
    }

    public Contrahent getContrahentById(int id) {
        return repository.findById(id);
    }

    public List<Contrahent> getAllContrahents() {
        return repository.findAll();
    }

    public List<Contrahent> getAllClients() {
        return repository.findAllClients();
    }

    private void validate(Contrahent contrahent) {
        if (contrahent == null) {
            throw new IllegalArgumentException("Contrahent cannot be null.");
        }

        if (contrahent.getName() == null || contrahent.getName().isBlank()) {
            throw new IllegalArgumentException("Contrahent name required.");
        }
    }
}