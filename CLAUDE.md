# CLAUDE.md: revolver-minecraft

Mod de Minecraft (NeoForge 1.21.1) com o revólver do projeto `revolver-3d`: atira, recarrega pela
portinhola e mostra as mesmas animações feitas no Blender. Criado em 16/10/2026 a pedido do Felipe
("faz um mod do minecraft com esse revolver"), com plano aprovado; repositório público (escolha dele).
Extras aprovados por ele: sons, receitas e partículas (o coice na câmera ele não quis). O `CLAUDE.md` da
raiz vale aqui também; o `README.md` explica o mod para ele. A pesquisa que decidiu a abordagem está em
`docs/pesquisa-mod-revolver-neoforge.md`.

## Como gerar, compilar e conferir

```powershell
# 1. modelo e animações (lê ..\revolver-3d\modelo\revolver.blend; não mexe nele)
& "C:\Program Files\Blender Foundation\Blender 5.2\blender.exe" -b ..\revolver-3d\modelo\revolver.blend --python ferramentas\exportar_modelo.py
# 2. sons (precisa do .venv: python -m venv .venv; .venv\Scripts\python.exe -m pip install av==17.0.1 numpy==2.5.2)
.venv\Scripts\python.exe ferramentas\gerar_sons.py
# 3. compilar e testar (o jar sai em build\libs)
.\gradlew.bat build
# 4. prévia da mão e do inventário, sem abrir o jogo (ferramentas\saida\previa-*.png)
& "C:\Program Files\Blender Foundation\Blender 5.2\blender.exe" -b --factory-startup --python ferramentas\previa_mao.py
```

O exportador termina com uma conferência (peça exportada + matriz do quadro contra o Blender; tem que dar
erro perto de 1e-6 bloco) e grava `ferramentas/exportacao.txt`. Os testes (`.\gradlew.bat test`, 19)
conferem a lógica do tambor, que os quadros do Java batem com os do arquivo exportado, e que todo modelo,
textura, som, tradução e receita existe e se lê.

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
  de ação simples, o polegar arma antes. Raio no servidor até 64 blocos, **dano 20** (pedido dele na
  1.0.1: 9 era pouco perto dos 8 da espada de netherite).
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

## O que ainda não foi visto

Nada disso foi testado dentro do Minecraft: tamanho e ângulo na mão, a luz do jogo nas peças, os sons, o
tiro acertando, a recarga e o desempenho com o modelo de ~46 mil triângulos (corpo de 27 mil). Shaders
(Iris/Oculus) não testados.
