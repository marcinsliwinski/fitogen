package com.egen.fitogen.service;

import com.egen.fitogen.model.AppUser;
import com.egen.fitogen.repository.AppUserRepository;

import java.util.List;
import java.util.Optional;

public class AppUserService {

    private final AppUserRepository repository;

    public AppUserService(AppUserRepository repository) {
        this.repository = repository;
    }

    public List<AppUser> getAll() {
        return repository.findAll();
    }

    public void save(AppUser user) {
        validate(user);
        if (user.getId() > 0) {
            repository.update(user);
        } else {
            repository.save(user);
        }
    }

    public void delete(int id) {
        repository.deleteById(id);
    }

    public Optional<Integer> getDefaultUserId() {
        return repository.findDefaultUserId();
    }

    public void setDefaultUserId(Integer userId) {
        repository.saveDefaultUserId(userId);
    }

    private void validate(AppUser user) {
        if (user == null) {
            throw new IllegalArgumentException("Użytkownik nie może być pusty.");
        }
        if (user.getFirstName() == null || user.getFirstName().isBlank()) {
            throw new IllegalArgumentException("Imię jest wymagane.");
        }
        if (user.getLastName() == null || user.getLastName().isBlank()) {
            throw new IllegalArgumentException("Nazwisko jest wymagane.");
        }
    }
}
