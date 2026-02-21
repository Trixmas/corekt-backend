package lab.corekt.backend.modules.auth.service;

import lab.corekt.backend.modules.auth.dto.LdapUserProfile;
import org.springframework.ldap.core.LdapTemplate;
import org.springframework.ldap.filter.AndFilter;
import org.springframework.ldap.filter.EqualsFilter;
import org.springframework.stereotype.Service;

import javax.naming.NamingException;
import java.util.ArrayList;
import java.util.List;

@Service
public class JumpCloudService {

    private final LdapTemplate ldapTemplate;

    public JumpCloudService(LdapTemplate ldapTemplate) {
        this.ldapTemplate = ldapTemplate;
    }

    public LdapUserProfile authenticateAndGetProfile(String email, String password) {
        AndFilter filter = new AndFilter();
        filter.and(new EqualsFilter("objectClass", "inetOrgPerson"));
        filter.and(new EqualsFilter("mail", email));

        List<LdapUserProfile> results = new ArrayList<>();

        boolean authenticated = ldapTemplate.authenticate("", filter.encode(), password,
            (dirContext, ldapEntryIdentification) -> {
                try {
                    var attributes = dirContext.getAttributes(ldapEntryIdentification.getRelativeName());
                    LdapUserProfile profile = new LdapUserProfile();
                    profile.setEmail((String) attributes.get("mail").get());
                    profile.setJumpCloudUid((String) attributes.get("uid").get());
                    results.add(profile);
                } catch (NamingException e) {
                    throw new RuntimeException("Failed to read LDAP attributes", e);
                }
            });

        if (!authenticated || results.isEmpty()) return null;
        return results.getFirst();
    }
}
