package com.line.library.web;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.security.core.Authentication;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

import com.line.library.loan.LoanService;

@Controller
public class LoanController {

    private final LoanService loanService;
    private static final Logger log = LoggerFactory.getLogger(LoanController.class);

    public LoanController(LoanService loanService) {
        this.loanService = loanService;
    }

    @PostMapping("/books/{bookId}/copies/{copyId}/loan")
    public String loanCopy(@PathVariable Long bookId,
                             @PathVariable Long copyId,
                             Authentication auth,
                             RedirectAttributes redirectAttributes) {
        try {
            String email = auth.getName();
            loanService.loanCopy(copyId, email);
            redirectAttributes.addAttribute("loanSuccess", "true");
        } catch (IllegalStateException ex) {
            redirectAttributes.addAttribute("loanError", ex.getMessage());
        } catch (Exception ex) {
            log.error("Unexpected error while loaning copy {} for book {}", copyId, bookId, ex);
            redirectAttributes.addAttribute("loanError", "Unable to loan this copy");
        }
        return "redirect:/books/" + bookId;
    }

    @PostMapping("/loans/{loanId}/return")
    public String returnLoan(@PathVariable Long loanId,
                             Authentication auth,
                             RedirectAttributes redirectAttributes) {
        try {
            String email = auth.getName();
            loanService.returnLoan(loanId, email);
            redirectAttributes.addAttribute("returnSuccess", "true");
        } catch (IllegalStateException ex) {
            redirectAttributes.addAttribute("returnError", ex.getMessage());
        } catch (Exception ex) {
            log.error("Unexpected error while returning loan {}", loanId, ex);
            redirectAttributes.addAttribute("returnError", "Unable to return this loan");
        }
        return "redirect:/loans";
    }
}
