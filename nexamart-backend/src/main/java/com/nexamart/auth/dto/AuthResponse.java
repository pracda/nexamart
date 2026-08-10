package com.nexamart.auth.dto;

import com.nexamart.auth.Role;

public record AuthResponse(String token, Long userId, String email, String fullName, Role role) {
}
