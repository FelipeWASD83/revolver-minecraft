# Changelog

## 2.3.0 (17/10/2026)

### Novo
- **Bandoleira do Fim:** com ela no inventário, recarregar não gasta cartucho. A arma enche com o melhor
  cartucho daquele calibre que você tiver (ou o comum, se não tiver nenhum). A receita leva o ovo do dragão, 4
  estrelas do Nether, 2 blocos de netherite, um totem da imortalidade e uma elytra.
- **Sete cartuchos com material de outro mod**, em todos os calibres, feitos com 8 cartuchos de netherite em
  volta do material: ardente (Twilight Forest, põe fogo), insanium (Mystical Agradditions), ignitium (Ender's
  Cataclysm, põe fogo), dracônico (Draconic Evolution), matéria vermelha (ProjectE), antimatéria (Mekanism,
  explode sem quebrar bloco) e infinito (Avaritia). Sem o mod, a receita não aparece e o cartucho sai da aba do
  criativo.
- **Cartucho infinito:** mata o que acertar, até chefe que limita o dano, e nunca acaba: atirar não gasta a
  bala. A receita dá um só. A ponta do ícone é arco-íris.

### Mudou
- A tecla de recarregar passou do R para o **Caps Lock**: no modpack o R recarrega os shaders, e nenhuma letra
  estava livre. Nas duas instâncias a tecla já foi trocada nas opções.
- O tipo de bala guardado no revólver e na Desert Eagle passou a caber 16 tipos. Arma salva antes é convertida
  sozinha ao carregar o mundo, sem perder as balas.

## 2.2.0 (17/10/2026)

### Mudou
- As armas ficaram bem mais leves: cerca de um quarto dos triângulos de antes (de 503 mil para 134 mil nas
  oito juntas) e texturas de 64 x 64 (eram 128, e as do revólver 1024). O jar caiu de 8,6 MB para 3,4 MB. Na mão
  a diferença quase não aparece: cada peça só é reduzida até onde a forma não muda mais que 0,75 mm.

### Novo
- A descrição de cada arma diz o cartucho que ela usa, logo abaixo da contagem de balas ("Munição: Cartucho .45").

## 2.1.0 (16/10/2026)

### Mudou
- Na barra de itens e no inventário as armas aparecem como ícones em pixel art (32 x 32, tirados do próprio
  modelo), em vez do modelo 3D animado, que pesava com várias armas na barra. Na mão e no chão continua o 3D.

## 2.0.1 (16/10/2026)

### Corrigido
- O jogo fechava ao abrir o inventário do criativo: os cartuchos 5,56, que a M4A4 e a M4A1-S dividem, entravam
  duas vezes na aba Combate.

## 2.0.0 (16/10/2026)

### Novo
- Seis armas novas, cada uma com o modelo e as animações do Blender: AK-47, M4A4 e M4A1-S (as do CS2),
  metralhadora, escopeta 12 e sniper .338 (desenho próprio).
- Rajada segurando o botão na AK, nas duas M4 e na metralhadora; a escopeta solta 8 bagos e recarrega
  cartucho por cartucho; a sniper tem luneta agachando.
- Cinco munições novas, cada uma com aço, diamante e netherite: 7,62x39, 5,56 (das duas M4), 7,62x51, calibre 12
  e .338 Lapua.
- Sons de tiro de cada arma feitos do seu tiro.mp3, e os da bomba da 12, da tampa da metralhadora e do
  ferrolho da sniper.

### Mudou
- O mod se chama Arsenal.
- Tiro que acerta o mesmo bicho logo em seguida conta inteiro (antes a invencibilidade de meio segundo do jogo
  engolia o segundo tiro).

## 1.2.1 (16/10/2026)

### Mudou
- A parte de cima da Desert Eagle ficou mais alta e mais grossa e afina só no topo, como a de verdade.

## 1.2.0 (16/10/2026)

### Novo
- Desert Eagle .50 AE, com o modelo e as animações do projeto deagle-3d. É semiautomática, leva 7 no pente
  e 1 na câmara, e o ferrolho fica aberto no último tiro. Troca o pente com a tecla R, e o pente velho cai.
- Quatro cartuchos .50 AE com 1,5 vez o dano dos .45: comum 30, aço 75, diamante 150 e netherite 300.
- Sons da deagle: o tiro mais grave e o pente saindo, entrando, a trava e o ferrolho.

### Mudou
- O mod se chama "Revólver e Desert Eagle", e a tecla R recarrega qualquer uma das duas armas.
- O revólver não aceita mais cartucho de outro calibre na recarga.

## 1.1.0 (16/10/2026)

### Novo
- Três cartuchos novos, cada um com seu dano: aço (50), diamante (100) e netherite (200); o cartucho
  .45 de antes continua dando 20. Cada um sai de 8 do anterior em volta de ferro, diamante ou netherite.
- A ponta da bala aparece na cor do tipo, no tambor e na recarga, e cada cartucho tem ícone próprio.
- A recarga pega primeiro os cartuchos da outra mão, como o arco faz com as flechas.
- O revólver mostra quantas balas de cada tipo tem; o cartucho mostra o dano.

### Corrigido
- O ícone do cartucho saía quadriculado.

## 1.0.1 (16/10/2026)

### Corrigido
- Cano, haste extratora, mira e parafusos apareciam quase invisíveis, pontilhados.
- O cabo podia pegar pedaço de outra textura nas bordas.

### Mudou
- Dano do tiro: de 9 para 20 (10 corações).
- Som do tiro e da bala entrando trocados pelos que você baixou; a bala toca uma vez por
  cartucho. Todos os sons mais altos.

## 1.0.0 (16/10/2026)

### Novo
- Revólver de ação simples (o modelo do `revolver-3d`) que atira com o botão direito e recarrega com a
  tecla R, com as animações de tiro e de recarregar feitas no Blender.
- Cartucho .45, com receita (6 por vez); receita do revólver.
- Sons de tiro, cão armando, clique seco, portinhola, estojo caindo, bala entrando e tambor girando.
- Fumaça na boca do cano e faísca onde a bala bate.
- Nomes em português e em inglês.
