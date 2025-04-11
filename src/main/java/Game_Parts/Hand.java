package Game_Parts;

import Game_Parts.Types.Color;
import Game_Parts.Types.Value;

import java.util.Random;
import java.util.ArrayList;

public class Hand 
{
    public ArrayList<Card> hand;
    
    public Hand(Deck deck)
    {
        this.hand = new ArrayList<Card>();

        for (int i = 0; i < 7; i++) 
        {
            hand.add(deck.drawCard());
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

    public void addCard(Card card) {
        hand.add(card);
    }

    public void addCards(Card[] cards) {
        for (Card c : cards) {
            hand.add(c);
        }
    }
}