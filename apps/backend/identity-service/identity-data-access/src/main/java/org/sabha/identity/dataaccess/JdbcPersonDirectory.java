package org.sabha.identity.dataaccess;

import java.sql.ResultSet;
import java.sql.SQLException;
import java.time.LocalDate;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

import org.sabha.identity.applicationservice.HomeSabhaDirectory;
import org.sabha.identity.applicationservice.NameCandidate;
import org.sabha.identity.applicationservice.PersonContactLookup;
import org.sabha.identity.applicationservice.PersonDirectory;
import org.sabha.identity.applicationservice.WalkInCandidate;
import org.sabha.identity.domain.Gender;
import org.sabha.identity.domain.HomeSabhaRef;
import org.sabha.identity.domain.Person;
import org.springframework.jdbc.core.simple.JdbcClient;
import org.springframework.stereotype.Repository;

/**
 * JDBC adapter over the {@code persons} Directory (ADR-0013). The hard-block
 * lookup is an equality on the unique {@code mobile} column; the soft-warn query
 * is scoped to the Home Sabha's Kshetra and matches names phonetically
 * ({@code dmetaphone}) or by edit-distance ({@code levenshtein}), backed by the
 * indexes from {@code slice-6/001-person-directory.sql}.
 */
@Repository
public class JdbcPersonDirectory implements PersonDirectory, HomeSabhaDirectory, PersonContactLookup {

    private static final int MAX_EDIT_DISTANCE = 2;

    private final JdbcClient jdbc;

    public JdbcPersonDirectory(JdbcClient jdbc) {
        this.jdbc = jdbc;
    }

    @Override
    public Optional<String> mobileOf(UUID personId) {
        return jdbc.sql("SELECT mobile FROM persons WHERE id = ?")
                .param(personId)
                .query((rs, n) -> rs.getString("mobile"))
                .optional()
                .filter(m -> m != null && !m.isBlank());
    }

    @Override
    public Optional<Person> findByMobile(String mobile) {
        return jdbc.sql("""
                SELECT id, full_name, gender, date_of_birth, mobile, guardian_person_id
                FROM persons
                WHERE mobile = ?
                """)
                .param(mobile)
                .query(JdbcPersonDirectory::toPerson)
                .optional();
    }

    @Override
    public Optional<Person> findById(UUID personId) {
        return jdbc.sql("""
                SELECT id, full_name, gender, date_of_birth, mobile, guardian_person_id
                FROM persons
                WHERE id = ?
                """)
                .param(personId)
                .query(JdbcPersonDirectory::toPerson)
                .optional();
    }

    @Override
    public List<NameCandidate> findNameCandidates(UUID kshetraId, String fullName, int limit) {
        return jdbc.sql("""
                SELECT p.id,
                       p.full_name,
                       array_agg(s.sabha_kind ORDER BY s.sabha_kind) AS home_sabhas,
                       MIN(levenshtein(lower(p.full_name), lower(?))) AS dist
                FROM persons p
                JOIN home_sabhas hs ON hs.person_id = p.id
                JOIN sabhas s ON s.id = hs.sabha_id
                WHERE s.kshetra_id = ?
                  AND (dmetaphone(p.full_name) = dmetaphone(?)
                       OR levenshtein(lower(p.full_name), lower(?)) <= ?)
                GROUP BY p.id, p.full_name
                ORDER BY dist ASC
                LIMIT ?
                """)
                .param(fullName)
                .param(kshetraId)
                .param(fullName)
                .param(fullName)
                .param(MAX_EDIT_DISTANCE)
                .param(limit)
                .query((rs, n) -> new NameCandidate(
                        rs.getObject("id", UUID.class),
                        rs.getString("full_name"),
                        homeSabhaKinds(rs)))
                .list();
    }

    @Override
    public Optional<WalkInCandidate> findByMobileForWalkIn(String mobile) {
        return jdbc.sql("""
                SELECT p.id,
                       p.full_name,
                       array_agg(s.sabha_kind ORDER BY s.sabha_kind) AS home_sabhas
                FROM persons p
                JOIN home_sabhas hs ON hs.person_id = p.id
                JOIN sabhas s ON s.id = hs.sabha_id
                WHERE p.mobile = ?
                GROUP BY p.id, p.full_name
                """)
                .param(mobile)
                .query((rs, n) -> new WalkInCandidate(
                        rs.getObject("id", UUID.class),
                        rs.getString("full_name"),
                        homeSabhaKinds(rs)))
                .optional();
    }

    @Override
    public Optional<UUID> kshetraIdOfSabha(UUID sabhaId) {
        return jdbc.sql("SELECT kshetra_id FROM sabhas WHERE id = ?")
                .param(sabhaId)
                .query((rs, n) -> rs.getObject("kshetra_id", UUID.class))
                .optional();
    }

    @Override
    public List<HomeSabhaRef> homeSabhasOf(UUID personId) {
        return jdbc.sql("""
                SELECT hs.sabha_id, s.sabha_kind
                FROM home_sabhas hs
                JOIN sabhas s ON s.id = hs.sabha_id
                WHERE hs.person_id = ?
                """)
                .param(personId)
                .query((rs, n) -> new HomeSabhaRef(
                        rs.getObject("sabha_id", UUID.class),
                        rs.getString("sabha_kind")))
                .list();
    }

    @Override
    public Optional<String> kindOf(UUID sabhaId) {
        return jdbc.sql("SELECT sabha_kind FROM sabhas WHERE id = ?")
                .param(sabhaId)
                .query((rs, n) -> rs.getString("sabha_kind"))
                .optional();
    }

    @Override
    public void replaceHomeSabha(UUID personId, UUID previousSabhaId, UUID destinationSabhaId) {
        jdbc.sql("DELETE FROM home_sabhas WHERE person_id = ? AND sabha_id = ?")
                .param(personId)
                .param(previousSabhaId)
                .update();
        jdbc.sql("INSERT INTO home_sabhas (person_id, sabha_id) VALUES (?, ?)")
                .param(personId)
                .param(destinationSabhaId)
                .update();
    }

    @Override
    public void add(Person person, UUID homeSabhaId) {
        jdbc.sql("""
                INSERT INTO persons (id, full_name, gender, date_of_birth, mobile, guardian_person_id)
                VALUES (?, ?, ?, ?, ?, ?)
                """)
                .param(person.id())
                .param(person.fullName())
                .param(person.gender().name())
                .param(person.dateOfBirth())
                .param(person.mobile())
                .param(person.guardianPersonId())
                .update();

        jdbc.sql("INSERT INTO home_sabhas (person_id, sabha_id) VALUES (?, ?)")
                .param(person.id())
                .param(homeSabhaId)
                .update();
    }

    private static Person toPerson(java.sql.ResultSet rs, int rowNum) throws java.sql.SQLException {
        return new Person(
                rs.getObject("id", UUID.class),
                rs.getString("full_name"),
                Gender.valueOf(rs.getString("gender")),
                rs.getObject("date_of_birth", LocalDate.class),
                rs.getString("mobile"),
                rs.getObject("guardian_person_id", UUID.class));
    }

    /** Reads a Postgres {@code text[]} of {@code sabha_kind}s into an immutable list. */
    private static List<String> homeSabhaKinds(ResultSet rs) throws SQLException {
        java.sql.Array array = rs.getArray("home_sabhas");
        if (array == null) {
            return List.of();
        }
        return List.of((String[]) array.getArray());
    }
}
