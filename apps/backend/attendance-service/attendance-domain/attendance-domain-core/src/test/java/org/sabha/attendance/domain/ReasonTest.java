package org.sabha.attendance.domain;

import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class ReasonTest {

    @Test
    void aBlankReasonIsRejected() {
        assertThatThrownBy(() -> new Reason("   "))
                .isInstanceOf(InvalidReasonException.class)
                .hasMessage("A reason is required.");
    }

    @Test
    void anAbsentReasonIsRejected() {
        assertThatThrownBy(() -> new Reason(null))
                .isInstanceOf(InvalidReasonException.class)
                .hasMessage("A reason is required.");
    }

    @Test
    void surroundingWhitespaceIsStrippedSoTheAuditRowStoresTheTextAlone() {
        assertThat(new Reason("  Forgot to mark Ravi\n").text()).isEqualTo("Forgot to mark Ravi");
    }

    @Test
    void aReasonLongerThanTheCapIsRejected() {
        String tooLong = "x".repeat(501);

        assertThatThrownBy(() -> new Reason(tooLong))
                .isInstanceOf(InvalidReasonException.class)
                .hasMessage("A reason must be at most 500 characters.");
    }

    @Test
    void aReasonExactlyAtTheCapIsAccepted() {
        String atCap = "x".repeat(500);

        assertThat(new Reason(atCap).text()).isEqualTo(atCap);
    }

    @Test
    void theCapIsMeasuredAfterStrippingSoPaddingAloneDoesNotBreachIt() {
        String padded = "  " + "x".repeat(500) + "  ";

        assertThat(new Reason(padded).text()).hasSize(500);
    }
}
