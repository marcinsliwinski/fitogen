package com.egen.fitogen.repository;

import com.egen.fitogen.model.AppUser;

import java.util.List;
import java.util.Optional;

public interface AppUserRepository {

    List<AppUser> findAll();

    void save(AppUser user);

    void update(AppUser user);

    void deleteById(int id);

    Optional<Integer> findDefaultUserId();

    void saveDefaultUserId(Integer userId);
}
