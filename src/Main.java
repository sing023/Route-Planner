import java.util.*;
import java.util.stream.Collectors;

//TIP To <b>Run</b> code, press <shortcut actionId="Run"/> or
// click the <icon src="AllIcons.Actions.Execute"/> icon in the gutter.
public class Main {

    private static final Scanner sc = new Scanner(System.in);

    public static List<Package> deliveries = new ArrayList<>();
    public static List<Train> trains = new ArrayList<>();

    public static void main(String[] args) {

        System.out.println("Pls enter the no of stations: ");
        int noStations= inputInt();
        for (int i=0; i<noStations; i++) {
            System.out.println(String.format("Pls enter the name of station %d: ",i+1));
            String readStation = readStation(ComputePath.stations);
            ComputePath.graph.putIfAbsent(readStation,new ArrayList<>());
        }

        getNoStations();
        System.out.print("Pls enter the no of edges: ");
        int noEdges = inputInt();
        for (int i=0; i<noEdges; i++) {
            System.out.println(String.format("Pls enter the edge no %d: ",i+1));
            readEdge(ComputePath.stations);
        }

        ComputePath.computeAllShortestPath();

        System.out.print("Pls enter the no of deliveries: ");
        int noDeliveries = inputInt();
        for (int i = 0; i < noDeliveries; i++) {
            System.out.println(String.format("Pls enter the delivery no %d: ",i+1));
            readDelivery(ComputePath.stations);
        }

        System.out.print("Pls enter the no of trains: ");
        int noTrains = inputInt();
        for (int i = 0; i < noTrains; i++) {
            System.out.println(String.format("Pls enter the train no %d: ",i+1));
            readTrain(ComputePath.stations);
        }

        DeliveryPlanner.assignPackage(trains, deliveries);
    }

    private static void getNoStations() {
        System.out.print("Stations: [");
        System.out.print(ComputePath.stations.stream().map(Object::toString).collect(Collectors.joining(", ")));
        System.out.print("]");
        System.out.print("\n");
    }

    private static void readTrain(Set<String> stations) {
        while (true) {
            String line = sc.nextLine().trim();
            String[] parts = line.split(",");
            if (parts.length != 3) {
                System.out.print("The train should contain a name,capacity and current station.Please re-enter the entire train again:");
                continue;
            }
            String currentStation = parts[2].trim();

            if (!stations.contains(currentStation)) {
                getNoStations();
                System.out.print(String.format("The station %s doesnt exist.Please re-enter the entire train again: ",currentStation));
                continue;
            }
            try {
                int capacity = Integer.parseInt(parts[1].trim());
                if (capacity <= 0) {
                    System.out.print("The capacity must be +ve.Please re-enter the entire train again: ");
                    continue;
                }
            } catch (Exception e) {
                System.out.print("The capacity value is not valid.Please re-enter the entire train again: ");
                continue;
            }
            trains.add(new Train(parts[0],Integer.parseInt(parts[1].trim()),currentStation));
            break;
        }
    }

    private static void readDelivery(Set<String> stations) {
        while (true) {
            String line = sc.nextLine().trim();
            String[] parts = line.split(",");
            if (parts.length != 4) {
                System.out.print("The delivery should contain a package name,weight of package,start station name & end station name.Please re-enter the entire delivery again: ");
                continue;
            }
            String startStation = parts[2].trim();
            String endStation = parts[3].trim();

            if (!stations.contains(startStation)) {
                getNoStations();
                System.out.print(String.format("The station %s doesnt exist.Please re-enter the entire delivery again: ",startStation));
                continue;
            }
            if (!stations.contains(endStation)) {
                getNoStations();
                System.out.print(String.format("The station %s doesnt exist.Please re-enter the entire delivery again: ",endStation));
                continue;
            }
            try {
                int weight = Integer.parseInt(parts[1].trim());
                if (weight <= 0) {
                    System.out.print("The weight must be +ve.Please re-enter the entire delivery again: ");
                    continue;
                }
            } catch (Exception e) {
                System.out.print("The weight value is not valid.Please re-enter the entire delivery again: ");
                continue;
            }
            deliveries.add(new Package(parts[0],Integer.parseInt(parts[1].trim()),new Node(startStation),new Node(endStation)));
            break;
        }
    }

    private static void readEdge(Set<String> stations) {
        while (true) {
            String line = sc.nextLine().trim();
            String[] parts = line.split(",");
            if (parts.length != 4) {
                System.out.print("The edge should contain a name, start station name, end station name & the time taken.Please re-enter the entire edge again: ");
                continue;
            }
            String startStation = parts[1].trim();
            String endStation = parts[2].trim();

            if (!stations.contains(startStation)) {
                getNoStations();
                System.out.print(String.format("The station %s doesnt exist.Please re-enter the entire edge again:  ",startStation));
                continue;
            }
            if (!stations.contains(endStation)) {
                getNoStations();
                System.out.print(String.format("The station %s doesnt exist.Please re-enter the entire edge again:  ",endStation));
                continue;
            }
            try {
                int duration = Integer.parseInt(parts[3].trim());
                if (duration < 0) {
                    System.out.print("The journey time must be +ve.Please re-enter the entire edge again:  ");
                    continue;
                }
            } catch (Exception e) {
                System.out.print("The duration value is not valid.Please re-enter the entire edge again:  ");
                continue;
            }
            ComputePath.graph.get(startStation).add(new Edge(parts[0],new Node(startStation),new Node(endStation),Integer.parseInt(parts[3].trim())));
            ComputePath.graph.get(endStation).add(new Edge(parts[0],new Node(endStation),new Node(startStation),Integer.parseInt(parts[3].trim())));
            break;
        }
    }

    private static String readStation(Set<String> stations) {
        while (true) {
            String station = sc.nextLine().trim();
            if (station.isEmpty()) {
                System.out.print("Station name is empty.Please enter a name: ");
            } else if (stations.contains(station)) {
                getNoStations();
                System.out.print("Station already exists.Please enter a different name: ");
            } else {
                stations.add(station);
                return station;
            }
        }
    }


    private static int inputInt() {
        while (true) {
            String input = sc.nextLine().trim();
            try  {
                int intInput = Integer.parseInt(input);
                if ( intInput > 0) {
                    return intInput;
                }   else {
                    throw new Exception();
                }
            }   catch (Exception e) {
                    System.out.print("Number input is not proper.Pls try again: ");
            }
        }
    }
}