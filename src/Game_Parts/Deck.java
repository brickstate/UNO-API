package Game_Parts;

import java.util.ArrayList;
import java.util.Collections;

import Types.Color;
import Types.ID;

public class Deck {
    private int cardNum = 108;
    private ArrayList<Card> deck;
    private ArrayList<Card> discard;

    public Deck() {
        deck = new ArrayList<Card>(108);
        discard = new ArrayList<Card>(108); // empty at first

        // Add number cards
        for (Color color : Color.values()) {
            if (color != Color.WILD) {  // Skip WILD for number cards
                // Add one Zero card for each color
                deck.add(new Card(color, ID.ZERO));
                
                // Add two of each number card 1-9 for each color
                for (ID id : new ID[] {ID.ONE, ID.TWO, ID.THREE, ID.FOUR, ID.FIVE, 
                                    ID.SIX, ID.SEVEN, ID.EIGHT, ID.NINE}) {
                    deck.add(new Card(color, id));
                    deck.add(new Card(color, id));
                }
                
                // Add two of each special colored card (SKIP, REVERSE, PLUSTWO)
                for (ID id : new ID[] {ID.SKIP, ID.REVERSE, ID.PLUSTWO}) {
                    deck.add(new Card(color, id));
                    deck.add(new Card(color, id));
                }
            }
        }
        
        // Add wild cards (4 COLORSWITCH and 4 PLUSFOUR)
        for (int i = 0; i < 4; i++) {
            deck.add(new Card(Color.WILD, ID.COLORSWITCH));
            deck.add(new Card(Color.WILD, ID.PLUSFOUR));
        }

        shuffle();
    }

    public Card drawCard() {
        Card c = deck.remove(cardNum - 1);
        cardNum -= 1;
        return c;
    }

    public void Card(Card c) {
        discard.add(c);
    }

    public int getNumOfCard() {
        return deck.size();
    }

    public Card[] drawMultipleCards(int count) {
        Card[] s = new Card[count];
        for (int i = 0; i < count; i ++) {
            s[i] = drawCard();
        }
        return s;
    }

    public void resetDeck() {
        if (isEmpty()) {
            deck = discard;
        }
        else {
            System.out.println("ERROR: Can not reset deck because deck is not empty!");
            System.out.println("Deck Size: " + deck.size());
        }
    }

    /*
     * gets the top card of the deck 
     */
    public Card peekDeck() {
        return deck.get(cardNum - 1);
    }

    /*
     * gets the top card of the discard pile
     */
    public Card peekDiscard() {
        return discard.get(discard.size() - 1);
    }

    public boolean isEmpty() {
        return deck.size() == 0;
    }

    public void shuffle() {
        Collections.shuffle(deck);
    }

    public void printDeck() {
        for (Card c : deck) {
            System.out.println("Color: " + c.color + "\t" + "Value: " + c.type);
        }
    }

    public void printDiscard() {
        for (Card c : discard) {
            System.out.println("Color: " + c.color + "\t" + "Value: " + c.type);
        }
    }
}
