# 📦 Autonomous Mail Delivery Train Coordinator

This project is a simulation and optimization engine for coordinating autonomous trains that deliver packages across a railway network. It is designed to ensure **correct, efficient**, and **conflict-free** movement of multiple trains while adhering to capacity and timing constraints.

---

## 🧩 Problem Overview

You are tasked with developing a system to control a network of autonomous mail delivery trains operating across a graph-based railway system.

Each problem instance includes:
- A **network of nodes** (stations)
- **Undirected edges** (routes with travel times between stations)
- A fleet of **trains**, each with a capacity and starting location
- A set of **packages**, each with weight, origin, and destination

The goal is to deliver **all packages to their respective destinations** in the **shortest total solution time** (i.e., the earliest time when all deliveries are complete), while respecting train capacities and edge travel times.

---

## 🧾 Input Format

Your program will receive structured input in the following format:

1. **Nodes**  
   ```
   [number_of_nodes]
   NodeName1
   NodeName2
   ...
   ```

2. **Edges**  
   ```
   [number_of_edges]
   EdgeName,Node1,Node2,TravelTimeInMinutes
   ...
   ```

3. **Packages**  
   ```
   [number_of_packages]
   PackageName,WeightInKg,StartNode,DestinationNode
   ...
   ```

4. **Trains**  
   ```
   [number_of_trains]
   TrainName,CapacityInKg,StartingNode
   ...
   ```

---

## 📤 Output Format

The system must generate a sequence of **moves**, each describing a train’s action:

```
W=<TimeInMinutes>, T=<TrainName>, N1=<FromNode>, P1=[PickedUpPackages], N2=<ToNode>, P2=[DroppedOffPackages]
```

- `W` = start time of move (in minutes)
- `T` = name of the train
- `N1` = starting node
- `P1` = packages picked up at start node
- `N2` = destination node
- `P2` = packages dropped off at destination node

---

## ✅ Constraints

- Trains must respect their **maximum weight capacity**
- Trains may **not teleport** — they must travel via defined edges
- Any number of trains can share edges or nodes simultaneously
- **Travel time is only affected by edge journey time**; loading/unloading takes no time
- The solution ends when **all packages are delivered to their destinations**

---

## 🧪 Example

**Input**
```
3
A
B
C
2
E1,A,B,30
E2,B,C,10
1
K1,5,A,C
1
Q1,6,B
```

**Output**
```
W=0, T=Q1, N1=B, P1=[], N2=A, P2=[]
W=30, T=Q1, N1=A, P1=[K1], N2=B, P2=[]
W=60, T=Q1, N1=B, P1=[], N2=C, P2=[K1]
```

📦 Package `K1` was successfully delivered from `A` to `C` in **70 minutes**.

---

## 🚀 Goals

- ✅ **Correctness** – ensure all deliveries complete under constraints  
- 🔄 **Optimization** – minimize total delivery time  
- 🔐 **Scalability** – handle large networks, multiple trains, and overlapping deliveries

---

## 🛠 Tech Stack

- Java for logic implementation
- Graph algorithms (Dijkstra) for shortest path
- Greedy/auction algorithms for route optimization

---

## 📂 File Structure

- `DeliveryPlanner.java` – main scheduling logic
- `Train`, `Package`, `Edge`, `Node`, `TrainRoute` – core domain models
- `ComputePath` – precomputed shortest paths between all nodes

---

## 🤝 Contribution

Contributions and optimization ideas are welcome! Feel free to fork, test with your own scenarios, or suggest improvements to the scheduling strategy.

---