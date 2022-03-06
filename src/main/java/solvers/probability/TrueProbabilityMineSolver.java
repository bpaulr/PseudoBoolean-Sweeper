package main.java.solvers.probability;

import com.google.common.math.BigIntegerMath;
import main.java.game.Cell;
import main.java.game.CellState;
import main.java.solvers.constraints.IPBConstraintGenerator;
import main.java.solvers.constraints.PBConstraintGeneratorOpenCells;
import main.java.solvers.constraints.PBConstraintGeneratorSea;
import main.java.solvers.SolverUtil;
import org.apache.commons.math3.fraction.BigFraction;
import org.sat4j.core.VecInt;
import org.sat4j.pb.SolverFactory;
import org.sat4j.pb.core.PBSolver;
import org.sat4j.specs.ContradictionException;
import org.sat4j.specs.IVecInt;
import org.sat4j.specs.TimeoutException;

import java.math.BigInteger;
import java.util.*;
import java.util.stream.Collectors;

public class TrueProbabilityMineSolver implements IProbabilityMineSolver {

    private final Cell[][] cells;
    private final int width;
    private final int height;
    private final int mines;

    public TrueProbabilityMineSolver(Cell[][] cells, int width, int height, int mines) {
        this.cells = cells;
        this.width = width;
        this.height = height;
        this.mines = mines;
    }

    public Cell getBestSafeProbabilityCell() {
        var probabilities = getProbabilities();
        List<Cell> lowestProbCells = new ArrayList<>();
        BigFraction bestProb = BigFraction.ONE;
        for (var pair : probabilities.entrySet()) {
            Cell cell = pair.getKey();
            BigFraction prob = pair.getValue();
            int comp = prob.compareTo(bestProb);
            if (comp < 0) {
                lowestProbCells.clear();
                bestProb = prob;
                lowestProbCells.add(cell);
            } else if (comp == 0) {
                lowestProbCells.add(cell);
            }
        }

        if (lowestProbCells.size() == 1) {
            return lowestProbCells.get(0);
        }

        Cell bestStrategicCell = lowestProbCells.get(0);
        int leastUnknownNeighbours = (int) SolverUtil.getNeighbours(cells, bestStrategicCell.getX(), bestStrategicCell.getY())
                .stream()
                .filter(c -> c.getState() == CellState.CLOSED)
                .count();

        for (int i = 1; i < lowestProbCells.size(); i++) {
            Cell cell = lowestProbCells.get(i);
            int unknownNeighbours = (int) SolverUtil.getNeighbours(cells, cell.getX(), cell.getY())
                    .stream()
                    .filter(c -> c.getState() == CellState.CLOSED)
                    .count();
            if (unknownNeighbours < leastUnknownNeighbours) {
                bestStrategicCell = cell;
                leastUnknownNeighbours = unknownNeighbours;
            }
        }
        return bestStrategicCell;
    }

    public Map<Cell, BigFraction> getProbabilities() {
        Map<Cell, BigInteger> cellMineCount = new HashMap<>();
        Map<Cell, BigFraction> probs = new HashMap<>();

        var totalModels = BigInteger.ZERO;
        var totalSeaModels = BigFraction.ZERO;
        var seaSize = SolverUtil.getSeaCells(cells).size();

        PBSolver solver = SolverFactory.newDefault();
        List<IPBConstraintGenerator> constraintGenerators = List.of(
                new PBConstraintGeneratorSea(),
                new PBConstraintGeneratorOpenCells()
        );

        for (var constraintGenerator : constraintGenerators) {
            try {
                constraintGenerator.generate(solver, cells, width, height, mines);
            } catch (ContradictionException e) {
                e.printStackTrace();
            }
        }

        try {
            while (solver.isSatisfiable()) {
                int[] model = solver.model();
                List<Cell> modelShoreMines = Arrays.stream(model)
                        .filter(i -> i >= 0)
                        .mapToObj(id -> SolverUtil.decodeCellId(cells, id, height, width))
                        .filter(Optional::isPresent)
                        .map(Optional::get)
                        .collect(Collectors.toList());

                int remainingMinesInModel = mines - modelShoreMines.size();

                BigInteger totalPossibleModels = seaSize > 0 ?
                        BigIntegerMath.binomial(seaSize, remainingMinesInModel) :
                        BigInteger.ONE; // One to include the current model

                totalModels = totalModels.add(totalPossibleModels);

                // update mine counts
                for (Cell cell : modelShoreMines) {
                    BigInteger currentCellMineCount = cellMineCount.getOrDefault(cell, BigInteger.ZERO);
                    BigInteger newCellMineCount = currentCellMineCount.add(totalPossibleModels);
                    cellMineCount.put(cell, newCellMineCount);
                }

                // update sea probability
                BigFraction currentModelSeaProb = seaSize > 0 ?
                        new BigFraction(remainingMinesInModel, seaSize) :
                        BigFraction.ZERO;

                totalSeaModels = totalSeaModels.add(currentModelSeaProb.multiply(totalPossibleModels));

                // Remove current solution from possible solutions
                for (int i = 0; i < model.length; i++) {
                    model[i] *= -1;
                }
                IVecInt block = new VecInt(model);
                solver.addBlockingClause(block);
            }
        } catch (TimeoutException | ContradictionException e) {
            e.printStackTrace();
        }

        // need to make sure that solver will get garbage collected
        // https://gitlab.ow2.org/sat4j/sat4j/-/issues/55
        solver.reset();
        solver = null;

        if (seaSize > 0) {
            BigFraction seaProb = totalSeaModels.divide(totalModels).reduce();
            SolverUtil.getSeaCells(cells).forEach(cell -> probs.put(cell, seaProb));
        }

        for (Cell cell : SolverUtil.getClosedShoreCells(cells)) {
            BigInteger currentCellMineCount = cellMineCount.getOrDefault(cell, BigInteger.ZERO);
            probs.put(cell, new BigFraction(currentCellMineCount, totalModels).reduce());
        }

        return probs;
    }
}
