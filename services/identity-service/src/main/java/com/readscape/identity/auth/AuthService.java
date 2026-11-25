package com.readscape.identity.auth;

import com.readscape.identity.user.User;
import com.readscape.identity.user.UserRepository;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class AuthService {

    private final UserRepository users;
    private final PasswordEncoder encoder;
    private final TokenService tokens;
    /** Compared against when the username is unknown, so both failure paths cost one bcrypt check. */
    private final String dummyHash;

    public AuthService(UserRepository users, PasswordEncoder encoder, TokenService tokens) {
        this.users = users;
        this.encoder = encoder;
        this.tokens = tokens;
        this.dummyHash = encoder.encode("not-a-real-password");
    }

    @Transactional(readOnly = true)
    public TokenResponse login(LoginRequest request) {
        User user = users.findByUsername(request.username()).orElse(null);
        boolean ok = encoder.matches(request.password(), user != null ? user.getPasswordHash() : dummyHash);
        if (user == null || !ok) {
            throw new InvalidCredentialsException();
        }
        return tokens.issue(user);
    }
}
