package com.line.library.user;

import org.springframework.context.annotation.Profile;
import org.springframework.stereotype.Service;

@Service
@Profile("!prod")
public class MockLibrarianVerificationClient implements LibrarianVerificationClient {
    @Override
    public boolean verifyEmail(String email) {
        // Allow all in non-prod for easy local dev and tests
        return true;
    }
}

