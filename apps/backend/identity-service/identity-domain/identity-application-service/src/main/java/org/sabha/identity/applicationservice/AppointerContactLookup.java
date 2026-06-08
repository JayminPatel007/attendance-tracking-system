package org.sabha.identity.applicationservice;

import java.util.List;
import java.util.UUID;

/**
 * Read port backing the unauthenticated "who appointed me?" lookup (ADR-0004).
 * Resolves the contact details a locked-out User needs to reach whoever can
 * reissue their password.
 *
 * <p>The JDBC implementation joins {@code role_assignments.appointed_by} to the
 * appointer's {@code users} / {@code persons} rows, and reads the MK members'
 * contacts separately; unit tests drive an in-memory fake.</p>
 */
public interface AppointerContactLookup {

    /** Contacts of the Karyakars recorded as appointing {@code targetUserId}; empty for a Sant. */
    List<AppointerContact> appointersOf(UUID targetUserId);

    /** Contacts of the Madhyastha Karyalaya members — the reissue path for Sants. */
    List<AppointerContact> madhyasthaKaryalayaContacts();
}
