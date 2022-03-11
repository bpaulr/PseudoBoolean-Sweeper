package main.java.solvers.strategic;

import main.java.game.Cell;
import main.java.game.CellState;
import main.java.solvers.SolverUtil;

import java.util.List;
import java.util.Optional;
import java.util.stream.Collectors;

public class LeastNeighboursSolver implements IStrategicSolver {

    private final Cell[][] cells;
    private final int width;
    private final int height;
    private final int mines;

    public LeastNeighboursSolver(Cell[][] cells, int width, int height, int mines) {
        this.cells = cells;
        this.width = width;
        this.height = height;
        this.mines = mines;
    }

    @Override
    public Cell getBestMove(List<Cell> closedCells) {
        if (closedCells.size() < 1) {
            throw new IllegalArgumentException("There must be at least 1 cell in the given list.");
        }

        closedCells = closedCells.stream()
                .filter(cell -> cell.getState() != CellState.OPEN)
                .collect(Collectors.toList());

        Cell bestStrategicCell = null;
        int leastUnknownNeighbours = Integer.MAX_VALUE;

        for (int i = 1; i < closedCells.size(); i++) {
            Cell cell = closedCells.get(i);
            int unknownNeighbours = (int) SolverUtil.getNeighbours(cells, cell.getX(), cell.getY())
                    .stream()
                    .filter(c -> c.getState() != CellState.OPEN)
                    .count();
            if (unknownNeighbours < leastUnknownNeighbours) {
                bestStrategicCell = cell;
                leastUnknownNeighbours = unknownNeighbours;
            }
        }
        return bestStrategicCell;
    }
}
