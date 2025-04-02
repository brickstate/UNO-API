package Game_Logic;

import Game_Parts.Card;
import Game_Parts.Hand;
import Types.Color;
import Types.Value;

import java.util.Scanner;
import java.util.Random;

public class Game 
{
    Scanner kb = new Scanner(System.in);
    int num_players;
    int player_turn = 0;
    Hand hands[];
    Card topCard;

    public Game()
    {
       num_players = playerSetup();
       hands = new Hand[num_players];

       for(int i = 0; i < num_players; i++)
       {
            hands[i] = new Hand();
       }

        gameLoop();
    }    

    public int playerSetup()
    {
        System.out.println("How many player? Two minimum, four max.");

        while(true)
        {
            int num_of_players = this.kb.nextInt();

            if(num_of_players < 2 || num_of_players > 4)
            {
                System.out.println("Invalid input. Please try again.");
            }
            else
            {
                return num_of_players;
            }
        }
    }

    public Boolean is_Valid(Card card, Hand player_hand)
    {   
        if(card.value == topCard.value || card.color == topCard.color)
        {
            return true;
        }
        else if(card.value == Value.COLORSWITCH || card.color == Color.WILD)
        {
            return true;
        }
        else
        {
            return false;
        }
    }

    public Card initTopCard(Card topCard)
    {
        Random random = new Random();
        Color[] valuesColors = Color.values();
        Value[] valuesID = Value.values();
        this.topCard = new Card(valuesColors[random.nextInt(valuesColors.length)], valuesID[random.nextInt(valuesID.length)]);
        topCard.color = valuesColors[random.nextInt(valuesColors.length)];
        topCard.value = valuesID[random.nextInt(valuesID.length)];

        return topCard;
    }

    public void gameLoop()
    {
        initTopCard(topCard);
        while(true)
        {
            System.out.printf("Player %d's turn\n", player_turn + 1);
            System.out.printf("Player %d's cards\n", player_turn + 1);
            for(int i = 0; i < hands[player_turn].hand.size(); i++)
            {
                System.out.printf("Card %d:\n Color: %s\n", i + 1, hands[player_turn].hand.get(i).color);
                System.out.printf("Card Type: %s\n", hands[player_turn].hand.get(i).value);
            }
            System.out.println("What card do you want to play? Please select the number printed");
            int num_played = kb.nextInt();
            Boolean index_valid = hands[player_turn].hand.size() < num_played || num_played < 0;
            Card card_played = hands[player_turn].hand.get(num_played);

            while(!is_Valid(card_played, hands[player_turn]) || index_valid)
            {
                System.out.println("Invalid card. Please enter again.");

            }

            player_turn = (player_turn + 1) % hands.length;
        }
    }
}
