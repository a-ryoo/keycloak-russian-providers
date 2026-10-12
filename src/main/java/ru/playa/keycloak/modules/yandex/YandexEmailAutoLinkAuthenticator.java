package ru.playa.keycloak.modules.yandex;

import org.keycloak.authentication.AuthenticationFlowContext;
import org.keycloak.authentication.AuthenticationFlowError;
import org.keycloak.authentication.AuthenticationFlowException;
import org.keycloak.authentication.authenticators.broker.IdpAutoLinkAuthenticator;
import org.keycloak.authentication.authenticators.broker.util.SerializedBrokeredIdentityContext;
import org.keycloak.broker.provider.AbstractIdentityProvider;
import org.keycloak.broker.provider.BrokeredIdentityContext;
import org.keycloak.models.FederatedIdentityModel;
import org.keycloak.models.UserModel;
import org.keycloak.util.Booleans;

/** Links a trusted Yandex identity only to an account with the same email. */
public class YandexEmailAutoLinkAuthenticator extends IdpAutoLinkAuthenticator {

    @Override
    protected void authenticateImpl(final AuthenticationFlowContext context,
        final SerializedBrokeredIdentityContext serializedCtx, final BrokeredIdentityContext brokerContext) {
        if (!YandexIdentityProviderFactory.PROVIDER_ID.equals(brokerContext.getIdpConfig().getProviderId())
            || !Booleans.isTrue(brokerContext.getIdpConfig().isTrustEmail())
            || Boolean.parseBoolean(context.getAuthenticationSession()
                .getAuthNote(AbstractIdentityProvider.UPDATE_PROFILE_EMAIL_CHANGED))) {
            throw new AuthenticationFlowException("Trusted Yandex email is required",
                AuthenticationFlowError.IDENTITY_PROVIDER_ERROR);
        }
        UserModel user = getExistingUser(context.getSession(), context.getRealm(), context.getAuthenticationSession());
        String email = brokerContext.getEmail();
        if (email == null || email.isBlank() || !email.equalsIgnoreCase(user.getEmail())) {
            throw new AuthenticationFlowException("Yandex email does not match the existing account",
                AuthenticationFlowError.INVALID_USER);
        }
        FederatedIdentityModel existing = context.getSession().users().getFederatedIdentity(context.getRealm(), user,
            brokerContext.getIdpConfig().getAlias());
        if (existing != null && !existing.getUserId().equals(brokerContext.getId())) {
            throw new AuthenticationFlowException("Account is linked to another Yandex identity",
                AuthenticationFlowError.IDENTITY_PROVIDER_ERROR);
        }
        user.setEmailVerified(true);
        context.setUser(user);
        context.success();
    }
}
