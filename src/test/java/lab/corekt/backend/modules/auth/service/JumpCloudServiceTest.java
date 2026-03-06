package lab.corekt.backend.modules.auth.service;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.ldap.core.AuthenticatedLdapEntryContextCallback;
import org.springframework.ldap.core.LdapTemplate;

import static org.assertj.core.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class JumpCloudServiceTest {

    @Mock
    private LdapTemplate ldapTemplate;

    @InjectMocks
    private JumpCloudService jumpCloudService;

    @Test
    @DisplayName("Should return null when LDAP authentication fails")
    void testReturnsNullWhenLdapAuthenticationFails() {
        when(ldapTemplate.authenticate(anyString(), anyString(), anyString(), (AuthenticatedLdapEntryContextCallback) any()))
            .thenReturn(false);

        assertThat(jumpCloudService.authenticateAndGetProfile("user@test.com", "wrongpassword"))
            .isNull();
    }

    @Test
    @DisplayName("Should return null when user does not exist in LDAP")
    void testReturnsNullWhenUserDoesNotExistInLdap() {
        when(ldapTemplate.authenticate(anyString(), anyString(), anyString(), (AuthenticatedLdapEntryContextCallback) any()))
            .thenReturn(false);

        assertThat(jumpCloudService.authenticateAndGetProfile("ghost@test.com", "password123"))
            .isNull();
    }
}