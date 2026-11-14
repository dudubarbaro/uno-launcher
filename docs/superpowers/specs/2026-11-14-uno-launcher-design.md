# Uno Launcher — documento do projeto

Data: 2026-11-14 · Status: aprovado pelo dono em 2026-11-14, com o pedido de testar num tablet antes do carro (seção 5.11)

> Nome confirmado pelo dono: **Uno Launcher**.

## 1. Intenção

Um launcher (tela inicial) para Android feito sob medida para **um único carro**: Fiat Uno Way 2016 1.0, edição Rio 450, rebaixado com molas esportivas e rodas originais pintadas de black piano. Ele vai rodar na central multimídia **Torque Hammer de 9", com Android 13**.

**Por quê:** o Rota Launcher foi feito para vários carros e traz recursos que o dono não quer. O objetivo é ter só o que importa, com visual próprio (escuro com detalhes em vermelho, combinando com o black piano). O app é escrito do zero e não copia código, imagens, marca nem o sistema de licença do Rota.

## 2. Objetivos, não-objetivos e anti-objetivos

**Objetivos**
- Tela inicial com a foto do Uno em destaque, hora, data, clima, dois números do motor, widget de música e só 4 atalhos: Waze, Spotify, OBD2 e Configurações.
- Painel OBD2 esportivo com conta-giros, velocidade, temperatura do motor, bateria, gasto da viagem e contagem de códigos de falha.
- Ler e apagar códigos de falha, com explicação em português.
- Calcular consumo e gasto da viagem em R$.
- Ao ligar o carro: abrir sozinho, abrir o app escolhido, retomar a música, conectar no OBD2 e ajustar o brilho para dia ou noite.
- Histórico de viagens, alertas, temas de cor, tela cheia e lista de todos os apps dentro de Configurações.

**Não-objetivos** (ficam de fora de propósito)
- Catálogo de marcas e modelos de carros, pacotes .zip, códigos de tema para compartilhar.
- Licença, versão paga, servidor próprio, contas.
- Suporte a adaptadores BLE ou Wi-Fi. Só Bluetooth clássico.
- Grade de apps na tela inicial.
- iPhone ou qualquer plataforma além de Android.

**Anti-objetivos** (se acontecer, o projeto falhou mesmo funcionando)
- Distrair o motorista: nada de texto pequeno, menus fundos ou animações chamativas na tela inicial. Todo toque importante precisa de alvo grande.
- Ficar lento na central: a tela inicial precisa abrir rápido e a leitura do OBD2 não pode travar a interface.
- Apagar códigos de falha sem querer: precisa de confirmação clara.
- Parecer cópia do Rota.

## 3. Restrições

- **Aparelho:** Torque Hammer 9", Android 13, sempre deitado. A resolução exata é desconhecida (centrais de 9" costumam ter 1024×600 ou 1280×720), então o layout tem que se adaptar e não pode depender de pixels fixos.
- **Adaptador OBD2:** já está pareado na central com o nome **"OBDII"**. Pelo anúncio (só Android), é quase certo um clone ELM327 de **Bluetooth clássico (SPP)**, v1.5 ou v2.1. Esses clones costumam falhar em comandos avançados, então o app usa só comandos básicos e deixa o protocolo no automático (`ATSP0`).
- **Carro:** Uno 1.0 Fire Evo flex, 999 cc, padrão OBD-BR2. Provavelmente não informa fluxo de ar (MAF), só pressão do coletor (MAP). Também não informa o hodômetro.
- **Sinal de ignição:** não se sabe se a Torque Hammer manda algum broadcast ao ligar o carro (como o `ACC_ON` das FYT) ou se ela só liga do zero. Isso precisa ser testado no aparelho.
- **Ambiente de build:** JDK 17, Gradle 8.9 e Android SDK 35 já instalados em `~/tools`.
- **Teste antes do carro:** o dono quer testar num tablet antes de levar o APK para a central. A versão do Android e o tamanho da tela do tablet são desconhecidos, e o adaptador OBD2 fica no carro.

## 4. Caminhos considerados

1. **App nativo Android (Kotlin + Jetpack Compose).** Acesso direto a tudo que o app precisa: virar tela inicial, ler a música por notificações, Bluetooth clássico, serviço em segundo plano e brilho. É o que tem mais código. **Escolhido.**
2. **Flutter.** Só compensaria para iPhone, e não há esse interesse. Bluetooth clássico, mídia e o papel de launcher exigiriam código nativo de qualquer jeito.
3. **Sem programar** (launcher pronto + Torque Lite). Fica pronto na hora, mas não é um app próprio, não mostra dados do motor na tela inicial e o visual é limitado.

**Decisão:** caminho 1, escolhido pelo dono porque é o único que entrega tudo do jeito que foi desenhado.

## 5. O que foi decidido

### 5.1 Telas

**Tela inicial** (modelo B escolhido nos protótipos)
- À esquerda, grande: a foto do Uno. Por cima dela ficam a hora, a data, o clima, e a temperatura do motor e a tensão da bateria em vermelho. Uma faixa vermelha fina fica na base da foto.
- À direita: o widget de música com capa, nome, artista, barra de progresso e ⏮ ⏯ ⏭.
- Embaixo, só 4 atalhos grandes: **Waze**, **Spotify**, **OBD2** (borda vermelha) e **Configurações**.
- Sem OBD2 conectado, os números do motor mostram "—" e um aviso discreto.

**Painel OBD2** (modelo A, abre ao tocar em OBD2)
- A foto do Uno aparece bem escura ao fundo, com um botão vermelho ‹ para voltar.
- No centro, o conta-giros grande com a velocidade.
- À direita: temperatura do motor e bateria (com barras), viagem (R$, km/l e combustível atual), "Códigos de falha: N" (toque abre a tela de códigos) e o estado da conexão.

**Códigos de falha**
- Lista os códigos armazenados, pendentes e permanentes (modos `03`, `07` e `0A`), cada um com explicação em português.
- Códigos de fabricante sem tradução aparecem como "código do fabricante, confirme com um mecânico".
- O botão **Apagar códigos** (modo `04`) pede confirmação. O aviso diz que apagar não conserta o defeito, que a luz volta se o problema continuar, e que é melhor fazer com a ignição ligada e o motor desligado.

**Histórico de viagens**
- Lista de viagens com data, km, tempo, litros estimados, km/l e R$.
- Desligar o carro não encerra a viagem. Ela só fecha quando o dono toca em **Nova viagem**.

**Configurações**, em seções:
- **Carro:** foto do Uno (escolhida na galeria), combustível no tanque (gasolina / etanol), preço do litro, ajuste fino do consumo, hodômetro atual e km da próxima revisão, e escolha do adaptador entre os pareados ("OBDII" como padrão).
- **Ao ligar:** abrir o app escolhido (ex.: Waze), retomar a música e conectar no OBD2. Cada item liga e desliga separado.
- **Tela:** tema de cor, brilho de dia, brilho de noite, brilho automático ligado/desligado e tela cheia.
- **Alertas:** limite de temperatura do motor, bateria baixa ou alta e aviso de revisão.
- **Apps:** lista de todos os apps instalados, para abrir qualquer um.
- **Permissões:** atalhos para liberar acesso a notificações, alterar configurações do sistema, localização e Bluetooth, mostrando o que falta.

### 5.2 Visual
- Fundo preto com leve brilho, efeito black piano. A cor de destaque padrão é o vermelho `#E10600`.
- **Temas de cor:** troca só a cor de destaque. Opções: vermelho (padrão), branco, laranja e azul.
- Fonte esportiva livre (licença OFL), diferente das usadas pelo Rota. Números grandes e legíveis de relance.

### 5.3 OBD2
- **Conexão:** Bluetooth clássico RFCOMM com UUID SPP `00001101-0000-1000-8000-00805F9B34FB`, no aparelho pareado escolhido. Reconecta sozinho com espera crescente quando cai.
- **Inicialização:** `ATZ`, `ATE0`, `ATL0`, `ATS0`, `ATH0`, `ATSP0` e depois `0100` para descobrir quais dados o carro informa.
- **Dados ao vivo:**

  | Comando | Dado |
  |---|---|
  | `010C` | RPM |
  | `010D` | velocidade |
  | `0105` | temperatura do motor |
  | `ATRV` | tensão da bateria |
  | `010B` | MAP, para o consumo |
  | `010F` | temperatura do ar, para o consumo |
  | `0110` | MAF, se o carro tiver |

  Os pedidos são feitos um por vez e em rodízio, para não engasgar o clone.
- **Consumo:** se o carro informar MAF, o app usa direto. Se não, calcula o ar admitido pela pressão do coletor, temperatura do ar, RPM e cilindrada de 999 cc, com eficiência volumétrica inicial de 0,80. O combustível sai pela razão ar/combustível de cada um (gasolina comum brasileira ≈ 13,2; etanol hidratado ≈ 9,0) e pela densidade de cada um.
  - O **ajuste fino do consumo** em Configurações é um multiplicador. O dono acerta ele comparando com um tanque cheio de verdade. Isso é uma estimativa, e a tela diz isso.
- **Distância:** integrada pela velocidade do OBD2. Não usa GPS.

### 5.4 Viagem em segundo plano
- Um serviço em primeiro plano do tipo "dispositivo conectado" mantém a leitura do OBD2 e soma a viagem enquanto o app não está na tela, por exemplo com o Waze aberto.
- A viagem atual é salva com frequência para sobreviver ao desligamento da central.

### 5.5 Ao ligar o carro
- O app é a tela inicial padrão (HOME), então aparece sozinho quando a central liga.
- Um receptor escuta `BOOT_COMPLETED`. Se o teste na central mostrar outro broadcast de ignição, ele é adicionado.
- Na partida: conecta no OBD2, abre o app escolhido e retoma a música, mandando "play" para a última sessão de mídia ou abrindo o Spotify e mandando "play". Cada passo segue o que estiver ligado em Configurações.

### 5.6 Música
- Um serviço de leitura de notificações dá acesso às sessões de mídia ativas, com título, artista, capa, progresso e controles.
- Sem a permissão liberada, o widget mostra um botão para liberar.

### 5.7 Clima
- Previsão do met.no (`locationforecast/2.0/compact`), atualizada no máximo a cada 30 minutos, com localização aproximada arredondada a 2 casas.
- Mostra a última previsão guardada quando estiver sem internet.
- User-Agent: `UnoLauncher/<versão> <e-mail do dono>`. O dono informou o e-mail para isso.
  - O e-mail fica fora do repositório, junto com a configuração da chave de assinatura, e só entra no APK na hora do build.
  - Ele fica visível dentro do APK para quem baixar pelo link.

### 5.8 Brilho dia/noite
- Nascer e pôr do sol calculados no próprio aparelho, sem internet, pela última localização conhecida.
- Aplica o brilho de dia ou de noite na hora certa. Precisa da permissão "alterar configurações do sistema".

### 5.9 Alertas
- Aviso na tela quando o motor passar do limite (padrão 105 °C) ou a bateria sair da faixa.
  - Bateria abaixo de 12,0 V com o motor ligado, ou acima de 15,0 V.
- Aviso de revisão quando o hodômetro informado mais os km das viagens chegarem perto da próxima revisão (padrão: 500 km antes).

### 5.10 Instalação
- O APK é assinado com uma chave do próprio dono, guardada fora do projeto.
- O dono quer as três formas de instalar:
  1. **Link na internet:** a central tem navegador. O APK vai num GitHub Release, num link fixo que sempre baixa a versão mais nova: `https://github.com/dudubarbaro/uno-launcher/releases/latest/download/uno-launcher.apk`. Na primeira vez, o navegador da central precisa da permissão "instalar apps desconhecidos". Vale salvar o link nos favoritos.
  2. **Pendrive:** copiar o .apk e abrir no gerenciador de arquivos.
  3. **adb via Wi-Fi:** com a central e o PC na mesma rede, instalar direto do PC e ler os registros da central.
- Depois é preciso escolher o app como tela inicial padrão.
- Atualizar pelo link funciona porque todas as versões usam a mesma chave de assinatura.

### 5.11 Teste no tablet (pedido do dono)
- **Ícone normal na gaveta de apps**, além de ser tela inicial. No tablet ele abre como qualquer app, sem precisar virar a tela inicial padrão.
- **Modo demonstração** em Configurações, desligado por padrão. Ele troca o OBD2 real por dados simulados:
  - RPM, velocidade, temperatura e bateria variam de forma suave, como num trajeto;
  - dois códigos de falha falsos, para testar a tela de códigos e o fluxo de apagar;
  - viagem e consumo calculados em cima desses dados.
- Com o modo ligado, aparece um selo **DEMO** fixo na tela, para nunca confundir com dados reais. Viagens do modo demonstração não entram no histórico real.
- **Sobre este aparelho**, em Configurações: mostra a resolução, a densidade, a versão do Android e os últimos eventos do sistema que o app recebeu (boot, energia ligada e tela ligada), com horário. Isso serve para o teste na central: descobrir a resolução e como ela avisa a ignição, sem depender de adb.
- O layout já se adapta a qualquer tela deitada, então funciona no tablet e na central. A tela fica travada deitada.
- O OBD2 real também funciona no tablet, se ele tiver Bluetooth e for pareado com o "OBDII" dentro do carro.

## 6. Chamadas minhas (o dono pode corrigir)

- **Android 8 ou mais novo** (`minSdk 26`), para rodar também no tablet de teste, cuja versão não se sabe. Custa poucas checagens de permissão por versão: Bluetooth antes e depois do Android 12, e notificações a partir do 13.
- **Distância pela velocidade do OBD2**, sem GPS, para evitar localização em segundo plano. A localização fica só para clima e brilho.
- **Ajuste fino do consumo** como multiplicador manual, porque o cálculo sem MAF é estimativa.
- **Hodômetro informado à mão**, porque o carro não informa pelo OBD2.
- **Temas = só a cor de destaque** (4 opções), com o fundo sempre preto.
- **Limites dos alertas:** 105 °C, 12,0 V, 15,0 V e revisão avisada 500 km antes. Todos editáveis.
- **Link de download no GitHub Releases**, na conta `dudubarbaro` escolhida pelo dono, porque o link fixo `latest` não muda entre versões. Exige repositório público, senão o navegador da central não baixa sem login. O dono autentica o `gh` nessa conta antes da primeira publicação.
- **E-mail do clima fora do código**, para não ficar público no repositório.

## 7. Fica a cargo de quem constrói

- Organização interna do código, bibliotecas e como guardar dados (configurações e histórico).
- Ritmo exato do rodízio de pedidos ao OBD2 e o tempo de reconexão.
- Conteúdo da tabela de códigos de falha em português: códigos genéricos SAE, começando pelos mais comuns de motor e emissões.
- Como enquadrar a foto do Uno (corte centralizado como base).
- Animações, desde que discretas.
- Testes automáticos de:
  - leitura das respostas do OBD2 (dados e códigos de falha);
  - cálculo de consumo;
  - cálculo do nascer e pôr do sol.

## 8. Pendências que só o dono resolve

Resolvidas: nome (**Uno Launcher**), contato do clima (e-mail do dono), instalação (as três formas) e conta do GitHub (`dudubarbaro`).

Falta:
1. **Teste na central:** descobrir a resolução e se existe broadcast de ignição. Pode ser por adb via Wi-Fi ou por uma versão de teste do app que mostre isso na tela.
