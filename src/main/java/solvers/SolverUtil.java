package main.java.solvers;

import main.java.game.Cell;
import main.java.game.CellState;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Optional;
import java.util.stream.Collectors;
import java.util.stream.Stream;

public class SolverUtil {

    public static Stream<Cell> cellMatrixToStream(Cell[][] cells) {
        return Arrays.stream(cells)
                .flatMap(Arrays::stream);
    }

    /**
     * When passed a cell, create a unique identifier (a single integer) for that
     * cell. To be used for creating literals.
     *
     * @param c cell to encode.
     * @return a unique integer identifier for given cell.
     */
    public static int encodeCellId(Cell c, int width) {
        return (c.getY() * width + c.getX()) + 1;
    }

    /**
     * Encodes a literal so that it does not collide with any cell literals.
     *
     * @param lit the ith literal wanting to be encoded.
     * @return an encoded literal.
     */
    public static int encodeLit(int lit, int height, int width) {
        return (height * width) + width + lit;
    }

    public static Stream<Cell> filterCellStatesToStream(Cell[][] cells, CellState state) {
        return cellMatrixToStream(cells)
                .filter(cell -> cell.getState() == state);
    }

    /**
     * When passed an identity, decode and return the cell it is referring to.
     *
     * @param id Unique encoded identity id literal.
     * @return the cell that the id refers to. Null if it is impossible for the
     * passed id to be a cell.
     */
    public static Optional<Cell> decodeCellId(Cell[][] cells, int id, int height, int width) {
        int posId = id < 0 ? id * -1 : id;
        if (posId > ((height - 1) * width + (width - 1)) + 1) {
            return Optional.empty();
        }
        int x = (posId - 1) % width;
        int y = ((posId - 1) - x) / width;
        return Optional.of(cells[x][y]);
    }

    public static List<Cell> getNeighbours(Cell[][] cells, int x, int y) {
        final int width = cells.length;
        final int height = cells[0].length;
        List<Cell> neighbours = new ArrayList<>();
        for (int i = x - 1; i <= x + 1; i++) {
            for (int j = y - 1; j <= y + 1; j++) {
                if (i >= 0 && i < width && j >= 0 && j < height && !(i == x && j == y)) {
                    neighbours.add(cells[i][j]);
                }
            }
        }
        return neighbours;
    }

    /**
     * Return a list of all the games land cells. Note: a land cell is a cell that
     * has been probed (is open).
     *
     * @return a list of cells that are classed as land cells.
     */
    public static List<Cell> getLandCells(Cell[][] cells) {
        return cellMatrixToStream(cells)
                .filter(cell -> cell.getState() == CellState.OPEN)
                .collect(Collectors.toList());
    }

    /**
     * Return a list of all the games sea cells. Note: a sea cell is a cell that has
     * not been probed and does not touch an open cell.
     *
     * @return a list of cells that are classed as sea cells.
     */
    public static List<Cell> getSeaCells(Cell[][] cells) {
        return cellMatrixToStream(cells)
                .filter(cell -> cell.getState() != CellState.OPEN)
                .filter(cell -> ((int) getNeighbours(cells, cell.getX(), cell.getY()).stream()
                        .filter(c -> c.getState() == CellState.OPEN)
                        .limit(1)
                        .count()) == 0)
                .collect(Collectors.toList());
    }

    /**
     * Return a list of all the games closed shore cells. Note: a "closed" shore
     * cell is a cell that has not been probed (is closed) and touches both a land
     * cell and a sea cell.
     *
     * @return a list of cells that are classed as closed shore cells.
     */
    public static List<Cell> getClosedShoreCells(Cell[][] cells) {
        return cellMatrixToStream(cells)
                .filter(cell -> cell.getState() != CellState.OPEN)
                .filter(cell -> {
                    List<Cell> neighbours = getNeighbours(cells, cell.getX(), cell.getY());
                    return ((int) neighbours.stream()
                            .filter(c -> c.getState() == CellState.OPEN)
                            .limit(1)
                            .count()) != 0;
                })
                .collect(Collectors.toList());
    }

    /**
     * Return a list of all the games open sore cells. Note: a "open" shore cell is
     * a cell that has has been probed (is open) and touches a closed cell.
     *
     * @return a list of cells that are classed as open shore cells.
     */
    public List<Cell> getOpenShoreCells(Cell[][] cells) {
        return cellMatrixToStream(cells)
                .filter(cell -> cell.getState() != CellState.OPEN)
                .filter(cell -> {
                    List<Cell> neighbours = getNeighbours(cells, cell.getX(), cell.getY());
                    return ((int) neighbours.stream()
                            .filter(c -> c.getState() != CellState.OPEN)
                            .limit(1)
                            .count()) != 0;
                })
                .collect(Collectors.toList());
    }

}