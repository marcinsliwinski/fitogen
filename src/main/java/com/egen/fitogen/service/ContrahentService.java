package com.egen.fitogen.service;

import com.egen.fitogen.domain.Contrahent;
import com.egen.fitogen.repository.ContrahentRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.List;

public class ContrahentService {

    private final ContrahentRepository repository;
    private static final Logger logger = LoggerFactory.getLogger(ContrahentService.class);

    public ContrahentService(ContrahentRepository repository) {
        this.repository = repository;
    }

    public List<Contrahent> getAllContrahents() {
        logger.info("Fetching all contrahents");
        return repository.findAll();
    }

    public Contrahent getContrahentById(int id) {
        logger.info("Fetching contrahent by id {}", id);
        return repository.findById(id);
    }

    public void addContrahent(Contrahent contrahent) {
        logger.info("Adding new contrahent: {}", contrahent.getName());
        repository.save(contrahent);
    }

    public void updateContrahent(Contrahent contrahent) {
        logger.info("Updating contrahent id {}: {}", contrahent.getId(), contrahent.getName());
        repository.update(contrahent);
    }

    public void deleteContrahent(int id) {
        logger.info("Deleting contrahent id {}", id);
        repository.delete(id);
    }
}