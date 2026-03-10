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



    public void addContrahent(Contrahent contrahent) {
        logger.info("Adding new contrahent: {}", contrahent.getName());
        repository.save(contrahent);
    }


}