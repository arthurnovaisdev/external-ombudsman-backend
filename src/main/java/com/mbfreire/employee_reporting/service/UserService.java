package com.mbfreire.employee_reporting.service;

import com.mbfreire.employee_reporting.dto.request.ChangePasswordRequestDTO;
import com.mbfreire.employee_reporting.entity.User;
import com.mbfreire.employee_reporting.exception.BusinessRuleException;
import com.mbfreire.employee_reporting.exception.ResourceNotFoundException;
import com.mbfreire.employee_reporting.repository.UserRepository;
import lombok.RequiredArgsConstructor;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.UUID;

@Service
@RequiredArgsConstructor
public class UserService {

    private final UserRepository userRepository;
    private final PasswordEncoder passwordEncoder;

    @Transactional
    public void changePassword(UUID userId, ChangePasswordRequestDTO dto) {
        User user = findById(userId);

        if (!passwordEncoder.matches(dto.currentPassword(), user.getPasswordHash())) {
            throw new BusinessRuleException("A senha atual está incorreta.");
        }

        if (passwordEncoder.matches(dto.newPassword(), user.getPasswordHash())) {
            throw new BusinessRuleException("A nova senha não pode ser igual à senha atual.");
        }

        user.setPasswordHash(passwordEncoder.encode(dto.newPassword()));
        user.setPasswordChanged(true);
        user.incrementTokenVersion();
        userRepository.save(user);
    }

    @Transactional(readOnly = true)
    public User findById(UUID id) {
        return userRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Usuário não encontrado."));
    }

    @Transactional(readOnly = true)
    public User findByUsername(String username) {

        return userRepository.findByUsername(username)
                .orElseThrow(() ->
                        new ResourceNotFoundException(
                                "Usuário não encontrado."
                        )
                );
    }

    @Transactional(readOnly = true)
    public Page<User> listPaged(Pageable pageable) {
        return userRepository.findAll(pageable);
    }

    @Transactional
    public void setActiveStatus(UUID id, boolean active, UUID loggedInAdminId) {
        if (!active && id.equals(loggedInAdminId)) {
            throw new BusinessRuleException("Você não pode desativar a própria conta.");
        }

        User user = findById(id);

        if (user.isActive() == active){
            return;
        }

        user.setActive(active);
        user.incrementTokenVersion();
        userRepository.save(user);
    }
}
