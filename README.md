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
- **Dano:** 4,5 corações, até 64 blocos.
- O texto do item mostra quantas balas tem no tambor.

Os dois itens ficam na aba **Combate** do criativo.

## Receitas

| Item | Receita |
|---|---|
| Revólver | três barras de ferro na linha de cima; no meio, um gancho de armadilha e uma tábua; embaixo à direita, outra tábua |
| 6 cartuchos .45 | pepita de ferro em cima, pólvora no meio, barra de cobre embaixo (em coluna) |

## Como instalar

Copie `build\libs\revolver-neoforge-1.21.1-1.0.0.jar` para a pasta `mods` da instância do Prism (a
"Horizonte Infinito" é NeoForge 1.21.1). Para gerar o jar: `.\gradlew.bat build` nesta pasta.

## Como mudar uma coisa

- **Posição do revólver na mão:** no `ferramentas\exportar_modelo.py`, procure `EXIBICAO = {` e mude os
  números da linha `firstperson_righthand` (primeira pessoa) ou `thirdperson_righthand` (terceira):
  `"escala"` é o tamanho e `"palma"` é onde o cabo cai (esquerda/direita, cima/baixo, perto/longe). Depois
  rode o exportador e o `gradlew build` (comandos no `CLAUDE.md`).
- **Dano e alcance:** no `src\main\java\dev\revolver\RevolverItem.java`, procure `ALCANCE = 64.0` e
  `DANO = 9.0f`.
- **Revólver mudou no `revolver-3d`:** rode o exportador de novo e o mod pega o modelo e a animação novos.

## Limites

Não foi testado dentro do jogo ainda: posição na mão, sons, tiro e recarga foram conferidos só por testes
e por prévia fora do jogo. Funciona só no NeoForge 1.21.1.
