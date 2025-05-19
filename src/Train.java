import lombok.*;

import java.util.ArrayList;
import java.util.List;

@Setter
@Getter
@RequiredArgsConstructor
public class Train {
    @NonNull
    private String name;

    @NonNull
    private int capacityInKg;
    @NonNull
    private String currentStation;


    private int currentLoad=0;
    private List<Package> packages;


    private int currentTime=0;
    private List<String> log = new ArrayList<>();
}