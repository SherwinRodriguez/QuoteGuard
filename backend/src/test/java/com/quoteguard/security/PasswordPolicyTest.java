package com.quoteguard.security;

import static org.assertj.core.api.Assertions.assertThatCode;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import org.junit.jupiter.api.Test;

class PasswordPolicyTest {

    @Test
    void rejectsPasswordShorterThanEightCharacters() {
        assertThatThrownBy(() -> PasswordPolicy.validate("ab1"))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("at least 8 characters");
    }

    @Test
    void rejectsPasswordWithNoDigit() {
        assertThatThrownBy(() -> PasswordPolicy.validate("onlyletters"))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("at least one letter and one digit");
    }

    @Test
    void rejectsPasswordWithNoLetter() {
        assertThatThrownBy(() -> PasswordPolicy.validate("12345678"))
                .isInstanceOf(IllegalArgumentException.class);
    }

    @Test
    void rejectsNullPassword() {
        assertThatThrownBy(() -> PasswordPolicy.validate(null))
                .isInstanceOf(IllegalArgumentException.class);
    }

    @Test
    void acceptsAReasonablePassword() {
        assertThatCode(() -> PasswordPolicy.validate("correcthorse1")).doesNotThrowAnyException();
    }
}
