package com.inventory.service;

import com.inventory.dto.AuthDtos.UserResponse;
import com.inventory.exception.NotFoundException;
import com.inventory.model.User;
import com.inventory.repository.UserRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * Kullanıcı bilgisine erişim. Yöneticiye özel işlemler UserAdminService'tedir.
 */
@Service
public class UserService {

    private final UserRepository userRepository;

    public UserService(UserRepository userRepository) {
        this.userRepository = userRepository;
    }

    @Transactional(readOnly = true)
    public User getById(Long id) {
        return userRepository.findById(id)
                .orElseThrow(() -> new NotFoundException("Kullanıcı bulunamadı"));
    }

    @Transactional(readOnly = true)
    public UserResponse getProfile(Long id) {
        return UserResponse.from(getById(id));
    }
}
