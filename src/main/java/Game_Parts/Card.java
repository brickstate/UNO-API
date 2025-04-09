package Game_Parts;
import Types.Color;
import Types.ID;

public class Card 
{
    public Color color;
    public Value type;

    public Card(Color color, Value value)
    {
        this.color = color;
        this.value = value;
    }
}
