package com.readscape.identity.user;

import com.readscape.security.CurrentUser;
import jakarta.validation.Valid;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import java.util.UUID;
import org.springframework.http.HttpStatus;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/v1/users")
public class UserController {

    private final UserService users;

    public UserController(UserService users) {
        this.users = users;
    }

    @GetMapping("/me")
    public UserView me() {
        return users.get(CurrentUser.userId());
    }

    @PutMapping("/me")
    public UserView updateMe(@Valid @RequestBody UpdateProfileRequest request) {
        return users.updateEmail(CurrentUser.userId(), request.email());
    }

    @GetMapping
    @PreAuthorize("hasRole('ADMIN')")
    public PageView<UserView> list(
            @RequestParam(defaultValue = "0") @Min(0) int page,
            @RequestParam(defaultValue = "20") @Min(1) @Max(100) int size) {
        return users.list(page, size);
    }

    @GetMapping("/{id}")
    @PreAuthorize("hasRole('ADMIN') or #id.toString() == authentication.name")
    public UserView get(@PathVariable UUID id) {
        return users.get(id);
    }

    @DeleteMapping("/{id}")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    @PreAuthorize("hasRole('ADMIN') or #id.toString() == authentication.name")
    public void delete(@PathVariable UUID id) {
        users.delete(id);
    }
}
