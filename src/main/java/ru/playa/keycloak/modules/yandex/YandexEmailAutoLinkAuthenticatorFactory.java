package ru.playa.keycloak.modules.yandex;

import org.keycloak.authentication.Authenticator;
import org.keycloak.authentication.authenticators.broker.IdpAutoLinkAuthenticatorFactory;
import org.keycloak.models.KeycloakSession;

/** Registers the Yandex email-only account linking authenticator. */
public class YandexEmailAutoLinkAuthenticatorFactory extends IdpAutoLinkAuthenticatorFactory {

    /** Stable authentication execution identifier. */
    public static final String PROVIDER_ID = "yandex-email-auto-link";

    /** Stateless shared authenticator. */
    private static final Authenticator INSTANCE = new YandexEmailAutoLinkAuthenticator();

    @Override
    public Authenticator create(final KeycloakSession session) {
        return INSTANCE;
    }

    @Override
    public String getId() {
        return PROVIDER_ID;
    }

    @Override
    public String getDisplayType() {
        return "Yandex: link existing account by exact email";
    }

    @Override
    public String getHelpText() {
        return "Links a trusted Yandex identity to an enabled account with the same email; "
            + "rejects username-only collisions and a different existing Yandex identity.";
    }
}
