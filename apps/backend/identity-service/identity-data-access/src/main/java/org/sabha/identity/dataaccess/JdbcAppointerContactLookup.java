package org.sabha.identity.dataaccess;

import java.util.List;
import java.util.UUID;

import org.sabha.identity.applicationservice.AppointerContact;
import org.sabha.identity.applicationservice.AppointerContactLookup;
import org.springframework.jdbc.core.simple.JdbcClient;
import org.springframework.stereotype.Repository;

/**
 * Reads the contacts the "who appointed me?" lookup surfaces (ADR-0004): the
 * appointing Karyakar(s) of a User, joined from {@code role_assignments.appointed_by}
 * through {@code users} to their {@code persons} name / mobile; and the Madhyastha
 * Karyalaya members (the {@code MADHYASTHA_KARYALAYA} role) for Sants, who have no
 * appointer.
 */
@Repository
public class JdbcAppointerContactLookup implements AppointerContactLookup {

    private static final String MK_ROLE = "MADHYASTHA_KARYALAYA";

    private final JdbcClient jdbc;

    public JdbcAppointerContactLookup(JdbcClient jdbc) {
        this.jdbc = jdbc;
    }

    @Override
    public List<AppointerContact> appointersOf(UUID targetUserId) {
        return jdbc.sql("""
                SELECT DISTINCT p.full_name AS name, p.mobile AS mobile
                FROM role_assignments ra
                JOIN users u ON u.id = ra.appointed_by
                JOIN persons p ON p.id = u.person_id
                WHERE ra.user_id = ? AND ra.appointed_by IS NOT NULL
                """)
                .param(targetUserId)
                .query((rs, n) -> new AppointerContact(rs.getString("name"), rs.getString("mobile")))
                .list();
    }

    @Override
    public List<AppointerContact> madhyasthaKaryalayaContacts() {
        return jdbc.sql("""
                SELECT DISTINCT p.full_name AS name, p.mobile AS mobile
                FROM role_assignments ra
                JOIN users u ON u.id = ra.user_id
                JOIN persons p ON p.id = u.person_id
                WHERE ra.role = ?
                """)
                .param(MK_ROLE)
                .query((rs, n) -> new AppointerContact(rs.getString("name"), rs.getString("mobile")))
                .list();
    }
}
