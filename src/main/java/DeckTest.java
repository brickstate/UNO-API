import java.util.ArrayList;
import java.util.HashMap;
import java.util.Iterator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ObjectNode;

import Game_Parts.Card;
import Game_Parts.Deck;

public class DeckTest {
    public static void main(String[] args) {

        /*
        // just run this class to see how the current deck system handles initialization and array list for [card COLOR VALUE]

        // Create a new deck
        Deck testDeck = new Deck();

        // Print entire deck using the Deck method
        //System.out.println("=== Printing Deck via printDeck() ===");
        //testDeck.printDeck();

        //BEST : Accessing deck cards directly using a getter
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
         */

    /////////////////////////////////////////////////////////////
        // WORKING ON BUG 
        // TODO fix weird json // why is it printing so ?
        Deck testDeck = new Deck();
        ArrayList<Card> cards = testDeck.getDeck(); 
        String deckjson = testDeck.convertDeckToNumberedJson(cards);
        Card topCard = testDeck.peekDiscard();
        String topCardjson = testDeck.convertDiscardToNumberedJson(topCard);    

        System.out.print("\n///// JSON DRAW PILE  /////// \n");
        String deckJSON = testDeck.convertDeckToNumberedJson(cards); 
        System.out.print(deckJSON);

        System.out.print("\n///// JSON DISCARD PILE  /////// \n");
        Card discard = testDeck.peekDiscard(); // You need to add a getter method if not present
        System.out.print(testDeck.convertDiscardToNumberedJson(discard));


        //testing what happens with cpu card handouts
        ObjectMapper mapper = new ObjectMapper();
        Map<String, String> drawdeck;
        try {
            drawdeck = mapper.readValue(deckJSON, LinkedHashMap.class);

            List<Integer> sortedKeys = drawdeck.keySet().stream()
            .map(Integer::parseInt)
            .sorted()
            .collect(Collectors.toList());

            ObjectNode drawnJSON = mapper.createObjectNode();
            ObjectNode updatedJSON = mapper.createObjectNode();

            ObjectNode reindexedUpdatedJSON = mapper.createObjectNode();
            int newIndex = 1;

            
    
            for (int i = 0; i < sortedKeys.size(); i++) {
                String key = String.valueOf(sortedKeys.get(i));
                if (i < 7) {
                    drawnJSON.put(key, drawdeck.get(key));
                } else {
                    updatedJSON.put(key, drawdeck.get(key));
                }
            }

            //important for updating draw deck keys
            for (Iterator<String> it = updatedJSON.fieldNames(); it.hasNext(); ) {
                String oldKey = it.next();
                String value = updatedJSON.get(oldKey).asText();
                reindexedUpdatedJSON.put(String.valueOf(newIndex++), value);
            }
    
                    // Extract first 7 cards

        Map<String, String> result = new HashMap<>();
        try {
            result.put("drawn", mapper.writerWithDefaultPrettyPrinter().writeValueAsString(drawnJSON));
        } catch (JsonProcessingException e) {
            // TODO Auto-generated catch block
            e.printStackTrace();
        }
        try {
            result.put("updated", mapper.writerWithDefaultPrettyPrinter().writeValueAsString(reindexedUpdatedJSON));
        } catch (JsonProcessingException e) {
            e.printStackTrace();
        }

        System.out.print(result.toString());


        } catch (JsonProcessingException e) {
            // TODO Auto-generated catch block
            e.printStackTrace();
        }
        
    }
}
