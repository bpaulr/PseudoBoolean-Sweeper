package main.java.simulation;

import main.java.game.Cell;
import main.java.game.CellState;
import main.java.game.GameState;
import main.java.game.MineSweeper;
import main.java.solvers.constant.IConstantMineSolver;

import java.util.Map;
import java.util.Random;

public class GamePlayer {

    private final MineSweeper game;
    private final IConstantMineSolver solver;
    private double startTime;
    private double endTime;

    public GamePlayer(MineSweeper game, IConstantMineSolver solver) {
        this.game = game;
        this.solver = solver;
    }

    public void play() {
        Random rand = new Random();
//        do {
//            int x = rand.nextInt(game.getWidth());
//            int y = rand.nextInt(game.getHeight());
//            startTime = System.nanoTime();
//            game.openCell(x, y);
//        } while (game.getState() != GameState.RUNNING);
        startTime = System.nanoTime();
        while (game.getState() == GameState.RUNNING) {
            Map<Cell, Boolean> known = solver.getKnownCells();
            boolean change = false;
            for (Map.Entry<Cell, Boolean> pair : known.entrySet()) {
                Cell cell = pair.getKey();
                if (pair.getValue()) {

                } else {
                    if (cell.getState() == CellState.CLOSED) {
                        game.openCell(cell.getX(), cell.getY());
                        change = true;
                    }
                }
            }
            if (!change) {
                int x = rand.nextInt(game.getWidth());
                int y = rand.nextInt(game.getHeight());
                game.openCell(x, y);
            }
        }
        endTime = System.nanoTime();
    }

    public double getEndTime() {
        return endTime;
    }

    public double getStartTime() {
        return startTime;
    }

    public double getElapsedTime() {
        return endTime - startTime;
    }
}
