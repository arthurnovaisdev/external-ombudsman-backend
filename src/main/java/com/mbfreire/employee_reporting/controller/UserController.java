package com.mbfreire.employee_reporting.controller;

import com.mbfreire.employee_reporting.dto.request.ChangePasswordRequestDTO;
import com.mbfreire.employee_reporting.dto.response.UserResponseDTO;
import com.mbfreire.employee_reporting.entity.User;
import com.mbfreire.employee_reporting.security.UserDetailsImpl;
import com.mbfreire.employee_reporting.service.UserService;
import jakarta.validation.Valid;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.data.web.PageableDefault;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.*;

import java.util.UUID;

@RestController
@RequestMapping("api/users")
@RequiredArgsConstructor
@Validated
public class UserController {

    private final UserService userService;

    @GetMapping("/me")
    public ResponseEntity<UserResponseDTO> myProfile(@AuthenticationPrincipal UserDetailsImpl principal) {
        return ResponseEntity.ok(toDTO(principal.getUser()));
    }

    @GetMapping
    public ResponseEntity<Page<UserResponseDTO>> list(@RequestParam(defaultValue = "0") @Min(value = 0, message = "A página não pode ser negativa.") int page, @RequestParam(defaultValue = "20") @Min(value = 1, message = "O tamanho da página deve ser no mínimo 1.") @Max(value = 50, message = "O tamanho da página deve ser no máximo 50.") int size) {
        Pageable pageable = PageRequest.of(page, size, Sort.by(Sort.Direction.ASC,"name"));
        return ResponseEntity.ok(userService.listPaged(pageable).map(this::toDTO));
    }

    @GetMapping("/{id}")
    public ResponseEntity<UserResponseDTO> findById(@PathVariable UUID id) {
        return ResponseEntity.ok(toDTO(userService.findById(id)));
    }

    @PatchMapping("/me/password")
    public ResponseEntity<Void> changePassword(
            @AuthenticationPrincipal UserDetailsImpl principal,
            @Valid @RequestBody ChangePasswordRequestDTO dto
            ) {
        userService.changePassword(principal.getUser().getId(), dto);
        return ResponseEntity.noContent().build();
    }

    @PatchMapping("/{id}/deactivate")
    public ResponseEntity<Void> deactivate(@PathVariable UUID id, @AuthenticationPrincipal UserDetailsImpl principal) {
        userService.setActiveStatus(id, false, principal.getUser().getId());
        return ResponseEntity.noContent().build();
    }

    @PatchMapping("/{id}/activate")
    public ResponseEntity<Void> activate(@PathVariable UUID id, @AuthenticationPrincipal UserDetailsImpl principal) {
        userService.setActiveStatus(id, true, principal.getUser().getId());
        return ResponseEntity.noContent().build();
    }

    private UserResponseDTO toDTO(User user) {

        return new UserResponseDTO(
                user.getId(),
                user.getName(),
                user.getUsername(),
                user.getContactEmail(),
                user.getRole().name(),
                user.isActive(),
                user.isPasswordChanged()
        );
    }
}
