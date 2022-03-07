package main.java.gui;

import main.java.game.Cell;
import main.java.game.CellState;
import main.java.game.GameState;
import main.java.game.MineSweeper;
import main.java.solvers.constant.IConstantMineSolver;
import main.java.solvers.probability.IProbabilityMineSolver;
import main.java.solvers.strategic.IStrategicSolver;

import javax.swing.*;
import java.util.List;
import java.util.Map;
import java.util.Optional;

public class SolverSwingWorker extends SwingWorker<Boolean, Boolean> {

    private final List<JComponent> disableComponents;
    private final List<IConstantMineSolver> constantSolvers;
    private final Optional<IProbabilityMineSolver> probabilitySolver;
    private final Optional<IStrategicSolver> strategicSolver;
    private final MineSweeper game;
    private final BoardPanel boardPanel;
    private final boolean loop;
    private volatile boolean running;

    private SolverSwingWorker(MineSweeper game, BoardPanel boardPanel, List<JComponent> disableComponents,
                              List<IConstantMineSolver> constantSolvers, Optional<IProbabilityMineSolver> probabilitySolver,
                              Optional<IStrategicSolver> strategicSolver, boolean loop) {
        this.running = true;
        this.disableComponents = disableComponents;
        this.game = game;
        this.boardPanel = boardPanel;
        this.loop = loop;
        this.constantSolvers = constantSolvers;
        this.probabilitySolver = probabilitySolver;
        this.strategicSolver = strategicSolver;
    }

    public void stop() {
        this.running = false;
    }

    private boolean makeChanges(Map<Cell, Boolean> knownCells) {
        boolean somethingChanged = false;
        for (var pair : knownCells.entrySet()) {
            Cell cell = pair.getKey();
            boolean isMine = pair.getValue();
            CellButton button = boardPanel.getButtonFromCell(cell);
            if (!running) {
                break;
            }
            if (isMine) {
                if (cell.getState() == CellState.CLOSED) {
                    boardPanel.flagButton(button, cell);
                    somethingChanged = true;
                }
            } else {
                // Goes by the assumption that user's flagged cells are correct
                if (cell.getState() == CellState.CLOSED) {
                    boardPanel.selectButton(button, cell);
                    somethingChanged = true;
                }
            }
        }
        return somethingChanged;
    }

    private void cycleConstantSolvers() {
        for (int i = 0; i < constantSolvers.size(); i++) {
            if (!this.running || game.getState() != GameState.RUNNING) {
                break;
            }
            IConstantMineSolver solver = constantSolvers.get(i);
            Map<Cell, Boolean> knownCells = solver.getKnownCells();
            boolean changesWereMade = makeChanges(knownCells);
            if (changesWereMade) {
                if (!loop) {
                    break;
                }
                i = -1; // start from first solver again
                continue;
            }
        }
    }

    @Override
    protected Boolean doInBackground() {
        while (this.running && game.getState() == GameState.RUNNING) {
            cycleConstantSolvers();
            if (probabilitySolver.isPresent()) {
                List<Cell> bestCells = probabilitySolver.get().getBestSafeProbabilityCells();
                if (bestCells.isEmpty()) {
                    break;
                }
                Cell bestCell;
                if (bestCells.size() == 1) {
                    bestCell = bestCells.get(0);
                } else if (strategicSolver.isPresent()) {
                    bestCell = strategicSolver.get().getBestMove(bestCells);
                } else {
                    break;
                }
                CellButton button = boardPanel.getButtonFromCell(bestCell);
                // the above calculation can take a long time to complete so,
                // it's safer to do what may seem as an "unnecessary" running
                // check again
                if (!this.running || game.getState() != GameState.RUNNING) {
                    break;
                }
                boardPanel.selectButton(button, bestCell);
                if (!loop) {
                    break;
                }
                continue;
            }
            break;
        }
        boardPanel.setEnabled(true);
        disableComponents.forEach(component -> component.setEnabled(true));
        return this.running;
    }

    public static class SwingWorkerBuilder {

        private final MineSweeper game;
        private List<JComponent> disableComponents;
        private BoardPanel board;
        private boolean loop;

        private List<IConstantMineSolver> constantSolvers;
        private Optional<IProbabilityMineSolver> probabilitySolver;
        private Optional<IStrategicSolver> strategicSolver;

        public SwingWorkerBuilder(MineSweeper game) {
            this.game = game;
        }

        public SwingWorkerBuilder withConstantSolvers(List<IConstantMineSolver> solvers) {
            this.constantSolvers = solvers;
            return this;
        }

        public SwingWorkerBuilder withProbabilitySolver(Optional<IProbabilityMineSolver> solver) {
            this.probabilitySolver = solver;
            return this;
        }

        public SwingWorkerBuilder withStrategicSolver(Optional<IStrategicSolver> solver) {
            this.strategicSolver = solver;
            return this;
        }

        public SwingWorkerBuilder disableComponents(List<JComponent> components) {
            this.disableComponents = components;
            return this;
        }

        public SwingWorkerBuilder withBoardPanel(BoardPanel board) {
            this.board = board;
            return this;
        }

        public SwingWorkerBuilder setLoop(boolean loop) {
            this.loop = loop;
            return this;
        }

        public SolverSwingWorker build() {
            return new SolverSwingWorker(game, board, disableComponents, constantSolvers, probabilitySolver, strategicSolver, loop);
        }

    }
}
