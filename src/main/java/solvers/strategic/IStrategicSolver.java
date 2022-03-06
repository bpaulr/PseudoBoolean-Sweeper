package main.java.solvers.strategic;

import main.java.game.Cell;

import java.util.List;

public interface IStrategicSolver {
    Cell getBestMove(List<Cell> closedCells);
}
