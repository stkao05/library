package com.line.library.user;

import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;

@Service
public class UserService {

    private final UserRepository userRepository;
    private final PasswordEncoder passwordEncoder;
    private final LibrarianVerificationClient librarianVerificationClient;

    public UserService(UserRepository userRepository,
                       PasswordEncoder passwordEncoder,
                       LibrarianVerificationClient librarianVerificationClient) {
        this.userRepository = userRepository;
        this.passwordEncoder = passwordEncoder;
        this.librarianVerificationClient = librarianVerificationClient;
    }

    public User registerUser(User user) {
        if (user.getRole() == UserRole.LIBRARIAN) {
            boolean ok = librarianVerificationClient.verifyEmail(user.getEmail());
            if (!ok) {
                throw new LibrarianVerificationException("Librarian verification failed");
            }
        }
        user.setPasswordHash(passwordEncoder.encode(user.getPasswordHash()));
        return userRepository.save(user);
    }
}

