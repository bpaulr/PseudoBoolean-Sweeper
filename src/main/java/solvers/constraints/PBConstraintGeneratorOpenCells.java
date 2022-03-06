package main.java.solvers.constraints;

import main.java.game.Cell;
import main.java.solvers.SolverUtil;
import org.sat4j.core.VecInt;
import org.sat4j.pb.core.PBSolver;
import org.sat4j.specs.ContradictionException;
import org.sat4j.specs.IVecInt;

import java.util.List;

public class PBConstraintGeneratorOpenCells implements IPBConstraintGenerator {


    @Override
    public void generate(PBSolver solver, Cell[][] cells, int width, int height, int mines) throws ContradictionException {
        IVecInt lits = new VecInt();
        IVecInt coeffs = new VecInt();

        List<Cell> openCells = SolverUtil.getLandCells(cells);

        for (Cell cell : openCells) {
            lits.push(SolverUtil.encodeCellId(cell, width));
            coeffs.push(1);
            solver.addAtMost(lits, coeffs, 0);
            solver.addAtLeast(lits, coeffs, 0);
            lits.clear();
            coeffs.clear();

            // Normal constraint
            List<Cell> neighbours = SolverUtil.getNeighbours(cells, cell.getX(), cell.getY());
            for (Cell c : neighbours) {
                lits.push(SolverUtil.encodeCellId(c, width));
                coeffs.push(1);
            }
            solver.addAtMost(lits, coeffs, cell.getNumber());
            solver.addAtLeast(lits, coeffs, cell.getNumber());
            lits.clear();
            coeffs.clear();
        }
    }

}
