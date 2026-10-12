# QATools fork

- Upstream mirror: playa-ru/keycloak-russian-providers, initial commit 06d9f8e.
- Owner requested this public fork, code publication, versioned GitHub release JAR and VK rollout.
- Target runtime is Keycloak 26.7.1; Java release 17, build/test on JDK 21.
- Validate with `mvn -s .mvn/settings.xml --batch-mode --no-transfer-progress -Dgpg.skip=true clean verify`.
- Central-only mirror avoids slow inherited JBoss repository lookups; no private Maven credentials are needed.
- PMD 3.21.2/Java 17 replaces the upstream parser that failed on every file under JDK 21. Three existing style violations in AbstractRussianEndpoint/VKIDEndpoint are explicitly baselined in exclude-pmd.properties.
- Tag `v<project.version>` publishes JAR and SHA-256 through `.github/workflows/release.yml`.
- Yandex picture is a trusted avatars.yandex.net URI; synchronize existing UserModel as well as first-login context.
- Never log OAuth config or tokens. No infrastructure credentials belong in this public repo.
- Infrastructure values and runtime verification belong in sibling argo-system-apps, not this source repository.
