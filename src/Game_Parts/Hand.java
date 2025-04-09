package Game_Parts;

import Types.Color;
import Types.Value;

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
            hand.add(new Card(initialize_color(), initialize_ID()));
        }

    }

    public Color initialize_color()
    {
        Random random = new Random();
        Color[] values = Color.values();
        return values[random.nextInt(values.length)];
    }

    public Value initialize_ID()
    {
        Random random = new Random();
        Value[] values = Value.values();
        return values[random.nextInt(values.length)];
    }

    /*
     * adds a set of cards to the hand
     */
    public void addCards(Card[] cards) {
        for (Card c : cards) {
            hand.add(c);
        }
    }

    /*
     * adds a single card to the hand
     */
    public void addCard(Card card) {
        hand.add(c);
    }
}