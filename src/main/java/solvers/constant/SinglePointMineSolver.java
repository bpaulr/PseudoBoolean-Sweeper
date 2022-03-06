package main.java.solvers.constant;

import main.java.game.Cell;
import main.java.game.CellState;
import main.java.solvers.SolverUtil;

import java.util.*;
import java.util.stream.Collectors;

/**
 * A class that uses the single point algorithm, see
 *
 * @author Bradley Read
 * @version 1.0
 * @since 2019-03-11
 */
public class SinglePointMineSolver implements IConstantMineSolver {
    private final Cell[][] cells;

    public SinglePointMineSolver(Cell[][] cells, int width, int height, int mines) {
        this.cells = cells;
    }

    public Map<Cell, Boolean> getKnownCells() {
        Map<Cell, Boolean> results = new HashMap<>();
        List<Cell> mineCells = getMineCells();
        for (Cell cell : mineCells) {
            results.put(cell, true);
        }
        List<Cell> safeCells = getSafeCells(new HashSet<>(mineCells));
        for (Cell cell : safeCells) {
            results.put(cell, false);
        }

        return results;
    }

    private List<Cell> getMineCells() {
        List<Cell> hasSurroundingMineCells = SolverUtil.filterCellStatesToStream(cells, CellState.OPEN)
                .filter(this::hasSinglePointMinePattern)
                .collect(Collectors.toList());

        return getNeighbouringClosedCells(hasSurroundingMineCells);
    }

    private List<Cell> getSafeCells(Set<Cell> knownMines) {
        List<Cell> hasSurroundingSafeCells = SolverUtil.filterCellStatesToStream(cells, CellState.OPEN)
                .filter(cell -> cell.getNumber() != 0)
                .filter(cell -> hasSinglePointSafePattern(cell, knownMines))
                .collect(Collectors.toList());

        return getNeighbouringClosedCells(hasSurroundingSafeCells).stream().filter(cell -> !knownMines.contains(cell)).collect(Collectors.toList());
    }

    private List<Cell> getNeighbouringClosedCells(List<Cell> knownCells) {
        Set<Cell> safeCells = new HashSet<>(); // filter out duplicates
        for (Cell cell : knownCells) {
            safeCells.addAll(
                    SolverUtil.getNeighbours(cells, cell.getX(), cell.getY()).stream()
                            .filter(c -> c.getState() != CellState.OPEN)
                            .collect(Collectors.toList())
            );
        }
        return new ArrayList<>(safeCells);
    }

    private boolean hasSinglePointSafePattern(Cell cell, Set<Cell> knownMines) {
        List<Cell> neighbours = SolverUtil.getNeighbours(cells, cell.getX(), cell.getY());
        int numOfKnownMines = (int) neighbours.stream().filter(c -> knownMines.contains(c)).count();
        return cell.getNumber() == numOfKnownMines;
    }

    private boolean hasSinglePointMinePattern(Cell cell) {
        int closedCount = (int) SolverUtil.getNeighbours(cells, cell.getX(), cell.getY()).stream()
                .filter(c -> c.getState() != CellState.OPEN)
                .count();
        return cell.getNumber() == closedCount;
    }
}