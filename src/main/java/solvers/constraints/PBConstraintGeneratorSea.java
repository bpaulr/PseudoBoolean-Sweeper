package main.java.solvers.constraints;

import main.java.game.Cell;
import main.java.solvers.SolverUtil;
import org.sat4j.core.VecInt;
import org.sat4j.pb.core.PBSolver;
import org.sat4j.specs.ContradictionException;
import org.sat4j.specs.IVecInt;

public class PBConstraintGeneratorSea implements IPBConstraintGenerator {


    @Override
    public void generate(PBSolver solver, Cell[][] cells, int width, int height, int mines) {
        int seaSize = SolverUtil.getSeaCells(cells).size();
        int noOfLiteralsNeeded = Integer.toBinaryString(seaSize).length();
        IVecInt literals = new VecInt();
        IVecInt coefficients = new VecInt();

        // binary format so any number of combinations in sea can be figured out.
        for (int i = 0; i < noOfLiteralsNeeded; i++) {
            int square = (int) Math.pow(2, i);
            literals.push(SolverUtil.encodeLit(i, height, width));
            coefficients.push(square);
        }

        try {
            solver.addAtMost(literals, coefficients, seaSize);
            for (Cell cell : SolverUtil.getClosedShoreCells(cells)) {
                literals.push(SolverUtil.encodeCellId(cell, width));
                coefficients.push(1);
            }
            solver.addAtMost(literals, coefficients, mines);
            solver.addAtLeast(literals, coefficients, mines);
        } catch (ContradictionException e) {
            e.printStackTrace();
        }
    }

}
