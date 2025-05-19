import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Setter
@Getter
@NoArgsConstructor
@AllArgsConstructor
public class Package {
    private String name;
    private int weightInKg;
    private Node startingNode;
    private Node endNode;
}