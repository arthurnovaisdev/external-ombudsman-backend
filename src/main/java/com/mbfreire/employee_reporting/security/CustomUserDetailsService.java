package com.mbfreire.employee_reporting.security;

import com.mbfreire.employee_reporting.entity.User;
import com.mbfreire.employee_reporting.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.stereotype.Service;

import java.util.Locale;

@Service
@RequiredArgsConstructor
public class CustomUserDetailsService implements UserDetailsService {

    private final UserRepository userRepository;

    @Override
    public UserDetails loadUserByUsername(String username)
            throws UsernameNotFoundException {

        String normalizedUsername = normalizeUsername(username);

        User user = userRepository.findByUsername(normalizedUsername)
                .orElseThrow(() ->
                        new UsernameNotFoundException(
                                "Usuário não encontrado."
                        )
                );

        return new UserDetailsImpl(user);
    }

    private String normalizeUsername(String username) {
        if (username == null) {
            return "";
        }

        return username
                .trim()
                .toLowerCase(Locale.ROOT);
    }
}