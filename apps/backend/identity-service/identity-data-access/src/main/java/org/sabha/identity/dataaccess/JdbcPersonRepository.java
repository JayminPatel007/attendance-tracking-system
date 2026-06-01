package org.sabha.identity.dataaccess;

import org.sabha.identity.applicationservice.PersonRepository;
import org.sabha.identity.domain.Person;
import org.springframework.jdbc.core.simple.JdbcClient;
import org.springframework.stereotype.Repository;

/**
 * JDBC adapter for persisting a bare {@link Person} into the {@code persons}
 * Directory table. Unlike {@code PersonDirectory.add} this does not register a
 * Home Sabha — used for People who are not roster members (e.g. the bootstrap
 * Madhyastha Karyalaya member, a State-level oversight role).
 */
@Repository
public class JdbcPersonRepository implements PersonRepository {

    private final JdbcClient jdbc;

    public JdbcPersonRepository(JdbcClient jdbc) {
        this.jdbc = jdbc;
    }

    @Override
    public void save(Person person) {
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
    }
}
