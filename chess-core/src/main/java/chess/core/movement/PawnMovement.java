package chess.core.movement;

import chess.core.board.Bitboard;
import chess.core.board.BoardGeometry;
import chess.core.board.Direction;
import chess.core.board.Square;
import chess.core.model.Color;
import chess.core.model.Move;
import chess.core.model.MovementRule;
import chess.core.model.PieceType;
import chess.core.model.Position;
import chess.core.move.DoublePushMove;
import chess.core.move.EnPassantMove;
import chess.core.move.PromotionMove;
import chess.core.move.StandardMove;

import java.util.List;
import java.util.function.Consumer;

public final class PawnMovement implements MovementRule {

    private final List<PieceType> promotionChoices;

    public PawnMovement(List<PieceType> promotionChoices) {
        if (promotionChoices.isEmpty()) {
            throw new IllegalArgumentException("Debe haber al menos una pieza de promoción");
        }
        this.promotionChoices = List.copyOf(promotionChoices);
    }

    @Override
    public Bitboard attacks(Position position, Square from, Color color) {
        BoardGeometry geometry = position.geometry();
        Bitboard origin = geometry.bit(from);
        int forward = color.forward();
        return geometry.shift(origin, new Direction(-1, forward))
                .or(geometry.shift(origin, new Direction(1, forward)));
    }

    @Override
    public void generate(
            Position position,
            Square from,
            Color color,
            Consumer<Move> out
    ) {
        BoardGeometry geometry = position.geometry();
        Bitboard origin = geometry.bit(from);
        Direction forward = new Direction(0, color.forward());
        Bitboard empty = geometry.complement(position.occupied());

        Bitboard oneStep = geometry.shift(origin, forward).and(empty);
        if (!oneStep.isEmpty()) {
            Square singleTarget = geometry.square(oneStep.lowestBit());
            advance(from, singleTarget, color, geometry, out);
            if (from.rank() == startRank(color, geometry)) {
                Bitboard twoSteps = geometry.shift(oneStep, forward).and(empty);
                if (!twoSteps.isEmpty()) {
                    out.accept(new DoublePushMove(
                            from,
                            geometry.square(twoSteps.lowestBit()),
                            singleTarget
                    ));
                }
            }
        }

        Bitboard diagonals = attacks(position, from, color);
        diagonals.and(position.occupiedBy(color.opposite()))
                .forEachBit(index -> advance(
                        from,
                        geometry.square(index),
                        color,
                        geometry,
                        out
                ));

        position.enPassantTarget().ifPresent(target -> {
            if (!geometry.contains(target)) return;
            if (!diagonals.has(geometry.index(target))) return;
            if (position.pieceAt(target).isPresent()) return;

            Square capturedPawnSquare = new Square(target.file(), from.rank());
            if (!geometry.contains(capturedPawnSquare)) return;

            boolean rivalPawnPresent = position.pieceAt(capturedPawnSquare)
                    .filter(piece -> piece.color() == color.opposite()
                            && piece.type().movement() instanceof PawnMovement)
                    .isPresent();

            if (rivalPawnPresent) {
                out.accept(new EnPassantMove(from, target, capturedPawnSquare));
            }
        });
    }

    private void advance(
            Square from,
            Square to,
            Color color,
            BoardGeometry geometry,
            Consumer<Move> out
    ) {
        if (to.rank() == promotionRank(color, geometry)) {
            for (PieceType choice : promotionChoices) {
                out.accept(new PromotionMove(from, to, choice));
            }
        } else {
            out.accept(new StandardMove(from, to));
        }
    }

    private static int startRank(Color color, BoardGeometry geometry) {
        return color == Color.WHITE ? 1 : geometry.ranks() - 2;
    }

    private static int promotionRank(Color color, BoardGeometry geometry) {
        return color == Color.WHITE ? geometry.ranks() - 1 : 0;
    }
}
