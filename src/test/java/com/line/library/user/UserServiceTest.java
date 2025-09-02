package com.line.library.user;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class UserServiceTest {

    @Mock
    private UserRepository userRepository;

    @Mock
    private LibrarianVerificationClient verificationClient;

    @Mock
    private org.springframework.security.crypto.password.PasswordEncoder passwordEncoder;

    @InjectMocks
    private UserService userService;

    @Test
    void registerUser_librarian_verificationPasses_savesUser() {
        // Arrange
        User user = new User();
        user.setName("Lib User");
        user.setEmail("lib@example.com");
        user.setPasswordHash("plain");
        user.setRole(UserRole.LIBRARIAN);

        when(verificationClient.verifyEmail("lib@example.com")).thenReturn(true);
        when(passwordEncoder.encode("plain")).thenReturn("ENCODED");
        when(userRepository.save(any(User.class))).thenAnswer(inv -> {
            User u = inv.getArgument(0);
            u.setId(1L);
            return u;
        });

        // Act
        User saved = userService.registerUser(user);

        // Assert
        assertNotNull(saved);
        assertEquals(1L, saved.getId());
        assertEquals("ENCODED", saved.getPasswordHash());
        verify(verificationClient, times(1)).verifyEmail("lib@example.com");
        verify(passwordEncoder, times(1)).encode("plain");
        verify(userRepository, times(1)).save(any(User.class));
    }

    @Test
    void registerUser_librarian_verificationFails_throwsException() {
        // Arrange
        User user = new User();
        user.setName("Lib User");
        user.setEmail("lib@example.com");
        user.setPasswordHash("plain");
        user.setRole(UserRole.LIBRARIAN);

        when(verificationClient.verifyEmail("lib@example.com")).thenReturn(false);

        // Act + Assert
        assertThrows(LibrarianVerificationException.class, () -> userService.registerUser(user));

        // Ensure we did not encode or save when verification fails
        verify(passwordEncoder, never()).encode(any());
        verify(userRepository, never()).save(any());
    }
}

