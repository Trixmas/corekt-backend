package lab.corekt.backend.modules.achievement.controller;

import lab.corekt.backend.modules.achievement.criteria.dto.AchievementResponse;
import lab.corekt.backend.modules.achievement.criteria.dto.UserAchievementResponse;
import lab.corekt.backend.modules.achievement.service.AchievementManager;
import lab.corekt.backend.modules.auth.service.JwtService;
import lab.corekt.backend.utils.BaseControllerTest;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.webmvc.test.autoconfigure.WebMvcTest;
import org.springframework.http.MediaType;
import org.springframework.security.test.context.support.WithMockUser;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

import java.time.Instant;
import java.util.List;

import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@WebMvcTest(AchievementController.class)
class AchievementControllerTest extends BaseControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @MockitoBean
    private JwtService jwtService;

    @MockitoBean
    private AchievementManager achievementManager;

    @Test
    @WithMockUser
    @DisplayName("Should return 200 with empty list when no achievements exist")
    void testGetAllAchievementsReturnsEmptyList() throws Exception {
        when(achievementManager.getAllAchievements()).thenReturn(List.of());

        mockMvc.perform(get("/achievements").contentType(MediaType.APPLICATION_JSON))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$").isArray())
            .andExpect(jsonPath("$").isEmpty());
    }

    @Test
    @WithMockUser
    @DisplayName("Should return 200 with achievement list")
    void testGetAllAchievementsReturnsList() throws Exception {
        when(achievementManager.getAllAchievements()).thenReturn(List.of(
            new AchievementResponse("first_analysis", "First Steps", "Complete your first analysis", false),
            new AchievementResponse("score_over_80", "Sharp Eye", "Reach a score of 80 or above", false)
        ));

        mockMvc.perform(get("/achievements").contentType(MediaType.APPLICATION_JSON))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$").isArray())
            .andExpect(jsonPath("$.length()").value(2))
            .andExpect(jsonPath("$[0].id").value("first_analysis"))
            .andExpect(jsonPath("$[0].name").value("First Steps"))
            .andExpect(jsonPath("$[1].id").value("score_over_80"));
    }

    @Test
    @WithMockUser
    @DisplayName("Should not expose hidden field as true in response")
    void testGetAllAchievementsSerializesHiddenField() throws Exception {
        when(achievementManager.getAllAchievements()).thenReturn(List.of(
            new AchievementResponse("secret", "Secret", "Hidden achievement", true)
        ));

        mockMvc.perform(get("/achievements").contentType(MediaType.APPLICATION_JSON))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$[0].hidden").value(true));
    }

    @Test
    @WithMockUser
    @DisplayName("Should return 200 with empty list when user has no achievements")
    void testGetUserAchievementsReturnsEmptyList() throws Exception {
        when(achievementManager.getUserAchievements()).thenReturn(List.of());

        mockMvc.perform(get("/achievements/me").contentType(MediaType.APPLICATION_JSON))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$").isArray())
            .andExpect(jsonPath("$").isEmpty());
    }

    @Test
    @WithMockUser
    @DisplayName("Should return 200 with user achievement list including acquired date")
    void testGetUserAchievementsReturnsList() throws Exception {
        Instant acquiredAt = Instant.parse("2026-01-15T10:00:00Z");

        when(achievementManager.getUserAchievements()).thenReturn(List.of(
            new UserAchievementResponse("first_analysis", "First Steps", "Complete your first analysis", acquiredAt)
        ));

        mockMvc.perform(get("/achievements/me").contentType(MediaType.APPLICATION_JSON))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$").isArray())
            .andExpect(jsonPath("$.length()").value(1))
            .andExpect(jsonPath("$[0].id").value("first_analysis"))
            .andExpect(jsonPath("$[0].acquiredAt").exists());
    }
}
