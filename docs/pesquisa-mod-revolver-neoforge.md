# Pesquisa: mod de Minecraft com o revólver 3D
Data: 2026-10-16 · Projeto: revolver-minecraft

## Pergunta
Como pôr o revólver do `revolver-3d` (51 mil triângulos, peças separadas, animações de tiro e de
recarregar feitas no Blender) dentro do Minecraft como uma arma que atira e recarrega, animada.

## Recomendação
NeoForge 1.21.1 (21.1.252), a mesma base do `feitio` e da instância "Horizonte Infinito" dele.
O modelo entra como **OBJ pelo carregador do próprio NeoForge** (`neoforge:obj`), uma peça móvel por
arquivo (corpo, cão, gatilho, tambor, portinhola, botão da haste, estojo, bala, clarão). Um
**renderizador próprio do item** (BEWLR) desenha as peças com a posição de cada quadro, lida de um
arquivo de animação **exportado das próprias animações do Blender**, então o que se vê no jogo é o
mesmo tiro e a mesma recarga. O tiro é um raio calculado no servidor; munição e estado da animação
ficam em componentes de dados do item; a recarga vai por uma tecla com mensagem ao servidor. Os sons
são gerados em Python e codificados em OGG Vorbis com o PyAV 17.0.1.

## Candidatos avaliados

**NeoForge 1.21.1 com OBJ + BEWLR (escolhido).** O NeoForge tem carregador de OBJ embutido; o
`feitio` já compila e roda nessa versão na máquina dele (ModDevGradle 2.0.148, Java 21 baixado pelo
Gradle). Licença do NeoForge: LGPL-2.1, sem problema para mod pessoal.

**Fabric 1.21.11 (instância "fancy craft").** Não tem carregador de OBJ no Fabric API; precisaria de
biblioteca de terceiros ou de escrever um. Fica para depois, se ele quiser, como o `feitio` também
deixou para depois.

**Modelo em blocos (JSON do Blockbench).** É o formato normal de item, mas exigiria refazer o
revólver em caixas: perderia o modelo que ele aprovou. Descartado.

**GeckoLib.** Biblioteca de animação conhecida, mas pede modelo no formato do Blockbench e
dependência a mais na instância dele; o carregador de OBJ do NeoForge mais as curvas exportadas do
Blender resolvem sem nada extra.

## O que eu testei
- Código-fonte do carregador de OBJ do NeoForge na ramificação 1.21.1 (`ObjModel.java`,
  `ObjMaterialLibrary.java`), lido no repositório: o `Kd` do `.mtl` multiplica a cor de cada vértice
  (cor de peça sem textura funciona), `map_Kd` aceita `#nome` resolvido nas texturas do JSON,
  triângulos viram quadriláteros com o último vértice repetido (funciona), grupos `o`/`g` viram
  partes com nome, 1,0 = um bloco.
- Jar de recursos do Minecraft 1.21.1 (o do `feitio`): o escudo e o tridente usam
  `"parent": "builtin/entity"` para cair no renderizador próprio, e o atlas de blocos inclui toda a
  pasta `textures/item/` (textura do revólver entra sem registro extra).
- Código do `feitio` (que compila e roda aqui): mensagens com `RegisterPayloadHandlersEvent` e
  `StreamCodec`, teclas com `RegisterKeyMappingsEvent`.
- Som: forma de onda gerada com numpy, codificada em OGG Vorbis com o PyAV 17.0.1 (o do `zelador`,
  que passa pelo Smart App Control) com reamostragem para `fltp`: 0,600 s gerado, 0,597 s lido de
  volta. **O mixdown de áudio do Blender sem janela não serve**: gravou só 0,02 s.

## Riscos e o que pode dar errado
- **Não dá para conferir o visual sem abrir o jogo.** A regra dele é não abrir o jogo por conta
  própria; a compilação e os testes do código passam sem janela, mas tamanho na mão, ângulo e
  iluminação só se veem no Minecraft. Ajuste fino de posição na mão vai precisar de uma rodada com ele
  testando.
- **Peso:** o corpo tem ~30 mil triângulos. Para a RTX 3060 é pouco, mas cada revólver no inventário
  também desenha o modelo inteiro. Se pesar, existe o caminho de um ícone pronto só para o inventário.
- **Iluminação do Minecraft é chapada:** metal não brilha como no Cycles. Vai parecer mais "jogo".
- **Shaders (Iris/Oculus):** BEWLR com OBJ costuma funcionar, mas não testei com shader.
- Custo de trocar depois: baixo. Modelo e animação saem de um script; a lógica do jogo é separada.

## Confirmado / inferência / incerto
Confirmado: carregador `neoforge:obj` e seus campos (documentação 1.21.1 e código-fonte), `Kd`
como tinta, `#textura` pelo JSON, `builtin/entity`, atlas de itens, BEWLR por
`RegisterClientExtensionsEvent`, modelos avulsos por `ModelEvent.RegisterAdditional` com
`ModelResourceLocation.standalone`, mensagens e teclas (código do `feitio`), OGG pelo PyAV.
Inferência: buscar o modelo assado com `ModelManager#getModel` e desenhar com o `ItemRenderer`
dentro do BEWLR (é o caminho padrão, mas a página que falaria disso não cobre); raio de tiro com
`ProjectileUtil` e `Level#clip` (API do jogo, não testada aqui).
Incerto: desempenho real no jogo com 30 mil triângulos por item; comportamento com shaders.

## Fontes
- https://docs.neoforged.net/docs/1.21.1/resources/client/models/modelloaders (campos do `neoforge:obj`)
- https://github.com/neoforged/NeoForge/tree/1.21.1 , `client/model/obj/ObjModel.java` e
  `ObjMaterialLibrary.java` (como o OBJ e o MTL são lidos)
- https://docs.neoforged.net/docs/1.21.1/blockentities/ber (BEWLR e `RegisterClientExtensionsEvent`)
- https://docs.neoforged.net/docs/1.21.1/resources/client/models/ (modelos avulsos)
- `feitio/neoforge-1.21.1` (rede, teclas, versão do Gradle que funciona aqui)
