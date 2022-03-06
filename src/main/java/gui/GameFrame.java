package main.java.gui;

import main.java.game.MineSweeper;
import main.java.solvers.constant.IConstantMineSolver;
import main.java.solvers.probability.IProbabilityMineSolver;
import main.java.solvers.probability.TrueProbabilityMineSolver;
import main.java.solvers.strategic.IStrategicSolver;
import main.java.solvers.constant.PBMineSolver;
import main.java.solvers.constant.SinglePointMineSolver;

import javax.swing.*;
import javax.swing.border.TitledBorder;
import java.awt.*;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

public class GameFrame extends JFrame {

    private final GameStatsPanel gameStats;
    private final JButton resetBtn;
    private final JButton hintBtn;
    private final JButton assistBtn;
    private final JButton solveBtn;
    private final JButton stopBtn;
    private final JCheckBox probabilityCheckBox;
    private final List<JComponent> disableComponents;
    private final GameMenuBar menuBar;
    // This game reference is passed around the gui a lot
    // not great programming but it will do
    private MineSweeper game;
    private SolverSwingWorker worker;
    private BoardPanel boardPanel;

    public GameFrame(MineSweeper game) {
        this.game = game;
        gameStats = new GameStatsPanel(game.getMines());
        this.boardPanel = new BoardPanel(this.game, gameStats);
        this.resetBtn = new JButton("Reset");
        this.hintBtn = new JButton("Hint");
        this.assistBtn = new JButton("Assist");
        this.solveBtn = new JButton("Solve");
        this.stopBtn = new JButton("Stop");
        this.probabilityCheckBox = new JCheckBox("Probabilities");
        this.menuBar = new GameMenuBar(this);
        disableComponents = List.of(
                resetBtn,
                hintBtn,
                assistBtn,
                solveBtn,
                probabilityCheckBox
        );
    }

    public void setGame(MineSweeper newGame) {
        this.game = newGame;
    }

    public void resetBoard() {
        this.game = new MineSweeper(game.getWidth(), game.getHeight(), game.getMines());
        gameStats.reset(game.getMines());
        this.boardPanel = new BoardPanel(this.game, gameStats);
        this.boardPanel.setShowProbabilities(probabilityCheckBox.isSelected());
    }

    public void resetGUI() {
        this.getContentPane().remove(((BorderLayout) this.getContentPane().getLayout())
                .getLayoutComponent(BorderLayout.CENTER));
        this.getContentPane().add(boardPanel, BorderLayout.CENTER);
        this.pack();
        Dimension dim = Toolkit.getDefaultToolkit().getScreenSize();
        this.setLocation(dim.width / 2 - getSize().width / 2, dim.height / 2 - getSize().height / 2);
        this.setJMenuBar(this.menuBar);
    }

    public void buildGUI() {
        this.setJMenuBar(this.menuBar);

        addButtonListeners();

        JPanel topFrame = new JPanel();
        topFrame.setLayout(new FlowLayout());

        topFrame.add(gameStats);

        JPanel buttonPanel = new JPanel();
        buttonPanel.setLayout(new FlowLayout());
        TitledBorder buttonTitle = new TitledBorder("Control Buttons");
        buttonTitle.setTitleJustification(TitledBorder.CENTER);
        buttonPanel.setBorder(buttonTitle);
        buttonPanel.add("Hint Button", hintBtn);
        buttonPanel.add("Assist Button", assistBtn);
        buttonPanel.add("Solve Button", solveBtn);
        buttonPanel.add("Stop Button", stopBtn);
        buttonPanel.add("Probability Button", probabilityCheckBox);
        topFrame.add(buttonPanel);

        this.getContentPane().add(topFrame, BorderLayout.NORTH);
        this.getContentPane().add(boardPanel, BorderLayout.CENTER);
        this.getContentPane().add(resetBtn, BorderLayout.SOUTH);

        this.setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
        this.pack();
        Dimension dim = Toolkit.getDefaultToolkit().getScreenSize();
        this.setLocation(dim.width / 2 - getSize().width / 2, dim.height / 2 - getSize().height / 2);
        this.setVisible(true);
    }

    private void addButtonListeners() {
        hintBtn.addActionListener(e -> {
            if (!boardPanel.knowsHints()) {
                // just use a pb solver rather than incremental solvers
                IConstantMineSolver p = new PBMineSolver(game.getCells(), game.getWidth(),
                        game.getHeight(), game.getMines());
                boardPanel.setHintCells(p.getKnownCells());
            }
            boardPanel.showHint();
        });

        assistBtn.addActionListener(e -> {
            configureSolverWorker(false);
            boardPanel.setEnabled(false);
            disableComponents.forEach(component -> component.setEnabled(false));
            this.worker.execute();
        });

        solveBtn.addActionListener(e -> {
            configureSolverWorker(true);
            boardPanel.setEnabled(false);
            disableComponents.forEach(component -> component.setEnabled(false));
            this.worker.execute();
        });

        stopBtn.addActionListener(e -> {
            this.boardPanel.setEnabled(true);
            if (worker != null) {
                worker.stop();
                disableComponents.forEach(component -> component.setEnabled(true));
            }
        });

        resetBtn.addActionListener(e -> {
            resetBoard();
            resetGUI();
        });

        probabilityCheckBox.setToolTipText("WARNING: Clicking this option may block the application due to intensive calculation.");
        probabilityCheckBox.addActionListener(e -> boardPanel.setShowProbabilities(probabilityCheckBox.isSelected()));
    }

    private void configureSolverWorker(boolean loop) {
        List<IConstantMineSolver> constantSolvers = new ArrayList<>();
        Optional<IProbabilityMineSolver> probabilitySolver = Optional.empty();
        Optional<IStrategicSolver> strategicSolver = Optional.empty();
        loadConstantSolvers(constantSolvers);

        if (menuBar.getProbabilityCb().isSelected()) {
            probabilitySolver = Optional.of(new TrueProbabilityMineSolver(game.getCells(), game.getWidth(), game.getHeight(), game.getMines()));
        }
        if (menuBar.getStrategyCb().isSelected()) {
            // ToDo: implement some strategic solvers...
        }

        this.worker = buildSolverWorker(constantSolvers, probabilitySolver, strategicSolver, loop);
    }

    private void loadConstantSolvers(List<IConstantMineSolver> constantSolvers) {
        if (menuBar.getSinglePointCb().isSelected()) {
            constantSolvers.add(new SinglePointMineSolver(game.getCells(), game.getWidth(), game.getHeight(), game.getMines()));
        }
        if (menuBar.getPseudoBooleanCb().isSelected()) {
            constantSolvers.add(new PBMineSolver(game.getCells(), game.getWidth(), game.getHeight(), game.getMines()));
        }
    }

    private SolverSwingWorker buildSolverWorker(
            List<IConstantMineSolver> constantSolvers,
            Optional<IProbabilityMineSolver> probabilitySolver,
            Optional<IStrategicSolver> strategicSolver,
            boolean loop) {
        return new SolverSwingWorker.SwingWorkerBuilder(game)
                .disableComponents(disableComponents)
                .withBoardPanel(boardPanel)
                .withConstantSolvers(constantSolvers)
                .withProbabilitySolver(probabilitySolver)
                .withStrategicSolver(strategicSolver)
                .setLoop(loop)
                .build();
    }

}
