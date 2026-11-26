package com.example.envanter_yonetimi.repository;

import com.example.envanter_yonetimi.model.Role;
import org.springframework.data.jpa.repository.JpaRepository;

public interface RoleRepository extends JpaRepository<Role, Long> {
    // İsteğe bağlı: roleName ile bulma
    Role findByRoleName(String roleName);
}
