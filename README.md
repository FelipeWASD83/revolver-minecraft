# Revólver (mod de Minecraft)

Um revólver de ação simples, estilo Colt, com cabo de nogueira, para Minecraft 1.21.1 com NeoForge. O
modelo e as animações são os do projeto `revolver-3d`, feitos no Blender: o cão arma, o tambor gira, o
clarão sai na boca e o revólver dá o coice; na recarga a portinhola abre e, câmara por câmara, o estojo
vazio sai empurrado pela haste e entra uma bala nova.

## Como usar no jogo

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

## Balas

| Cartucho | Dano | Ponta |
|---|---|---|
| Cartucho .45 | 20 (10 corações) | chumbo |
| Cartucho .45 de aço | 50 | aço |
| Cartucho .45 de diamante | 100 | diamante |
| Cartucho .45 de netherite | 200 | netherite (não queima na lava) |

O revólver e os quatro cartuchos ficam na aba **Combate** do criativo.

## Receitas

| Item | Receita |
|---|---|
| Revólver | três barras de ferro na linha de cima; no meio, um gancho de armadilha e uma tábua; embaixo à direita, outra tábua |
| 6 cartuchos .45 | pepita de ferro em cima, pólvora no meio, barra de cobre embaixo (em coluna) |
| 8 cartuchos de aço | 8 cartuchos .45 em volta de 1 barra de ferro |
| 8 cartuchos de diamante | 8 cartuchos de aço em volta de 1 diamante |
| 8 cartuchos de netherite | 8 cartuchos de diamante em volta de 1 barra de netherite |

## Como instalar

Já existe uma instância só com este mod no Prism: **"Revólver (teste do mod)"** (Minecraft 1.21.1 com
NeoForge 21.1.251, a mesma base da "Horizonte Infinito", com as suas opções de vídeo e controles). É só
abrir o Prism e jogar nela.

Para pôr em outra instância NeoForge 1.21.1, copie o jar de `build\libs` (hoje `revolver-neoforge-1.21.1-1.1.0.jar`) para a
pasta `mods` dela. Para gerar o jar: `.\gradlew.bat build` nesta pasta. Depois de gerar de novo, copie o
jar de novo para a instância (o arquivo lá não se atualiza sozinho).

## Como mudar uma coisa

- **Posição do revólver na mão:** no `ferramentas\exportar_modelo.py`, procure `EXIBICAO = {` e mude os
  números da linha `firstperson_righthand` (primeira pessoa) ou `thirdperson_righthand` (terceira):
  `"escala"` é o tamanho e `"palma"` é onde o cabo cai (esquerda/direita, cima/baixo, perto/longe). Depois
  rode o exportador e o `gradlew build` (comandos no `CLAUDE.md`).
- **Dano de cada bala:** no `src\main\java\dev\revolver\logica\TipoBala.java`, o número com `f` no fim
  de cada linha (`COMUM(... 20f)`, `NETHERITE(... 200f)`). **Alcance:** no `RevolverItem.java`, procure
  `ALCANCE = 64.0`.
- **Sons:** o tiro e a bala entrando saem dos arquivos em `ferramentas\sons-baixados` (`tiro.mp3` e o
  primeiro som do `uzi-reload.mp3`). Para trocar, ponha outro arquivo lá com o mesmo nome e rode
  `.venv\Scripts\python.exe ferramentas\gerar_sons.py`. Esses arquivos ficam fora do GitHub; os sons
  prontos (os OGG do mod) vão junto no repositório.
- **Revólver mudou no `revolver-3d`:** rode o exportador de novo e o mod pega o modelo e a animação novos.

## Limites

Não foi testado dentro do jogo ainda: posição na mão, sons, tiro e recarga foram conferidos só por testes
e por prévia fora do jogo. Funciona só no NeoForge 1.21.1.
