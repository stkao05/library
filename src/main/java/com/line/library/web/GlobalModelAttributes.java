package com.line.library.web;

import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.ControllerAdvice;
import org.springframework.web.bind.annotation.ModelAttribute;

import com.line.library.security.AuthUtils;

@ControllerAdvice
public class GlobalModelAttributes {

    @ModelAttribute("isLibrarian")
    public boolean isLibrarian(Authentication auth) {
        return AuthUtils.isLibrarian(auth);
    }

    @ModelAttribute("isAuthenticated")
    public boolean isAuthenticated(Authentication auth) {
        if (auth == null || !auth.isAuthenticated()) {
            return false;
        }
        // Exclude anonymous authentication
        return auth.getAuthorities().stream().noneMatch(a -> "ROLE_ANONYMOUS".equals(a.getAuthority()));
    }

    @ModelAttribute("currentUserEmail")
    public String currentUserEmail(Authentication auth) {
        if (auth == null || !auth.isAuthenticated()) {
            return null;
        }
        // Hide when it's an anonymous authentication
        boolean isAnonymous = auth.getAuthorities().stream()
                .anyMatch(a -> "ROLE_ANONYMOUS".equals(a.getAuthority()));
        if (isAnonymous) {
            return null;
        }
        // In this app, username is the email
        return auth.getName();
    }
}
