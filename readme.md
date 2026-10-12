# Russian Providers для Keycloak 26.8.x

QATools fork [playa-ru/keycloak-russian-providers](https://github.com/playa-ru/keycloak-russian-providers).
Поддерживаются только Keycloak **26.8.x** и Java **21**; текущая сборка использует
Keycloak **26.8.0** и JDK **21.0.12.1**, как официальный runtime.
WildFly, прежние версии Keycloak и Java 17 больше не поддерживаются.

Провайдеры: [Yandex](docs/yandex.md), [Mail.Ru](docs/mailru.md),
[VK](docs/vk.md), [VK ID](docs/vkid.md), [OK](docs/ok.md).
Документация провайдеров сохранена из upstream; старые скриншоты в ней показывают
названия настроек, а текущая Admin Console использует стандартную конфигурацию SPI.

## Изменения форка

- Yandex `default_avatar_id` переносится в стандартный атрибут `picture`;
  повторный вход обновляет аватар существующего пользователя.
- Отсутствующий или некорректный портрет удаляет прежний `picture`.
  Принимаются только безопасные пути на `avatars.yandex.net`.
- Вывод OAuth-конфигурации с client secret в лог удалён.
- JSONPath `3.0.0`, json-smart/accessors-smart `2.6.0` и ASM `9.11` включены
  в JAR в отдельном namespace. Keycloak SPI и SLF4J предоставляются сервером.

## Сборка и выпуск

```bash
mvn -s .mvn/settings.xml --batch-mode --no-transfer-progress clean verify
```

GitHub Actions проверяет код на push/PR. Тег `v<version>` публикует JAR,
SHA-256, `bom.json` и `vulnerability-report.json` в
[GitHub Releases](https://github.com/a-ryoo/keycloak-russian-providers/releases).
Выпуск блокируется при любой известной Trivy уязвимости в упакованных библиотеках;
provided/test зависимости исключены из runtime SBOM. Образ Keycloak нужно
проверять отдельно. Проверка CVE отражает базу на момент запуска CI.

## Установка

Скачайте JAR и SHA-256 из конкретного выпуска, проверьте checksum и поместите
`keycloak-russian-providers.jar` в `/opt/keycloak/providers/`, затем выполните
стандартную сборку/запуск Keycloak. Отдельные JAR зависимостей не нужны.
`Dockerfile` использует закреплённый официальный Keycloak 26.8.0 и готовый JAR.

Для отображения аватара объявите `picture` в Keycloak User profile: пользователь
может читать, администратор — изменять. Ограничьте URI регулярным выражением
`^https://avatars\.yandex\.net/get-yapic/[A-Za-z0-9_-]+(?:/[A-Za-z0-9_-]+)*/islands-200$`.
Стандартный OIDC scope `profile` содержит mapper `picture`.

Лицензия: Apache 2.0; исходные авторы и LICENSE сохранены.
