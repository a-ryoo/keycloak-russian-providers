package ru.playa.keycloak.modules.yandex;

import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.Test;
import org.keycloak.broker.oidc.mappers.AbstractJsonUserAttributeMapper;
import org.keycloak.broker.provider.BrokeredIdentityContext;
import org.keycloak.models.IdentityProviderMapperModel;

import java.util.List;
import java.util.Map;

import static org.junit.Assert.assertEquals;

/** Verifies JSONPath 3 array results through the existing mapper. */
public class YandexUserAttributeMapperTest {

    /** Preserves multi-valued mapping after updating the JSONPath dependencies. */
    @Test
    public void mapsJsonArray() {
        YandexIdentityProviderConfig config = new YandexIdentityProviderConfig();
        config.setEnabled(true);
        config.setAlias("yandex");
        BrokeredIdentityContext context = new BrokeredIdentityContext("fixture-id", config);
        AbstractJsonUserAttributeMapper.storeUserProfileForMapper(context,
            new ObjectMapper().createObjectNode().set("roles",
                new ObjectMapper().createArrayNode().add("one").add("two")), "yandex");
        IdentityProviderMapperModel mapper = new IdentityProviderMapperModel();
        mapper.setConfig(Map.of(AbstractJsonUserAttributeMapper.CONF_JSON_FIELD, "roles",
            AbstractJsonUserAttributeMapper.CONF_USER_ATTRIBUTE, "fixture-roles"));
        new YandexUserAttributeMapper().preprocessFederatedIdentity(null, null, mapper, context);
        assertEquals(List.of("one", "two"), context.getAttributes().get("fixture-roles"));
    }
}
