
public class GameLauncher {

	public static void main(String[] args) {

		int x = 9; // Width of the board
		int y = 9; // Height of the board
		double diff = 0.2; // The difficulty of the game (percent of cells that are mines)
		int mines = 2; // Integer number of mines on the board

		// Example of the JSON format needed to create a MineField
		// Note: no dimentions or number of mines stored (just essentially a 2D array of
		// booleans; true = mine)
		// dimentions and mine count need to be passed manually
		String mineFeildJson = "{\"field\":[[true,false,false,false,false,false,false,false,false],[false,false,false,false,false,false,false,false,false],[false,false,true,false,false,false,true,false,false],[false,false,false,true,false,false,false,false,true],[false,false,false,false,false,true,false,false,true],[false,false,false,false,false,false,false,false,false],[false,false,false,false,false,true,false,false,false],[false,false,false,false,false,false,true,false,false],[false,false,false,false,false,false,false,false,true]],\"exploded\":false,\"opened\":false}";

		Minesweeper game = new Minesweeper(); // Default beginner board
		game.buildGUI();
		// Example of how load a game with a pre-generated MineField JSON
		// new Minesweeper(9, 9, 10, mineFeildJson);
	}
}