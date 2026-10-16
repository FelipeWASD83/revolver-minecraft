# Revólver e Desert Eagle (mod de Minecraft)

Duas armas para Minecraft 1.21.1 com NeoForge, com o modelo e as animações feitos no Blender:

- **Revólver** de ação simples, estilo Colt, com cabo de nogueira (projeto `revolver-3d`). O cão arma, o tambor
  gira, o clarão sai na boca e o revólver dá o coice. Na recarga a portinhola abre e, câmara por câmara, o estojo
  vazio sai empurrado pela haste e entra uma bala nova.
- **Desert Eagle** .50 AE (projeto `deagle-3d`, a mesma arma do CS2). Semiautomática: o ferrolho recua, o estojo
  voa pela janela e o coice levanta a arma. No último tiro o ferrolho fica aberto. Na recarga o pente velho cai e o
  novo entra pelo cabo. Dá **1,5 vez o dano do revólver**.

## Revólver

- **Atirar:** botão direito. O polegar arma o cão primeiro (como num Colt de verdade), e o tiro sai logo
  depois. Sem bala na câmara, só o clique seco do cão.
- **Recarregar:** tecla **R** (dá para trocar em Opções > Controles > Revólver). Leva uns 11 segundos e
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
- **Recarregar:** a mesma tecla **R**. Troca o pente em uns 2,5 segundos. Aberta, leva uns 3 segundos, porque no
  fim o ferrolho bate para a frente e já põe uma bala na câmara. O pente novo leva as balas que sobraram no velho
  (elas saem primeiro) e completa com cartuchos .50 AE do inventário, na mesma ordem do revólver: outra mão
  primeiro, depois barra e inventário. No criativo não gasta cartucho.
- **Munição:** só cartucho .50 AE, e o revólver só aceita .45.
- **Alcance:** 64 blocos.
- O texto da deagle mostra quantas balas há no pente e na câmara, e de que tipo.

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

As duas armas e os oito cartuchos ficam na aba **Combate** do criativo.

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

## Como instalar

Já existe uma instância só com este mod no Prism, a **"Revólver (teste do mod)"** (Minecraft 1.21.1 com
NeoForge 21.1.251, a mesma base da "Horizonte Infinito", com as suas opções de vídeo e controles). É só
abrir o Prism e jogar nela.

Para pôr em outra instância NeoForge 1.21.1, copie o jar de `build\libs` (hoje `revolver-neoforge-1.21.1-1.2.1.jar`) para a
pasta `mods` dela. Para gerar o jar: `.\gradlew.bat build` nesta pasta. Depois de gerar de novo, copie o
jar de novo para a instância (o arquivo lá não se atualiza sozinho).

## Como mudar uma coisa

- **Posição na mão:** no `ferramentas\exportar_modelo.py` (revólver) ou `ferramentas\exportar_deagle.py`
  (deagle), procure `EXIBICAO = {` e mude os
  números da linha `firstperson_righthand` (primeira pessoa) ou `thirdperson_righthand` (terceira):
  `"escala"` é o tamanho e `"palma"` é onde o cabo cai (esquerda/direita, cima/baixo, perto/longe). Depois
  rode o exportador e o `gradlew build` (comandos no `CLAUDE.md`).
- **Dano de cada bala:** no `src\main\java\dev\revolver\logica\TipoBala.java`, o número com `f` no fim
  de cada linha (`COMUM(... 20f)`, `NETHERITE(... 200f)`). A deagle multiplica esse número: no
  `logica\Calibre.java`, `C50("cartucho_50", 1.5f)`. **Alcance:** no `Disparo.java`, procure `ALCANCE = 64.0`.
- **Sons:** saem dos arquivos em `ferramentas\sons-baixados`:
  - o `tiro.mp3` faz o tiro do revólver e, mais grave, o da deagle;
  - do `uzi-reload.mp3`, o primeiro som é a bala entrando no tambor, o segundo é o pente da deagle entrando e o
    terceiro é o ferrolho dela batendo.
 Para trocar, ponha outro arquivo lá com o mesmo nome e rode
  `.venv\Scripts\python.exe ferramentas\gerar_sons.py`. Esses arquivos ficam fora do GitHub; os sons
  prontos (os OGG do mod) vão junto no repositório.
- **Arma mudou no `revolver-3d` ou no `deagle-3d`:** rode o exportador dela de novo e o mod pega o modelo e a
  animação novos.

## Limites

O revólver você já testou no jogo (a 1.0.1 e a 1.1.0 saíram do que você viu lá). A Desert Eagle ainda não foi
aberta no jogo: posição na mão, sons, tiro e troca de pente foram conferidos só por testes e por prévia fora do
jogo. Funciona só no NeoForge 1.21.1.
