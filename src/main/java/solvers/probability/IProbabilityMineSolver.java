package main.java.solvers.probability;

import main.java.game.Cell;
import org.apache.commons.math3.fraction.BigFraction;

import java.util.Map;

public interface IProbabilityMineSolver {
    Cell getBestSafeProbabilityCell();
    Map<Cell, BigFraction> getProbabilities();
}
