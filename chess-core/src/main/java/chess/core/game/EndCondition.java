package chess.core.game;

import java.util.Optional;

/**
 * Estrategia que decide si el estado actual de una partida determina su finalización
 * (jaque mate, ahogado y, en el futuro, regla de los 50 movimientos, repetición,
 * material insuficiente, etc.).
 *
 * <p>{@code Game} recibe una lista de estas condiciones: agregar una nueva regla
 * de fin de partida consiste en agregar una implementación de {@code EndCondition}
 * y registrarla, evitando concentrar todas las reglas de finalización dentro
 * de {@code Game}.
 *
 * <p>Cada condición recibe un {@link GameContext}, que agrupa la información
 * necesaria para evaluar el estado de la partida: la posición actual,
 * sus movimientos legales y el historial de posiciones.
 *
 * <p>Una condición utiliza únicamente la información que necesita. Por ejemplo,
 * jaque mate y ahogado utilizan la posición actual y los movimientos legales,
 * mientras que una condición basada en repetición podrá consultar también
 * el historial de posiciones.
 */
public interface EndCondition {

    /**
     * Evalúa si el contexto actual determina el final de la partida.
     *
     * @param context contexto actual de la partida, incluyendo posición,
     *                movimientos legales e historial de posiciones
     * @return el resultado de la partida si esta condición determina que terminó;
     *         vacío si la partida debe continuar
     */
    Optional<GameResult> evaluate(GameContext context);
}