package com.egen.fitogen.service;

import com.egen.fitogen.model.AppUser;
import com.egen.fitogen.repository.AppUserRepository;

import java.util.List;
import java.util.Optional;

public class AppUserService {

    private final AppUserRepository repository;
    private final AuditLogService auditLogService;

    public AppUserService(AppUserRepository repository, AuditLogService auditLogService) {
        this.repository = repository;
        this.auditLogService = auditLogService;
    }

    public List<AppUser> getAll() {
        return repository.findAll();
    }

    public void save(AppUser user) {
        validate(user);
        boolean update = user.getId() > 0;
        if (update) {
            repository.update(user);
        } else {
            repository.save(user);
        }

        if (auditLogService != null) {
            auditLogService.log(
                    "APP_USER",
                    user.getId() > 0 ? user.getId() : null,
                    update ? "UPDATE" : "CREATE",
                    (update ? "Zaktualizowano użytkownika: " : "Dodano użytkownika: ") + buildUserSummary(user)
            );
        }
    }

    public void delete(int id) {
        repository.deleteById(id);
        if (auditLogService != null) {
            auditLogService.log("APP_USER", id, "DELETE", "Usunięto użytkownika o ID=" + id);
        }
    }

    public Optional<Integer> getDefaultUserId() {
        return repository.findDefaultUserId();
    }

    public void setDefaultUserId(Integer userId) {
        repository.saveDefaultUserId(userId);
        if (auditLogService != null) {
            auditLogService.log(
                    "APP_USER",
                    userId,
                    "SET_DEFAULT",
                    userId == null
                            ? "Wyczyszczono domyślnego użytkownika."
                            : "Ustawiono domyślnego użytkownika o ID=" + userId
            );
        }
    }

    private String buildUserSummary(AppUser user) {
        return user.getDisplayName() == null || user.getDisplayName().isBlank()
                ? "Użytkownik bez nazwy"
                : user.getDisplayName();
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
