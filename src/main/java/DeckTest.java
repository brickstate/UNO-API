import java.util.ArrayList;

import Game_Parts.Card;
import Game_Parts.Deck;

public class DeckTest {
    public static void main(String[] args) {
        // just run this class to see how the current deck system handles initialization and array list for [card COLOR VALUE]

        // Create a new deck
        Deck testDeck = new Deck();

        // Print entire deck using the Deck method
        //System.out.println("=== Printing Deck via printDeck() ===");
        //testDeck.printDeck();

        // Optional: Accessing deck cards directly using a getter
        System.out.println("\n=== Printing Deck via custom loop (accessing cards directly) ===\n");
        ArrayList<Card> cards = testDeck.getDeck(); // You need to add a getter method if not present
        for (Card c : cards) {
            System.out.println("Card: " + c.color + " " + c.value);
        }



        // You could add more tests here (like drawCard, shuffle, reset, etc.)
        System.out.print("\n///// JSON DRAW PILE  /////// \n");
        System.out.print(testDeck.convertDeckToNumberedJson(cards));


        // You could add more tests here (like drawCard, shuffle, reset, etc.)
        System.out.print("\n///// JSON DISCARD PILE  /////// \n");
        Card discard = testDeck.peekDiscard(); // You need to add a getter method if not present
        //System.out.println("Card: " + discard.color + " " + discard.value);
        System.out.print(testDeck.convertDiscardToNumberedJson(discard));
        
    }
}
