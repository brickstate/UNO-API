package Game_Parts;

import java.util.ArrayList;
import java.util.List;

public class Player 
{
    public String username;
    public List<Card> hand = new ArrayList<>();

    public Player() {}

    public Player(String username) 
    {
        this.username = username;
    }
}

