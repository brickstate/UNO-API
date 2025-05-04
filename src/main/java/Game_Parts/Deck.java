package Game_Parts;
 
import java.util.ArrayList;
import java.util.Collections;

import Game_Parts.Types.Color;
import Game_Parts.Types.Value;
 
public class Deck 
{
    private int cardNum = 108;
    private ArrayList<Card> deck;
    private ArrayList<Card> discard;
 
    public Deck() 
    {
        deck = new ArrayList<Card>(108);
        discard = new ArrayList<Card>(108); // empty at first
 
        // Add number cards
        for (Color color : Color.values()) 
        {   
            if (color != Color.WILD) 
            {  // Skip WILD for number cards
                // Add one Zero card for each color
                deck.add(new Card(color, Value.ZERO));
                 
                // Add two of each number card 1-9 for each color
                for (Value id : new Value[] {Value.ONE, Value.TWO, Value.THREE, Value.FOUR, Value.FIVE, 
                    Value.SIX, Value.SEVEN, Value.EIGHT, Value.NINE}) 
                {
                    deck.add(new Card(color, id));
                    deck.add(new Card(color, id));
                }
                 
                 // Add two of each special colored card (SKIP, REVERSE, PLUSTWO)
                for (Value value : new Value[] {Value.SKIP, Value.REVERSE, Value.PLUSTWO}) 
                {
                    deck.add(new Card(color, value));
                    deck.add(new Card(color, value));
                }
            }
        }
         
        // Add wild cards (4 COLORSWITCH and 4 PLUSFOUR)
        for (int i = 0; i < 4; i++) 
        {
            deck.add(new Card(Color.WILD, Value.COLORSWITCH));
            deck.add(new Card(Color.WILD, Value.PLUSFOUR));
        }
 
        shuffle();
        //TODO maybe?  add shuffled deck into db for game id on initilization
            
        // Draw a card from the pile and put in discard pile
        // This is the starting card
        discard.add(drawCard());
    }
 
    public Card drawCard() 
    {
        Card c = deck.remove(deck.size() - 1);
        cardNum -= 1;
        return c;
    }

    public void playCard(Card c) {
        discard.add(c);
    }
 
    public void Card(Card c) 
    {
        discard.add(c);
    }
 
    public int getNumOfCard() 
    {
        return deck.size();
    }
 
     public Card[] drawMultipleCards(int count) 
    {
        Card[] s = new Card[count];
        for (int i = 0; i < count; i ++) 
        {
            s[i] = drawCard();
        }
        return s;
    }
 
    public void resetDeck()
    {
        if (isEmpty()) 
        {
            deck = discard;
        }
        else 
        {
            System.out.println("ERROR: Can not reset deck because deck is not empty!");
            System.out.println("Deck Size: " + deck.size());
        }
    }
 
     /*
      * gets the top card of the deck 
      */
    public Card peekDeck() 
    {
        return deck.get(cardNum - 1);
    }
 
     /*
      * gets the top card of the discard pile
      */
    public Card peekDiscard() 
    {
        return discard.get(discard.size() - 1);
    }
 
    public boolean isEmpty() 
    {
        return deck.size() == 0;
    }
 
    public void shuffle() 
    {
        Collections.shuffle(deck);
    }
 
    public void printDeck() 
    {
        for (Card c : deck) 
        {
            System.out.println("Color: " + c.color + "\t" + "Value: " + c.value);
        }
    }
 
    public void printDiscard() 
    {
        for (Card c : discard) 
        {
            System.out.println("Color: " + c.color + "\t" + "Value: " + c.value);
        }
    }

    public ArrayList<Card> getDeck()
    {
        return deck;
    }

    public String convertDeckToNumberedJson(ArrayList<Card> deck) {
        StringBuilder jsonBuilder = new StringBuilder();
        jsonBuilder.append("{\n");
    
        for (int i = 0; i < deck.size(); i++) {
            Card c = deck.get(i);
            String entry = String.format("  \"%d\" : \"%s %s\"", i + 1, c.color, c.value);
            jsonBuilder.append(entry);
    
            if (i < deck.size() - 1) {
                jsonBuilder.append(",\n");
            } else {
                jsonBuilder.append("\n");
            }
        }
    
        jsonBuilder.append("}");
        return jsonBuilder.toString();
    }

    //just gets the top one discard in json format { 1 : COLOR VALUE }
    public String convertDiscardToNumberedJson(Card Graveyard) {
        StringBuilder jsonBuilder = new StringBuilder();
        jsonBuilder.append("{\n");
    
        for (int i = 0; i < discard.size(); i++) {
    
            String entry = String.format("  \"%d\" : \"%s %s\"", i + 1, Graveyard.color, Graveyard.value);
            jsonBuilder.append(entry);
            //add a check here for reuse -- check to see if you only need one element and folowing ' , ' after each entry
                jsonBuilder.append("\n");
        }
    
        jsonBuilder.append("}");
        return jsonBuilder.toString();
    }

    
 }
