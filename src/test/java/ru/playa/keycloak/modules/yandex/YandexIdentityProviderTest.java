package ru.playa.keycloak.modules.yandex;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ObjectNode;
import org.junit.Before;
import org.junit.Test;
import org.keycloak.broker.provider.BrokeredIdentityContext;
import org.keycloak.models.KeycloakSession;
import org.keycloak.models.IdentityProviderSyncMode;
import org.keycloak.models.RealmModel;
import org.keycloak.models.UserModel;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNull;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoMoreInteractions;

/**
 * Checks Yandex portrait import and synchronization for an existing user.
 */
public class YandexIdentityProviderTest {

    /** Provider under test. */
    private YandexIdentityProvider provider;

    /** Keycloak session. */
    private KeycloakSession session;

    /** Valid Yandex profile fixture. */
    private ObjectNode profile;

    /** Provider configuration. */
    private YandexIdentityProviderConfig config;

    /** Initializes a valid profile without a portrait. */
    @Before
    public void setUp() {
        session = mock(KeycloakSession.class);
        config = new YandexIdentityProviderConfig();
        config.setAlias("yandex");
        config.setEnabled(true);
        provider = new YandexIdentityProvider(session, config);
        profile = new ObjectMapper().createObjectNode();
        profile.put("id", "fixture-id");
        profile.put("login", "fixture-login");
        profile.put("default_email", "fixture@example.org");
        profile.put("first_name", "First");
        profile.put("last_name", "Last");
    }

    /** Imports a Yandex portrait while retaining the identity fields. */
    @Test
    public void importsPortraitAndPreservesIdentity() {
        profile.put("default_avatar_id", "12345/fixture-avatar_1");
        BrokeredIdentityContext identity = provider.extractIdentityFromProfile(null, profile);
        assertEquals("https://avatars.yandex.net/get-yapic/12345/fixture-avatar_1/islands-200",
            identity.getUserAttribute("picture"));
        assertEquals("fixture-id", identity.getId());
        assertEquals("fixture@example.org", identity.getUsername());
        assertEquals("fixture@example.org", identity.getEmail());
        assertEquals("First", identity.getFirstName());
        assertEquals("Last", identity.getLastName());
    }

    /** Refreshes the portrait on an existing account after another broker login. */
    @Test
    public void refreshesExistingUserPortrait() {
        profile.put("default_avatar_id", "12345/new-avatar");
        UserModel user = mock(UserModel.class);
        provider.updateBrokeredUser(session, mock(RealmModel.class), user,
            provider.extractIdentityFromProfile(null, profile));
        verify(user).setSingleAttribute("picture", "https://avatars.yandex.net/get-yapic/12345/new-avatar/islands-200");
        verifyNoMoreInteractions(user);
    }

    /** Removes an old portrait if Yandex no longer supplies it. */
    @Test
    public void clearsMissingPortrait() {
        UserModel user = mock(UserModel.class);
        BrokeredIdentityContext identity = provider.extractIdentityFromProfile(null, profile);
        assertNull(identity.getUserAttribute("picture"));
        provider.updateBrokeredUser(session, mock(RealmModel.class), user, identity);
        verify(user).removeAttribute("picture");
        verifyNoMoreInteractions(user);
    }

    /** Rejects URL injection, traversal, empty values and malformed path segments. */
    @Test
    public void rejectsInvalidPortraitIds() {
        String[] invalidIds = {"", "../portrait", "//evil.example", "123/avatar?redirect=evil", "123//avatar", "a b"};
        for (String avatarId : invalidIds) {
            profile.put("default_avatar_id", avatarId);
            BrokeredIdentityContext identity = provider.extractIdentityFromProfile(null, profile);
            assertNull(identity.getUserAttribute("picture"));
            UserModel user = mock(UserModel.class);
            provider.updateBrokeredUser(session, mock(RealmModel.class), user, identity);
            verify(user).removeAttribute("picture");
            verifyNoMoreInteractions(user);
        }
    }

    /** Uses email as username and overwrites optional profile values in FORCE mode. */
    @Test
    public void synchronizesCompleteProfile() {
        config.setSyncMode(IdentityProviderSyncMode.FORCE);
        config.setTrustEmail(true);
        profile.put("birthday", "1990-01-02");
        profile.put("sex", "male");
        profile.putObject("default_phone").put("number", "+70000000000");
        UserModel user = mock(UserModel.class);
        provider.updateBrokeredUser(session, mock(RealmModel.class), user,
            provider.extractIdentityFromProfile(null, profile));
        verify(user).setEmail("fixture@example.org");
        verify(user).setUsername("fixture@example.org");
        verify(user).setFirstName("First");
        verify(user).setLastName("Last");
        verify(user).setEmailVerified(true);
        verify(user).setSingleAttribute("birthdate", "1990-01-02");
        verify(user).setSingleAttribute("gender", "male");
        verify(user).setSingleAttribute("phone_number", "+70000000000");
        verify(user).removeAttribute("picture");
        verifyNoMoreInteractions(user);
    }

    /** Clears stale optional profile fields when Yandex no longer supplies them. */
    @Test
    public void clearsMissingOptionalProfile() {
        config.setSyncMode(IdentityProviderSyncMode.FORCE);
        UserModel user = mock(UserModel.class);
        provider.updateBrokeredUser(session, mock(RealmModel.class), user,
            provider.extractIdentityFromProfile(null, profile));
        verify(user).setEmail("fixture@example.org");
        verify(user).setUsername("fixture@example.org");
        verify(user).setFirstName("First");
        verify(user).setLastName("Last");
        verify(user).removeAttribute("birthdate");
        verify(user).removeAttribute("gender");
        verify(user).removeAttribute("phone_number");
        verify(user).removeAttribute("picture");
        verifyNoMoreInteractions(user);
    }
}
