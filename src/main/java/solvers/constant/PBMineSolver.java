package main.java.solvers.constant;

import main.java.game.Cell;
import main.java.solvers.constraints.PBConstraintGeneratorBoard;
import main.java.solvers.constraints.PBConstraintGeneratorOpenCells;
import main.java.solvers.constraints.IPBConstraintGenerator;
import main.java.solvers.SolverUtil;
import org.sat4j.core.VecInt;
import org.sat4j.pb.SolverFactory;
import org.sat4j.pb.core.PBSolver;
import org.sat4j.specs.ContradictionException;
import org.sat4j.specs.IConstr;
import org.sat4j.specs.IVecInt;
import org.sat4j.specs.TimeoutException;

import java.util.*;

public class PBMineSolver implements IConstantMineSolver {

    public final List<String> constraintLog;

    private final Cell[][] cells;
    private final int width;
    private final int height;
    private final int mines;

    public PBMineSolver(Cell[][] cells, int width, int height, int mines) {
        this.cells = cells;
        this.width = width;
        this.height = height;
        this.mines = mines;
        constraintLog = new ArrayList<>();
    }

    public Map<Cell, Boolean> getKnownCells() {
        Map<Cell, Boolean> results = new HashMap<>();

        PBSolver solver = SolverFactory.newDefault();
        List<IPBConstraintGenerator> constraintGenerators = List.of(
            new PBConstraintGeneratorBoard(),
            new PBConstraintGeneratorOpenCells()
        );

        for (var constraintGenerator : constraintGenerators) {
            try {
                constraintGenerator.generate(solver, cells, width, height, mines);
            } catch (ContradictionException e) {
                e.printStackTrace();
            }
        }

        List<Cell> shoreCells = SolverUtil.getClosedShoreCells(cells);

        // Test all shore cells
        for (Cell cell : shoreCells) {
            for (int weight = 0; weight <= 1; weight++) {
                Optional<Boolean> isMine =
                        checkCellWithWeight(solver, cell, weight);
                if (isMine.isPresent()) {
                    results.put(cell, isMine.get());
                    break;
                }
            }
        }

        // Test a sea cell
        List<Cell> seaCells = SolverUtil.getSeaCells(cells);
        if (!seaCells.isEmpty()) {
            // if one sea cell is safe/a mine than all sea cells are safe/a mine
            Cell cell = seaCells.get(0);
            for (int weight = 0; weight <= 1; weight++) {
                Optional<Boolean> isMine =
                        checkCellWithWeight(solver, cell, weight);
                if (isMine.isPresent()) {
                    for (Cell c : seaCells) {
                        results.put(c, isMine.get());
                    }
                    break;
                }
            }
        }

        // need to make sure that solver will get garbage collected
        // https://gitlab.ow2.org/sat4j/sat4j/-/issues/55
        solver.reset();
        solver = null;
        return results;
    }



    private Optional<Boolean> checkCellWithWeight(PBSolver solver, final Cell cell, final int weight) {
        IVecInt lit = new VecInt();
        IVecInt coeff = new VecInt();
        IConstr atMostConstr = null;
        IConstr atLeastConstr = null;

        Optional<Boolean> result = Optional.empty();

        lit.push(SolverUtil.encodeCellId(cell, width));
        coeff.push(1);

        try {
            atMostConstr = solver.addAtMost(lit, coeff, weight);
            atLeastConstr = solver.addAtLeast(lit, coeff, weight);
            if (!solver.isSatisfiable()) {
                boolean isMine = weight != 1;
                result = Optional.of(isMine);
            }
        } catch (ContradictionException e) {
            result = Optional.of(weight != 1);
        } catch (TimeoutException t) {
            t.printStackTrace();
        }
        if (atMostConstr != null) {
            solver.removeConstr(atMostConstr);
        }
        if (atLeastConstr != null) {
            solver.removeConstr(atLeastConstr);
        }

        return result;
    }
}
