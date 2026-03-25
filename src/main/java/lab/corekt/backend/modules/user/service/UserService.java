package lab.corekt.backend.modules.user.service;

import lab.corekt.backend.modules.user.dto.ApiKeyRequest;
import lab.corekt.backend.modules.user.entity.UserEntity;
import lab.corekt.backend.modules.user.repository.UserRepository;
import lombok.AllArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.Optional;
import java.util.UUID;

@Service
@AllArgsConstructor
public class UserService {
    private final UserRepository repository;
    private final EncryptionService encryptionService;

    @Transactional
    public UserEntity createOrUpdateUser(String email, String username) {
        Optional<UserEntity> existingUser = repository.findByEmail(email);

        if (existingUser.isPresent()) {
            return existingUser.get();
        }

        UserEntity newUser = new UserEntity(email, username);
        return repository.save(newUser);
    }

    @Transactional(readOnly = true)
    public Optional<UserEntity> findByEmail(String email) {
        return repository.findByEmail(email);
    }

    @Transactional
    public void saveApiKey(UserEntity user, ApiKeyRequest request) throws Exception {
        String encryptedToken = encryptionService.decryptAndReEncryptApiKey(
            request.getEncryptedApiKey(),
            request.getEncryptedDataKey(),
            request.getInitializationVector()
        );
        user.setMistralToken(encryptedToken);
        repository.save(user);
    }

    @Transactional
    public void deleteById(UUID userId, UserEntity requestingUser) {
        if (!requestingUser.getId().equals(userId)) {
            throw new IllegalArgumentException("Cannot delete another user's account");
        }
        repository.deleteById(userId);
    }
}
