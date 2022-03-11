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

    public List<Cell> getBestSafeProbabilityCells() {
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
        return lowestProbCells;
    }

    public Map<Cell, BigFraction> getProbabilities() {
        Map<Cell, BigInteger> cellMineCount = new HashMap<>();
        Map<Cell, BigFraction> probabilities = new HashMap<>();

        BigInteger totalModels = BigInteger.ZERO;
        BigFraction totalSeaModels = BigFraction.ZERO;
        int seaSize = SolverUtil.getSeaCells(cells).size();

        PBSolver solver = SolverFactory.newDefault();
        List<IPBConstraintGenerator> constraintGenerators = List.of(
                new PBConstraintGeneratorSea(),
                new PBConstraintGeneratorOpenCells()
        );

        for (var constraintGenerator : constraintGenerators) {
            constraintGenerator.generate(solver, cells, width, height, mines);
        }

        try {
            while (solver.isSatisfiable()) {
                int[] model = solver.model();

                List<Cell> modelShoreMines = convertModelToCells(model);

                int remainingMinesInModel = mines - modelShoreMines.size();

                BigInteger totalPossibleModels = calculateAllPossibleModels(seaSize, remainingMinesInModel);

                totalModels = totalModels.add(totalPossibleModels);

                updateAllMineCounts(cellMineCount, modelShoreMines, totalPossibleModels);

                BigFraction currentModelSeaProbability = calculateSeaProbability(seaSize, remainingMinesInModel);

                totalSeaModels = totalSeaModels.add(
                        currentModelSeaProbability.multiply(totalPossibleModels)
                );

                blockModelInSolver(solver, model);
            }
        } catch (TimeoutException t) {
            return probabilities;
        } finally {
            // need to make sure that solver will get garbage collected
            // https://gitlab.ow2.org/sat4j/sat4j/-/issues/55
            solver.reset();
            solver = null;
        }

        if (seaSize > 0) {
            updateSeaCellProbabilities(probabilities, totalModels, totalSeaModels);
        }

        for (Cell cell : SolverUtil.getClosedShoreCells(cells)) {
            updateShoreCellProbabilities(cellMineCount, probabilities, totalModels, cell);
        }

        return probabilities;
    }

    private void updateShoreCellProbabilities(Map<Cell, BigInteger> cellMineCount, Map<Cell, BigFraction> probabilities, BigInteger totalModels, Cell cell) {
        BigInteger currentCellMineCount = cellMineCount.getOrDefault(cell, BigInteger.ZERO);
        probabilities.put(cell, new BigFraction(currentCellMineCount, totalModels).reduce());
    }

    private void updateSeaCellProbabilities(Map<Cell, BigFraction> probabilities, BigInteger totalModels, BigFraction totalSeaModels) {
        BigFraction seaProb = totalSeaModels.divide(totalModels).reduce();
        SolverUtil.getSeaCells(cells).forEach(cell -> probabilities.put(cell, seaProb));
    }

    private BigFraction calculateSeaProbability(int seaSize, int remainingMinesInModel) {
        BigFraction currentModelSeaProb = seaSize > 0 ?
                new BigFraction(remainingMinesInModel, seaSize) :
                BigFraction.ZERO;
        return currentModelSeaProb;
    }

    private BigInteger calculateAllPossibleModels(int seaSize, int remainingMinesInModel) {
        BigInteger totalPossibleModels = seaSize > 0 ?
                BigIntegerMath.binomial(seaSize, remainingMinesInModel) :
                BigInteger.ONE; // One to include the current model
        return totalPossibleModels;
    }

    private List<Cell> convertModelToCells(int[] model) {
        List<Cell> modelShoreMines = Arrays.stream(model)
                .filter(i -> i >= 0)  // only consider literals that are for cells
                .mapToObj(id -> SolverUtil.decodeCellId(cells, id, height, width))
                .filter(Optional::isPresent)
                .map(Optional::get)
                .collect(Collectors.toList());
        return modelShoreMines;
    }

    private void updateAllMineCounts(Map<Cell, BigInteger> cellMineCount, List<Cell> modelShoreMines, BigInteger totalPossibleModels) {
        // update mine counts
        for (Cell cell : modelShoreMines) {
            BigInteger currentCellMineCount = cellMineCount.getOrDefault(cell, BigInteger.ZERO);
            BigInteger newCellMineCount = currentCellMineCount.add(totalPossibleModels);
            cellMineCount.put(cell, newCellMineCount);
        }
    }

    private void blockModelInSolver(PBSolver solver, int[] model) {
        // Remove current solution from possible solutions
        for (int i = 0; i < model.length; i++) {
            model[i] *= -1;
        }
        IVecInt block = new VecInt(model);
        try {
            solver.addBlockingClause(block);
        } catch (ContradictionException e) {
            e.printStackTrace();
        }
    }
}
