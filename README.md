# Arsenal (mod de Minecraft)

Oito armas para Minecraft 1.21.1 com NeoForge, cada uma com o modelo e as animações feitos no Blender num projeto
próprio, e cada uma com a sua munição. As duas primeiras:

- **Revólver** de ação simples, estilo Colt, com cabo de nogueira (projeto `revolver-3d`). O cão arma, o tambor
  gira, o clarão sai na boca e o revólver dá o coice. Na recarga a portinhola abre e, câmara por câmara, o estojo
  vazio sai empurrado pela haste e entra uma bala nova.
- **Desert Eagle** .50 AE (projeto `deagle-3d`, a mesma arma do CS2). Semiautomática: o ferrolho recua, o estojo
  voa pela janela e o coice levanta a arma. No último tiro o ferrolho fica aberto. Na recarga o pente velho cai e o
  novo entra pelo cabo. Dá **1,5 vez o dano do revólver**.

E as seis da 2.0.0, cada uma com o próprio projeto de modelo:

| Arma | Projeto | Munição | Pente | Como atira |
|---|---|---|---|---|
| AK-47 (a do CS2) | `ak47-3d` | 7,62x39 | 30 | rajada enquanto segura o botão |
| M4A4 (a do CS2) | `m4a4-3d` | 5,56 | 30 | rajada |
| M4A1-S (a do CS2, com silenciador) | `m4a1-3d` | 5,56, a mesma da M4A4 | 20 | rajada, tiro abafado |
| Metralhadora (desenho próprio) | `metralhadora-3d` | 7,62x51 | caixa de 100 | rajada |
| Escopeta 12 de bomba (desenho próprio) | `escopeta-3d` | calibre 12 | 8 no tubo | um tiro por clique, 8 bagos, bombeia sozinha |
| Sniper .338 (desenho próprio, nada de AWP) | `sniper-3d` | .338 Lapua | 5 | um tiro por clique, puxa o ferrolho |

## Revólver

- **Atirar:** botão direito. O polegar arma o cão primeiro (como num Colt de verdade), e o tiro sai logo
  depois. Sem bala na câmara, só o clique seco do cão.
- **Recarregar:** tecla **Caps Lock** (era o R até a 2.3.0; dá para trocar em Opções > Controles > Armas). Leva uns 11 segundos e
  carrega uma bala por cartucho que você tiver; sem cartucho, só tira os estojos vazios. No modo criativo
  não gasta cartucho.
- **Qual bala entra:** como as flechas no arco, primeiro os cartuchos da outra mão, depois na ordem da
  barra e do inventário. Se uma pilha acabar no meio, o tambor fica misturado, e cada tiro usa o dano da
  bala daquela câmara.
- **Alcance:** 64 blocos.
- O texto do revólver mostra quantas balas tem e de que tipo; o do cartucho mostra o dano.

## Desert Eagle

- **Atirar:** botão direito. O tiro sai na hora, e dá para dar uns 2 por segundo.
- **Capacidade:** 7 no pente e 1 na câmara. Com o pente vazio, o último tiro deixa o ferrolho aberto. Aberta, o
  gatilho só dá clique.
- **Recarregar:** a mesma tecla **Caps Lock**. Troca o pente em uns 2,5 segundos. Aberta, leva uns 3 segundos, porque no
  fim o ferrolho bate para a frente e já põe uma bala na câmara. O pente novo leva as balas que sobraram no velho
  (elas saem primeiro) e completa com cartuchos .50 AE do inventário, na mesma ordem do revólver: outra mão
  primeiro, depois barra e inventário. No criativo não gasta cartucho.
- **Munição:** só cartucho .50 AE, e o revólver só aceita .45.
- **Alcance:** 64 blocos.
- O texto da deagle mostra quantas balas há no pente e na câmara, e de que tipo.

## As seis armas novas

- **Atirar:** botão direito. Na AK, nas duas M4 e na metralhadora, segure o botão para a rajada (uns 10 tiros por
  segundo). Na escopeta e na sniper é um tiro por clique, e a animação já bombeia ou puxa o ferrolho.
- **Recarregar:** a mesma tecla **Caps Lock**. A recarga troca o pente (ou a caixa da metralhadora); as balas que sobraram
  no pente velho saem primeiro. Na escopeta a recarga põe um cartucho por vez até encher o tubo ou acabar o
  cartucho. Como nas outras armas, os cartuchos saem primeiro da outra mão, e no criativo não gasta.
- **Luneta da sniper:** agache (Shift) com ela na mão. O zoom fecha, a imagem da luneta toma a tela e a arma some
  da mão.
- **Bagos da escopeta:** cada tiro solta 8 bagos que abrem um pouco; o dano dos que acertam o mesmo bicho soma.
- **Rajada que pega:** o jogo dá meio segundo de invencibilidade depois de um golpe; os tiros do mod zeram isso, senão
  só a primeira bala da rajada contaria.

## Balas

| Cartucho | Dano | Ponta |
|---|---|---|
| Cartucho .45 | 20 (10 corações) | chumbo |
| Cartucho .45 de aço | 50 | aço |
| Cartucho .45 de diamante | 100 | diamante |
| Cartucho .45 de netherite | 200 | netherite (não queima na lava) |

| Cartucho .50 AE | 30 (15 corações) | cobre |
| Cartucho .50 AE de aço | 75 | aço |
| Cartucho .50 AE de diamante | 150 | diamante |
| Cartucho .50 AE de netherite | 300 | netherite (não queima na lava) |

Os cartuchos das armas novas, no tipo comum (aço, diamante e netherite multiplicam por 2,5, 5 e 10, como os
outros):

| Cartucho | Arma | Dano |
|---|---|---|
| 7,62x39 | AK-47 | 12 |
| 5,56 | M4A4 | 10 |
| 5,56 | M4A1-S | 11 (ela bate um pouco mais, como no CS2) |
| 7,62x51 | Metralhadora | 10 |
| Calibre 12 | Escopeta | 4 por bago, 8 bagos (32 se todos acertarem) |
| .338 Lapua | Sniper | 50 (500 no de netherite) |

Os cartuchos com material de outro mod (2.3.0) existem em todos os calibres; o dano é o do .45 abaixo vezes o
multiplicador do calibre, como nos outros. Sem o mod do material, a receita não aparece e o cartucho sai da aba do
criativo.

| Tipo | Mod | Material | Dano no .45 | Extra |
|---|---|---|---|---|
| Ardente | Twilight Forest | barra ardente (fiery ingot) | 250 | fogo por 5 s |
| Insanium | Mystical Agradditions | barra de insanium | 300 | |
| Ignitium | L_Ender's Cataclysm | barra de ignitium | 350 | fogo por 8 s |
| Dracônico | Draconic Evolution | barra de draconium desperto | 500 | |
| Matéria vermelha | ProjectE | matéria vermelha | 600 | |
| Antimatéria | Mekanism | pelota de antimatéria | 800 | explode sem quebrar bloco |
| Infinito | Avaritia | barra infinita | infinito | mata o que acertar e nunca acaba |

## Bandoleira do Fim

Com a bandoleira em qualquer lugar do inventário, recarregar não gasta cartucho: a arma enche com o melhor cartucho
daquele calibre que você tiver (um basta), ou com o comum se não tiver nenhum. Receita: ovo do dragão no centro,
estrela do Nether nos quatro cantos, bloco de netherite dos lados, totem da imortalidade em cima e elytra embaixo. O
ovo do dragão só existe um por mundo.

As oito armas, a bandoleira e os cartuchos ficam na aba **Combate** do criativo.

## Receitas

| Item | Receita |
|---|---|
| Revólver | três barras de ferro na linha de cima; no meio, um gancho de armadilha e uma tábua; embaixo à direita, outra tábua |
| 6 cartuchos .45 | pepita de ferro em cima, pólvora no meio, barra de cobre embaixo (em coluna) |
| 8 cartuchos de aço | 8 cartuchos .45 em volta de 1 barra de ferro |
| 8 cartuchos de diamante | 8 cartuchos de aço em volta de 1 diamante |
| 8 cartuchos de netherite | 8 cartuchos de diamante em volta de 1 barra de netherite |
| Desert Eagle | três barras de ferro em cima; no meio, um bloco de ferro, um gancho de armadilha e uma barra de ferro; embaixo à direita, um corante preto |
| 4 cartuchos .50 AE | pólvora, pepita de ferro e pólvora em cima; barra de cobre embaixo, no meio |
| 8 cartuchos .50 AE de aço, diamante e netherite | como os .45: 8 do anterior em volta de barra de ferro, diamante ou barra de netherite |
| AK-47 | três barras de ferro em cima; no meio, tábua, gancho de armadilha e tábua; embaixo à esquerda, tábua |
| M4A4 | três barras de ferro em cima; no meio, corante cinza, gancho de armadilha e barra de ferro; embaixo à esquerda, corante cinza |
| M4A1-S | lã, barra de ferro e barra de ferro em cima; o resto igual à M4A4 |
| Metralhadora | bloco de ferro e duas barras em cima; corante cinza, gancho e barra no meio; bloco de ferro e corante cinza embaixo |
| Escopeta | três barras de ferro em cima; tábua, gancho e barra no meio; duas tábuas embaixo |
| Sniper | luneta e duas barras de ferro em cima; tábua, gancho e barra no meio; tábua embaixo à esquerda |
| 8 cartuchos 7,62x39 | pepita, pólvora e pepita em cima; barra de cobre embaixo, no meio |
| 8 cartuchos 5,56 | pepita e pepita nas pontas de cima; pólvora no meio; barra de cobre embaixo (coluna do meio) |
| 8 cartuchos 7,62x51 | pepita, pólvora e pepita em cima; pólvora, barra de cobre e pólvora embaixo |
| 6 cartuchos calibre 12 | três pepitas em cima; papel, pólvora e papel no meio; barra de cobre embaixo |
| 4 cartuchos .338 | pólvora, pepita e pólvora em cima; pólvora, barra de cobre e pólvora embaixo |
| Cartuchos de aço, diamante e netherite das armas novas | como os outros: 8 do anterior em volta de barra de ferro, diamante ou barra de netherite |
| 8 cartuchos com material de mod (1 no infinito) | 8 cartuchos de netherite do mesmo calibre em volta do material (tabela em Balas) |
| Bandoleira do Fim | ver acima |

## Como instalar

Já existe uma instância só com este mod no Prism, a **"Revólver (teste do mod)"** (Minecraft 1.21.1 com
NeoForge 21.1.251, a mesma base da "Horizonte Infinito", com as suas opções de vídeo e controles). É só
abrir o Prism e jogar nela.

Para pôr em outra instância NeoForge 1.21.1, copie o jar de `build\libs` (hoje `revolver-neoforge-1.21.1-2.3.0.jar`) para a
pasta `mods` dela. Para gerar o jar: `.\gradlew.bat build` nesta pasta. Depois de gerar de novo, copie o
jar de novo para a instância (o arquivo lá não se atualiza sozinho).

## Como mudar uma coisa

- **Posição na mão das armas novas:** no `ferramentas\exportar_arma.py`, procure `CONFIG = {`: o número `"mao"`
  de cada arma é o comprimento dela na tela, em blocos. Depois rode o exportador dela e o `gradlew build`.
- **Posição na mão:** no `ferramentas\exportar_modelo.py` (revólver) ou `ferramentas\exportar_deagle.py`
  (deagle), procure `EXIBICAO = {` e mude os
  números da linha `firstperson_righthand` (primeira pessoa) ou `thirdperson_righthand` (terceira):
  `"escala"` é o tamanho e `"palma"` é onde o cabo cai (esquerda/direita, cima/baixo, perto/longe). Depois
  rode o exportador e o `gradlew build` (comandos no `CLAUDE.md`).
- **Dano de cada bala:** no `src\main\java\dev\revolver\logica\TipoBala.java`, o número com `f` no fim
  de cada linha (`COMUM(... 20f)`, `NETHERITE(... 200f)`). A deagle multiplica esse número: no
  `logica\Calibre.java`, `C50("cartucho_50", 1.5f)`; as armas novas também (`C762X39("cartucho_762x39", 0.6f, 1)`
  é 0,6 vez o .45 = 12). Capacidade, rajada e zoom de cada arma nova ficam no `logica\Arma.java`, uma linha por
  arma. **Alcance:** no `Disparo.java`, procure `ALCANCE = 64.0`.
- **Sons:** saem dos arquivos em `ferramentas\sons-baixados`:
  - o `tiro.mp3` faz o tiro do revólver e, com outra cara, o de todas as outras (mais grave na deagle, na 12 e na
    sniper; abafado na M4A1-S);
  - do `uzi-reload.mp3`, o primeiro som é a bala entrando no tambor, o segundo é o pente da deagle entrando e o
    terceiro é o ferrolho dela batendo.

  Para trocar, ponha outro arquivo lá com o mesmo nome e rode
  `.venv\Scripts\python.exe ferramentas\gerar_sons.py`. Esses arquivos ficam fora do GitHub; os sons
  prontos (os OGG do mod) vão junto no repositório.
- **Ícones do inventário:** são pixel art de 32 x 32 tirados do próprio modelo (de lado, com poucos tons e o
  contorno escuro). Mudou o modelo, rode `blender -b --factory-startup --python ferramentas\gerar_icones.py`
  (depois do exportador); a barra de itens de mentira fica em `ferramentas\saida\icones-barra.png`. Para desenhar
  um à mão, é só trocar o PNG em `src\main\resources\assets\revolver\textures\item\icone\`.
- **Arma mudou no projeto dela (`revolver-3d`, `deagle-3d`, `ak47-3d`...):** rode o exportador dela de novo e o
  mod pega o modelo e a animação novos (comandos no `CLAUDE.md`).

## Limites

O revólver você já testou no jogo (a 1.0.1 e a 1.1.0 saíram do que você viu lá). A Desert Eagle e as seis armas
novas ainda não foram abertas no jogo: posição na mão, sons, rajada, bagos, luneta e recargas foram conferidos só
por testes e por prévia fora do jogo. As armas compridas usam a mesma posição na mão das pistolas, só que maiores;
se ficarem estranhas, é o `"mao"` do `exportar_arma.py`. Funciona só no NeoForge 1.21.1.
