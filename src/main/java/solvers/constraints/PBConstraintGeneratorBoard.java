package main.java.solvers.constraints;

import main.java.game.Cell;
import main.java.solvers.SolverUtil;
import org.sat4j.core.VecInt;
import org.sat4j.pb.core.PBSolver;
import org.sat4j.specs.ContradictionException;
import org.sat4j.specs.IVecInt;

public class PBConstraintGeneratorBoard implements IPBConstraintGenerator {


    @Override
    public void generate(PBSolver solver, Cell[][] cells, int width, int height, int mines) {
        IVecInt literals = new VecInt();
        IVecInt coefficients = new VecInt();

        // all cells must sum to the no of mines present on the board
        for (int i = 0; i < width; i++) {
            for (int j = 0; j < height; j++) {
                Cell current = cells[i][j];
                literals.push(SolverUtil.encodeCellId(current, width));
                coefficients.push(1);
            }
        }
        try {
            solver.addAtMost(literals, coefficients, mines);
            solver.addAtLeast(literals, coefficients, mines);
        } catch (ContradictionException e) {
            e.printStackTrace();
        }
        literals.clear();
        coefficients.clear();
    }

}
