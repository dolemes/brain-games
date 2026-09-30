# Brain Games (projeto inicial)

App Android com uma coleção de brain games, feito em Kotlin + Jetpack Compose, pensado para ser
desenvolvido com o Claude Code e o GitHub, sem Android Studio obrigatório.

- `core/`: as regras dos jogos, em Kotlin puro, com testes.
- `app/`: as telas, o save, os anúncios e os minijogos.
- `.github/workflows/`: testes e APK a cada envio (`ci.yml`); versão para a Play a cada tag (`release.yml`).
- `CLAUDE.md`: as regras do projeto, lidas pelo Claude Code em toda sessão.

Vem com um minijogo de exemplo completo: **Setas no Meio** (atenção).

## Primeiros passos

1. **Crie um repositório privado no GitHub** e envie esta pasta:
   ```bash
   git init && git add . && git commit -m "Projeto inicial"
   git branch -M main
   git remote add origin https://github.com/SEU_USUARIO/brain-games.git
   git push -u origin main
   ```
2. **Acompanhe o CI** na aba *Actions*. O primeiro build baixa tudo e leva alguns minutos.
   No fim, a execução traz dois arquivos para baixar: `apk-debug` (o app) e `capturas-de-tela`.
3. **Instale no celular.** O caminho mais simples é pelo cabo USB, com `adb install app-debug.apk`.
   Desde 30/09/2026, no Brasil, instalar um APK fora da loja num celular certificado exige
   que o app esteja registrado por um desenvolvedor verificado; pelo `adb` isso não é exigido.
   Quando a conta da Play estiver pronta, a faixa de teste interno também resolve.
4. **Use o Claude Code** na pasta do projeto. Para chamar o Claude em issues e pull requests
   (`@claude crie o minijogo Stroop`), rode `/install-github-app` dentro do Claude Code.

## Antes de publicar

- [ ] `applicationId` e `namespace` em `app/build.gradle.kts`: **definitivos** depois do primeiro envio.
- [ ] Nome do app (`app_name` nos dois `strings.xml`) e ícone (`res/drawable/ic_launcher_foreground.xml`).
- [ ] IDs do AdMob em `gradle.properties` (o release recusa os de teste).
- [ ] URL da política de privacidade em `gradle.properties` (`privacyPolicyUrl`).
- [ ] Testar o formulário de consentimento como se estivesse na Europa (`ConsentDebugSettings`
      com `DEBUG_GEOGRAPHY_EEA` em `ConsentManager`, só no debug).

## Publicar pelo GitHub

1. **Crie a chave de upload** (uma vez) e guarde-a fora do computador:
   ```bash
   keytool -genkeypair -v -keystore upload.jks -keyalg RSA -keysize 2048 -validity 10000 -alias upload
   base64 -w0 upload.jks   # no macOS: base64 -i upload.jks
   ```
2. **Cadastre os segredos** em *Settings › Secrets and variables › Actions*: `KEYSTORE_BASE64`,
   `KEYSTORE_PASSWORD`, `KEY_ALIAS` (`upload`), `KEY_PASSWORD`.
3. **Gere a primeira versão:** `git tag v0.1.0 && git push --tags`. Baixe o `app-release-aab` da
   execução e envie **manualmente** ao Play Console (a API da Play exige que o primeiro envio seja manual).
   Ative o *Play App Signing*.
4. **Envio automático (opcional):** crie uma conta de serviço com acesso ao app no Play Console,
   guarde o JSON no segredo `PLAY_SERVICE_ACCOUNT_JSON` e crie a variável `PLAY_UPLOAD=true`.
   Cada tag nova vai para o teste fechado como rascunho; você revisa e publica no Console.

Conta pessoal da Play criada depois de 13/11/2023: o primeiro lançamento exige teste fechado com
12 testadores inscritos por 14 dias seguidos.

## Versões

Conferidas em setembro de 2026 (`gradle/libs.versions.toml`): Android Gradle Plugin 9.4.0,
Gradle 9.6.0, Kotlin 2.4.20, Compose BOM 2026.09.00, Google Mobile Ads Next-Gen SDK 1.5.0,
target API 36 (Android 16), compileSdk 37 (exigido pelas bibliotecas), minSdk 26.
