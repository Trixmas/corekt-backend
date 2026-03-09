package lab.corekt.backend.modules.upload.taiga.controller;

import java.util.Map;

import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import jakarta.validation.Valid;
import lab.corekt.backend.modules.upload.taiga.dto.LoginRequest;
import lab.corekt.backend.modules.upload.taiga.dto.LoginResponse;
import lab.corekt.backend.modules.upload.taiga.dto.ProjectResponse;
import lab.corekt.backend.modules.upload.taiga.dto.TaigaCredentials;
import lab.corekt.backend.modules.upload.taiga.service.TaigaService;
import lab.corekt.backend.modules.upload.taiga.service.TaigaTokenService;

@RestController
@RequestMapping("/taiga")
class TaigaAuthController {

    private final TaigaTokenService taigaTokenService;
    private final TaigaService taigaService;

    TaigaAuthController(TaigaTokenService taigaTokenService, TaigaService taigaService) {
        this.taigaTokenService = taigaTokenService;
        this.taigaService = taigaService;
    }

    @PostMapping("/login")
    public ResponseEntity<LoginResponse> loginToTaiga(@Valid @RequestBody LoginRequest request) {
        try {
            TaigaCredentials credentials = taigaService.authenticate(request.getUsername(), request.getPassword());
            taigaTokenService.storeTaigaToken(credentials.token(), credentials.userId());
            return ResponseEntity.ok(new LoginResponse(true, "Successfully logged in to Taiga"));
        } catch (Exception e) {
            return ResponseEntity.status(500).body(new LoginResponse(false, "An error occurred: " + e.getMessage()));
        }
    }

    @GetMapping("/projects")
    public ResponseEntity<ProjectResponse> getProjects() {
        String taigaToken = taigaTokenService.getTaigaToken();
        String taigaUserId = taigaTokenService.getTaigaUserId();

        if (taigaToken == null || taigaUserId == null) {
            return ResponseEntity.status(401)
                    .body(new ProjectResponse(false, "Not authenticated with Taiga. Please login first."));
        }

        try {
            Map<String, String> projects = taigaService.fetchProjects(taigaToken, taigaUserId);
            return ResponseEntity.ok(new ProjectResponse(projects));
        } catch (Exception e) {
            return ResponseEntity.status(500)
                    .body(new ProjectResponse(false, "Failed to fetch projects from Taiga: " + e.getMessage()));
        }
    }
}
