package com.line.library.web;

import java.time.Instant;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

import org.springframework.http.MediaType;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;

import com.line.library.loan.Loan;
import com.line.library.loan.LoanRepository;
import com.line.library.user.User;
import com.line.library.user.UserRepository;

@Controller
public class MyLoanController {

    private final UserRepository userRepository;
    private final LoanRepository loanRepository;

    public MyLoanController(UserRepository userRepository, LoanRepository loanRepository) {
        this.userRepository = userRepository;
        this.loanRepository = loanRepository;
    }

    @GetMapping(value = {"/loans", "/info"}, produces = MediaType.TEXT_HTML_VALUE)
    public String loans(@AuthenticationPrincipal UserDetails principal, Model model) {
        // Resolve domain User from authenticated principal (username is email)
        User user = userRepository.findByEmail(principal.getUsername()).orElseThrow();

        Map<Boolean, List<Loan>> loans = loanRepository.findByUserOrderByReturnedAtDescDueAtAsc(user)
                .stream()
                .collect(Collectors.partitioningBy(l -> l.getReturnedAt() == null));

        List<Loan> currentLoans = loans.get(true);
        List<Loan> pastLoans = loans.get(false);

        boolean hasOverdue = currentLoans.stream()
                .anyMatch(l -> l.getDueAt() != null && l.getDueAt().isBefore(Instant.now()));

        model.addAttribute("user", user);
        model.addAttribute("currentLoans", currentLoans);
        model.addAttribute("pastLoans", pastLoans);
        model.addAttribute("hasOverdue", hasOverdue);

        return "my_loans";
    }
}
