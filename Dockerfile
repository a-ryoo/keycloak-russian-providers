FROM quay.io/keycloak/keycloak:26.8.0@sha256:ad1b0f0fa4bb5eff10f7a78eaafbb6d65b25431c1f824b0710836d74373a7dfb
COPY --chown=1000:0 target/keycloak-russian-providers.jar /opt/keycloak/providers/keycloak-russian-providers.jar
