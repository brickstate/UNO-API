package Game_Logic;

import java.util.Random;
import java.util.Scanner;

import Game_Parts.Card;
import Game_Parts.Deck;
import Game_Parts.Hand;
import Game_Parts.Types.Color;
import Game_Parts.Types.Value;

public class Game 
{
    Scanner kb = new Scanner(System.in);
    int num_players;
    int player_turn = 0;
    Hand hands[];
    Card topCard;
    Deck deck;
    Boolean CPUgame = false;
    Boolean plusTwoPlayed = false;
    Boolean plusFourPlayed = false;
    int direction = 1;


    /*
     * Game constructor. Initializes the number of players, and starts the main
     * game loop.
     */
    public Game()
    {
        deck = new Deck(); // intialize deck
        num_players = playerSetup();
        hands = new Hand[num_players];

        for(int i = 0; i < num_players; i++)
        {
            hands[i] = new Hand(deck);
        }
        
        if(CPUgame)
        {   //assumes there is only (Player1 vs. CPU) playing
            CPUgameloop();
        }
        else
        {
            gameLoop(); // start game for humans
        }
        
    }    

    /*
     * 
     */
    public int playerSetup()
    {
         System.out.println("How many players? Two minimum, four max.");

        while(true)
        {
            int num_of_players = this.kb.nextInt();

            if(num_of_players < 2 || num_of_players > 4)
            {
                System.out.println("Invalid input. Please try again.");
            }
            else if(num_of_players == 2 )
            {
                System.out.println("Play against a computer? (yes/no)");
                String response = kb.next().toLowerCase();
                if (response.equals("yes"))
                {
                    //TODO Update "Is_CPU_Game" field in GameState Table
                    CPUgame = true;
                    return num_of_players;
                }
                else{
                    return num_of_players;
                }
            }
            else
            {
                return num_of_players;
            }
        }

    }

    public void updateTopCard()
    {
        topCard = deck.peekDiscard();
    }

    public static Boolean is_Valid(Card card, Card passedtopCard)
    {   
        if(card.value == passedtopCard.value || card.color == passedtopCard.color)
        {
            return true;
        }
        else if(card.value == Value.COLORSWITCH || card.color == Color.WILD || card.value == Value.PLUSFOUR)
        {
            return true;
        }
        else
        {
            return false;
        }
    }

    public Boolean handIsValid(Hand player_hand)
    {
        Boolean anyIsInvalid = false;

        for(int i = 0; i < hands[player_turn].hand.size(); i++)
        {
            if(is_Valid(player_hand.hand.get(i), this.topCard))
            {
                anyIsInvalid = anyIsInvalid | true;
            }
        }

        return anyIsInvalid;
    }

    public void gameLoop()
    {   
        updateTopCard(); // Initialize top card
        
        //TODO add error handling for kb input NAN's
        while(true)
        {
            System.out.printf("Top card color: %s\n", topCard.color);
            System.out.printf("Top card type: %s\n", topCard.value);
            //TODO Update "Top_Card" in Hands Table
            //     change last_updated time in game_state Tabl

            if(plusTwoPlayed)
            {
                //Draw two cards
                hands[player_turn].addCards(deck.drawMultipleCards(2));
                plusTwoPlayed = false;
            }
            else if(plusFourPlayed)
            {
                //Draw four cards
                hands[player_turn].addCards(deck.drawMultipleCards(4));
                plusFourPlayed = false;
            }

            //TODO 1. add in USERNAME to print to the screen
            //     2. add "COMPUTER's turn" to print for cpu
            //     change last_updated time in game_state Tabl

            System.out.printf("Player %d's turn\n", player_turn + 1);
            System.out.printf("Player %d's cards\n", player_turn + 1);

            while (!handIsValid(hands[player_turn]))
            {
                //Draw cards until a valid card can be played
                hands[player_turn].addCard(deck.drawCard());
            }

            // Prints a player's hand
            for(int i = 0; i < hands[player_turn].hand.size(); i++)
            {
                System.out.printf("Card %d:\n Color: %s\n", i + 1, hands[player_turn].hand.get(i).color);
                System.out.printf(" Card Type: %s\n", hands[player_turn].hand.get(i).value);
            }
            //TODO Update current Player# In_Hand field for Hands Table
            //     should be able to keep track of the actual cards in hand
            //     change last_updated time in game_state Tabl



            //Process of playing a card with input and card validation
            System.out.println("What card do you want to play? Please select the number printed");
            int num_played = kb.nextInt() - 1;
            Boolean index_valid = num_played >= 0 && num_played < hands[player_turn].hand.size();
            Card card_played = hands[player_turn].hand.get(num_played);

            while(!index_valid || !is_Valid(card_played, this.topCard))
            {   
                System.out.println("=================================");
                System.out.println("is_Valid: " + is_Valid(card_played, this.topCard));
                System.out.println("index_valid: " + index_valid);
                System.out.println("=================================");
                System.out.println("Invalid card. Please enter again.");
                num_played = kb.nextInt() - 1;

                // update index and card chosen 
                index_valid = num_played >= 0 && num_played < hands[player_turn].hand.size();
                card_played = hands[player_turn].hand.get(num_played);
            }

            //TODO format output as "USERNAME played card color: ??"
            //     change last_updated time in game_state Tabl
            System.out.printf("Card played color: %s\n", card_played.color);
            System.out.printf("Card played value: %s\n", card_played.value);
           
            deck.playCard(hands[player_turn].hand.remove(num_played)); // place the card down on discard pile
            // TODO 1.Update Discard field in Hands Table
            //      2.Update this.Player# hand field in Hands Table 
            //     change last_updated time in game_state Tabl

            if(hands[player_turn].hand.size() == 0)
            {   
                //TODO Update game_state table's winner field
                //     Trigger a POST for new entry in Completed_Games Table
                //     change last_updated time in game_state Table

                System.out.printf("Player %d wins!\n", player_turn + 1);
                break;
            }

            // Effects for special cards
            if(card_played.value == Value.SKIP)
            {
                updateTopCard();
                player_turn = (player_turn + direction + hands.length + 1) % hands.length;
            }
            else if(card_played.value == Value.REVERSE)
            {
                updateTopCard();
                player_turn = (player_turn + direction + hands.length) % hands.length;
            }
            else if(card_played.value == Value.COLORSWITCH)
            {
                Boolean colorIsValid = false;

                kb.nextLine(); // eat newline
                System.out.println("Please choose a color. The options are blue, yellow, red, green");
                String colorChosen = kb.nextLine().toUpperCase();

                while(!colorIsValid)
                {
                    if (colorChosen.equals("BLUE") || colorChosen.equals("YELLOW") ||
                        colorChosen.equals("RED") || colorChosen.equals("GREEN"))
                    {
                        colorIsValid = true;
                    }
                    else
                    {
                        System.out.println("Invalid color. Please try again.");
                        colorChosen = kb.nextLine().toUpperCase(); // update and lowercase again
                    }
                }

                // Update color of top card to color chosen by player from wild card
                card_played.color = Color.valueOf(colorChosen.toUpperCase());
                
                if(colorChosen.equals("BLUE"))
                {
                    updateTopCard();
                }
                else if(colorChosen.equals("YELLOW"))
                {
                    updateTopCard();
                }
                else if(colorChosen.equals("RED"))
                {
                    updateTopCard();
                }
                else if(colorChosen.equals("GREEN"))
                {
                    updateTopCard();
                }

                player_turn = (player_turn + direction + hands.length) % hands.length;
            }
            else if(card_played.value == Value.PLUSFOUR)
            {
                Boolean colorIsValid = false;

                kb.nextLine(); // eat newline
                System.out.println("Please choose a color. The options are blue, yellow, red, green");
                String colorChosen = kb.nextLine().toUpperCase();

                while(!colorIsValid)
                {
                    if (colorChosen.equals("BLUE") || colorChosen.equals("YELLOW") ||
                        colorChosen.equals("RED") || colorChosen.equals("GREEN"))
                    {
                        colorIsValid = true;
                    }
                    else
                    {
                        System.out.println("Invalid color. Please try again.");
                        colorChosen = kb.nextLine().toUpperCase(); // update and lowercase again
                    }
                }
                
                // Update color of top card to color chosen by player from wild card
                card_played.color = Color.valueOf(colorChosen.toUpperCase());

                if(colorChosen.equals("BLUE"))
                {
                    updateTopCard();
                }
                else if(colorChosen.equals("YELLOW"))
                {
                    updateTopCard();
                }
                else if(colorChosen.equals("RED"))
                {
                    updateTopCard();
                }
                else if(colorChosen.equals("GREEN"))
                {
                    updateTopCard();
                }
                
                plusFourPlayed = true;

                player_turn = (player_turn + direction + hands.length) % hands.length;
            }
            else if(card_played.value == Value.PLUSTWO)
            {
                plusTwoPlayed = true;
                updateTopCard();
                player_turn = (player_turn + direction + hands.length) % hands.length;
            }
            else
            {
                updateTopCard();
                player_turn = (player_turn + direction + hands.length) % hands.length;
            }
        }
    }


    //EVERYTHING BELOW HERE IS USED FOR CPU GAME LOGIC
    //tldr its basically the same exact method at gameloop()
    //     BUT assumes player2 is a CPU and will span inputs until valid
    // Clean up outputs so you can see what CPU cards are played
     public String getRandomColor() // should select a color at random
    {
        String[] colors = {"Blue", "Red", "Green", "Yellow"};
        Random rand = new Random();
        int index = rand.nextInt(colors.length); // generates a number between 0 and 3
        return colors[index];
    }

    public void CPUgameloop()
    {
        updateTopCard(); // Initialize top card
        //assume that first turn will never be the computer player 
        Boolean computerTurn = false;

        while(true)
        {   
            if(player_turn == 0)
            {   // Player1 is never CPU
                computerTurn = false;
            }
            else if (player_turn == 1)
            {   // Player2 IS a cpu 
                computerTurn = true;
            }
            

            System.out.printf("Top card color: %s\n", topCard.color);
            System.out.printf("Top card type: %s\n", topCard.value);
            if(plusTwoPlayed)
            {
                //Draw two cards
                hands[player_turn].addCards(deck.drawMultipleCards(2));
                plusTwoPlayed = false;
            }
            else if(plusFourPlayed)
            {
                //Draw four cards
                hands[player_turn].addCards(deck.drawMultipleCards(4));
                plusFourPlayed = false;
            }

            if(!computerTurn) //if !computerTurn should detect if the computer is playing and just not display their cards
            { //if bugs then remove this if wrapping
                System.out.printf("Player %d's turn\n", player_turn + 1);
                System.out.printf("Player %d's cards\n", player_turn + 1);
            }
            
            while (!handIsValid(hands[player_turn]))
            {
                //Draw cards until a valid card can be played
                hands[player_turn].addCard(deck.drawCard());
            }

            if(!computerTurn)
            {
                for(int i = 0; i < hands[player_turn].hand.size(); i++)
                {
                    System.out.printf("Card %d:\n Color: %s\n", i + 1,
                                        hands[player_turn].hand.get(i).color);
                    System.out.printf(" Card Type: %s\n", 
                                        hands[player_turn].hand.get(i).value);
                }
            }
            
            if(!computerTurn)
            {
                System.out.println("What card do you want to play? Please select the number printed");
            }

            //trying to jam a valid number if it is the computer player 
            // (will literally play anything that is the first valid card it sees #1 --> #7)
            int num_played;
            if(computerTurn)
            { // trying to jam numbers her until valid // very stupid computer player logic 
                num_played = 0;
                while (is_Valid(hands[player_turn].hand.get(num_played), this.topCard) == false)
                {
                    num_played++;
                }
                
            }
            else 
            {
                num_played = kb.nextInt() - 1;
            }
            
            //og was here
            //num_played = kb.nextInt() - 1;
            Boolean index_valid = num_played >= 0 && num_played < hands[player_turn].hand.size();
            Card card_played = hands[player_turn].hand.get(num_played);

            while(!index_valid || !is_Valid(card_played, this.topCard))
            {
                System.out.println("=================================");
                System.out.println("is_Valid: " + is_Valid(card_played, this.topCard));
                System.out.println("index_valid: " + index_valid);
                System.out.println("=================================");
                System.out.println("Invalid card. Please enter again.");
                num_played = kb.nextInt() - 1;

                // update index and card chosen 
                index_valid = num_played >= 0 && num_played < hands[player_turn].hand.size();
                card_played = hands[player_turn].hand.get(num_played);
            }

            System.out.printf("Card played color: %s\n", card_played.color);
            System.out.printf("Card played value: %s\n", card_played.value);
           
            deck.playCard(hands[player_turn].hand.remove(num_played)); // place the card down on discard pile

            if(hands[player_turn].hand.size() == 0)
            {
                System.out.printf("Player %d wins!\n", player_turn + 1);
                break;
            }

            if(card_played.value == Value.SKIP)
            {
                updateTopCard();
                player_turn = (player_turn + direction + hands.length + 1) % hands.length;
            }
            else if(card_played.value == Value.REVERSE)
            {
                updateTopCard();
                player_turn = (player_turn + direction + hands.length) % hands.length;
            }
            else if(card_played.value == Value.COLORSWITCH)
            {
                Boolean colorIsValid = false;

                kb.nextLine(); // eat newline
                
                String colorChosen = "";
                if(computerTurn)
                {//say random color 
                    colorChosen = getRandomColor().toUpperCase();
                }
                else
                {
                    System.out.println("Please choose a color. The options are blue, yellow, red, green");
                    colorChosen = kb.nextLine().toUpperCase();
                }
                
                while(!colorIsValid)
                {
                    if (colorChosen.equals("BLUE") || colorChosen.equals("YELLOW") ||
                        colorChosen.equals("RED") || colorChosen.equals("GREEN"))
                    {
                        colorIsValid = true;
                    }
                    else
                    {
                        System.out.println("Invalid color. Please try again.");
                        colorChosen = kb.nextLine().toUpperCase(); // update and lowercase again
                    }
                }

                // Update color of top card to color chosen by player from wild card
                card_played.color = Color.valueOf(colorChosen.toUpperCase());
                
                if(colorChosen.equals("BLUE"))
                {
                    updateTopCard();
                }
                else if(colorChosen.equals("YELLOW"))
                {
                    updateTopCard();
                }
                else if(colorChosen.equals("RED"))
                {
                    updateTopCard();
                }
                else if(colorChosen.equals("GREEN"))
                {
                    updateTopCard();
                }

                player_turn = (player_turn + direction + hands.length) % hands.length;
            }
            else if(card_played.value == Value.PLUSFOUR)
            {
                Boolean colorIsValid = false;

                kb.nextLine(); // eat newline
                System.out.println("Please choose a color. The options are blue, yellow, red, green");
                String colorChosen = "";

                if(computerTurn)
                {//say random color
                    colorChosen = getRandomColor().toUpperCase();
                }
                else
                {
                    colorChosen = kb.nextLine().toUpperCase();
                }
                
                while(!colorIsValid)
                {
                    if (colorChosen.equals("BLUE") || colorChosen.equals("YELLOW") ||
                        colorChosen.equals("RED") || colorChosen.equals("GREEN"))
                    {
                        colorIsValid = true;
                    }
                    else
                    {
                        System.out.println("Invalid color. Please try again.");
                        colorChosen = kb.nextLine().toUpperCase(); // update and lowercase again
                    }
                }
                
                // Update color of top card to color chosen by player from wild card
                card_played.color = Color.valueOf(colorChosen.toUpperCase());

                if(colorChosen.equals("BLUE"))
                {
                    updateTopCard();
                }
                else if(colorChosen.equals("YELLOW"))
                {
                    updateTopCard();
                }
                else if(colorChosen.equals("RED"))
                {
                    updateTopCard();
                }
                else if(colorChosen.equals("GREEN"))
                {
                    updateTopCard();
                }
                
                plusFourPlayed = true;

                player_turn = (player_turn + direction + hands.length) % hands.length;
            }
            else if(card_played.value == Value.PLUSTWO)
            {
                plusTwoPlayed = true;
                updateTopCard();
                player_turn = (player_turn + direction + hands.length) % hands.length;
            }
            else
            {
                updateTopCard();
                player_turn = (player_turn + direction + hands.length) % hands.length;
            }
        }

    }

    //EVERYTHING BELOW HERE IS JUST USED AS HELP FOR ENDPOINTS

    //Takes an index for the number played and the playerHand. Makes sure it can be played 
    //and returns the card if it can be. Otherwise return null.
    public static Card playCard(int num_played, Hand playerHand, Card passedTopCard)
    {
        Card card_played = playerHand.hand.get(num_played);
        
        // If the card cannot be played, return nothing
        if(!is_Valid(card_played, passedTopCard))
        {
            return null;
        }

        card_played = playerHand.hand.get(num_played);
            
        System.out.printf("Card played color: %s\n", card_played.color);
        System.out.printf("Card played value: %s\n", card_played.value);
           
        return card_played;
    }
}