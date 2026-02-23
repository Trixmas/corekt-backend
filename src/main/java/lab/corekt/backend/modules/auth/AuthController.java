package lab.corekt.backend.modules.auth;

import jakarta.validation.Valid;
import lab.corekt.backend.modules.auth.dto.LdapUserProfile;
import lab.corekt.backend.modules.auth.dto.LoginRequest;
import lab.corekt.backend.modules.auth.dto.LoginResponse;
import lab.corekt.backend.modules.auth.service.JumpCloudService;
import lab.corekt.backend.modules.auth.service.JwtService;
import lab.corekt.backend.modules.user.UserEntity;
import lab.corekt.backend.modules.user.dto.UserDetails;
import lab.corekt.backend.modules.user.service.UserService;
import org.jspecify.annotations.NonNull;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.HashMap;
import java.util.Map;

@RestController
@RequestMapping("/auth")
@Validated
public class AuthController {

    private final JumpCloudService jumpCloudService;
    private final JwtService jwtService;
    private final UserService userService;

    public AuthController(
            JumpCloudService jumpCloudService,
            JwtService jwtService,
            UserService userService) {
        this.jumpCloudService = jumpCloudService;
        this.jwtService = jwtService;
        this.userService = userService;
    }

    private static @NonNull LoginResponse createLoginResponse(String token, Long expiresAt, UserDetails userDetails) {
        LoginResponse response = new LoginResponse();
        response.setToken(token);
        response.setExpiresAt(expiresAt);
        response.setUser(userDetails);
        return response;
    }

    @PostMapping("/login")
    public ResponseEntity<LoginResponse> login(@Valid @RequestBody LoginRequest request) {
        LdapUserProfile ldapProfile = jumpCloudService.authenticateAndGetProfile(request.getEmail(), request.getPassword());

        if (ldapProfile == null) {
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED).build();
        }

        UserEntity user = userService.createOrUpdateUser(ldapProfile.getEmail(), ldapProfile.getUsername());

        Map<String, Object> claims = new HashMap<>();
        claims.put("email", user.getEmail());

        long issuedAt = System.currentTimeMillis();
        String token = jwtService.generateToken(user.getEmail(), claims, issuedAt);
        Long expiresAt = jwtService.computeExpirationTime(issuedAt);

        UserDetails userDetails = new UserDetails();
        userDetails.setUsername(user.getUsername());
        userDetails.setEmail(user.getEmail());

        LoginResponse response = createLoginResponse(token, expiresAt, userDetails);

        return ResponseEntity.ok(response);
    }
}
