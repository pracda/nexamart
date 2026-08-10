package com.nexamart.auth;

import io.jsonwebtoken.Claims;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

class JwtServiceTest {

    // 64 hex-like chars — comfortably long enough for any HMAC-SHA algorithm jjwt might select.
    private static final String TEST_SECRET = "0123456789abcdef0123456789abcdef0123456789abcdef0123456789abcd";

    private User buyer() {
        User user = new User("buyer@test.dev", "hash", "Bailey Buyer", Role.BUYER);
        user.setId(42L);
        return user;
    }

    @Test
    void generateToken_thenParseClaims_returnsSubjectAndRole() {
        JwtService jwtService = new JwtService(TEST_SECRET, 60_000);

        String token = jwtService.generateToken(buyer());
        Claims claims = jwtService.parseClaims(token);

        assertThat(claims.getSubject()).isEqualTo("buyer@test.dev");
        assertThat(claims.get("role", String.class)).isEqualTo("BUYER");
        assertThat(jwtService.isValid(token)).isTrue();
    }

    @Test
    void isValid_returnsFalseForExpiredToken() throws InterruptedException {
        JwtService jwtService = new JwtService(TEST_SECRET, 1);

        String token = jwtService.generateToken(buyer());
        Thread.sleep(15);

        assertThat(jwtService.isValid(token)).isFalse();
    }

    @Test
    void isValid_returnsFalseForTamperedToken() {
        JwtService jwtService = new JwtService(TEST_SECRET, 60_000);

        String token = jwtService.generateToken(buyer());
        String tampered = token.substring(0, token.length() - 4) + "abcd";

        assertThat(jwtService.isValid(tampered)).isFalse();
    }

    @Test
    void isValid_returnsFalseForGarbageInput() {
        JwtService jwtService = new JwtService(TEST_SECRET, 60_000);

        assertThat(jwtService.isValid("not-a-real-token")).isFalse();
    }
}
