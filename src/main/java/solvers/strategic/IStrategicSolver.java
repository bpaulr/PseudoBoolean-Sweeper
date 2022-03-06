package main.java.solvers.strategic;

import main.java.game.Cell;

import java.util.Map;

public interface IStrategicSolver {
    Cell getBestMove(Map<Cell, Boolean> knownCells);
}
