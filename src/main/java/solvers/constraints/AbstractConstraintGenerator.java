package main.java.solvers.constraints;

import main.java.solvers.SolverUtil;
import org.sat4j.pb.IPBSolver;
import org.sat4j.specs.ContradictionException;
import org.sat4j.specs.IVecInt;

import java.io.BufferedWriter;
import java.io.FileWriter;
import java.io.IOException;
import java.io.PrintWriter;
import java.time.LocalDateTime;
import java.util.Optional;

public abstract class AbstractConstraintGenerator implements IPBConstraintGenerator {

    private final String LOG_FILE_NAME = "constraints.log";

    private void writeLog(IVecInt literals, IVecInt coefficients, String comparator, int degree) {
        try(FileWriter fw = new FileWriter(LOG_FILE_NAME, true);
            BufferedWriter bw = new BufferedWriter(fw);
            PrintWriter out = new PrintWriter(bw))
        {
            StringBuilder sb = new StringBuilder();
            sb.append(LocalDateTime.now()).append(": ");
            for (int i = 0; i < literals.size(); i++) {
                int lit = literals.get(i);
                int coeff = coefficients.get(i);
                sb.append(coeff).append("x").append(lit).append(" ");
            }
            sb.append(comparator).append(" ");
            sb.append(degree);
            out.println(sb);
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    protected void addExactly(IPBSolver solver, IVecInt literals, IVecInt coefficients, int degree) {
        try {
            solver.addAtLeast(literals, coefficients, degree);
            solver.addAtMost(literals, coefficients, degree);
        } catch (ContradictionException e) {
            solver.reset();
        }
        writeLog(literals, coefficients, "=", degree);
    }

    protected void addAtLeast(IPBSolver solver, IVecInt literals, IVecInt coefficients, int degree) {
        try {
            solver.addAtLeast(literals, coefficients, degree);
        } catch (ContradictionException e) {
            solver.reset();
            e.printStackTrace();
        }
        writeLog(literals, coefficients, ">=", degree);
    }

    protected void addAtMost(IPBSolver solver, IVecInt literals, IVecInt coefficients, int degree) {
        try {
            solver.addAtMost(literals, coefficients, degree);
        } catch (ContradictionException e) {
            solver.reset();
            e.printStackTrace();
        }
        writeLog(literals, coefficients, "<=", degree);
    }

}
