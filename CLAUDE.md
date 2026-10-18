# CLAUDE.md: revolver-minecraft

Mod de Minecraft (NeoForge 1.21.1), "Arsenal" desde a 2.0.0, com oito armas, cada uma modelada e animada no
Blender num projeto próprio:

- o revólver do `revolver-3d`;
- a Desert Eagle do `deagle-3d` (1.2.0);
- desde a 2.0.0, AK-47, M4A4, M4A1-S, metralhadora, escopeta 12 e sniper, dos projetos `<id>-3d`.

Todas atiram, recarregam e mostram as mesmas animações feitas no Blender. Criado em 16/10/2026 a pedido do Felipe
("faz um mod do minecraft com esse revolver"), com plano aprovado; repositório público (escolha dele).
Extras aprovados por ele: sons, receitas e partículas (o coice na câmera ele não quis). O `CLAUDE.md` da
raiz vale aqui também; o `README.md` explica o mod para ele. A pesquisa que decidiu a abordagem está em
`docs/pesquisa-mod-revolver-neoforge.md`.

## Como gerar, compilar e conferir

```powershell
# 1. modelo e animações (lê o .blend de cada projeto; não mexe nele)
& "C:\Program Files\Blender Foundation\Blender 5.2\blender.exe" -b ..\revolver-3d\modelo\revolver.blend --python ferramentas\exportar_modelo.py
& "C:\Program Files\Blender Foundation\Blender 5.2\blender.exe" -b ..\deagle-3d\modelo\deagle.blend --python ferramentas\exportar_deagle.py
# as armas novas: o .blend do projeto e o id (ak47, m4a4, m4a1, metralhadora, escopeta, sniper)
& "C:\Program Files\Blender Foundation\Blender 5.2\blender.exe" -b ..\ak47-3d\modelo\ak47.blend --python ferramentas\exportar_arma.py -- ak47
# os ícones em pixel art do inventário (depois dos exportadores; textures/item/icone e models/item/icone)
& "C:\Program Files\Blender Foundation\Blender 5.2\blender.exe" -b --factory-startup --python ferramentas\gerar_icones.py
# a imagem da luneta da sniper (textures/gui/luneta.png)
.venv\Scripts\python.exe ferramentas\gerar_luneta.py
# 2. sons (precisa do .venv: python -m venv .venv; .venv\Scripts\python.exe -m pip install av==17.0.1 numpy==2.5.2)
.venv\Scripts\python.exe ferramentas\gerar_sons.py
# 3. compilar e testar (o jar sai em build\libs)
.\gradlew.bat build
# 4. prévia da mão e do inventário, sem abrir o jogo (ferramentas\saida\previa-*.png)
& "C:\Program Files\Blender Foundation\Blender 5.2\blender.exe" -b --factory-startup --python ferramentas\previa_mao.py
& "C:\Program Files\Blender Foundation\Blender 5.2\blender.exe" -b --factory-startup --python ferramentas\previa_mao.py -- deagle
# (ou o id de qualquer arma nova: -- ak47, -- sniper...)
```

O exportador termina com uma conferência (peça exportada + matriz do quadro contra o Blender; tem que dar
erro perto de 1e-6 bloco) e grava `ferramentas/exportacao.txt` (`exportacao-deagle.txt` na deagle). Os dois
exportadores dividem o `ferramentas/comum.py` (espaço do item, escrita do OBJ, exibição na mão, ícones); a saída do
revólver ficou igual byte a byte quando ele foi separado. Os testes (`.\gradlew.bat test`, 40) conferem:

- a lógica do tambor e a do pente;
- que os quadros do Java batem com os dos arquivos exportados;
- que todo modelo, textura, som, tradução e receita das duas armas existe e se lê.

**Instância de teste no Prism** (criada em 16/10/2026 a pedido dele): `%APPDATA%\PrismLauncher\instances\Revolver`, nome "Revólver (teste do mod)", só com este mod, base copiada da "Horizonte Infinito"
(Minecraft 1.21.1, NeoForge 21.1.251, Java do Prism, 2 a 6 GB) e o `options.txt` dele. Recompilou: copiar o
jar de `build\libs` para `minecraft\mods` de lá (e apagar o antigo se a versão mudar de nome).

**Também está na "Horizonte Infinito"** (o modpack dele, ~230 mods, mesma base), desde 17/10/2026, a
pedido dele ("coloca o mod no horizonte infinito"). Versão nova do mod: trocar o jar lá também (o antigo
sai). Fora isso, não mexer em nada dessa instância nem na "fancy craft".

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
  guardado em cada câmara: não reordenar; tipo novo entra no fim** (desde a 2.3.0 são 4 bits por lugar,
  cabem 16; ver "Tipos de mod" abaixo). O estado ganhou `tipos` e `tipos_novos` como campos **opcionais**: revólver salvo na
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
- **No inventário, ícone em pixel art; na mão, o 3D** (2.1.0, pedido dele: "na barra de itens o jogo fica
  renderizando os modelos, isso tá muito pesado"):
  - os três renderizadores, no contexto `GUI`, desenham só o modelo `item/icone/<arma>` (item/generated com a
    textura 32 x 32) e voltam;
  - por isso o `display.gui` de todos os itens é neutro (escala 1, sem giro nem deslocamento), e os três
    exportadores escrevem assim;
  - os ícones saem do `ferramentas/gerar_icones.py`: as peças exportadas fotografadas de lado em ortográfica com
    as cores do jogo, média por bloco até 32 x 32 (alfa > 0,3 para o cano fino não sumir), mais contraste e cor,
    chanfro (borda de cima clara, de baixo escura), 15 tons por canal e contorno escuro;
  - há teste (ícone, textura e gui neutro).
- **O jogo recebe uma versão leve do modelo** (2.2.0, pedido dele: "deixa eles mais low poly e diminui a
  resolução das texturas"):
  - o `comum.reduzir` (chamado pelo `escrever_obj`, vale para os três exportadores) reduz cada peça por colapso
    de arestas, tentando 25 %, 35 %, 50 % e 70 % dos triângulos e ficando com a primeira que não se afasta da
    original mais que `TOLERANCIA` (0,003 bloco = 0,75 mm) nos dois sentidos; peça pequena fica com no mínimo
    160 triângulos;
  - cortar a mesma fração de tudo, e juntar faces planas antes, destruía a mira da deagle e a bolinha do
    ferrolho da sniper: não voltar a isso;
  - texturas em `LADO_TEXTURA` (64 px), por média; os projetos `<arma>-3d` continuam com o detalhe todo;
  - de 503 mil para 134 mil triângulos nas oito armas, jar de 8,6 MB para 3,4 MB.
- **A descrição da arma diz o cartucho** (2.2.0): `item.revolver.arma.calibre` ("Munição: %s") com o nome do
  cartucho comum do calibre, logo depois da linha das balas, nos três itens.
- **Tipos de mod e Bandoleira do Fim** (2.3.0, pedido dele):
  - sete tipos depois do netherite no `TipoBala`, cada um com o mod e o item do material (ardente, insanium,
    ignitium, dracônico, matéria vermelha, antimatéria, infinito); os ids foram conferidos nos jars da Horizonte
    Infinito. As receitas (`ferramentas/gerar_tipos_mod.py`, que também escreve os nomes e o ícone da bandoleira)
    levam `neoforge:conditions` com `neoforge:mod_loaded`; o item é sempre registrado (arma carregada com ele não
    perde a bala), mas só entra na aba do criativo com o mod instalado;
  - **formato 4 bits:** o revólver e a deagle guardam o tipo em 4 bits por lugar. O codec dos dois estados lê
    `formato_tipos` (ausente = 1 = 2 bits, convertido por `Tambor.de2Para4Bits`) e grava sempre 2. Os testes não
    têm as classes do Minecraft, então só a conversão é testada, não o codec;
  - **infinito:** `Disparo.matar` (dano máximo; se o alvo continuar vivo, vida a zero e `die`; parte do dragão vira o
    dragão; jogador no criativo ou espectador é poupado); a bala não sai da arma no tiro (revólver não deflagra a
    câmara, deagle não sobe a próxima nem abre o ferrolho, armas novas não tiram da lista);
  - fogo no ardente (5 s) e no ignitium (8 s); explosão de 1,5 sem quebrar bloco no primeiro ponto atingido pela
    antimatéria;
  - **`Municao`** junta a fila da recarga das oito armas: outra mão e inventário como o arco; criativo completa
    com o comum; com a Bandoleira do Fim nada é gasto e a fila inteira é o melhor tipo do calibre que o jogador
    tiver;
  - as cores das pontas novas estão em `comum.TIPOS_MOD`; o ícone do infinito sai em arco-íris.
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
  - 7 no pente e 1 na câmara, com o tipo de cada bala em 4 bits (2 até a 2.2.0) e a de cima no lugar 0;
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
- **Tecla de recarregar e nome do mod:** a tecla recarrega a arma que estiver na mão (`RecarregarMsg` decide
  pelo item). **Desde a 2.3.0 é o Caps Lock**, não o R: na Horizonte Infinito o R recarrega os shaders (Iris) e
  todas as letras têm dono (conferido no `options.txt` dela; livres eram Caps Lock, mouse 5, `;`, `'`, `,`, `.`,
  `-`, `=`, Insert e afins). Mudar o padrão no código não muda quem já jogou: a tecla gravada fica no `options.txt`
  de cada instância (`key_key.revolver.recarregar`), e foi trocada lá nas duas. O
  nome do mod passou para "Revólver e Desert Eagle", e a categoria de teclas para "Armas".

## As seis armas novas (2.0.0, 16/10/2026)

Pedido dele: "agora faça uma metralhadora, uma 12, uma ak47, uma m4a4, uma m4a1 e uma sniper, faça um agente para
cada [...] cada uma deve ter a sua munição específica com o seu dano". O que ficou decidido:

- **Referências:** AK-47, M4A4 e M4A1-S seguem as do CS2. Metralhadora e 12 são desenho próprio, escolha dele (o CS2
  tem as duas, e ele foi avisado). A sniper é de desenho próprio, nada parecida com a AWP, pedido dele.
- **Munição:** as duas M4 dividem o 5,56 (escolha dele; a M4A1-S bate 10% mais pelo `fatorDano`).
- **Dano do cartucho comum:** a proposta aprovada, metralhadora 10, 12 com 8 bagos de 4, AK 12, M4A4 10, M4A1-S 11
  e sniper 50. Os tipos multiplicam como no revólver. Capacidades: 100, 8, 30, 30, 20 e 5.
- **Projetos:** cada modelo foi feito por uma sessão em paralelo, num projeto `<id>-3d` público. Todos seguem o
  mesmo contrato, escrito no `CLAUDE.md` de cada um:
  - o empty raiz com o id, na pegada da mão;
  - os nomes das peças (`ferrolho`, `alavanca`, `gatilho`, `pente` com `estojo_topo`/`bala_topo`, `botao_pente`,
    `clarao`, `estojo_ejetado`; na escopeta, `telha` e `cartucho_carga`);
  - as faixas `repouso`/`tiro`/`recarregar` (na escopeta, `inserir`);
  - o `modelo/animacoes.json` com início, quadros, eventos e peças.

  **Mexeu no contrato lá, quebra aqui.**
- **Uma classe para as seis:** `logica/Arma` é um enum com uma linha de dados por arma: calibre, capacidade, tubo
  ou pente, automática, quadros do tiro e da recarga, quadro do disparo e da troca, peças, espalhamento,
  `fatorDano`, zoom, som de tiro e sons por quadro. O teste confere tudo contra o `animacoes/<id>.json`.
  - `ArmaItem` faz a lógica e `ArmaRenderer` desenha.
  - O estado é o componente `estado_arma` (`EstadoArma`): a munição é uma lista com o tipo de cada bala, a próxima
    no lugar 0.
  - `logica/Carregador` tem a conta da recarga, testada.
- **Rajada:** as automáticas usam `startUsingItem` e atiram no `onUseTick`, uma vez por faixa de tiro. Com 3 ou 4
  quadros a 30 qps, dá um tiro a cada 2 ticks (~10 por segundo). As outras atiram no quadro `disparo` pelo
  `inventoryTick`, como a deagle.
- **Tiro comum** (`Disparo.atirar` com bagos e espalhamento): o dano de todos os bagos que acertam a mesma criatura
  é somado, e `invulnerableTime` é zerado antes do golpe. Sem isso a invencibilidade de 10 ticks do jogo engolia a
  rajada e os bagos. Vale para o revólver e a deagle também.
- **Escopeta:** a recarga é a faixa `inserir` repetida. Cada volta tira um cartucho do inventário no fim, põe no
  tubo e, se ainda cabe e tem cartucho, começa outra. O espalhamento é de 5°.
- **Luneta da sniper:** agachado em primeira pessoa com arma de `zoom` < 1, o campo de visão vai a 0,2. A imagem
  `textures/gui/luneta.png` (do `gerar_luneta.py`) é desenhada no meio da tela com preto dos lados, e a mira do
  jogo e a arma na mão somem (`RevolverCliente`: `zoom`, `luneta`, `semMira`, `semMao`).
- **Exportador comum** (`ferramentas/exportar_arma.py`, com o `comum.py`):
  - lê o contrato e tira as cores dos materiais do Blender, clareadas (brilho 0,24 + 1,4 v, até 0,92), e as
    imagens dos materiais, clareadas e reduzidas a 64 px;
  - o tamanho na mão sai do `CONFIG` (`"mao"` = comprimento na tela, em blocos);
  - a terceira pessoa usa escala 0,32, um pouco maior que o tamanho real;
  - o ícone de cada cartucho tem formato próprio (o de 12 tem o corpo vermelho de plástico e a ponta na cor do
    tipo).
- **Sons:** o tiro de cada uma é o `tiro.mp3` com outra cara (`tiro_variado` no `gerar_sons.py`); a M4A1-S é o tiro
  abafado. Bomba, tampa e ferrolho da sniper são sintetizados. Pente e ferrolho usam os sons da deagle, e o cartucho
  entrando na 12 usa o `bala_entra` do revólver.
- **Receitas:** cada arma tem um padrão próprio (a M4A1-S leva lã para o silenciador, e a sniper uma luneta). Os
  cartuchos de cada calibre têm padrão próprio, e os tipos melhores seguem a regra dos outros.

## O que ainda não foi visto

O revólver ele testou no jogo (as correções da 1.0.1 vieram do que ele viu). **A Desert Eagle e as seis armas
novas não foram abertas no Minecraft**: tamanho e ângulo na mão (só a prévia; as compridas usam a posição das
pistolas), a luz nas peças, os sons, a rajada, os bagos, a luneta, as recargas e o desempenho (o corpo da M4A4 tem
~79 mil triângulos). A terceira pessoa das duas armas foi conferida só na conta. Shaders (Iris/Oculus) não
testados.
