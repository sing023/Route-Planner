import java.util.*;
import java.util.stream.Collectors;

public class DeliveryPlanner {

    private static final double FUTURE_COST_WEIGHT = 0.3;
    private static final double UTILIZATION_BONUS = 0.2;
    private static final int MAX_AUCTION_ROUNDS = 50;

    public static void assignPackage(List<Train> trains, List<Package> packages) {
        for (Train train : trains) {
            if (train.getLog() == null) {
                train.setLog(new ArrayList<>());
            }
        }

        Map<Package, Train> assignments = runAuctionAlgorithm(trains, packages);


        Map<Train, List<Package>> trainPackages = new HashMap<>();
        for (Train train : trains) {
            trainPackages.put(train, new ArrayList<>());
        }

        for (Map.Entry<Package, Train> entry : assignments.entrySet()) {
            trainPackages.get(entry.getValue()).add(entry.getKey());
        }

        for (Train train : trains) {
            List<Package> assignedPackages = trainPackages.get(train);
            if (!assignedPackages.isEmpty()) {
                executeOptimizedRoute(train, assignedPackages);
            }
        }

        printResults(trains, assignments);
    }

    private static Map<Package, Train> runAuctionAlgorithm(List<Train> trains, List<Package> packages) {
        Map<Package, Train> assignments = new HashMap<>();
        Map<Package, Double> packagePrices = new HashMap<>();

        for (Package pkg : packages) {
            packagePrices.put(pkg, 0.0);
        }

        int round = 0;
        boolean improved = true;

        while (improved && round < MAX_AUCTION_ROUNDS) {
            improved = false;

            for (Package pkg : packages) {
                AuctionResult result = conductPackageAuction(pkg, trains, assignments, packagePrices);

                if (result.bestTrain != null) {
                    Train currentAssignment = assignments.get(pkg);

                    if (currentAssignment != result.bestTrain) {
                        if (currentAssignment != null) {

                        }
                        assignments.put(pkg, result.bestTrain);
                        packagePrices.put(pkg, result.winningBid);

                        improved = true;
                    }
                }
            }

            round++;
        }

        return assignments;
    }

    private static AuctionResult conductPackageAuction(Package pkg, List<Train> trains,
                                                       Map<Package, Train> currentAssignments,
                                                       Map<Package, Double> packagePrices) {
        double bestCost = Double.MAX_VALUE;
        double secondBestCost = Double.MAX_VALUE;
        Train bestTrain = null;

        for (Train train : trains) {
            if (!canTrainHandlePackage(train, pkg, currentAssignments)) {
                continue;
            }

            double totalCost = calculateTrainCostForPackage(train, pkg, currentAssignments, packagePrices);

            if (totalCost < bestCost) {
                secondBestCost = bestCost;
                bestCost = totalCost;
                bestTrain = train;
            } else if (totalCost < secondBestCost) {
                secondBestCost = totalCost;
            }
        }

        double winningBid = secondBestCost > bestCost ?
                (secondBestCost - bestCost) * 0.1 + packagePrices.get(pkg) :
                packagePrices.get(pkg);

        return new AuctionResult(bestTrain, winningBid);
    }

    private static boolean canTrainHandlePackage(Train train, Package pkg, Map<Package, Train> assignments) {
        int currentLoad = train.getCurrentLoad();
        for (Map.Entry<Package, Train> entry : assignments.entrySet()) {
            if (entry.getValue() == train) {
                currentLoad += entry.getKey().getWeightInKg();
            }
        }

        return currentLoad + pkg.getWeightInKg() <= train.getCapacityInKg();
    }

    private static double calculateTrainCostForPackage(Train train, Package pkg,
                                                       Map<Package, Train> assignments,
                                                       Map<Package, Double> packagePrices) {
        List<Package> trainPackages = assignments.entrySet().stream()
                .filter(entry -> entry.getValue() == train)
                .map(Map.Entry::getKey)
                .collect(Collectors.toList());

        List<Package> allPackages = new ArrayList<>(trainPackages);
        allPackages.add(pkg);

        double immediateCost = calculateOptimalDeliveryTime(train, allPackages);
        double futureCost = calculateFuturePositioningCost(train, allPackages);
        double utilizationBonus = calculateUtilizationBonus(train, allPackages);

        return immediateCost + FUTURE_COST_WEIGHT * futureCost - UTILIZATION_BONUS * utilizationBonus;
    }

    private static double calculateOptimalDeliveryTime(Train train, List<Package> packages) {
        if (packages.isEmpty()) return 0;

        Map<String, List<Package>> pickupGroups = packages.stream()
                .collect(Collectors.groupingBy(p -> p.getStartingNode().getName()));

        String currentLocation = train.getCurrentStation();
        int currentTime = train.getCurrentTime();
        double totalTime = 0;

        Set<String> visitedPickups = new HashSet<>();

        while (visitedPickups.size() < pickupGroups.size()) {
            String nearestPickup = null;
            int shortestTime = Integer.MAX_VALUE;

            for (String pickup : pickupGroups.keySet()) {
                if (visitedPickups.contains(pickup)) continue;

                TrainRoute route = ComputePath.shortestPathToEveryNode
                        .get(currentLocation)
                        .get(pickup);

                if (route != null && route.getTime() < shortestTime) {
                    shortestTime = route.getTime();
                    nearestPickup = pickup;
                }
            }

            if (nearestPickup != null) {
                totalTime += shortestTime;
                currentLocation = nearestPickup;
                visitedPickups.add(nearestPickup);

                List<Package> packagesAtLocation = pickupGroups.get(nearestPickup);
                totalTime += calculateDeliveryTimeFromLocation(currentLocation, packagesAtLocation);

                if (!packagesAtLocation.isEmpty()) {
                    currentLocation = findOptimalFinalDeliveryLocation(currentLocation, packagesAtLocation);
                }
            }
        }

        return totalTime;
    }

    private static double calculateDeliveryTimeFromLocation(String startLocation, List<Package> packages) {
        if (packages.isEmpty()) return 0;

        Map<String, List<Package>> destinations = packages.stream()
                .collect(Collectors.groupingBy(p -> p.getEndNode().getName()));

        String currentLocation = startLocation;
        double deliveryTime = 0;
        Set<String> visitedDestinations = new HashSet<>();

        while (visitedDestinations.size() < destinations.size()) {
            String nearestDestination = null;
            int shortestTime = Integer.MAX_VALUE;

            for (String destination : destinations.keySet()) {
                if (visitedDestinations.contains(destination)) continue;

                TrainRoute route = ComputePath.shortestPathToEveryNode
                        .get(currentLocation)
                        .get(destination);

                if (route != null && route.getTime() < shortestTime) {
                    shortestTime = route.getTime();
                    nearestDestination = destination;
                }
            }

            if (nearestDestination != null) {
                deliveryTime += shortestTime;
                currentLocation = nearestDestination;
                visitedDestinations.add(nearestDestination);
            } else {
                break;
            }
        }

        return deliveryTime;
    }

    private static String findOptimalFinalDeliveryLocation(String startLocation, List<Package> packages) {
        String finalLocation = startLocation;

        Map<String, List<Package>> destinations = packages.stream()
                .collect(Collectors.groupingBy(p -> p.getEndNode().getName()));

        String currentLocation = startLocation;
        Set<String> visited = new HashSet<>();

        while (visited.size() < destinations.size()) {
            String nearest = null;
            int shortestTime = Integer.MAX_VALUE;

            for (String dest : destinations.keySet()) {
                if (visited.contains(dest)) continue;

                TrainRoute route = ComputePath.shortestPathToEveryNode
                        .get(currentLocation)
                        .get(dest);

                if (route != null && route.getTime() < shortestTime) {
                    shortestTime = route.getTime();
                    nearest = dest;
                }
            }

            if (nearest != null) {
                visited.add(nearest);
                currentLocation = nearest;
                finalLocation = nearest;
            } else {
                break;
            }
        }

        return finalLocation;
    }

    private static double calculateFuturePositioningCost(Train train, List<Package> packages) {
        if (packages.isEmpty()) return 0;

        String finalPosition = findOptimalFinalDeliveryLocation(train.getCurrentStation(), packages);

        double totalDistance = 0;
        int stationCount = 0;

        Map<String, Map<String, TrainRoute>> allPaths = ComputePath.shortestPathToEveryNode;
        if (allPaths.containsKey(finalPosition)) {
            for (TrainRoute route : allPaths.get(finalPosition).values()) {
                totalDistance += route.getTime();
                stationCount++;
            }
        }

        return stationCount > 0 ? totalDistance / stationCount : 0;
    }

    private static double calculateUtilizationBonus(Train train, List<Package> packages) {
        int totalWeight = packages.stream().mapToInt(Package::getWeightInKg).sum();
        double utilization = (double) totalWeight / train.getCapacityInKg();
        return 100 * (1 / (1 + Math.exp(-10 * (utilization - 0.5))));
    }

    private static void executeOptimizedRoute(Train train, List<Package> packages) {
        if (packages.isEmpty()) return;

        Map<String, List<Package>> pickupGroups = packages.stream()
                .collect(Collectors.groupingBy(p -> p.getStartingNode().getName()));

        String currentLocation = train.getCurrentStation();
        int currentTime = train.getCurrentTime();
        int currentLoad = train.getCurrentLoad();

        Set<String> visitedPickups = new HashSet<>();

        while (visitedPickups.size() < pickupGroups.size()) {
            String nearestPickup = findNearestUnvisitedPickup(currentLocation, pickupGroups, visitedPickups);

            if (nearestPickup != null) {
                if (!currentLocation.equals(nearestPickup)) {
                    TrainRoute route = ComputePath.shortestPathToEveryNode
                            .get(currentLocation)
                            .get(nearestPickup);

                    if (route != null) {
                        currentTime = logMovement(train, currentLocation, nearestPickup,
                                route, currentTime, new ArrayList<>());
                        currentLocation = nearestPickup;
                    }
                }

                List<Package> packagesHere = pickupGroups.get(nearestPickup);
                currentLoad += packagesHere.stream().mapToInt(Package::getWeightInKg).sum();

                currentTime = executeDeliveriesFromLocation(train, currentLocation,
                        packagesHere, currentTime, true);
                currentLocation = findOptimalFinalDeliveryLocation(currentLocation, packagesHere);

                visitedPickups.add(nearestPickup);
            } else {
                break;
            }
        }

        train.setCurrentStation(currentLocation);
        train.setCurrentTime(currentTime);
        train.setCurrentLoad(currentLoad);
    }

    private static String findNearestUnvisitedPickup(String currentLocation,
                                                     Map<String, List<Package>> pickupGroups,
                                                     Set<String> visited) {
        String nearest = null;
        int shortestTime = Integer.MAX_VALUE;

        for (String pickup : pickupGroups.keySet()) {
            if (visited.contains(pickup)) continue;

            TrainRoute route = ComputePath.shortestPathToEveryNode
                    .get(currentLocation)
                    .get(pickup);

            if (route != null && route.getTime() < shortestTime) {
                shortestTime = route.getTime();
                nearest = pickup;
            }
        }

        return nearest;
    }

    private static int executeDeliveriesFromLocation(Train train, String startLocation,
                                                     List<Package> packages, int startTime,
                                                     boolean includePickupInFirstMove) {
        Map<String, List<Package>> destinations = packages.stream()
                .collect(Collectors.groupingBy(p -> p.getEndNode().getName()));

        String currentLocation = startLocation;
        int currentTime = startTime;
        Set<String> visitedDestinations = new HashSet<>();

        while (visitedDestinations.size() < destinations.size()) {
            String nearestDestination = null;
            int shortestTime = Integer.MAX_VALUE;

            for (String destination : destinations.keySet()) {
                if (visitedDestinations.contains(destination)) continue;

                TrainRoute route = ComputePath.shortestPathToEveryNode
                        .get(currentLocation)
                        .get(destination);

                if (route != null && route.getTime() < shortestTime) {
                    shortestTime = route.getTime();
                    nearestDestination = destination;
                }
            }

            if (nearestDestination != null) {
                List<Package> packagesToDeliver = destinations.get(nearestDestination);
                TrainRoute route = ComputePath.shortestPathToEveryNode
                        .get(currentLocation)
                        .get(nearestDestination);

                List<String> pickupNames = includePickupInFirstMove && visitedDestinations.isEmpty() ?
                        packages.stream().map(Package::getName).collect(Collectors.toList()) :
                        new ArrayList<>();

                currentTime = logMovement(train, currentLocation, nearestDestination, route,
                        currentTime, pickupNames);

                currentLocation = nearestDestination;
                visitedDestinations.add(nearestDestination);
                includePickupInFirstMove = false;
            } else {
                break;
            }
        }

        return currentTime;
    }

    private static int logMovement(Train train, String from, String to, TrainRoute route,
                                   int startTime, List<String> pickupPackages) {
        List<String> path = new ArrayList<>(route.getShortestPath());
        if (!path.get(0).equals(from)) {
            path.add(0, from);
        }

        int currentTime = startTime;

        for (int i = 1; i < path.size(); i++) {
            String fromStation = path.get(i - 1);
            String toStation = path.get(i);

            List<String> pickup = (i == 1) ? pickupPackages : new ArrayList<>();
            List<String> dropOff = toStation.equals(to) ?
                    Arrays.asList(to) : new ArrayList<>(); // Simplified - should include actual package names

            train.getLog().add("W=" + currentTime +
                    ", T=" + train.getName() +
                    ", N1=" + fromStation +
                    ", P1=" + pickup +
                    ", N2=" + toStation +
                    ", P2=" + dropOff);

            currentTime += getEdgeTime(fromStation, toStation);
        }

        return currentTime;
    }

    private static int getEdgeTime(String from, String to) {
        for (Edge edge : ComputePath.graph.get(from)) {
            if (edge.getEndStation().getName().equals(to)) {
                return edge.getJourneyTimeInMinutes();
            }
        }
        return 0;
    }

    private static void printResults(List<Train> trains, Map<Package, Train> assignments) {
        System.out.println("Result of AUCTION ALGORITHM ");

        for (Train train : trains) {
            long assignedPackages = assignments.values().stream()
                    .filter(t -> t == train)
                    .count();

            System.out.println("Train " + train.getName() + " assigned " + assignedPackages + " packages:");

            for (String log : train.getLog()) {
                System.out.println(log);
            }
            System.out.println("Total time: " + train.getCurrentTime() + " minutes.\n");
        }

        System.out.println("Package assignments:");
        for (Map.Entry<Package, Train> entry : assignments.entrySet()) {
            System.out.println("Package " + entry.getKey().getName() +
                    " -> Train " + entry.getValue().getName());
        }
    }

    private static class AuctionResult {
        final Train bestTrain;
        final double winningBid;

        AuctionResult(Train bestTrain, double winningBid) {
            this.bestTrain = bestTrain;
            this.winningBid = winningBid;
        }
    }
}