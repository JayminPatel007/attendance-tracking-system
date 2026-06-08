package org.sabha.identity.applicationservice;

import java.util.List;

import org.sabha.identity.domain.User;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * Unauthenticated "who appointed me?" lookup (ADR-0004): a locked-out User who
 * has lost their mobile finds, keyed only on their username, the contact details
 * of whoever can reissue their password. A User with an appointer gets their
 * appointer(s); a Sant — who has no appointer per ADR-0011 — gets the Madhyastha
 * Karyalaya members instead (the no-appointer fallback).
 */
@Service
public class WhoAppointedMeService {

    private final UserRepository users;
    private final AppointerContactLookup contacts;

    public WhoAppointedMeService(UserRepository users, AppointerContactLookup contacts) {
        this.users = users;
        this.contacts = contacts;
    }

    @Transactional(readOnly = true)
    public List<AppointerContact> lookup(String username) {
        User user = users.findByUsername(username)
                .orElseThrow(() -> new UnknownUsernameException(username));

        List<AppointerContact> appointers = contacts.appointersOf(user.id());
        return appointers.isEmpty() ? contacts.madhyasthaKaryalayaContacts() : appointers;
    }
}
