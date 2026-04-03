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
        if (user.getId() > 0) {
            AppUser existing = findById(user.getId());
            repository.update(user);
            logAudit("UPDATE", user,
                    "Zaktualizowano użytkownika " + summarize(user)
                            + ". Wcześniej: " + summarize(existing));
        } else {
            repository.save(user);
            logAudit("CREATE", user, "Utworzono użytkownika " + summarize(user));
        }
    }

    public void delete(int id) {
        AppUser existing = findById(id);
        repository.deleteById(id);
        logAudit("DELETE", existing, "Usunięto użytkownika " + summarize(existing));
    }

    public Optional<Integer> getDefaultUserId() {
        return repository.findDefaultUserId();
    }

    public void setDefaultUserId(Integer userId) {
        Integer previousUserId = repository.findDefaultUserId().orElse(null);
        repository.saveDefaultUserId(userId);

        if (auditLogService != null && !java.util.Objects.equals(previousUserId, userId)) {
            AppUser current = userId == null ? null : findById(userId);
            AppUser previous = previousUserId == null ? null : findById(previousUserId);
            auditLogService.log(
                    "APP_USER",
                    userId,
                    "UPDATE",
                    "Zmieniono domyślnego użytkownika z " + summarize(previous) + " na " + summarize(current)
            );
        }
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

    private void logAudit(String actionType, AppUser user, String description) {
        if (auditLogService != null) {
            auditLogService.log("APP_USER", user == null ? null : user.getId(), actionType, description);
        }
    }

    private String summarize(AppUser user) {
        if (user == null) {
            return "[brak danych]";
        }

        String displayName = user.getDisplayName();
        return displayName == null || displayName.isBlank() ? "[bez nazwy]" : displayName;
    }


    private AppUser findById(int id) {
        for (AppUser user : repository.findAll()) {
            if (user != null && user.getId() == id) {
                return user;
            }
        }
        return null;
    }

}
