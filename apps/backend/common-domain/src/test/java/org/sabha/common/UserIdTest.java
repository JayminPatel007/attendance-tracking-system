package org.sabha.common;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import java.util.UUID;

import org.junit.jupiter.api.Test;

class UserIdTest {

    @Test
    void equalsByValue() {
        UUID raw = UUID.randomUUID();
        assertThat(UserId.of(raw)).isEqualTo(new UserId(raw));
        assertThat(UserId.of(raw)).isNotEqualTo(UserId.of(UUID.randomUUID()));
    }

    @Test
    void rejectsNull() {
        assertThatThrownBy(() -> UserId.of(null)).isInstanceOf(NullPointerException.class);
    }

    @Test
    void rendersAsThePlainUuidSoLogsAndAuditRowsReadTheSame() {
        UUID raw = UUID.randomUUID();
        assertThat(UserId.of(raw)).hasToString(raw.toString());
    }
}
