package Game_Parts;

import Game_Parts.Types.Color;
import Game_Parts.Types.Value;

public class Card 
{
    public Color color;
    public Value value;

    public Card(Color color, Value value)
    {
        this.color = color;
        this.value = value;
    }
}
