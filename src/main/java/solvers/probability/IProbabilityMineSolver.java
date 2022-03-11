package main.java.solvers.probability;

import main.java.game.Cell;
import org.apache.commons.math3.fraction.BigFraction;

import java.util.List;
import java.util.Map;

public interface IProbabilityMineSolver {
    List<Cell> getBestSafeProbabilityCells();
    Map<Cell, BigFraction> getProbabilities();
}
