package Game_Parts;

import Game_Parts.Types.Color;
import Game_Parts.Types.Value;

import java.util.Random;
import java.util.ArrayList;

public class Hand 
{
    public ArrayList<Card> hand;
    
    public Hand()
    {
        this.hand = new ArrayList<Card>();

        for (int i = 0; i < 7; i++) 
        {
            hand.add(new Card(initialize_color(), initialize_Value()));
        }

    }

    public Color initialize_color()
    {
        Random random = new Random();
        Color[] values = Color.values();
        return values[random.nextInt(values.length)];
    }

    public Value initialize_Value()
    {
        Random random = new Random();
        Value[] values = Value.values();
        return values[random.nextInt(values.length)];
    }
}