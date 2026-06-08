package org.sabha.identity.application;

import java.util.List;

import org.sabha.identity.applicationservice.AppointerContact;
import org.sabha.identity.applicationservice.WhoAppointedMeService;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

/**
 * Unauthenticated "who appointed me?" lookup (ADR-0004): a User who has lost
 * their mobile and so cannot self-serve a reset finds, keyed only on their
 * username from the login screen, the contact details of whoever can reissue
 * their password. Sits on the public API chain (see {@code SecurityConfig}).
 * Unknown username surfaces as a 404.
 */
@RestController
public class WhoAppointedMeRestController {

    private final WhoAppointedMeService lookup;

    public WhoAppointedMeRestController(WhoAppointedMeService lookup) {
        this.lookup = lookup;
    }

    @GetMapping("/api/who-appointed-me")
    public WhoAppointedMeResponse whoAppointedMe(@RequestParam String username) {
        return new WhoAppointedMeResponse(lookup.lookup(username));
    }

    public record WhoAppointedMeResponse(List<AppointerContact> contacts) {
    }
}
