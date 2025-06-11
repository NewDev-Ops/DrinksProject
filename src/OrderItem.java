
import java.io.*;

public class OrderItem implements Serializable {
    public int drinkId;
    public int quantity;

    public OrderItem(int drinkId, int quantity) {
        this.drinkId = drinkId;
        this.quantity = quantity;
    }

}
