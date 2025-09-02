package com.line.library.security;

import org.springframework.security.core.Authentication;
import org.springframework.security.core.GrantedAuthority;

public final class AuthUtils {

    private AuthUtils() { }

    public static boolean hasRole(Authentication auth, String role) {
        if (auth == null || role == null || role.isBlank()) {
            return false;
        }
        String authority = role.startsWith("ROLE_") ? role : "ROLE_" + role;
        for (GrantedAuthority ga : auth.getAuthorities()) {
            if (authority.equals(ga.getAuthority())) {
                return true;
            }
        }
        return false;
    }

    public static boolean isLibrarian(Authentication auth) {
        return hasRole(auth, "LIBRARIAN");
    }
}

