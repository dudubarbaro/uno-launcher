# Uno Launcher

Tela inicial feita sob medida para um Fiat Uno Way 2016 1.0, numa central Android (Torque Hammer 9", Android 13).
Mostra a foto do carro, música, clima e dados do OBD2, lê e apaga códigos de falha e calcula o gasto da viagem.

## Instalar

Escolha uma das formas:

1. **Pelo link** (a central e o tablet têm navegador). Abra:
   `https://github.com/dudubarbaro/uno-launcher/releases/latest/download/uno-launcher.apk`
   Na primeira vez, permita que o navegador "instale apps desconhecidos". Salve o link nos favoritos: ele sempre baixa a versão mais nova.
2. **Pendrive:** copie o `uno-launcher.apk` e abra pelo gerenciador de arquivos.
3. **adb pelo Wi-Fi** (PC e central na mesma rede, "Depuração sem fio" ligada):
   ```sh
   adb connect <ip>:<porta>
   adb install -r uno-launcher.apk
   ```

Depois, na central, escolha **Uno Launcher** como tela inicial padrão (Configurações › Permissões › Tela inicial padrão).

## Testar no tablet antes do carro

1. Instale pelo link e abra pelo ícone. Não precisa virar a tela inicial do tablet.
2. Em **Configurações › Teste**, ligue o **Modo demonstração**. Aparece o selo DEMO e o painel mostra dados simulados.
   As viagens do modo demonstração não entram no histórico.
3. Em **Configurações › Sobre este aparelho** aparecem a resolução e os eventos do sistema. Na central, eles mostram como ela avisa a ignição.

## Compilar

Precisa de JDK 17, Gradle 8.9 e Android SDK 35 em `~/tools` (o script `./g` chama o Gradle com isso).

```sh
./g :app:testDebugUnitTest   # testes (gera fotos das telas em app/build/shots/)
./g :app:assembleRelease     # APK assinado
```

A assinatura e o contato do clima ficam em `~/.gradle/gradle.properties`, fora do repositório:
`unoKeystore`, `unoKeystorePassword`, `unoKeyAlias`, `unoKeyPassword`, `unoContact`.

## Publicar uma versão nova

1. Aumente `versionCode` e `versionName` em `app/build.gradle.kts`.
2. Rode `scripts/release.sh`.

## ⚠ Backup da chave

Guarde uma cópia de `~/.android-keys/uno-launcher.jks` **e** de `~/.gradle/gradle.properties` (as senhas estão lá) fora deste computador.
Sem eles, uma versão nova não instala por cima da antiga. Aí é preciso desinstalar, e isso apaga o histórico de viagens e as configurações.

## Licenças

Fonte [Chakra Petch](https://github.com/m4rc1e/Chakra-Petch) sob SIL Open Font License 1.1 (`app/src/main/assets/licenses/`).
