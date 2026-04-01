package com.egen.fitogen.service;

import com.egen.fitogen.model.AppUser;
import com.egen.fitogen.repository.AppUserRepository;

import java.util.List;
import java.util.Optional;

public class AppUserService {

    private final AppUserRepository repository;
    private AuditLogService auditLogService;

    public AppUserService(AppUserRepository repository) {
        this.repository = repository;
    }

    public void setAuditLogService(AuditLogService auditLogService) {
        this.auditLogService = auditLogService;
    }

    public List<AppUser> getAll() {
        return repository.findAll();
    }

    public void save(AppUser user) {
        validate(user);

        if (user.getId() > 0) {
            repository.update(user);
            log("UPDATE", user.getId(), "Zaktualizowano użytkownika aplikacji: " + describe(user));
            return;
        }

        repository.save(user);
        log("CREATE", null, "Dodano użytkownika aplikacji: " + describe(user));
    }

    public void delete(int id) {
        AppUser existing = findById(id);
        String actor = currentActor();
        repository.deleteById(id);
        logAsActor(actor, "DELETE", id, "Usunięto użytkownika aplikacji: " + describe(existing));
    }

    public Optional<Integer> getDefaultUserId() {
        return repository.findDefaultUserId();
    }

    public void setDefaultUserId(Integer userId) {
        Integer previousUserId = getDefaultUserId().orElse(null);
        if (sameUser(previousUserId, userId)) {
            return;
        }

        String actor = currentActor();
        repository.saveDefaultUserId(userId);
        logAsActor(
                actor,
                "SET_DEFAULT",
                userId,
                "Zmieniono domyślnego użytkownika z " + describe(findById(previousUserId))
                        + " na " + describe(findById(userId))
        );
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

    private AppUser findById(Integer id) {
        if (id == null || id <= 0) {
            return null;
        }

        return repository.findAll().stream()
                .filter(user -> user.getId() == id)
                .findFirst()
                .orElse(null);
    }

    private boolean sameUser(Integer left, Integer right) {
        if (left == null && right == null) {
            return true;
        }
        if (left == null || right == null) {
            return false;
        }
        return left.intValue() == right.intValue();
    }

    private void log(String actionType, Integer entityId, String description) {
        if (auditLogService == null) {
            return;
        }
        auditLogService.log("APP_USER", entityId, actionType, description);
    }

    private void logAsActor(String actor, String actionType, Integer entityId, String description) {
        if (auditLogService == null) {
            return;
        }
        auditLogService.logAsActor(actor, "APP_USER", entityId, actionType, description);
    }

    private String currentActor() {
        return auditLogService == null ? "System" : auditLogService.getCurrentActor();
    }

    private String describe(AppUser user) {
        if (user == null) {
            return "[brak użytkownika]";
        }

        String displayName = user.getDisplayName();
        if (displayName == null || displayName.isBlank()) {
            return "[brak użytkownika]";
        }

        return displayName;
    }
}
