import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Setter
@Getter
@NoArgsConstructor
@AllArgsConstructor
public class Edge {
    private String name;
    private Node startStation;
    private Node endStation;
    private int journeyTimeInMinutes;

    @Override
    public String toString() {
        return String.format("Edge{name='%s', from='%s' -> to='%s', time=%d mins}",
                name,
                startStation != null ? startStation.getName() : "null",
                endStation != null ? endStation.getName() : "null",
                journeyTimeInMinutes);
    }

}