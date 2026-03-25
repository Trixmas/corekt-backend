package lab.corekt.backend.modules.taiga.controller;

import jakarta.validation.Valid;
import lab.corekt.backend.modules.taiga.dto.LoginRequest;
import lab.corekt.backend.modules.taiga.dto.ProjectsResponse;
import lab.corekt.backend.modules.taiga.dto.TaigaCredentials;
import lab.corekt.backend.modules.taiga.service.TaigaService;
import lab.corekt.backend.modules.taiga.service.TaigaTokenService;
import lombok.AllArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/taiga")
@AllArgsConstructor
class TaigaAuthController {

    private final TaigaTokenService taigaTokenService;
    private final TaigaService taigaService;

    @PostMapping("/login")
    public ResponseEntity<Void> login(@Valid @RequestBody LoginRequest request) {
        TaigaCredentials credentials = taigaService.authenticate(request.getUsername(), request.getPassword());
        taigaTokenService.store(credentials.token(), credentials.userId());
        return ResponseEntity.noContent().build();
    }

    @GetMapping("/projects")
    public ResponseEntity<ProjectsResponse> getProjects() {
        return ResponseEntity.ok(new ProjectsResponse(
            taigaService.fetchProjects(
                taigaTokenService.getToken(),
                taigaTokenService.getUserId()
            )));
    }
}
