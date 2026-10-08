# Ajedrez con bitboards — TPO Ingeniería de Software (UADE)

Implementación de ajedrez en Java 17 con una arquitectura pensada para **crecer sin modificar
código existente**: el núcleo no depende de ningún framework, se prueba solo con tests
unitarios y la consola es apenas un adaptador.

## Alcance

| Consigna | Estado |
|---|---|
| Tablero 8x8, 6 piezas con su movimiento, turnos alternados, captura | ✅ |
| Validación de movimientos inválidos, detección de jaque | ✅ |
| Jaque mate, enroque, captura al paso, promoción, rey ahogado (tablas) | ✅ |
| Notación FEN (leer/escribir posiciones) | ✅ |
| Tableros de otro tamaño y piezas nuevas sin tocar el núcleo | ✅ (ver `ExtensibilityTest`) |
| Interfaz de consola | ✅ |
| Deshacer/rehacer, IA, GUI | ❌ no implementado |
| Tablas por regla de 50 movimientos, repetición o material insuficiente | ❌ no implementado (el FEN emite `0 1`) |

## Requisitos

JDK 17 o superior. Maven 3.9+ es opcional (hay un script que compila sin Maven).

## Cómo correrlo

```bash
# Con Maven: compila, corre TODOS los tests y arma el jar
mvn verify
java -jar chess-cli/target/ajedrez.jar
java -jar chess-cli/target/ajedrez.jar --fen "4k3/8/8/8/8/8/4P3/4K3 w - - 0 1"

# Sin Maven (solo JDK)
./run.sh            # Linux / macOS
run.bat             # Windows
```

Desde el IDE (Eclipse o IntelliJ): importá la carpeta como proyecto Maven existente, ejecutá `chess.cli.ConsoleApp` como aplicación Java y corré los tests con JUnit sobre `chess-core/src/test` y `chess-cli/src/test`. Tras cambiar un `pom.xml` en Eclipse: botón derecho sobre el proyecto → Maven → Update Project.

Comandos dentro de la partida: `e2e4` (también `e2 e4` o `e2-e4`), `e7e8q` para promocionar,
`jugadas [casilla]`, `tablero`, `fen`, `ayuda`, `salir`. El enroque se juega moviendo el rey
dos casillas (`e1g1`).

> En consolas de Windows con otra codificación los acentos pueden verse mal; `run.bat` ejecuta
> `chcp 65001` para evitarlo.

## Estructura

```
chess-core/   núcleo: reglas, tablero, partida. CERO dependencias de compilación
chess-cli/    adaptador de consola; depende del núcleo, nunca al revés
docs/uml/     diagramas PlantUML (.puml) + imágenes (.png/.svg)
```

Paquetes del núcleo (`chess.core`):

| Paquete | Qué hay |
|---|---|
| `board` | `Bitboard`, `BoardGeometry` (índices, shifts, rayos), `Square`, `Direction` |
| `model` | `Position` (inmutable), `PieceType`, `Piece`, `Color`, `Move`, `MovementRule` |
| `movement` | Reglas de movimiento: `SlidingMovement`, `LeapingMovement`, `CompositeMovement`, `PawnMovement`, `CastlingMovement` |
| `move` | Tipos de jugada: `StandardMove`, `DoublePushMove`, `EnPassantMove`, `PromotionMove`, `CastlingMove` |
| `game` | `Game`, `MoveGenerator`, `EndCondition` (`Checkmate`, `Stalemate`), `GameListener` |
| `standard` | Armado del ajedrez clásico: `StandardPieces`, `StandardChess`, `Fen` |

### Ideas centrales

- **Bitboard**: cada conjunto de casillas es una máscara de bits. Las piezas deslizantes usan
  rayos precalculados y detección del primer bloqueo; los saltadores usan shifts con máscaras de
  columna (sin "dar la vuelta" al tablero). Detalle: el almacenamiento es un `BigInteger`
  escondido dentro de `Bitboard`, para no limitar el tablero a 64 casillas.
- **Una pieza es un dato, no una clase por pieza**: `PieceType(nombre, símbolo, movimiento, esRey)`.
  Agregar una pieza es componer reglas existentes; no se modifica ninguna clase.
- **Patrones**: Strategy (`MovementRule`, `EndCondition`, `MoveGenerator`), Composite
  (`CompositeMovement`: reina = torre + alfil), Command (`Move`), Observer (`GameListener`),
  Builder (`Position.Builder`), objetos inmutables e inyección por constructor.
- **Legalidad**: se generan movimientos pseudo-legales y se descartan los que dejan al propio rey
  en jaque (`position.play(move).isInCheck(mover)`).

## Cómo agregar una pieza (sin tocar el núcleo)

```java
// Canciller = torre + caballo
PieceType chancellor = new PieceType("Canciller", 'C',
        new CompositeMovement(StandardPieces.ROOK.movement(), StandardPieces.KNIGHT.movement()));

// Camello: saltador (1,3)
PieceType camel = new PieceType("Camello", 'M',
        new LeapingMovement(Direction.symmetries(1, 3)));
```

Para jugar con ellas se las pasa al `Fen` y a la `Position` (ejemplos completos, incluida una
variante tipo Capablanca en 10x8 y una condición de fin de partida propia, en
`chess-core/src/test/java/chess/core/extension/ExtensibilityTest.java`).

## Tests

- `mvn verify` corre todo. Hay 159 tests en el núcleo y 20 en la consola.
- **Perft**: cuenta nodos de movimientos hasta cierta profundidad y se compara con valores de
  referencia públicos. Además se comparó contra `python-chess` en 66.720 posiciones aleatorias
  (misma lista de movimientos legales en todas).
- `CoreIndependenceTest`: falla si algún archivo del núcleo importa algo fuera de `java.*`/`chess.core`.
- `UmlConsistencyTest` (en `chess-cli`): **falla si el UML y el código se desalinean** (clases,
  tipo, atributos, métodos, herencia, anidamiento y flechas de dependencia contra el bytecode).
  Si cambiás el código, actualizá los `.puml` y volvé a generar las imágenes.

Cobertura (opcional): `mvn -pl chess-core -Pcoverage verify` genera el reporte JaCoCo en
`chess-core/target/site/jacoco`.

## Regenerar los diagramas

```bash
java -jar plantuml.jar -tpng -tsvg docs/uml/0*.puml
```

## Límites conocidos

- Mensajes de error y salida de consola están en español dentro del núcleo; internacionalizar
  implicaría sacar los textos del núcleo.
- `Bitboard` con `BigInteger` es unas 2,5 a 4 veces más lento que una versión con `long`
  (medido; ver la justificación). Alcanza para una partida interactiva, no para un motor de IA.
- El FEN no guarda contadores de medio-movimiento ni de jugadas.
