package main.java.solvers.constraints;

import main.java.game.Cell;
import main.java.solvers.SolverUtil;
import org.sat4j.core.VecInt;
import org.sat4j.pb.core.PBSolver;
import org.sat4j.specs.ContradictionException;
import org.sat4j.specs.IVecInt;

import java.util.List;

public class PBConstraintGeneratorOpenCells extends AbstractConstraintGenerator {


    @Override
    public void generate(PBSolver solver, Cell[][] cells, int width, int height, int mines) {
        IVecInt literals = new VecInt();
        IVecInt coefficients = new VecInt();

        List<Cell> openCells = SolverUtil.getLandCells(cells);

        for (Cell cell : openCells) {
            literals.push(SolverUtil.encodeCellId(cell, width));
            coefficients.push(1);
            try {
                // every cell that is open can not be a mine
                addExactly(solver, literals, coefficients, 0);

                literals.clear();
                coefficients.clear();

                addSurroundingCellsConstraint(solver, cells, width, literals, coefficients, cell);

                literals.clear();
                coefficients.clear();
            } catch (ContradictionException e) {
                e.printStackTrace();
            }
        }
    }

    private void addSurroundingCellsConstraint(PBSolver solver, Cell[][] cells, int width, IVecInt literals, IVecInt coefficients, Cell cell) throws ContradictionException {
        List<Cell> neighbours = SolverUtil.getNeighbours(cells, cell.getX(), cell.getY());
        for (Cell c : neighbours) {
            literals.push(SolverUtil.encodeCellId(c, width));
            coefficients.push(1);
        }
        addExactly(solver, literals, coefficients, cell.getNumber());
    }

}
