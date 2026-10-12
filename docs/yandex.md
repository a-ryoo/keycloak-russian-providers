# Яндекс

## Email identity and automatic account linking

The Yandex email is the Keycloak username. Existing users retain their internal ID,
groups and roles. With provider sync mode `FORCE`, each login refreshes email,
username, first/last name, picture, birthdate, gender and phone_number; missing
optional attributes clear stale values.

To link pre-created accounts without an email message or local password, create a
dedicated first broker login flow with `idp-create-user-if-unique` followed by
`yandex-email-auto-link`, both `ALTERNATIVE`, and assign it only to Yandex.
Enable `Trust Email`; disable duplicate emails. Do not add a profile-review step:
the email authorizing a link must come directly from the trusted Yandex profile.
The custom authenticator rejects username-only collisions, user-edited emails,
disabled users and accounts linked to a different Yandex identity. Do not replace
it with the generic `idp-auto-link` authenticator.

**Создание приложения в [Яндекс](https://oauth.yandex.ru/client/new)**

_Создаём приложение._
1. Заполняем поле *Название приложения*.
2. Заполняем поле *Ссылка на сайт приложения*
![Общие настройки приложения](screenshots/yandex_provider_1.png)
3. Раздел *Платформы*.
+ В разделе *Платформы* выбираем *Веб-сервисы*.
+ Заполняем поле *Callback URI #1* - https://${keycloak-host}/auth/realms/${realm-name}/broker/yandex/endpoint, где
 _keycloak-host_ - домен на котором размещен _Keycloak_,
 _realm-name_ - название _Realm_ в _Keycloak_.
 ![Раздел платформы](screenshots/yandex_provider_2.png)
4. Раздел *Доступы* - *API Яндекс.Паспорта*.
+ Выбираем пункт _Доступ к адресу электронной почты._
+ Выбираем пункт _Доступ к логину, имени и фамилии, полу._
 ![Раздел Доступы](screenshots/yandex_provider_3.png)
5. Нажимаем кнопку *Создать приложение*.
 ![Созданое приложение Яндекс](screenshots/yandex_provider_4.png)

**Создание провайдера Keycloak**
1. Из списка провайдеров выбрать [Яндекс](https://yandex.ru).
![Выбор Яндекс провайдера](screenshots/yandex_provider_5.png)

2. Копируем значение *ID* в поле *Client ID* в админке *Keycloak*.
3. Копируем значение *Пароль* в поле *Client Secret* в админке *Keycloak*.
4. Нажимаем кнопку *Save* в админке *Keycloak*.
![Создание провайдера Keycloak](screenshots/yandex_provider_6.png)
