import java.util.List;
import java.util.Map;
import java.util.concurrent.atomic.AtomicBoolean;

import com.google.common.math.BigIntegerMath;

import java.math.BigInteger;
import java.util.ArrayList;
import java.util.HashMap;

import org.sat4j.core.VecInt;
import org.sat4j.specs.ContradictionException;
import org.sat4j.specs.IVecInt;
import org.sat4j.specs.TimeoutException;

import org.apache.commons.math3.fraction.BigFraction;

public class ProbabilitySolver extends Solver {

    /**
     * Constructor for ProbabilitySolver.
     * 
     * @param game the game that the solver is going to perform moves on.
     */
    public ProbabilitySolver(Minesweeper game) {
        super(game);
    }

    /**
     * Constructor for ProbabilitySolver. The solver can be stopped by changing the
     * passed running boolean to false.
     * 
     * @param game    the game that the solver is going to perform moves on.
     * @param running the value that controls whether the solver should
     *                continue/stop solving.
     */
    public ProbabilitySolver(Minesweeper game, AtomicBoolean running) {
        super(game, running);
    }

    /**
     * Perform the best move currently found on the board.
     */
    private void makeBestMove() {
        // Get a mapping of each cell to is probabiltiy of being a mine
        Map<Cell, BigFraction> probs = calcAllCellsProb();
        if (probs == null) {
            return;
        }
        // Get the cell with the best probabilitiy (takes strategy into account if more
        // than 1 cell has the lowest probabitliy).
        Cell bestCell = getBestMove(probs);
        if (bestCell == null) {
            return;
        }
        if (quiet) {
            game.quietProbe(bestCell.getX(), bestCell.getY());
        } else {
            String detail = "Strategically Selecting Cell " + bestCell + " with prob. "
                    + probs.get(bestCell).doubleValue();
            game.setDetail(detail);
            game.probe(bestCell.getX(), bestCell.getY());
        }
    }

    public boolean hint() {
        cells = game.getCells();
        Map<Cell, BigFraction> probs = calcAllCellsProb();
        if (probs == null) {
            return false;
        }
        List<Cell> bestProbCells = getBestProbCells(probs);
        if (bestProbCells.isEmpty()) {
            return false;
        }
        if (probs.get(bestProbCells.get(0)).doubleValue() == 0.0) {
            for (Cell c : bestProbCells) {
                if (!c.isSafeHint()) {
                    c.setSafeHint(true);
                    game.getHintCells().add(c);
                    game.refresh();
                    return true;
                }
            }
            return false;
        }
        List<Cell> bestCells = getBestStratCells(bestProbCells, probs);
        bestCells.removeIf(c -> c.isBestCell());
        if (bestCells == null || bestCells.isEmpty()) {
            return false;
        }
        Cell bestCell = getRandomCell(bestCells);
        bestCell.setProb(probs.get(bestCell).doubleValue());
        bestCell.setBestCell();
        game.refresh();
        return true;
    }

    public boolean assist() {
        cells = game.getCells();
        if (game.isGameOver()) {
            return false;
        }
        makeBestMove();
        // Return true as a move will always be made (as long as the game is not over)
        return true;
    }

    public void solve() {
        while (assist())
            ;
    }

    /**
     * Returns the cell with the best move. If more than one cell has the lowest
     * probabiltiy of being a mine then {@link #getBestStratCell(List, Map)
     * getBestStratCell} is then used.
     * 
     * @param probs a mapping of cells to their probabilties.
     * 
     * @return the cell that would be the best move to make.
     */
    private Cell getBestMove(Map<Cell, BigFraction> probs) {
        List<Cell> cells = getBestProbCells(probs);
        if (cells == null) {
            return null;
        }
        if (cells.size() == 1) {
            return cells.get(0);
        }
        Cell bestCell = getBestStratCell(cells, probs);
        if (bestCell == null) {
            return null;
        }
        return bestCell;
    }

    /**
     * Calculates each cells probabiltiy of being a mine. Esentially iterates
     * through all shore solutions, counting the occurances of each cell being a
     * mine and divides this by the total number of solutions. Uses combinatorics to
     * smallen the problem size by removing the need to include sea cells in the
     * problem set.
     * 
     * @return a mapping of cells to the probability value of being a mine.
     */
    private Map<Cell, BigFraction> calcAllCellsProb() {
        cells = game.getCells();
        // Key = Cell
        // Value = Cell appearance as mine count
        Map<Cell, BigInteger> cellT = new HashMap<>();

        // Key = Cell
        // Value = Cell's final probabiltiy of being a mine
        Map<Cell, BigFraction> probs = new HashMap<>();

        BigInteger T = BigInteger.ZERO;
        BigFraction seaT = BigFraction.ZERO;
        int totalMines = game.getNoOfMines();
        int seaSize = getSeaCells().size();

        try {
            genBinaryConstraints();
        } catch (ContradictionException e3) {
        }

        try {
            while (solver.isSatisfiable() && running.get()) {
                List<Cell> currentSol = new ArrayList<>();
                int[] model = solver.model();
                int noOfMines = 0;
                for (int i : model) {
                    boolean mine = i < 0 ? false : true;
                    Cell testForCell = decodeCellId(i);
                    if (testForCell != null && mine) {
                        currentSol.add(testForCell);
                        noOfMines++;
                    }
                }

                int remainingMines = totalMines - noOfMines;

                BigInteger toAdd = BigInteger.ONE;
                if (seaSize > 0) {
                    toAdd = BigIntegerMath.binomial(seaSize, remainingMines);
                }
                // Increment T
                T = T.add(toAdd);

                for (Cell c : currentSol) {
                    BigInteger testForNull = cellT.get(c);
                    if (testForNull == null) {
                        cellT.put(c, toAdd);
                    } else {
                        testForNull = testForNull.add(toAdd);
                        cellT.put(c, testForNull);
                    }
                }

                BigFraction seaFrac = BigFraction.ZERO;
                if (seaSize > 0) {
                    seaFrac = new BigFraction(remainingMines, seaSize);
                }
                BigFraction toAddFraction = new BigFraction(toAdd, BigInteger.ONE);
                BigFraction both = seaFrac.multiply(toAddFraction);

                seaT = seaT.add(both);

                // Find another solution
                for (int i = 0; i < model.length; i++) {
                    model[i] = model[i] * -1;
                }
                IVecInt block = new VecInt(model);

                solver.addBlockingClause(block);
            }
        } catch (TimeoutException e) {
            solver.reset();
        } catch (ContradictionException e) {
            solver.reset();
        }
        solver.reset();
        if (!running.get()) {
            return null;
        }

        if (seaSize > 0) {
            seaT = seaT.reduce();
            BigFraction TFrac = new BigFraction(T);
            BigFraction seaProb = seaT.divide(TFrac).reduce();
            List<Cell> seaCells = getSeaCells();
            for (Cell c : seaCells) {
                probs.put(c, seaProb);
            }
        }

        List<Cell> shore = getClosedShoreCells();
        for (Cell current : shore) {
            BigInteger currentCellT = cellT.get(current);
            // currentCellT is null when a cell does not appear in any solution
            // and is therefore safe, meaning 0.0 prob of being a mine
            if (currentCellT == null) {
                currentCellT = BigInteger.ZERO;
            }
            BigFraction cellProb = new BigFraction(currentCellT, T);
            probs.put(current, cellProb);
        }
        solver.reset();

        return probs;
    }

    /**
     * Display the probabiltiies of cells to the GUI.
     */
    public void displayProb() {
        Map<Cell, BigFraction> probs = calcAllCellsProb();
        if (probs == null) {
            return;
        }
        for (Map.Entry<Cell, BigFraction> pair : probs.entrySet()) {
            Cell current = pair.getKey();
            BigFraction prob = pair.getValue();

            current.setProb(prob.doubleValue());
        }

        List<Cell> bestProbCells = getBestProbCells(probs);
        for (Cell c : bestProbCells) {
            c.setBestCell();
        }
        game.refresh();
    }

    /**
     * Fetch a list of the cells with the lowest probability or being a mine.
     * 
     * @param probs a mapping of cells to their probabilitiy of being a mine.
     * 
     * @return a list of the cell(s) with the lowst probabilitiy of being a mine.
     */
    private List<Cell> getBestProbCells(Map<Cell, BigFraction> probs) {
        List<Cell> cellsWithBestProb = new ArrayList<>();

        if (probs == null) {
            return cellsWithBestProb;
        }

        for (Map.Entry<Cell, BigFraction> pair : probs.entrySet()) {
            Cell current = pair.getKey();
            BigFraction prob = pair.getValue();
            if (cellsWithBestProb.isEmpty()) {
                cellsWithBestProb.add(current);
                continue;
            }
            BigFraction currentBestProb = probs.get(cellsWithBestProb.get(0));
            int val = prob.compareTo(currentBestProb);
            if (val == 0) {
                cellsWithBestProb.add(current);
            } else if (val < 0) {
                cellsWithBestProb.clear();
                cellsWithBestProb.add(current);
            }
        }
        return cellsWithBestProb;
    }

    /**
     * Fetch the cells that have the best strategic value from a given list.
     * 
     * @param bestProbCells list of cells that have the lowest probabilities
     * @param probs         a mapping of cells to their probabilitiy of being a
     *                      mine.
     * 
     * @return a list of cells that have the best strategic value.
     */
    private List<Cell> getBestStratCells(List<Cell> bestProbCells, Map<Cell, BigFraction> probs) {
        if (bestProbCells.isEmpty()) {
            return null;
        }
        List<Cell> bestCells = new ArrayList<>();
        int lowestClosed = 9;
        for (Cell c : bestProbCells) {
            List<Cell> neighbours = getNeighbours(c);
            neighbours.removeIf(c2 -> c2.isOpen() || c2.isFlagged());
            int closedCount = neighbours.size();
            if (closedCount < lowestClosed) {
                lowestClosed = closedCount;
                bestCells.clear();
                bestCells.add(c);
            } else if (closedCount == lowestClosed) {
                bestCells.add(c);
            }
        }
        return bestCells;
    }

    /**
     * Fetch a single cell that has the best strategic value from a given list.
     * 
     * @param bestProbCells list of cells that have the lowest probabilities
     * @param probs         a mapping of cells to their probabilitiy of being a
     *                      mine.
     * 
     * @return a cell that has the best strategic value.
     */
    private Cell getBestStratCell(List<Cell> bestProbCells, Map<Cell, BigFraction> probs) {
        List<Cell> bestCells = getBestStratCells(bestProbCells, probs);
        if (bestCells.size() > 1) {
            return getRandomCell(bestCells);
        } else {
            return bestCells.get(0);
        }
    }

    /**
     * Generates the binary constraints for the problem set.
     * 
     * @param solver the solver to add the constraints to.
     * 
     * @throws ContradictionException when a contraint is added that directly
     *                                contradicts an already existing constraint.
     */
    private void genBinaryConstraints() throws ContradictionException {
        cells = game.getCells();
        List<Cell> closedShore = getClosedShoreCells();
        List<Cell> seaCells = getSeaCells();
        int seaSize = seaCells.size();
        List<Cell> landCells = getLandCells();

        int noOfMines = game.getNoOfMines();
        int noOfLits = Integer.toBinaryString(seaCells.size()).length();
        IVecInt lits = new VecInt();
        IVecInt coeffs = new VecInt();

        for (int i = 0; i < noOfLits; i++) {
            int square = (int) Math.pow(2, i);
            lits.push(encodeLit(i));
            coeffs.push(square);
        }
        solver.addAtMost(lits, coeffs, seaSize);

        for (int i = 0; i < closedShore.size() && running.get() && !Thread.interrupted(); i++) {
            Cell current = closedShore.get(i);
            lits.push(encodeCellId(current));
            coeffs.push(1);
        }
        solver.addExactly(lits, coeffs, noOfMines);

        lits.clear();
        coeffs.clear();

        IVecInt landLits = new VecInt();
        IVecInt landCoeffs = new VecInt();
        // Every open cell is guarenteed to not be a mine (cell=0)
        for (Cell current : landCells) {
            landLits.push(encodeCellId(current));
            landCoeffs.push(1);
            solver.addExactly(landLits, landCoeffs, 0);
            landLits.clear();
            landCoeffs.clear();
            List<Cell> neighbours = getNeighbours(current);
            for (Cell c : neighbours) {
                lits.push(encodeCellId(c));
                coeffs.push(1);
            }
            solver.addExactly(lits, coeffs, current.getNumber());
            lits.clear();
            coeffs.clear();
        }

        lits.clear();
        coeffs.clear();

        int limit = 0;
        for (Cell c : getOpenShoreCells()) {
            limit += c.getNumber();
        }

        for (Cell c : closedShore) {
            lits.push(encodeCellId(c));
            coeffs.push(1);
        }
        solver.addAtMost(lits, coeffs, limit);
        lits.clear();
        coeffs.clear();
    }
}