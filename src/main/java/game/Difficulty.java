package main.java.game;

public enum Difficulty {
    BEGINNER(9, 9, 10), INTERMEDIATE(16, 16, 40), EXPERT(30, 16, 99);

    public final int width;
    public final int height;
    public final int mines;

    Difficulty(int width, int height, int mines) {
        this.width = width;
        this.height = height;
        this.mines = mines;
    }

}