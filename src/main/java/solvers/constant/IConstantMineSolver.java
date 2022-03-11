package main.java.solvers.constant;

import main.java.game.Cell;

import java.util.Map;

public interface IConstantMineSolver {
    /**
     * Return a mapping of cells to if they are a mine.
     * <p>
     * Only known cells are return.
     *
     * @return Mapping of cell to Boolean, true means mine and false means safe.
     */
    Map<Cell, Boolean> getKnownCells();
}
