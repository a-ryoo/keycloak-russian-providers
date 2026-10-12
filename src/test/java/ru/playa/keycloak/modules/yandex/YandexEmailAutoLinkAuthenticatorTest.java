package ru.playa.keycloak.modules.yandex;

import org.junit.Before;
import org.junit.Test;
import org.keycloak.authentication.AuthenticationFlowContext;
import org.keycloak.authentication.AuthenticationFlowException;
import org.keycloak.authentication.authenticators.broker.AbstractIdpAuthenticator;
import org.keycloak.authentication.authenticators.broker.util.ExistingUserInfo;
import org.keycloak.broker.provider.AbstractIdentityProvider;
import org.keycloak.broker.provider.BrokeredIdentityContext;
import org.keycloak.models.FederatedIdentityModel;
import org.keycloak.models.IdentityProviderModel;
import org.keycloak.models.KeycloakSession;
import org.keycloak.models.RealmModel;
import org.keycloak.models.UserModel;
import org.keycloak.models.UserProvider;
import org.keycloak.sessions.AuthenticationSessionModel;

import static org.junit.Assert.assertThrows;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoMoreInteractions;
import static org.mockito.Mockito.when;

/** Checks the trust boundary of Yandex email-only linking. */
public class YandexEmailAutoLinkAuthenticatorTest {
    /** Execution context. */
    private AuthenticationFlowContext context;
    /** Existing account. */
    private UserModel user;
    /** Broker configuration. */
    private IdentityProviderModel config;
    /** Broker identity. */
    private BrokeredIdentityContext identity;
    /** Authentication session. */
    private AuthenticationSessionModel authSession;
    /** User storage. */
    private UserProvider users;
    /** Realm. */
    private RealmModel realm;
    /** Authenticator. */
    private YandexEmailAutoLinkAuthenticator authenticator;

    /** Sets up a trusted provider and a passwordless existing account. */
    @Before
    public void setUp() {
        context = mock(AuthenticationFlowContext.class);
        user = mock(UserModel.class);
        authSession = mock(AuthenticationSessionModel.class);
        users = mock(UserProvider.class);
        realm = mock(RealmModel.class);
        KeycloakSession session = mock(KeycloakSession.class);
        when(context.getSession()).thenReturn(session);
        when(context.getRealm()).thenReturn(realm);
        when(context.getAuthenticationSession()).thenReturn(authSession);
        when(session.users()).thenReturn(users);
        when(authSession.getAuthNote(AbstractIdpAuthenticator.EXISTING_USER_INFO))
            .thenReturn(new ExistingUserInfo("existing-id", "email", "fixture@example.org").serialize());
        when(users.getUserById(realm, "existing-id")).thenReturn(user);
        when(user.isEnabled()).thenReturn(true);
        when(user.getEmail()).thenReturn("fixture@example.org");
        config = new IdentityProviderModel();
        config.setProviderId("yandex");
        config.setAlias("yandex");
        config.setEnabled(true);
        config.setTrustEmail(true);
        identity = new BrokeredIdentityContext("upstream-id", config);
        identity.setEmail("fixture@example.org");
        authenticator = new YandexEmailAutoLinkAuthenticator();
    }

    /** Links by email without touching username, profile, groups or roles. */
    @Test
    public void linksMatchingEmail() {
        authenticator.authenticateImpl(context, null, identity);
        verify(user).isEnabled();
        verify(user).getEmail();
        verify(user).setEmailVerified(true);
        verifyNoMoreInteractions(user);
        verify(context).setUser(user);
        verify(context).success();
    }

    /** Follows Keycloak's case-insensitive email comparison. */
    @Test
    public void linksEmailIgnoringCase() {
        identity.setEmail("FIXTURE@EXAMPLE.ORG");
        authenticator.authenticateImpl(context, null, identity);
        verify(context).success();
    }

    /** Rejects a collision detected only by username. */
    @Test
    public void rejectsUsernameOnlyMatch() {
        identity.setEmail("another@example.org");
        reject();
    }

    /** Rejects absent and blank emails. */
    @Test
    public void rejectsMissingEmail() {
        identity.setEmail(null);
        reject();
        identity.setEmail(" ");
        reject();
    }

    /** Requires an explicitly trusted provider email. */
    @Test
    public void rejectsUntrustedEmail() {
        config.setTrustEmail(false);
        reject();
    }

    /** Does not authorize another identity provider. */
    @Test
    public void rejectsOtherProvider() {
        config.setProviderId("other");
        reject();
    }

    /** Prevents user-edited profile email from authorizing a link. */
    @Test
    public void rejectsEditedEmail() {
        when(authSession.getAuthNote(AbstractIdentityProvider.UPDATE_PROFILE_EMAIL_CHANGED)).thenReturn("true");
        reject();
    }

    /** Does not log in a disabled account. */
    @Test
    public void rejectsDisabledAccount() {
        when(user.isEnabled()).thenReturn(false);
        reject();
    }

    /** Does not replace an existing link with another Yandex account. */
    @Test
    public void rejectsDifferentExistingYandexIdentity() {
        when(users.getFederatedIdentity(realm, user, "yandex"))
            .thenReturn(new FederatedIdentityModel("yandex", "another-upstream-id", "fixture"));
        reject();
    }

    /** Ensures rejection never sets a user, verifies email, or completes the flow. */
    private void reject() {
        assertThrows(AuthenticationFlowException.class, () -> authenticator.authenticateImpl(context, null, identity));
        verify(context, never()).setUser(user);
        verify(context, never()).success();
        verify(user, never()).setEmailVerified(true);
    }
}
