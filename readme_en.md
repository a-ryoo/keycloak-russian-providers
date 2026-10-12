# Russian Providers for Keycloak 26.8.x

[Russian documentation](readme.md).

This fork supports **Keycloak 26.8.x and Java 21 only**. The current release is
built against Keycloak 26.8.0 using JDK 21.0.12.1. It imports and refreshes Yandex
avatars and removes OAuth configuration logging.
Yandex uses email as username and refreshes the full profile in `FORCE` mode.
The `yandex-email-auto-link` authenticator links pre-created accounts by trusted
email without a local password or email message; username-only matches are rejected.

```bash
mvn -s .mvn/settings.xml --batch-mode --no-transfer-progress clean verify
```

Download the JAR and SHA-256 from a pinned
[GitHub Release](https://github.com/a-ryoo/keycloak-russian-providers/releases),
verify the checksum, and install it in `/opt/keycloak/providers/` before the
standard Keycloak build/start. JSONPath dependencies are bundled and relocated;
Keycloak and SLF4J are supplied by the server. Define a read-only user `picture`
attribute limited to trusted Yandex avatar URLs to display the avatar.

GitHub Actions publishes the JAR, checksum, runtime-library CycloneDX SBOM and
Trivy vulnerability report on tags. Known bundled-library vulnerabilities block
the release. The Keycloak image requires a separate scan.

Upstream: [playa-ru/keycloak-russian-providers](https://github.com/playa-ru/keycloak-russian-providers).
Apache 2.0; original authors and LICENSE retained.
