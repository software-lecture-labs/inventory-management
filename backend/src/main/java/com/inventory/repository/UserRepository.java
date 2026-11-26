package com.inventory.repository;

import com.inventory.model.Role;
import com.inventory.model.User;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;

import java.util.List;
import java.util.Optional;

public interface UserRepository extends JpaRepository<User, Long>, JpaSpecificationExecutor<User> {

    Optional<User> findByEmailIgnoreCase(String email);

    boolean existsByEmailIgnoreCase(String email);

    boolean existsByPhone(String phone);

    boolean existsByRole(Role role);

    List<User> findByRoleAndEnabledTrue(Role role);
}
