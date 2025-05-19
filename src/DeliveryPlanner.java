import java.util.ArrayList;
import java.util.List;
import java.util.*;
import java.util.stream.Collectors;

public class DeliveryPlanner {

    public static void assignPackage(List<Train> trains, List<Package> packages) {
        List<Package> remainingPackages = new ArrayList<>(packages);

        for (Train train : trains) {
            if (train.getLog() == null) {
                train.setLog(new ArrayList<>());
            }
        }

        while (!remainingPackages.isEmpty()) {
            boolean assignedAny = false;

            for (Train train : trains) {
                Map<String, List<Package>> pickupLocationPackages = groupPackagesByPickupLocation(remainingPackages);

                String bestPickupLocation = null;
                List<Package> bestPackageBatch = null;
                int bestTotalTime = Integer.MAX_VALUE;
                TrainRoute bestPickupRoute = null;

                if (pickupLocationPackages.containsKey(train.getCurrentStation())) {
                    List<Package> packagesAtCurrentStation = pickupLocationPackages.get(train.getCurrentStation());
                    int totalWeight = calculateTotalWeight(packagesAtCurrentStation);

                    if (train.getCurrentLoad() + totalWeight <= train.getCapacityInKg()) {
                        processSingleStationPickup(train, packagesAtCurrentStation, remainingPackages);
                        assignedAny = true;
                        break;
                    }
                }

                for (Map.Entry<String, List<Package>> entry : pickupLocationPackages.entrySet()) {
                    String pickupLocation = entry.getKey();
                    if (pickupLocation.equals(train.getCurrentStation())) {
                        continue;
                    }

                    List<Package> packagesAtLocation = entry.getValue();
                    int totalWeight = calculateTotalWeight(packagesAtLocation);

                    if (train.getCurrentLoad() + totalWeight > train.getCapacityInKg()) {
                        continue;
                    }

                    TrainRoute toPickUp = ComputePath.shortestPathToEveryNode
                            .get(train.getCurrentStation())
                            .get(pickupLocation);

                    if (toPickUp == null) {
                        continue;
                    }

                    int routeTime = toPickUp.getTime() + train.getCurrentTime();

                    if (routeTime < bestTotalTime) {
                        bestTotalTime = routeTime;
                        bestPickupLocation = pickupLocation;
                        bestPackageBatch = packagesAtLocation;
                        bestPickupRoute = toPickUp;
                    }
                }

                if (bestPickupLocation != null && bestPackageBatch != null) {
                    processPickupAndDelivery(train, bestPickupRoute, bestPickupLocation, bestPackageBatch);
                    remainingPackages.removeAll(bestPackageBatch);
                    assignedAny = true;
                    break;
                }
            }

            if (!assignedAny) {
                for (Package pkg : remainingPackages) {
                    System.out.println("Cannot deliver package: " + pkg.getName());
                }
                break;
            }
        }

        for (Train train : trains) {
            System.out.println("Final route for " + train.getName() + ":");
            for (String log : train.getLog()) {
                System.out.println(log);
            }
            System.out.println("Total time: " + train.getCurrentTime() + " minutes.\n");
        }
    }

    private static int calculateTotalWeight(List<Package> packages) {
        return packages.stream().mapToInt(Package::getWeightInKg).sum();
    }

    private static void processSingleStationPickup(Train train, List<Package> packages, List<Package> remainingPackages) {
        train.setCurrentLoad(train.getCurrentLoad() + calculateTotalWeight(packages));
        planAndLogDeliveries(train, packages, true);
        remainingPackages.removeAll(packages);
    }

    private static void processPickupAndDelivery(Train train, TrainRoute pickupRoute, String pickupLocation, List<Package> packages) {
        int currentTime = train.getCurrentTime();
        String currentStation = train.getCurrentStation();

        List<String> path = new ArrayList<>(pickupRoute.getShortestPath());
        if (!path.get(0).equals(currentStation)) {
            path.add(0, currentStation);
        }

        for (int i = 1; i < path.size(); i++) {
            String from = path.get(i - 1);
            String to = path.get(i);
            int travelTime = getEdgeTime(from, to);
            List<String> emptyList = new ArrayList<>();

            train.getLog().add("W=" + currentTime +
                    ", T=" + train.getName() +
                    ", N1=" + from +
                    ", P1=" + emptyList +
                    ", N2=" + to +
                    ", P2=" + emptyList);

            currentTime += travelTime;
        }

        train.setCurrentStation(pickupLocation);
        train.setCurrentTime(currentTime);
        train.setCurrentLoad(train.getCurrentLoad() + calculateTotalWeight(packages));
        planAndLogDeliveries(train, packages, true);
    }

    private static void planAndLogDeliveries(Train train, List<Package> packages, boolean includePickupInFirstMove) {
        Map<String, List<Package>> destinationPackages = groupPackagesByDropOffLocation(packages);

        String currentLocation = train.getCurrentStation();
        int currentTime = train.getCurrentTime();

        while (!destinationPackages.isEmpty()) {
            String nextDestination = null;
            int shortestTime = Integer.MAX_VALUE;

            for (String destination : destinationPackages.keySet()) {
                TrainRoute route = ComputePath.shortestPathToEveryNode
                        .get(currentLocation)
                        .get(destination);

                if (route != null && route.getTime() < shortestTime) {
                    shortestTime = route.getTime();
                    nextDestination = destination;
                }
            }

            if (nextDestination == null) {
                System.out.println("Cant find any route to deliver packages: " +
                        destinationPackages.values().stream()
                                .flatMap(List::stream)
                                .map(Package::getName)
                                .collect(Collectors.joining(", ")));
                break;
            }

            List<Package> packagesToDeliver = destinationPackages.get(nextDestination);
            TrainRoute deliveryRoute = ComputePath.shortestPathToEveryNode
                    .get(currentLocation)
                    .get(nextDestination);

            List<String> pathToDestination = new ArrayList<>(deliveryRoute.getShortestPath());
            if (!pathToDestination.get(0).equals(currentLocation)) {
                pathToDestination.add(0, currentLocation);
            }

            for (int i = 1; i < pathToDestination.size(); i++) {
                String from = pathToDestination.get(i - 1);
                String to = pathToDestination.get(i);

                List<String> pickUp = new ArrayList<>();
                List<String> dropOff = new ArrayList<>();

                if (includePickupInFirstMove && i == 1) {
                    pickUp = packages.stream()
                            .map(Package::getName)
                            .toList();
                }

                if (to.equals(nextDestination)) {
                    for (Package pkg : packagesToDeliver) {
                        dropOff.add(pkg.getName());
                    }
                }

                int travelTime = getEdgeTime(from, to);

                train.getLog().add("W=" + currentTime +
                        ", T=" + train.getName() +
                        ", N1=" + from +
                        ", P1=" + pickUp +
                        ", N2=" + to +
                        ", P2=" + dropOff);

                currentTime += travelTime;
            }

            destinationPackages.remove(nextDestination);
            includePickupInFirstMove = false;
            currentLocation = nextDestination;
        }

        train.setCurrentStation(currentLocation);
        train.setCurrentTime(currentTime);
    }

    private static Map<String, List<Package>> groupPackagesByPickupLocation(List<Package> packages) {
        Map<String, List<Package>> result = new HashMap<>();
        for (Package pkg : packages) {
            String pickupLocation = pkg.getStartingNode().getName();
            result.putIfAbsent(pickupLocation, new ArrayList<>());
            result.get(pickupLocation).add(pkg);
        }

        return result;
    }

    private static Map<String, List<Package>> groupPackagesByDropOffLocation(List<Package> packages) {
        Map<String, List<Package>> result = new HashMap<>();

        for (Package pkg : packages) {
            String dropOffLocation = pkg.getEndNode().getName();
            result.putIfAbsent(dropOffLocation, new ArrayList<>());
            result.get(dropOffLocation).add(pkg);
        }

        return result;
    }

    private static int getEdgeTime(String from, String to) {
        for (Edge edge : ComputePath.graph.get(from)) {
            if (edge.getEndStation().getName().equals(to)) {
                return edge.getJourneyTimeInMinutes();
            }
        }
        return 0;
    }
}

