package lab.corekt.backend.modules.user.controller;

import jakarta.validation.Valid;
import lab.corekt.backend.modules.auth.AuthContext;
import lab.corekt.backend.modules.user.dto.ApiKeyRequest;
import lab.corekt.backend.modules.user.dto.UserDetails;
import lab.corekt.backend.modules.user.entity.UserEntity;
import lab.corekt.backend.modules.user.service.UserService;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.UUID;

@RestController
@RequestMapping("/user")
public class UserController {

    private final UserService userService;
    private final AuthContext authContext;

    public UserController(UserService userService, AuthContext authContext) {
        this.userService = userService;
        this.authContext = authContext;
    }

    @GetMapping("/me")
    public ResponseEntity<UserDetails> getAuthenticatedUser() {
        UserEntity user = authContext.getCurrentUser();

        UserDetails userDetails = new UserDetails();
        userDetails.setId(user.getId());
        userDetails.setEmail(user.getEmail());
        userDetails.setUsername(user.getUsername());
        userDetails.setMistralToken(user.getMistralToken());

        return ResponseEntity.ok(userDetails);
    }

    @PostMapping("/me/api-key")
    public ResponseEntity<Void> saveApiKey(@Valid @RequestBody ApiKeyRequest request) throws Exception {
        UserEntity user = authContext.getCurrentUser();
        userService.saveApiKey(user, request);
        return ResponseEntity.noContent().build();
    }

    @DeleteMapping("/{userId}")
    public ResponseEntity<Void> deleteAccount(@PathVariable UUID userId) {
        UserEntity currentUser = authContext.getCurrentUser();
        userService.deleteById(userId, currentUser);
        return ResponseEntity.noContent().build();
    }
}
