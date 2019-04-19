import java.io.BufferedReader;
import java.io.FileNotFoundException;
import java.io.FileReader;
import java.io.IOException;
import java.io.PrintWriter;
import java.math.BigInteger;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

import com.google.gson.Gson;

import org.apache.commons.math3.fraction.BigFraction;
import org.apache.commons.math3.fraction.Fraction;

/*
 * Main.java
 * 
 * Created by Potrik
 * Last modified: 07.22.13
 * 
 * Heavily modified by Bradley Read
 * Last modified: @date
 */

public class LaunchSim {

	private final String EASY_PATH = "resources/easyFields.txt";
	private final String MEDIUM_PATH = "resources/mediumFields.txt";
	private final String HARD_PATH = "resources/hardFields.txt";

	private final String PATTERN_NAME = "PatternMatching-Results.csv";
	private final String PATTERN_NAME_FIRSTGUESS = "PatternMatchingFirstGuess-Results.csv";
	private final String PB_NAME = "SAT-Results.csv";
	private final String PB_NAME_FIRSTGUESS = "SATFirstGuess-Results.csv";
	private final String JOINT_NAME = "Joint-Results.csv";
	private final String FULL_NAME = "Full-Results.csv";

	private String RESULT_DIR;
	private int noOfGames;
	private int batchSize;
	private StringBuilder resultString;
	private boolean firstGuess;

	private int winCount = 0;
	private int guessCount = 0;
	private BigInteger winTimes = BigInteger.ZERO;
	private BigInteger totalTime = BigInteger.ZERO;

	public LaunchSim(int noOfSims, String path) throws IOException, InterruptedException {
		this.noOfGames = noOfSims;
		RESULT_DIR = path;
		resultString = new StringBuilder();
		batchSize = 10000;
		firstGuess = false;
	}

	public LaunchSim(int noOfSims, int batchSize, String path) throws IOException, InterruptedException {
		this.noOfGames = noOfSims;
		RESULT_DIR = path;
		resultString = new StringBuilder();
		this.batchSize = batchSize;
		firstGuess = false;
	}

	public void resetScores() {
		winCount = 0;
		guessCount = 0;
		totalTime = BigInteger.ZERO;
		winTimes = BigInteger.ZERO;
	}

	public void startPTSim() {
		try {
			writeTitle();
			System.out.println("Pattern Match Easy");
			playSinglePoint(Difficulty.BEGINNER, EASY_PATH);
			System.out.println("Pattern Match Medium");
			playSinglePoint(Difficulty.INTERMEDIATE, MEDIUM_PATH);
			System.out.println("Pattern Match Hard");
			playSinglePoint(Difficulty.EXPERT, HARD_PATH);
			writeResults(PATTERN_NAME);
			resetResults();
		} catch (IOException e) {
			e.printStackTrace();
		}
	}

	public void startPTFirstGuessSim() {
		try {
			this.firstGuess = true;
			writeTitle();
			System.out.println("Pattern Match (FG) Easy");
			playSinglePoint(Difficulty.BEGINNER, EASY_PATH);
			System.out.println("Pattern Match (FG) Medium");
			playSinglePoint(Difficulty.INTERMEDIATE, MEDIUM_PATH);
			System.out.println("Pattern Match (FG) Hard");
			playSinglePoint(Difficulty.EXPERT, HARD_PATH);
			writeResults(PATTERN_NAME_FIRSTGUESS);
			resetResults();
		} catch (IOException e) {
			e.printStackTrace();
		}
	}

	public void startPBSim() {
		try {
			writeTitle();
			System.out.println("PB Easy");
			playPB(Difficulty.BEGINNER, EASY_PATH);
			resetScores();
			System.out.println("PB Medium");
			playPB(Difficulty.INTERMEDIATE, MEDIUM_PATH);
			resetScores();
			System.out.println("PB Hard");
			playPB(Difficulty.EXPERT, HARD_PATH);
			writeResults(PB_NAME);
			resetResults();
		} catch (IOException e) {
			e.printStackTrace();
		}
	}

	public void startPBFirstGuessSim() {
		try {
			this.firstGuess = true;
			writeTitle();
			System.out.println("PB Easy");
			playPB(Difficulty.BEGINNER, EASY_PATH);
			resetScores();
			System.out.println("PB Medium");
			playPB(Difficulty.INTERMEDIATE, MEDIUM_PATH);
			resetScores();
			System.out.println("PB Hard");
			playPB(Difficulty.EXPERT, HARD_PATH);
			writeResults(PB_NAME_FIRSTGUESS);
			resetResults();
		} catch (IOException e) {
			e.printStackTrace();
		}
	}

	public void startJointSim() {
		try {
			writeTitle();
			System.out.println("Pattern Match + SAT Easy");
			playSinglePointPB(Difficulty.BEGINNER, EASY_PATH);
			// System.out.println("Pattern Match + SAT Medium");
			// playerPatternMatchSAT(Difficulty.INTERMEDIATE, MEDIUM_PATH);
			// System.out.println("Pattern Match + SAT Hard");
			// playerPatternMatchSAT(Difficulty.EXPERT, HARD_PATH);
			writeResults(JOINT_NAME);
			resetResults();
		} catch (IOException e) {
			e.printStackTrace();
		}
	}

	public void startFullSim() {
		try {
			writeTitle();
			// System.out.println("Full Easy");
			// playerFull(Difficulty.BEGINNER, EASY_PATH);
			// System.out.println("Full Medium");
			// playerFull(Difficulty.INTERMEDIATE, MEDIUM_PATH);
			System.out.println("Full Hard");
			playerFull(Difficulty.EXPERT, HARD_PATH);
			writeResults(FULL_NAME);
			resetResults();
		} catch (IOException e) {
			e.printStackTrace();
		}
	}

	public void playSinglePoint(Difficulty diff, String path) throws IOException {
		int winCount = 0;
		int guessCount = 0;
		int gamesLostOnFirstMove = 0;
		BigInteger winTimes = BigInteger.ZERO;
		BigInteger totalTime = BigInteger.ZERO;

		int count = 0;
		try (BufferedReader br = new BufferedReader(new FileReader(path))) {
			for (String fieldJson; (fieldJson = br.readLine()) != null && count < noOfGames;) {
				SinglePointSolver sp;
				Minesweeper game;
				long startTime;
				if (firstGuess) {
					boolean opening = false;
					MineField mineField = new Gson().fromJson(fieldJson, MineField.class);
					// System.out.println(fieldJson);
					game = new Minesweeper(diff, mineField);
					sp = new SinglePointSolver(game);
					sp.setQuiet();
					startTime = System.nanoTime();
					// Cell c = sp.getFirstGuess();
					// System.out.println(c);
					opening = sp.makeFirstGuess();
					while (!opening && !game.isGameOver()) {
						opening = sp.makeFirstGuess();
						guessCount++;
					}
				} else {
					do {
						MineField mineField = new Gson().fromJson(fieldJson, MineField.class);
						game = new Minesweeper(diff, mineField);
						sp = new SinglePointSolver(game);
						sp.setQuiet();
						startTime = System.nanoTime();
						sp.selectRandomCell();
					} while (game.isGameOver());
				}
				guessCount++; // First move
				while (!game.isGameOver()) {
					if (!sp.assist()) {
						sp.selectRandomCell();
						guessCount++;
					}
				}
				long endTime = System.nanoTime();
				long elapsedTime = endTime - startTime;
				totalTime = totalTime.add(BigInteger.valueOf(elapsedTime));
				if (game.isGameWon()) {
					winCount++;
					winTimes = winTimes.add(BigInteger.valueOf(elapsedTime));
				}
				if (!game.isGameWon()) {
					if (game.getNoOfMoves() == 1 || game.getNoOfMoves() == 0) {
						gamesLostOnFirstMove++;
					}
				}
				count++;
				System.out.println(count);
			}
		}
		if (firstGuess) {
			writeLine(diff.toString(), winCount, winTimes, guessCount, gamesLostOnFirstMove);
		} else {
			writeLine(diff.toString(), winCount, winTimes, guessCount);
		}
	}

	public void playPB(Difficulty diff, String path) throws IOException {
		int gamesLostOnFirstMove = 0;
		int noOfThreads = Runtime.getRuntime().availableProcessors();
		int batch;
		batch = calcNoOfThreads(diff);
		List<GamePlayer> games = new ArrayList<>(batch);
		int lineCount = 0;
		while (lineCount < noOfGames) {
			// ExecutorService pool = Executors.newFixedThreadPool(noOfThreads);
			ExecutorService pool = Executors.newCachedThreadPool();

			try (BufferedReader br = new BufferedReader(new FileReader(path))) {
				int count = 0;
				for (int j = 0; j < lineCount; j++) {
					br.readLine();
				}
				for (String fieldJson; (fieldJson = br.readLine()) != null && count < batch;) {
					lineCount++;
					count++;
					GamePlayer player = new GamePlayer(diff, fieldJson);
					player.setPB(true);
					player.setFirstGuess(firstGuess);
					games.add(player);
				}
			}

			for (GamePlayer game : games) {
				pool.execute(game);
			}

			pool.shutdown();
			int print = 0;
			while (!pool.isTerminated()) {
				if (print % 20 == 0)
					System.out.println(pool);
				print++;
				try {
					Thread.sleep(500);
				} catch (InterruptedException e) {
					e.printStackTrace();
				}
			}

			for (GamePlayer gameSim : games) {
				totalTime = totalTime.add(BigInteger.valueOf(gameSim.getElapsedTime()));
				if (gameSim.isGameWon()) {
					winCount++;
					guessCount += gameSim.getGuessCount();
					winTimes = winTimes.add(BigInteger.valueOf(gameSim.getElapsedTime()));
				} else {
					if (gameSim.getGame().getNoOfMoves() == 1) {
						gamesLostOnFirstMove++;
					}
				}
			}
			games.clear();
		}
		if (firstGuess) {
			writeLine(diff.toString(), winCount, winTimes, guessCount, gamesLostOnFirstMove);
		} else {
			writeLine(diff.toString(), winCount, winTimes, guessCount);
		}
	}

	private int calcNoOfThreads(Difficulty diff) {
		int batch;
		switch (diff) {
		case BEGINNER:
			batch = 50;
			break;
		case INTERMEDIATE:
			batch = 24;
			break;
		case EXPERT:
			batch = 16;
			break;
		default:
			batch = 16;
			break;
		}
		return batch;
	}

	public void playSinglePointPB(Difficulty diff, String path) throws IOException {
		int gamesLostOnFirstMove = 0;
		int noOfThreads = Runtime.getRuntime().availableProcessors();
		int batch;
		batch = calcNoOfThreads(diff);
		List<GamePlayer> games = new ArrayList<>(batch);
		int lineCount = 0;
		while (lineCount < noOfGames) {
			// ExecutorService pool = Executors.newFixedThreadPool(noOfThreads);
			ExecutorService pool = Executors.newCachedThreadPool();

			try (BufferedReader br = new BufferedReader(new FileReader(path))) {
				int count = 0;
				for (int j = 0; j < lineCount; j++) {
					br.readLine();
				}
				for (String fieldJson; (fieldJson = br.readLine()) != null && count < batch;) {
					lineCount++;
					count++;
					GamePlayer player = new GamePlayer(diff, fieldJson);
					player.setSinglePoint(true);
					player.setPB(true);
					player.setFirstGuess(firstGuess);
					games.add(player);
				}
			}

			for (GamePlayer game : games) {
				pool.execute(game);
			}

			pool.shutdown();
			int print = 0;
			while (!pool.isTerminated()) {
				if (print % 20 == 0)
					System.out.println(pool);
				print++;
				try {
					Thread.sleep(500);
				} catch (InterruptedException e) {
					e.printStackTrace();
				}
			}

			for (GamePlayer gameSim : games) {
				totalTime = totalTime.add(BigInteger.valueOf(gameSim.getElapsedTime()));
				if (gameSim.isGameWon()) {
					winCount++;
					guessCount += gameSim.getGuessCount();
					winTimes = winTimes.add(BigInteger.valueOf(gameSim.getElapsedTime()));
				} else {
					if (gameSim.getGame().getNoOfMoves() == 1) {
						gamesLostOnFirstMove++;
					}
				}
			}
			games.clear();
		}
		if (firstGuess) {
			writeLine(diff.toString(), winCount, winTimes, guessCount, gamesLostOnFirstMove);
		} else {
			writeLine(diff.toString(), winCount, winTimes, guessCount);
		}
	}

	public void playerFull(Difficulty diff, String path) throws IOException {
		int noOfThreads = Runtime.getRuntime().availableProcessors();

		ExecutorService pool = Executors.newFixedThreadPool(noOfThreads);

		List<GamePlayer> games = new ArrayList<>(noOfGames);
		int limit = 0;

		try (BufferedReader br = new BufferedReader(new FileReader(path))) {
			for (String fieldJson; (fieldJson = br.readLine()) != null && limit < noOfGames;) {
				limit++;
				GamePlayer player = new GamePlayer(diff, fieldJson);
				player.setSinglePoint(true);
				player.setPB(true);
				player.setStrat(true);
				games.add(player);
			}
		}

		for (GamePlayer game : games) {
			pool.execute(game);
		}

		pool.shutdown();
		int consoleNum = 0;
		while (!pool.isTerminated()) {
			System.out.println(consoleNum++ + " - " + pool);
			try {
				Thread.sleep(10000);
			} catch (InterruptedException e) {
				e.printStackTrace();
			}
		}
		int winCount = 0;
		int guessCount = 0;
		BigInteger winTimes = BigInteger.ZERO;
		for (GamePlayer gameSim : games) {
			if (gameSim.isGameWon()) {
				winCount++;
				guessCount += gameSim.getGuessCount();
				winTimes = winTimes.add(BigInteger.valueOf(gameSim.getElapsedTime()));
			}
		}
		writeLine(diff.toString(), winCount, winTimes, guessCount);
		games.clear();
	}

	public void writeResults(String fileName) throws FileNotFoundException {
		try (PrintWriter writer = new PrintWriter(RESULT_DIR + fileName)) {
			writer.write(resultString.toString());
		}
	}

	public void writeTitle() {
		resultString.append("difficulty");
		resultString.append(",");
		resultString.append("no. of games");
		resultString.append(",");
		resultString.append("wins");
		resultString.append(",");
		resultString.append("loss");
		resultString.append(",");
		resultString.append("win (%)");
		resultString.append(",");
		resultString.append("avg. win time (m/s)");
		resultString.append(",");
		resultString.append("no. of guesses");
		resultString.append(",");
		// resultString.append("avg. guesses per win");
		// resultString.append(",");
		resultString.append("\n");
	}

	public void writeLine(String diff, int winCount, BigInteger gameTime, int guessCount) {
		resultString.append(diff);
		resultString.append(",");
		resultString.append(noOfGames);
		resultString.append(",");
		resultString.append(winCount);
		resultString.append(",");
		resultString.append(noOfGames - winCount);
		resultString.append(",");
		Fraction winPercent = new Fraction(winCount, noOfGames);
		resultString.append(winPercent.percentageValue());
		resultString.append(",");
		if (winCount == 0) {
			resultString.append("0");
		} else {
			BigFraction avgTime = new BigFraction(gameTime, BigInteger.valueOf(winCount));
			// Convert from nano to ms
			avgTime = avgTime.divide(BigInteger.valueOf(1000000));
			resultString.append(avgTime.doubleValue());
		}
		resultString.append(",");
		resultString.append(guessCount);
		resultString.append(",");
		// if (guessCount == 0) {
		// resultString.append("0");
		// } else {
		// Fraction guessAvg = new Fraction(guessCount, winCount);
		// resultString.append(guessAvg.doubleValue());
		// }
		// resultString.append(",");
		resultString.append("\n");
	}

	public void writeLine(String diff, int winCount, BigInteger gameTime, int guessCount, int gamesLostOnFirstMove) {
		resultString.append(diff);
		resultString.append(",");
		resultString.append(noOfGames - gamesLostOnFirstMove);
		resultString.append(",");
		resultString.append(winCount);
		resultString.append(",");
		resultString.append(noOfGames - gamesLostOnFirstMove - winCount);
		resultString.append(",");
		Fraction winPercent = new Fraction(winCount, noOfGames - gamesLostOnFirstMove);
		resultString.append(winPercent.percentageValue());
		resultString.append(",");
		if (winCount == 0) {
			resultString.append("0");
		} else {
			BigFraction avgTime = new BigFraction(gameTime, BigInteger.valueOf(winCount));
			// Convert from nano to ms
			avgTime = avgTime.divide(BigInteger.valueOf(1000000));
			resultString.append(avgTime.doubleValue());
		}
		resultString.append(",");
		resultString.append(guessCount);
		resultString.append(",");
		// if (guessCount == 0) {
		// resultString.append("0");
		// } else {
		// Fraction guessAvg = new Fraction(guessCount, winCount);
		// resultString.append(guessAvg.doubleValue());
		// }
		// resultString.append(",");
		resultString.append("\n");
	}
	
	

	public void resetResults() {
		resultString = new StringBuilder();
	}

	public void setPath(String path) {
		this.RESULT_DIR = path;
	}

	public static void main(String[] args) throws IOException, InterruptedException {

		LaunchSim s = new LaunchSim(10000, "resources/");
		s.startPBFirstGuessSim();

		System.out.println("\n\n\nDONE!!!!!");

		// Gson gson = new Gson();

		// PrintWriter writer;
		// try {
		// writer = new PrintWriter("resources/easyFields.txt", "UTF-8");
		// for (int i = 0; i < 10000; i++) {
		// writer.println(gson.toJson(new MineField(9, 9, 10)));
		// }
		// writer.close();
		// writer = new PrintWriter("resources/mediumFields.txt", "UTF-8");
		// for (int i = 0; i < 10000; i++) {
		// writer.println(gson.toJson(new MineField(16, 16, 40)));
		// }
		// writer.close();
		// writer = new PrintWriter("resources/hardFields.txt", "UTF-8");
		// for (int i = 0; i < 10000; i++) {
		// writer.println(gson.toJson(new MineField(16, 30, 99)));
		// }
		// writer.close();
		// } catch (FileNotFoundException | UnsupportedEncodingException e) {
		// e.printStackTrace();
		// }

		// GameSimulator player = new GameSimulator(Difficulty.BEGINNER,
		// "{\"field\":[[false,false,false,false,false,false,false,false,false],[false,false,false,false,false,false,false,false,false],[false,false,false,false,false,false,false,false,false],[false,false,false,false,false,false,false,false,false],[false,false,false,false,false,false,false,false,false],[true,true,false,false,false,false,false,false,false],[true,true,false,false,false,false,false,false,false],[true,true,true,false,false,false,false,false,false],[true,true,true,false,false,false,false,false,false]],\"exploded\":false,\"opened\":false}");
		// new LaunchSim();
	}
}
