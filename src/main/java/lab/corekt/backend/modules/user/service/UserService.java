package lab.corekt.backend.modules.user.service;

import lab.corekt.backend.modules.user.UserEntity;
import lab.corekt.backend.modules.user.UserRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.Optional;

@Service
public class UserService {
    private final UserRepository repository;

    public UserService(UserRepository repository) {
        this.repository = repository;
    }

    @Transactional
    public UserEntity createOrUpdateUser(String email, String username) {
        Optional<UserEntity> existingUser = repository.findByEmail(email);

        if (existingUser.isPresent()) {
            UserEntity user = existingUser.get();
            boolean dirty = false;

            if (!user.getEmail().equals(email)) {
                user.setEmail(email);
                user.setUsername(username);
                dirty = true;
            }

            return dirty ? repository.save(user) : user;
        }

        if (repository.existsByEmail(email)) {
            throw new IllegalArgumentException("Email already exists");
        }

        UserEntity newUser = new UserEntity(email, username);
        return repository.save(newUser);
    }

    @Transactional(readOnly = true)
    public Optional<UserEntity> findByEmail(String email) {
        return repository.findByEmail(email);
    }
}
