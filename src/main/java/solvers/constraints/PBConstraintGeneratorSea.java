package main.java.solvers.constraints;

import main.java.game.Cell;
import main.java.solvers.SolverUtil;
import org.sat4j.core.VecInt;
import org.sat4j.pb.core.PBSolver;
import org.sat4j.specs.ContradictionException;
import org.sat4j.specs.IVecInt;

public class PBConstraintGeneratorSea implements IPBConstraintGenerator {


    @Override
    public void generate(PBSolver solver, Cell[][] cells, int width, int height, int mines) throws ContradictionException {
        int seaSize = SolverUtil.getSeaCells(cells).size();
        int noOfLits = Integer.toBinaryString(seaSize).length();
        IVecInt lits = new VecInt();
        IVecInt coeffs = new VecInt();
        for (int i = 0; i < noOfLits; i++) {
            int square = (int) Math.pow(2, i);
            lits.push(SolverUtil.encodeLit(i, height, width));
            coeffs.push(square);
        }
        solver.addAtMost(lits, coeffs, seaSize);

        for (Cell cell : SolverUtil.getClosedShoreCells(cells)) {
            lits.push(SolverUtil.encodeCellId(cell, width));
            coeffs.push(1);
        }
        solver.addAtMost(lits, coeffs, mines);
        solver.addAtLeast(lits, coeffs, mines);
    }

}
