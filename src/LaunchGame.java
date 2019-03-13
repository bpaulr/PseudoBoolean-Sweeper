
public class LaunchGame {

	public static void main(String[] args) {
        
        int x = 3; // Width of the board
        int y = 3; // Height of the board
        double diff = 0.2; // The difficulty of the game (percent of cells that are mines)
        int mines = 2; // Integer number of mines on the board
        assert diff >= 0.00 && diff < 1.00 && mines >= 0 && mines < (x * y);
        
        new Minesweeper();
        // new Minesweeper(Difficulty.INTERMEDIATE);
        // new Minesweeper(Difficulty.EXPERT);

        // new Minesweeper(x, y, diff); // Constructor for % mines
        // new Minesweeper(x, y, mines); // Constructor for int mines
	}
}