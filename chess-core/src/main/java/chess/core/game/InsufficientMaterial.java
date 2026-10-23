package chess.core.game;

import chess.core.model.Color;
import chess.core.model.PieceType;
import chess.core.model.Position;
import chess.core.standard.StandardPieces;

import java.util.List;
import java.util.Optional;
import java.util.Set;

/**
 * Condición de fin de partida que detecta las tablas por material insuficiente.
 *
 * <p>Se declara en tablas cuando ninguno de los dos bandos conserva pieza
 * capaz de dar mate: posición con los dos reyes y ninguna otra pieza
 * ("rey contra rey").
 *
 * <p>Solo se aplica cuando todas las piezas presentes pertenecen al ajedrez
 * clásico: en variantes con piezas propias (por ejemplo Capablanca) no puede
 * afirmarse que el material sea insuficiente, ya que esos tipos de piezas son
 * capaces de dar mate. Los finales de rey más un menor (rey y alfil o rey y
 * caballo contra rey) tampoco tablan automáticamente: se dejan en juego para
 * no interferir con otras condiciones, como la regla de los 50 movimientos,
 * que exige posiciones de ese tipo jugables.
 */
public final class InsufficientMaterial implements EndCondition {

    @Override
    public Optional<GameResult> evaluate(GameContext context) {
        Position position = context.position();

        if (!soloPiezasEstandar(position)) {
            return Optional.empty();
        }

        boolean sinPeones = position.pieces(StandardPieces.PAWN).isEmpty();
        boolean sinTorres = position.pieces(StandardPieces.ROOK).isEmpty();
        boolean sinDamas = position.pieces(StandardPieces.QUEEN).isEmpty();
        int menores = position.pieces(Color.WHITE, StandardPieces.KNIGHT).cardinality()
                + position.pieces(Color.BLACK, StandardPieces.KNIGHT).cardinality()
                + position.pieces(Color.WHITE, StandardPieces.BISHOP).cardinality()
                + position.pieces(Color.BLACK, StandardPieces.BISHOP).cardinality();

        if (sinPeones && sinTorres && sinDamas && menores == 0) {
            return Optional.of(GameResult.draw("Material insuficiente"));
        }

        return Optional.empty();
    }

    private boolean soloPiezasEstandar(Position position) {
        Set<PieceType> presentes = position.pieceTypes();
        List<PieceType> estandar = StandardPieces.ALL;
        return presentes.stream().allMatch(estandar::contains);
    }
}
