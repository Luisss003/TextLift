package com.luis.textlift_backend.features.auth.service;

import com.luis.textlift_backend.features.auth.api.dto.LoginUserDto;
import com.luis.textlift_backend.features.auth.api.dto.RegisterUserDto;
import com.luis.textlift_backend.features.auth.domain.User;
import com.luis.textlift_backend.features.auth.repository.UserRepository;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.web.server.ResponseStatusException;

import static org.springframework.http.HttpStatus.CONFLICT;

@Service
public class AuthenticationService {
    private final UserRepository userRepository;

    private final PasswordEncoder passwordEncoder;

    private final AuthenticationManager authenticationManager;

    public AuthenticationService(
            UserRepository userRepository,
            AuthenticationManager authenticationManager,
            PasswordEncoder passwordEncoder
    ) {
        this.authenticationManager = authenticationManager;
        this.userRepository = userRepository;
        this.passwordEncoder = passwordEncoder;
    }

    public User signup(RegisterUserDto input) {
        User user = new User();
        user.setFullName(input.fullName());

        //Check email uniqueness
        if (userRepository.findByEmail(input.email()).isPresent()) {
            throw new ResponseStatusException(CONFLICT, "Email is already in use");
        }

        user.setEmail(input.email());
        user.setPassword(passwordEncoder.encode(input.password()));
        user.setEnabled(true);

        return userRepository.save(user);
    }

    public User authenticate(LoginUserDto input) {

        //given the users creds, authenticate them
        //This triggers DaoAuthenticationProvider, which calls on our UserDetailsService
        //which loads user from DB, and compares their raw password to our Bcrypt hash
        authenticationManager.authenticate(
                new UsernamePasswordAuthenticationToken(
                        input.email(),
                        input.password()
                )
        );

        //Once authenticated, return the user obj via their email
        return userRepository.findByEmail(input.email())
                .orElseThrow();
    }
}
