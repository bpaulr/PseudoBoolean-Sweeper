package main.java.solvers.constraints;

import main.java.game.Cell;
import org.sat4j.pb.core.PBSolver;

public interface IPBConstraintGenerator {

    void generate(PBSolver solver, Cell[][] cells, int width, int height, int mines);

}
