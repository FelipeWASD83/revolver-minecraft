# CLAUDE.md: revolver-minecraft

Mod de Minecraft (NeoForge 1.21.1) com o revólver do projeto `revolver-3d` e, desde a 1.2.0, a Desert Eagle do
`deagle-3d`. As duas atiram, recarregam e mostram as mesmas animações feitas no Blender. O revólver recarrega pela
portinhola, e a deagle troca o pente. Criado em 16/10/2026 a pedido do Felipe
("faz um mod do minecraft com esse revolver"), com plano aprovado; repositório público (escolha dele).
Extras aprovados por ele: sons, receitas e partículas (o coice na câmera ele não quis). O `CLAUDE.md` da
raiz vale aqui também; o `README.md` explica o mod para ele. A pesquisa que decidiu a abordagem está em
`docs/pesquisa-mod-revolver-neoforge.md`.

## Como gerar, compilar e conferir

```powershell
# 1. modelo e animações (lê o .blend de cada projeto; não mexe nele)
& "C:\Program Files\Blender Foundation\Blender 5.2\blender.exe" -b ..\revolver-3d\modelo\revolver.blend --python ferramentas\exportar_modelo.py
& "C:\Program Files\Blender Foundation\Blender 5.2\blender.exe" -b ..\deagle-3d\modelo\deagle.blend --python ferramentas\exportar_deagle.py
# 2. sons (precisa do .venv: python -m venv .venv; .venv\Scripts\python.exe -m pip install av==17.0.1 numpy==2.5.2)
.venv\Scripts\python.exe ferramentas\gerar_sons.py
# 3. compilar e testar (o jar sai em build\libs)
.\gradlew.bat build
# 4. prévia da mão e do inventário, sem abrir o jogo (ferramentas\saida\previa-*.png)
& "C:\Program Files\Blender Foundation\Blender 5.2\blender.exe" -b --factory-startup --python ferramentas\previa_mao.py
& "C:\Program Files\Blender Foundation\Blender 5.2\blender.exe" -b --factory-startup --python ferramentas\previa_mao.py -- deagle
```

O exportador termina com uma conferência (peça exportada + matriz do quadro contra o Blender; tem que dar
erro perto de 1e-6 bloco) e grava `ferramentas/exportacao.txt` (`exportacao-deagle.txt` na deagle). Os dois
exportadores dividem o `ferramentas/comum.py` (espaço do item, escrita do OBJ, exibição na mão, ícones); a saída do
revólver ficou igual byte a byte quando ele foi separado. Os testes (`.\gradlew.bat test`, 35) conferem:

- a lógica do tambor e a do pente;
- que os quadros do Java batem com os dos arquivos exportados;
- que todo modelo, textura, som, tradução e receita das duas armas existe e se lê.

**Instância de teste no Prism** (criada em 16/10/2026 a pedido dele): `%APPDATA%\PrismLauncher\instances\Revolver`, nome "Revólver (teste do mod)", só com este mod, base copiada da "Horizonte Infinito"
(Minecraft 1.21.1, NeoForge 21.1.251, Java do Prism, 2 a 6 GB) e o `options.txt` dele. Recompilou: copiar o
jar de `build\libs` para `minecraft\mods` de lá (e apagar o antigo se a versão mudar de nome). Nunca
mexer na "Horizonte Infinito" nem na "fancy craft".

**Não abrir o jogo por conta própria** (`runClient` abre uma janela do Minecraft na tela dele; a regra
vem do `feitio`, e ele joga no mesmo PC). Conferir pela prévia, pelos testes e pelo exportador; abrir só
quando ele pedir.

## Decisões que não se desfazem sem motivo

- **NeoForge 1.21.1 (21.1.252)**, a mesma base do `feitio` e da instância "Horizonte Infinito" dele. O
  Fabric não tem carregador de OBJ; fica para depois, se ele pedir.
- **O modelo é o do `revolver-3d`, em OBJ pelo carregador do NeoForge** (`neoforge:obj`), uma peça móvel
  por arquivo em `assets/revolver/models/item/revolver/` (o corpo parado num só; um estojo e uma bala que
  o jogo desenha seis vezes). O item usa `"parent": "builtin/entity"` e um renderizador próprio
  (`RevolverRenderer`, um BEWLR) que desenha cada peça com a matriz do quadro.
- **As animações são as do Blender**, amostradas pelo exportador em `assets/revolver/animacoes/revolver.json`
  (uma matriz 3x4 por peça por quadro, a 30 qps, relativa à peça exportada). O jogo roda a 20 ticks; o
  renderizador mistura quadros vizinhos. Mexeu na animação ou no modelo no `revolver-3d`: rodar o
  exportador de novo. Mexeu nos quadros dos eventos lá: atualizar `logica/Tempos.java` (o teste avisa).
- **Espaço do item:** 1 = um bloco, o revólver com 1 de comprimento, centrado em (0,5; 0,5; 0,5), cano em
  +X, topo em +Y, lado direito em +Z (é o lado que aparece no inventário).
- **Posição na mão:** a tabela `EXIBICAO` do exportador (giro, escala e onde a pegada cai) gera o
  `display` do `revolver.json`. Ajuste fino depois de ele ver no jogo é lá; a prévia mostra a primeira
  pessoa e o inventário. A terceira pessoa foi conferida só na conta (cano para a frente com o braço
  parado), sem imagem.
- **Tambor:** câmara física (guardada no item) e encaixe (lugar na animação). O tiro dispara sempre o
  encaixe 5; depois a posição anda um, e o encaixe e mostra a câmara (e - posição) mod 6, o que deixa a
  troca do fim da animação para o repouso sem pulo (há teste). A recarga passa pelos encaixes na ordem
  da portinhola (1, 0, 5, 4, 3, 2), gasta um cartucho por câmara sem bala e, sem cartucho, só tira os
  estojos vazios. No criativo não gasta.
- **O tiro sai no quadro em que o cão cai** (tick 14 depois do clique), como na animação: é um revólver
  de ação simples, o polegar arma antes. Raio no servidor até 64 blocos; **o dano é o da bala da câmara**.
- **Tipos de bala (1.1.0, pedido dele: "a mais básica dá 20, a melhor 200")**: `logica/TipoBala` com
  comum 20, aço 50, diamante 100 e netherite 200 (o de netherite não queima). **A ordem do enum é o número
  guardado em cada câmara: não reordenar; tipo novo entra no fim** (só cabem 4 em 2 bits; o quinto pede
  ampliar o campo). O estado ganhou `tipos` e `tipos_novos` como campos **opcionais**: revólver salvo na
  1.0.x lê tudo como comum (há teste). A recarga pega os cartuchos como o arco pega flecha (outra mão,
  depois a ordem do inventário) e pode misturar tipos. O cartucho comum mantém o nome `cartucho_45` da 1.0.
  Cada tipo tem um modelo de bala (`bala_<tipo>`, a mesma malha com a ponta na cor dele) e um ícone; a
  tabela `TIPOS` do exportador tem as cores e precisa casar com o `TipoBala`.
- **Sons** (`ferramentas/gerar_sons.py`, OGG Vorbis pelo PyAV 17.0.1, versão que passa pelo Smart App
  Control; o mixdown de áudio do Blender sem janela grava só o começo do som: não usar). **O tiro e a
  bala entrando são os arquivos que ele baixou** (`ferramentas/sons-baixados/`, fora do git porque a
  licença é desconhecida e o repositório é público): o tiro inteiro até 1,6 s com a cauda sumindo, e só
  o primeiro dos três sons do `uzi-reload.mp3`, tocado uma vez por bala (pedido dele). Os outros
  (cão, clique seco, portinhola, estojo, tambor) continuam sintetizados. Tudo mono, comprimido com tanh
  e gravado no pico mais alto sem passar de 0,98 depois de codificado (o Vorbis estoura além do
  original; o gerador confere e recodifica). No jogo todos tocam com volume 1,0 (o máximo; o tiro usa
  4,0, que só aumenta o alcance). Ele achou os sons da 1.0.0 baixos e sem cara de tiro.
- **Coordenada de textura sempre dentro do sprite** (o exportador põe a cor lisa no centro do branco e
  encolhe a textura que passa da borda): no atlas a textura não repete, e a do canto exato mistura com
  a borda transparente no mipmap. Na 1.0.0 isso deixou o cano, a haste, a mira e os parafusos
  pontilhados, quase invisíveis (print dele). Há teste.
- **O nome do mod vai como escape Unicode no `gradle.properties`** (`Rev\u00f3lver`): o arquivo é lido
  como Latin-1 e o "ó" direto sai duplicado no jar.
- **Fora da regra de atualização dentro do app**: mod é carregado pelo launcher, não é app.

## Desert Eagle (1.2.0, 16/10/2026)

Pedido dele: "faz uma deagle, ela vai dar 1,5x mais dano que o revolver, faz que nem fizemos o revolver". Plano
aprovado com munição .50 AE própria. O modelo é o do `deagle-3d`.

- **Mesmo caminho do revólver:**
  - `ferramentas/exportar_deagle.py` escreve `models/item/deagle/`, `textures/item/deagle/` e
    `animacoes/deagle.json`;
  - `DeagleRenderer` desenha as peças;
  - `DeagleItem` faz a lógica, e o tiro é o `Disparo.atirar` comum às duas armas.
- **Calibre e dano:**
  - `logica/Calibre` tem C45 (multiplicador 1) e C50 (1,5);
  - os tipos de bala continuam no `TipoBala` (a ordem não muda) e valem para os dois calibres;
  - o dano é `tipo.dano(calibre)`: .50 dá 30, 75, 150 e 300;
  - os cartuchos são `cartucho_50` e `cartucho_50_<tipo>`, e `CartuchoItem` guarda o calibre;
  - **cada arma só aceita o próprio calibre**, e o revólver passou a filtrar.
- **Pente** (`logica/Pente`, sem Minecraft, testado):
  - 7 no pente e 1 na câmara, com o tipo de cada bala em 2 bits e a de cima no lugar 0;
  - o tiro gasta a da câmara e sobe a de cima do pente; sem bala no pente, câmara vazia e **ferrolho aberto**;
  - a recarga mantém as que sobraram no pente velho (saem primeiro) e completa com cartuchos, na ordem do arco;
  - aberta, no fim da recarga a de cima vai para a câmara.
- **Estado** (`EstadoDeagle`, componente `estado_deagle`): pente, tipos, câmara, tipo da câmara, ação, início e o
  pente novo da recarga. As ações são:

  | Ação | Quando acontece | Animação |
  |---|---|---|
  | TIRO | atira com bala no pente | `tiro` |
  | TIRO_ULTIMO | atira a última bala, sem nenhuma no pente | `tiro_ultimo` |
  | SECO | aperta o gatilho aberta | `seco` |
  | RECARGA | troca o pente com bala na câmara | `recarregar` |
  | RECARGA_VAZIA | troca o pente aberta | `recarregar_vazia` |

  **Parada e aberta, o renderizador usa o quadro 0 da `recarregar_vazia`**, onde o ferrolho está atrás e a trava
  erguida.
- **Tempos** (`logica/TemposDeagle`, mesmo `Tempos.tick` do revólver):
  - o tiro sai no quadro 4 (tick 2) e a animação inteira tem 14 quadros (~2 tiros por segundo);
  - a troca de pente leva 76 quadros, ou 86 aberta;
  - no quadro 34 (`REC_TROCA`) o renderizador passa a mostrar a bala de cima do pente novo.

  Os eventos estão também no `EVENTOS` do exportador, e o teste confere que batem.
- **Peças do jogo:**
  - `ferrolho`, `cao`, `gatilho`, `trava`, `botao`, `pente` e `clarao`;
  - `estojo`: a mesma malha para o que voa no tiro (pista `ejetado`) e para o de cima do pente (pista
    `topo_estojo`);
  - `bala_<tipo>`, na pista `topo_bala`.
- **Cores no jogo:**
  - inox 0,80;
  - aço escuro 0,27, porque preto vira borrão;
  - borracha com textura própria 32x32 gerada pelo exportador, mais clara que a do Blender.

  A deagle tem ~27 cm, então fica um pouco maior que o revólver na mesma escala (250 mm = 1 bloco). As escalas de
  exibição são um pouco menores: primeira pessoa 0,78 e inventário 0,80.
- **Sons novos:**
  - `tiro_deagle`: o `tiro.mp3` 18% mais grave;
  - `pente_entra` e `ferrolho`: o segundo e o terceiro som do `uzi-reload.mp3`, que até então não eram usados;
  - `pente_sai` e `trava`: sintetizados.

  O gerador recodifica todos os OGG e o Vorbis muda os bytes a cada vez. Se os sons do revólver não mudaram,
  devolver os OGG dele ao commit (`git checkout`) em vez de commitar o ruído.
- **Tecla R e nome do mod:** a tecla R recarrega a arma que estiver na mão (`RecarregarMsg` decide pelo item). O
  nome do mod passou para "Revólver e Desert Eagle", e a categoria de teclas para "Armas".

## O que ainda não foi visto

O revólver ele testou no jogo (as correções da 1.0.1 vieram do que ele viu). **A Desert Eagle não foi aberta no
Minecraft**: tamanho e ângulo na mão (só a prévia), a luz nas peças, os sons, o tiro, a troca de pente e o
ferrolho aberto. A terceira pessoa das duas armas foi conferida só na conta. Shaders (Iris/Oculus) não
testados.
