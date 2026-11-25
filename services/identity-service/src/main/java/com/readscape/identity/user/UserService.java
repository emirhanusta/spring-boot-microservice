package com.readscape.identity.user;

import com.readscape.common.web.NotFoundException;
import java.util.Locale;
import java.util.UUID;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class UserService {

    private final UserRepository users;
    private final PasswordEncoder encoder;

    public UserService(UserRepository users, PasswordEncoder encoder) {
        this.users = users;
        this.encoder = encoder;
    }

    /** Always creates a plain USER; there is intentionally no way to choose the role here. */
    @Transactional
    public UserView register(String username, String email, String rawPassword) {
        String normalizedEmail = email.trim().toLowerCase(Locale.ROOT);
        if (users.existsByUsername(username) || users.existsByEmail(normalizedEmail)) {
            throw new ConflictException("Username or email is already in use");
        }
        User saved = users.saveAndFlush(new User(username, normalizedEmail, encoder.encode(rawPassword), Role.USER));
        return UserView.from(saved);
    }

    @Transactional(readOnly = true)
    public UserView get(UUID id) {
        return UserView.from(find(id));
    }

    @Transactional(readOnly = true)
    public PageView<UserView> list(int page, int size) {
        return PageView.of(users.findAll(PageRequest.of(page, size, Sort.by("username"))).map(UserView::from));
    }

    @Transactional
    public UserView updateEmail(UUID id, String email) {
        String normalized = email.trim().toLowerCase(Locale.ROOT);
        if (users.existsByEmailAndIdNot(normalized, id)) {
            throw new ConflictException("Username or email is already in use");
        }
        User user = find(id);
        user.setEmail(normalized);
        return UserView.from(users.saveAndFlush(user));
    }

    @Transactional
    public void delete(UUID id) {
        users.delete(find(id));
    }

    private User find(UUID id) {
        return users.findById(id).orElseThrow(() -> NotFoundException.of("User", id));
    }
}
