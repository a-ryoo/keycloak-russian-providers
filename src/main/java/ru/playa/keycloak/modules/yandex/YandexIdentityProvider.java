package ru.playa.keycloak.modules.yandex;

import com.fasterxml.jackson.databind.JsonNode;
import jakarta.ws.rs.core.UriBuilder;
import org.keycloak.broker.oidc.mappers.AbstractJsonUserAttributeMapper;
import org.keycloak.broker.provider.AuthenticationRequest;
import org.keycloak.broker.provider.BrokeredIdentityContext;
import org.keycloak.broker.provider.IdentityBrokerException;
import org.keycloak.broker.social.SocialIdentityProvider;
import org.keycloak.events.EventBuilder;
import org.keycloak.http.simple.SimpleHttp;
import org.keycloak.http.simple.SimpleHttpRequest;
import org.keycloak.models.KeycloakSession;
import org.keycloak.models.RealmModel;
import org.keycloak.models.UserModel;
import ru.playa.keycloak.modules.AbstractRussianOAuth2IdentityProvider;
import ru.playa.keycloak.modules.RussianException;
import ru.playa.keycloak.modules.Utils;

import java.io.IOException;

import static ru.playa.keycloak.modules.RussianException.EMAIL_CAN_NOT_EMPTY_KEY;

/**
 * Провайдер OAuth-авторизации через <a href="https://yandex.ru">Яндекс</a>.
 * <a href="https://tech.yandex.ru/oauth/">Подробнее</a>.
 *
 * @author Anatoliy Pokhresnyi
 */
public class YandexIdentityProvider
    extends AbstractRussianOAuth2IdentityProvider<YandexIdentityProviderConfig>
    implements SocialIdentityProvider<YandexIdentityProviderConfig> {

    /**
     * Запрос кода подтверждения.
     */
    private static final String AUTH_URL = "https://oauth.yandex.ru/authorize";

    /**
     * Обмен кода подтверждения на токен.
     */
    private static final String TOKEN_URL = "https://oauth.yandex.ru/token";

    /**
     * Запрос информации о пользователе.
     */
    private static final String PROFILE_URL = "https://login.yandex.ru/info";

    /**
     * Права доступа к данным пользователя по умолчанию.
     */
    private static final String DEFAULT_SCOPE = "";

    /**
     * Создает объект OAuth-авторизации через
     * <a href="https://yandex.ru">Яндекс</a>.
     *
     * @param session Сессия Keycloak.
     * @param config  Конфигурация OAuth-авторизации.
     */
    public YandexIdentityProvider(final KeycloakSession session, final YandexIdentityProviderConfig config) {
        super(session, config);
        config.setAuthorizationUrl(AUTH_URL);
        config.setTokenUrl(TOKEN_URL);
        config.setUserInfoUrl(PROFILE_URL);
    }

    @Override
    protected boolean supportsExternalExchange() {
        return true;
    }

    @Override
    protected String getProfileEndpointForValidation(final EventBuilder event) {
        return PROFILE_URL;
    }

    @Override
    protected SimpleHttpRequest buildUserInfoRequest(final String subjectToken, final String userInfoUrl) {
        return SimpleHttp.create(session).doGet(PROFILE_URL + "?oauth_token=" + subjectToken);
    }

    @Override
    protected BrokeredIdentityContext extractIdentityFromProfile(final EventBuilder event, final JsonNode node) {
        BrokeredIdentityContext user = new BrokeredIdentityContext(getJsonProperty(node, "id"), getConfig());

        String email = getJsonProperty(node, "default_email");
        if (Utils.isNullOrEmpty(email)) {
            throw new RussianException(YandexIdentityProviderFactory.PROVIDER_ID, EMAIL_CAN_NOT_EMPTY_KEY);
        } else {
            Utils.isHostedDomain(email, getConfig().getHostedDomain(), YandexIdentityProviderFactory.PROVIDER_ID);
        }

        user.setUsername(email);
        user.setEmail(email);
        user.setLastName(getJsonProperty(node, "last_name"));
        user.setFirstName(getJsonProperty(node, "first_name"));
        setProfileAttribute(user, "birthdate", getJsonProperty(node, "birthday"));
        setProfileAttribute(user, "gender", getJsonProperty(node, "sex"));
        setProfileAttribute(user, "phone_number", getJsonProperty(node.path("default_phone"), "number"));

        String avatarId = getJsonProperty(node, "default_avatar_id");
        if (avatarId != null && avatarId.matches("[A-Za-z0-9_-]+(?:/[A-Za-z0-9_-]+)*")) {
            user.setUserAttribute("picture", "https://avatars.yandex.net/get-yapic/" + avatarId + "/islands-200");
        }

        user.setIdp(this);

        AbstractJsonUserAttributeMapper.storeUserProfileForMapper(user, node, getConfig().getAlias());

        return user;
    }

    @Override
    protected BrokeredIdentityContext doGetFederatedIdentity(final String accessToken) {
        try {
            return extractIdentityFromProfile(
                null,
                SimpleHttp
                    .create(session)
                    .doGet(PROFILE_URL + "?oauth_token=" + accessToken)
                    .asJson()
            );
        } catch (IOException e) {
            throw new IdentityBrokerException("Could not obtain user profile from Yandex: " + e.getMessage(), e);
        }
    }

    @Override
    protected UriBuilder createAuthorizationUrl(final AuthenticationRequest request) {
        UriBuilder builder = super.createAuthorizationUrl(request);
        if (getConfig().isForceConfirm()) {
            builder.queryParam("force_confirm", "yes");
        }
        return builder;
    }


    @Override
    public void updateBrokeredUser(
        final KeycloakSession session, final RealmModel realm,
        final UserModel user, final BrokeredIdentityContext context
    ) {
        super.updateBrokeredUser(session, realm, user, context);
        if (getConfig().getSyncMode() == org.keycloak.models.IdentityProviderSyncMode.FORCE) {
            user.setEmail(context.getEmail());
            user.setUsername(context.getEmail());
            user.setFirstName(context.getFirstName());
            user.setLastName(context.getLastName());
            if (Boolean.TRUE.equals(getConfig().isTrustEmail())) {
                user.setEmailVerified(true);
            }
            for (String attribute : new String[] {"birthdate", "gender", "phone_number"}) {
                String value = context.getUserAttribute(attribute);
                if (value == null || value.isBlank()) {
                    user.removeAttribute(attribute);
                } else {
                    user.setSingleAttribute(attribute, value);
                }
            }
        }
        String picture = context.getUserAttribute("picture");
        if (picture == null) {
            user.removeAttribute("picture");
        } else {
            user.setSingleAttribute("picture", picture);
        }
    }

    @Override
    protected String getDefaultScopes() {
        return DEFAULT_SCOPE;
    }

    /**
     * Adds a present optional profile value to the broker context.
     * @param context broker identity
     * @param name attribute name
     * @param value upstream value, possibly absent
     */
    private void setProfileAttribute(final BrokeredIdentityContext context, final String name, final String value) {
        if (value != null && !value.isBlank()) {
            context.setUserAttribute(name, value);
        }
    }
}
