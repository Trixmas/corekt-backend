package lab.corekt.backend.modules.user.service;

import lab.corekt.backend.modules.user.UserEntity;
import lab.corekt.backend.modules.user.UserRepository;
import lab.corekt.backend.utils.Constants;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.Optional;
import java.util.UUID;

@Service
public class UserService {
    private final UserRepository repository;

    public UserService(UserRepository repository) {
        this.repository = repository;
    }

    @Transactional
    public UserEntity createOrUpdateUser(String jumpCloudId, String email) {
        Optional<UserEntity> existingUser = repository.findByUsername(jumpCloudId);

        if (existingUser.isPresent()) {
            UserEntity user = existingUser.get();
            boolean dirty = false;

            if (!user.getEmail().equals(email)) {
                user.setEmail(email);
                dirty = true;
            }

            if (user.getAvatarUrl() == null) {
                user.setAvatarUrl(Constants.DICEBEAR_API_URL + jumpCloudId);
                dirty = true;
            }

            return dirty ? repository.save(user) : user;
        }

        if (repository.existsByEmail(email)) {
            throw new IllegalArgumentException("Email already exists");
        }

        UserEntity newUser = new UserEntity(email, jumpCloudId);
        newUser.setAvatarUrl(Constants.DICEBEAR_API_URL + jumpCloudId);
        return repository.save(newUser);
    }

    @Transactional(readOnly = true)
    public UserEntity getById(UUID id) {
        return repository.findById(id).orElseThrow(() -> new IllegalArgumentException("User not found"));
    }

    @Transactional(readOnly = true)
    public Optional<UserEntity> findByEmail(String email) {
        return repository.findByEmail(email);
    }
}
