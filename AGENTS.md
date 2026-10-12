# QATools fork

- Upstream mirror: playa-ru/keycloak-russian-providers, initial commit 06d9f8e.
- Owner requested this public fork, code publication, versioned GitHub release JAR and VK rollout.
- Target runtime is Keycloak 26.8.0; compiler release 21, build/test on JDK 21.0.12.1.
- Validate with `mvn -s .mvn/settings.xml --batch-mode --no-transfer-progress clean verify`.
- Central-only mirror avoids slow inherited JBoss repository lookups; no private Maven credentials are needed.
- PMD 3.21.2/Java 17 replaces the upstream parser that failed on every file under JDK 21. Three existing style violations in AbstractRussianEndpoint/VKIDEndpoint are explicitly baselined in exclude-pmd.properties.
- Tag `v<project.version>` (no organization suffix) publishes JAR, SHA-256 and Trivy report; all known bundled-library CVEs block publication.
- JSONPath 3.0.0/json-smart 2.6.0/accessors-smart 2.6.0/ASM 9.11 are shaded; Keycloak and SLF4J remain provided. Do not add server libraries to the provider JAR.
- Yandex picture is a trusted avatars.yandex.net URI; synchronize existing UserModel as well as first-login context.
- Never log OAuth config or tokens. No infrastructure credentials belong in this public repo.
- Infrastructure values and runtime verification belong in sibling argo-system-apps, not this source repository.

- Only Keycloak 26.8.x/Java 21 is supported. Legacy WildFly descriptors, Angular theme assets, old compatibility docs and Sonatype/Spotify publication paths were removed.
- Runtime CycloneDX SBOM excludes provided/test dependencies; scan the SBOM to retain identity/version coverage for relocated libraries. Scan the official server image separately.
- setup-java rejects four-component Java versions: download the pinned official JDK archive, verify SHA-256, use distribution=jdkfile/java-version=21 and assert java -version is 21.0.12.1.
- Owner explicitly requested a Java email-only auto-link authenticator, email=username, and Yandex profile overwrite at login. Register yandex-email-auto-link through AuthenticatorFactory SPI; reject username-only matches, untrusted/edited emails, disabled users and another existing Yandex identity. Keep existing IDs/groups/roles.
- Yandex FORCE mode refreshes username/email, names, picture, birthdate, gender and phone_number. Missing optional attributes clear old values; no profile review before auto-link, because users must not edit the trusted upstream email.
