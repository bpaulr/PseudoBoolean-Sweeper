package main.java.solvers.constraints;

import main.java.game.Cell;
import main.java.solvers.SolverUtil;
import org.sat4j.core.VecInt;
import org.sat4j.pb.core.PBSolver;
import org.sat4j.specs.ContradictionException;
import org.sat4j.specs.IVecInt;

public class PBConstraintGeneratorBoard implements IPBConstraintGenerator {


    @Override
    public void generate(PBSolver solver, Cell[][] cells, int width, int height, int mines) throws ContradictionException {
        IVecInt lits = new VecInt();
        IVecInt coeffs = new VecInt();

        // Constraint that sum of all cells must be the no.
        // of mines present on the board
        for (int i = 0; i < width; i++) {
            for (int j = 0; j < height; j++) {
                Cell current = cells[i][j];
                lits.push(SolverUtil.encodeCellId(current, width));
                coeffs.push(1);
            }
        }
        solver.addAtMost(lits, coeffs, mines);
        solver.addAtLeast(lits, coeffs, mines);
        lits.clear();
        coeffs.clear();
    }

}
