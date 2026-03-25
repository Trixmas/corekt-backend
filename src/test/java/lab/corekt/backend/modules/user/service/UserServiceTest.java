package lab.corekt.backend.modules.user.service;

import lab.corekt.backend.modules.user.dto.ApiKeyRequest;
import lab.corekt.backend.modules.user.entity.UserEntity;
import lab.corekt.backend.modules.user.repository.UserRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.Optional;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class UserServiceTest {

    @Mock
    private UserRepository repository;

    @Mock
    private EncryptionService encryptionService;

    @InjectMocks
    private UserService userService;

    private UserEntity existingUser;

    @BeforeEach
    void setUp() {
        existingUser = new UserEntity("user@example.com", "originalUsername");
        existingUser.setId(UUID.randomUUID());
    }

    @Test
    @DisplayName("createOrUpdateUser creates a new user when email is not found")
    void testCreateOrUpdateUserCreatesNewUser() {
        when(repository.findByEmail("new@example.com")).thenReturn(Optional.empty());
        when(repository.save(any(UserEntity.class))).thenAnswer(invocation -> invocation.getArgument(0));

        UserEntity result = userService.createOrUpdateUser("new@example.com", "newUser");

        assertThat(result.getEmail()).isEqualTo("new@example.com");
        assertThat(result.getUsername()).isEqualTo("newUser");
        verify(repository).save(any(UserEntity.class));
    }

    @Test
    @DisplayName("createOrUpdateUser returns existing user without modification on re-login")
    void testCreateOrUpdateUserReturnsExistingUserUnmodified() {
        when(repository.findByEmail("user@example.com")).thenReturn(Optional.of(existingUser));

        UserEntity result = userService.createOrUpdateUser("user@example.com", "ldapUsername");

        assertThat(result.getUsername()).isEqualTo("originalUsername");
        verify(repository, never()).save(any());
    }

    @Test
    @DisplayName("saveApiKey encrypts and persists the Mistral token")
    void testSaveApiKeyEncryptsAndPersistsToken() throws Exception {
        ApiKeyRequest request = new ApiKeyRequest();
        request.setEncryptedApiKey("encApiKey");
        request.setEncryptedDataKey("encDataKey");
        request.setInitializationVector("iv");

        when(encryptionService.decryptAndReEncryptApiKey("encApiKey", "encDataKey", "iv"))
            .thenReturn("stored:encrypted");
        when(repository.save(any(UserEntity.class))).thenAnswer(invocation -> invocation.getArgument(0));

        userService.saveApiKey(existingUser, request);

        assertThat(existingUser.getMistralToken()).isEqualTo("stored:encrypted");
        verify(repository).save(existingUser);
    }

    @Test
    @DisplayName("deleteById deletes the user when ID matches the requesting user")
    void testDeleteByIdDeletesUser() {
        UUID userId = existingUser.getId();

        userService.deleteById(userId, existingUser);

        verify(repository).deleteById(userId);
    }

    @Test
    @DisplayName("deleteById throws when ID does not match the requesting user")
    void testDeleteByIdThrowsWhenIdMismatch() {
        UUID differentUserId = UUID.randomUUID();

        assertThatThrownBy(() -> userService.deleteById(differentUserId, existingUser))
            .isInstanceOf(IllegalArgumentException.class)
            .hasMessage("Cannot delete another user's account");

        verify(repository, never()).deleteById(any());
    }
}
