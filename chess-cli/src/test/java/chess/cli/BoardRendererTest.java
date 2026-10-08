package chess.cli;

import chess.core.board.BoardGeometry;
import chess.core.model.Position;
import chess.core.standard.Fen;
import chess.core.standard.StandardChess;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;

class BoardRendererTest {

    private final BoardRenderer renderer = new BoardRenderer();

    @Test
    void rendersTheStartingPositionWithWhiteAtTheBottom() {
        String expected = String.join("\n",
                "  a b c d e f g h",
                "8 r n b q k b n r 8",
                "7 p p p p p p p p 7",
                "6 . . . . . . . . 6",
                "5 . . . . . . . . 5",
                "4 . . . . . . . . 4",
                "3 . . . . . . . . 3",
                "2 P P P P P P P P 2",
                "1 R N B Q K B N R 1",
                "  a b c d e f g h");
        assertEquals(expected, renderer.render(StandardChess.initialPosition()));
    }

    @Test
    void adaptsToOtherBoardSizes() {
        Position wide = Fen.standard().parse("4k5/P9/10/10/10/10/10/4K5 w - - 0 1");
        assertEquals(BoardGeometry.of(10, 8), wide.geometry());
        String[] lines = renderer.render(wide).split("\n");
        assertEquals(10, lines.length);
        assertEquals("  a b c d e f g h i j", lines[0]);
        assertEquals("8 . . . . k . . . . . 8", lines[1]);
        assertEquals("7 P . . . . . . . . . 7", lines[2]);
    }

    @Test
    void usesTwoDigitLabelsWhenThereAreTenOrMoreRanks() {
        Position tall = Fen.standard().parse("8/8/8/8/8/8/8/8/8/K7 w - - 0 1");
        String[] lines = renderer.render(tall).split("\n");
        assertEquals("   a b c d e f g h", lines[0]);
        assertEquals("10 . . . . . . . . 10", lines[1]);
        assertEquals(" 1 K . . . . . . .  1", lines[10]); // etiquetas alineadas a la derecha en ambos lados
    }
}
