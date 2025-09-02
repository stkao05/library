package com.line.library.web;

import com.line.library.user.LibrarianVerificationException;
import com.line.library.user.User;
import com.line.library.user.UserService;
import com.line.library.user.CustomUserDetailsService;

import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.context.SecurityContext;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.web.context.HttpSessionSecurityContextRepository;

import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.validation.BindingResult;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.ModelAttribute;
import org.springframework.web.bind.annotation.PostMapping;

import jakarta.validation.Valid;

@Controller
public class AuthController {

    private final UserService userService;
    private final CustomUserDetailsService userDetailsService;

    public AuthController(UserService userService, CustomUserDetailsService userDetailsService) {
        this.userService = userService;
        this.userDetailsService = userDetailsService;
    }

    @GetMapping("/signup")
    public String signupForm(Model model) {
        model.addAttribute("user", new User());
        return "signup";
    }

    @PostMapping("/signup")
    public String signupSubmit(@Valid @ModelAttribute("user") User user, BindingResult result, jakarta.servlet.http.HttpServletRequest request) {
        if (result.hasErrors()) {
            return "signup";
        }
        try {
            userService.registerUser(user);
        } catch (LibrarianVerificationException ex) {
            result.rejectValue("role", "librarian.verification.failed", "Librarian verification failed. Contact library admin.");
            return "signup";
        }
        // Auto-login the newly registered user and redirect to /books
        UserDetails userDetails = userDetailsService.loadUserByUsername(user.getEmail());
        UsernamePasswordAuthenticationToken authentication =
                new UsernamePasswordAuthenticationToken(userDetails, null, userDetails.getAuthorities());

        SecurityContext context = SecurityContextHolder.createEmptyContext();
        context.setAuthentication(authentication);
        SecurityContextHolder.setContext(context);

        // Persist authentication in HTTP session
        request.getSession(true).setAttribute(
                HttpSessionSecurityContextRepository.SPRING_SECURITY_CONTEXT_KEY,
                context
        );

        return "redirect:/books";
    }

    @GetMapping("/login")
    public String login() {
        return "login";
    }
}
