# Brain Games — guia para o Claude Code

App Android (Kotlin + Jetpack Compose) com uma coleção de brain games (memória, lógica, atenção e
cálculo/linguagem) num só app com anúncios. Público: adultos, idosos e estudantes (13+).
Idiomas: pt-BR e inglês. Converse com o dono do projeto em português do Brasil.

## Módulos
- `core/`: Kotlin puro (JVM), sem Android. Regras de todos os jogos, escada adaptativa
  (`AdaptiveStaircase`), motor da rodada (`RoundEngine`), progresso (`PlayerProgress`, `ProgressJson`),
  treino do dia (`DailyWorkoutPlanner`), política de anúncios (`AdPolicy`). Testes JUnit 4.
- `app/`: Android/Compose. `GameHostScreen` (fluxo da rodada), `RoundSession` (ponte motor ↔ Compose),
  `ProgressRepository` (save com AtomicFile), anúncios (`GmaAdService`, SDK Next-Gen + `ConsentManager`/UMP),
  minijogos em `games/<id>/`, catálogo em `game/GameCatalog.kt`.

## Comandos
- `./gradlew :core:test`: lógica (segundos; rode sempre).
- `./gradlew :app:testDebugUnitTest`: testes do app (Robolectric).
- `./gradlew :app:assembleDebug`: APK de teste.
- `./gradlew :app:recordRoborazziDebug`: capturas de tela em `app/build/outputs/roborazzi/`.
  Depois de mudar qualquer tela, gere e OLHE as imagens (idioma, texto grande, cortes).

## Como adicionar um minijogo `<id>`
1. `core/src/main/kotlin/com/dolemes/braingames/core/games/<id>/<Nome>Rules.kt`: tabela nível → parâmetros,
   sorteio com o `Random` recebido, verificação de resposta. Testes ao lado, em `core/src/test/...`.
2. `app/src/main/kotlin/com/dolemes/braingames/games/<id>/<Nome>Game.kt`: `object <Nome>Game : MiniGame`
   com a `MiniGameDefinition` e `Play(session, modifier)`. Estímulo num composable sem estado.
   Modelo completo: `games/flanker/FlankerGame.kt`.
3. Registrar em `GameCatalog.all`.
4. Strings `game_<id>_name`, `game_<id>_howto` (e estímulos textuais) em `values/strings.xml` (en)
   e `values-pt/strings.xml`.
5. Capturas no `ScreenshotTest` (pt-BR e texto 1.6).

## Regras do projeto
- O minijogo nunca salva, mostra anúncio nem navega: isso é do `GameHostScreen`.
- Toda resposta chama `session.registerTrial(...)`; depois confira `session.isRunning`.
- Em pausa (`session.isPaused`), esconda o estímulo e descarte a tentativa; ao retomar, tentativa nova.
- Aleatoriedade só por `session.random` (com semente). Nunca `Random.Default` nem `Math.random()`.
- Tempo de reação: `withFrameNanos` no quadro em que o estímulo aparece e `session.nowNanos()` na resposta.
- Nenhum texto visível no código: sempre recursos de string, nos dois idiomas.
- Nada de informação só por cor (use ✓/✗ + cor; paleta Okabe-Ito). Alvos de toque ≥ 48 dp.
- `id` dos jogos e nomes do enum `CognitiveDomain` são chaves do save: não renomeie.
- Mudou o formato do save de forma incompatível? Suba `PlayerProgress.CURRENT_SCHEMA_VERSION`
  e trate em `ProgressJson.migrate`. Campos novos precisam de valor padrão.
- Nunca escreva alegações de saúde (previne Alzheimer/demência, aumenta QI, "clinicamente comprovado").
- O debug usa IDs de anúncio de teste; ninguém clica em anúncio real do próprio app.
- Versões ficam em `gradle/libs.versions.toml`; mudou versão, rode o CI inteiro.
