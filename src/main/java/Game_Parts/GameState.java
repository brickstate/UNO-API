package Game_Parts;

import java.util.ArrayList;
import java.util.List;

public class GameState {
    public int gameId;
    public List<Player> players = new ArrayList<>();
    public Deck deck = new Deck();
    public List<Card> discardPile = new ArrayList<>();
    public int currentTurn = 0;
    public String direction = "clockwise"; // or "counterclockwise"
    public String activeEffect = null; // like "draw2", "skip", "wild+4"
    public String chosenColor = null;  // for wild cards
    public boolean isGameOver = false;
    public String winner = null;

    // Optional: constructor, getters/setters if you want them
}
