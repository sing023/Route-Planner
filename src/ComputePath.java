import java.util.*;

public class ComputePath {
    public static Set<String> stations = new HashSet<>();

    public static Map<String, List<Edge>> graph = new HashMap<>();

    public static Map<String, Map<String, TrainRoute>> shortestPathToEveryNode = new HashMap<>();

    static void computeAllShortestPath() {
        for(String node: graph.keySet()) {
            shortestPathToEveryNode.put(node, djikstra(node));
        }

        //for debugging purposes
        for(Map.Entry<String,List<Edge>> node: graph.entrySet()) {
            System.out.println(node.getKey()+ ": ");
            for(Edge edge: node.getValue()) {
                System.out.println("\t"+edge.getStartStation().getName() + " -> "+edge.getEndStation().getName() + " | journeyTime: "+edge.getJourneyTimeInMinutes());
            }
        }

        //for debugging purposes
        for(Map.Entry<String, Map<String, TrainRoute>> outerEntry: shortestPathToEveryNode.entrySet()) {
            System.out.println("Shortest Path of stations from Station "+outerEntry.getKey());
            for(Map.Entry<String,TrainRoute> entry: outerEntry.getValue().entrySet()) {
                System.out.print("\t"+entry.getKey() + " ");
                System.out.println(entry.getValue().getShortestPath());
            }
        }
    }

    private static Map<String,TrainRoute> djikstra(String startStation) {
        Map<String, Integer> distances = new HashMap<>();
        Map<String,List<String>> paths = new HashMap<>();
        PriorityQueue<String> pq = new PriorityQueue<>(Comparator.comparingInt(distances::get));

        for (String station: graph.keySet()) {
            distances.put(station, Integer.MAX_VALUE);
        }
        distances.put(startStation,0);

        List<String> startList = new ArrayList<>();
        startList.add(startStation);
        paths.put(startStation, startList);

        pq.offer(startStation);

        while(!pq.isEmpty()) {
            String currentStation = pq.poll();
            for(Edge edge: graph.get(currentStation)) {
                String endStation = edge.getEndStation().getName();
                int newDistance = distances.get(currentStation) + edge.getJourneyTimeInMinutes();
                if (newDistance < distances.get(endStation) ){
                    distances.put(edge.getEndStation().getName(), newDistance);
                    List<String> newPath = new ArrayList<>(paths.get(currentStation));
                    newPath.add(endStation);
                    paths.put(endStation, newPath);
                    pq.add(endStation);
                }
            }
        }

        Map<String, TrainRoute> trainRoutes = new HashMap<>();
        for(String destination: distances.keySet()) {
            if(paths.containsKey(destination)) {
                trainRoutes.put(destination, new TrainRoute(distances.get(destination),paths.get(destination)));
            }
        }
        return trainRoutes;
    }

}
