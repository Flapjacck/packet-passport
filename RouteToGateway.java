import java.util.*;

/*
 * Simple directed weighted graph representation backed by an adjacency matrix.
 *
 * The adjacency matrix uses -1 to denote the absence of an edge (infinite cost).
 * Node IDs in the input are 1..n; internal arrays are 0..n-1.
 */
class Graph {
    private final int n;
    private final int[][] adj;

    // Value used to represent "no edge" when returning weights.
    static final int NO_EDGE = -1;

    /**
     * Construct a graph from a provided Scanner.
     * Expects exactly n*n integers following.
     */
    Graph(int n, Scanner sc) {
        this.n = n;
        this.adj = new int[n][n];
        for (int i = 0; i < n; i++) {
            for (int j = 0; j < n; j++) {
                adj[i][j] = sc.nextInt();
            }
        }
    }

    /**
     * Construct a graph from standard input.
     * Expects exactly n*n integers following the initial n.
     */
    @SuppressWarnings("resource")
    Graph(int n) {
        this.n = n;
        this.adj = new int[n][n];
        Scanner sc = new Scanner(System.in);
        for (int i = 0; i < n; i++) {
            for (int j = 0; j < n; j++) {
                adj[i][j] = sc.nextInt();
            }
        }
    }

    // Create a graph from an existing adjacency matrix (used for transpose, tests,
    // and Graph instantiation).
    Graph(int n, int[][] adj) {
        this.n = n;
        this.adj = adj;
    }

    /** Returns the number of nodes in the graph. */
    int size() {
        return n;
    }

    /**
     * Returns the weight of the edge from `u` to `v`, or NO_EDGE if none exists.
     * Nodes are expected to be in the range [0, n).
     */
    int weight(int u, int v) {
        return adj[u][v];
    }

    /** Returns true if there is an outgoing edge from `u` to `v`. */
    boolean hasEdge(int u, int v) {
        return adj[u][v] != NO_EDGE;
    }

    /**
     * Returns an iterable over all outgoing neighbors of `u` (nodes v where u->v
     * exists).
     */
    Iterable<Integer> outgoingNeighbors(int u) {
        List<Integer> neighbors = new ArrayList<>();
        for (int v = 0; v < n; v++) {
            if (hasEdge(u, v)) {
                neighbors.add(v);
            }
        }
        return neighbors;
    }

    /**
     * Return a new graph with every edge reversed.
     */
    Graph transpose() {
        int[][] t = new int[n][n];
        for (int i = 0; i < n; i++) {
            for (int j = 0; j < n; j++) {
                t[j][i] = adj[i][j];
            }
        }
        return new Graph(n, t);
    }
}

/*
 * Utility implementation of Dijkstra's algorithm on a directed weighted graph.
 * The run method returns both shortest distances and predecessor information.
 * Tracks all equal-cost predecessors to support the alternate next-hop bonus.
 */
class Dijkstra {
    /**
     * Container for the result of a single Dijkstra run.
     * distances[i] = shortest distance from source to node i (Integer.MAX_VALUE if
     * unreachable).
     * predecessors[i] = list of all nodes with equal-cost predecessor paths to i.
     */
    static class Result {
        int[] distances;
        List<List<Integer>> predecessors;

        Result(int n) {
            distances = new int[n];
            predecessors = new ArrayList<>(n);
            for (int i = 0; i < n; i++) {
                predecessors.add(new ArrayList<>());
            }
        }
    }

    /**
     * Run Dijkstra's algorithm from a given source node on the graph.
     * Returns distances and predecessors (supporting equal-cost paths for the
     * bonus).
     */
    static Result run(Graph g, int source) {
        int n = g.size();
        Result res = new Result(n);

        // Initialize: source has distance 0, others unreachable (MAX_VALUE).
        for (int i = 0; i < n; i++) {
            res.distances[i] = Integer.MAX_VALUE;
        }
        res.distances[source] = 0;

        // Priority queue: (distance, node).
        PriorityQueue<int[]> pq = new PriorityQueue<>((a, b) -> Integer.compare(a[0], b[0]));
        pq.offer(new int[] { 0, source });

        // Track processed nodes to avoid revisiting.
        boolean[] visited = new boolean[n];

        while (!pq.isEmpty()) {
            int[] curr = pq.poll();
            int dist = curr[0];
            int u = curr[1];

            // Skip if already processed or if distance is stale.
            if (visited[u] || dist > res.distances[u]) {
                continue;
            }
            visited[u] = true;

            // Relax neighbors: check all outgoing edges from u.
            for (int v : g.outgoingNeighbors(u)) {
                int weight = g.weight(u, v);
                if (weight == Graph.NO_EDGE)
                    continue;

                int newDist = res.distances[u] + weight;

                // Found a shorter path to v: update distance and clear old predecessors.
                if (newDist < res.distances[v]) {
                    res.distances[v] = newDist;
                    List<Integer> preds = res.predecessors.get(v);
                    preds.clear();
                    preds.add(u);
                    pq.offer(new int[] { newDist, v });
                }
                // Found an equal-cost path: add u as another predecessor (bonus)
                else if (newDist == res.distances[v] && res.distances[v] != Integer.MAX_VALUE) {
                    List<Integer> preds = res.predecessors.get(v);
                    if (!preds.contains(u)) {
                        preds.add(u);
                    }
                }
            }
        }

        return res;
    }
}

/*
 * Builds and prints forwarding tables for non-gateway routers. Uses the
 * distance/predecessor information from the Dijkstra runs to determine next
 * hop(s) and cost to each gateway, respecting the SA policy.
 *
 * For each gateway, computes the shortest policy-compliant path:
 * - Path must go through SA before reaching the gateway.
 * - Cost = dist_forward[SA] + dist_backward_from_gw[SA].
 * - Next hop is extracted from predecessors in forward Dijkstra.
 * - Supports multiple equal-cost next hops (bonus feature).
 */
class ForwardingTable {
    private final int source; // Source router (0-indexed)
    private final int[] gateways; // Gateway router IDs (0-indexed)
    private final int saIndex; // Security Agent index (0-indexed)
    private final Graph graph; // Original graph
    private final Dijkstra.Result forward; // Dijkstra from source on G
    private final Map<Integer, Dijkstra.Result> backward; // Dijkstra from each gateway on G'

    // Data structure: for each gateway, store (cost, list of next hops)
    private final Map<Integer, Entry> routeTable;

    private static class Entry {
        int cost;
        List<Integer> nextHops;

        Entry(int cost, List<Integer> nextHops) {
            this.cost = cost;
            this.nextHops = new ArrayList<>(nextHops);
        }
    }

    /**
     * Create a forwarding table for a single non-gateway router.
     * 
     * @param source   Source router ID (0-indexed)
     * @param gateways Array of gateway IDs (0-indexed)
     * @param saIndex  Security Agent router ID (0-indexed)
     * @param graph    Original directed weighted graph
     * @param backward Dijkstra result from each gateway on transposed graph
     */
    ForwardingTable(int source, int[] gateways, int saIndex, Graph graph,
            Map<Integer, Dijkstra.Result> backward) {
        this.source = source;
        this.gateways = gateways.clone();
        this.saIndex = saIndex;
        this.graph = graph;
        this.forward = Dijkstra.run(graph, source);
        this.backward = backward;
        this.routeTable = new LinkedHashMap<>();

        computeRoutesToGateways();
    }

    /**
     * Compute shortest policy-compliant paths to all gateways.
     * For each gateway, ensures the path goes through SA.
     */
    private void computeRoutesToGateways() {
        for (int gateway : gateways) {
            int cost = -1;
            List<Integer> nextHops = new ArrayList<>();

            if (source == saIndex) {
                // Special case: source IS the SA, so path is directly source -> gateway
                // (no need to enforce SA policy again)
                if (forward.distances[gateway] != Integer.MAX_VALUE) {
                    cost = forward.distances[gateway];
                    nextHops = buildDirectNextHops(gateway);
                }
            } else {
                // General case: source is not SA, enforce policy: source -> SA -> gateway
                if (forward.distances[saIndex] != Integer.MAX_VALUE &&
                        backward.get(gateway).distances[saIndex] != Integer.MAX_VALUE) {

                    // Cost = dist(source → SA) + dist(SA → gateway)
                    int totalCost = forward.distances[saIndex] +
                            backward.get(gateway).distances[saIndex];

                    // Verify this is valid (not MAX result)
                    if (totalCost < Integer.MAX_VALUE) {
                        cost = totalCost;
                        nextHops = buildNextHopsForGateway(gateway);
                    }
                }
            }

            if (cost == -1 || nextHops.isEmpty()) {
                routeTable.put(gateway, new Entry(-1, Collections.emptyList()));
            } else {
                routeTable.put(gateway, new Entry(cost, nextHops));
            }
        }
    }

    /**
     * Build next hop(s) for a gateway when source == SA.
     * Returns immediate successors of source that are on the shortest path to
     * gateway.
     */
    private List<Integer> buildDirectNextHops(int gateway) {
        Set<Integer> nextHopsSet = new LinkedHashSet<>();
        int shortestDist = forward.distances[gateway];

        for (int neighbor : graph.outgoingNeighbors(source)) {
            if (neighbor == source)
                continue; // Skip self-loops

            int edgeWeight = graph.weight(source, neighbor);
            if (edgeWeight == Graph.NO_EDGE)
                continue;

            int distFromNeighbor = backward.get(gateway).distances[neighbor];
            if (distFromNeighbor == Integer.MAX_VALUE)
                continue;

            if (edgeWeight + distFromNeighbor == shortestDist) {
                nextHopsSet.add(neighbor);
            }
        }

        List<Integer> hops = new ArrayList<>(nextHopsSet);
        Collections.sort(hops);
        return hops;
    }

    /**
     * Build next hop(s) for a gateway when source != SA.
     * Returns all immediate successors of source that are on a shortest path to SA.
     * A neighbor N is valid if: edge(source,N) + dist(N to SA) = dist(source to SA)
     * Supports bonus: multiple equal-cost next hops.
     */
    private List<Integer> buildNextHopsForGateway(int gateway) {
        Set<Integer> nextHopsSet = new LinkedHashSet<>();
        int shortestDistToSA = forward.distances[saIndex];

        Dijkstra.Result backwardFromSA = backward.get(saIndex);
        if (backwardFromSA == null) {
            return new ArrayList<>();
        }

        for (int neighbor : graph.outgoingNeighbors(source)) {
            if (neighbor == source)
                continue; // Skip self-loops

            int edgeWeight = graph.weight(source, neighbor);
            if (edgeWeight == Graph.NO_EDGE)
                continue;

            int distFromNeighborToSA = backwardFromSA.distances[neighbor];
            if (distFromNeighborToSA == Integer.MAX_VALUE)
                continue;

            if (edgeWeight + distFromNeighborToSA == shortestDistToSA) {
                nextHopsSet.add(neighbor);
            }
        }

        List<Integer> hops = new ArrayList<>(nextHopsSet);
        Collections.sort(hops);
        return hops;
    }

    /**
     * Print the forwarding table for this router in the required format.
     * Output uses 1-indexed router IDs.
     */
    void printTable() {
        System.out.printf("Forwarding Table for %d%n", source + 1);
        System.out.println(" To Cost Next Hop");

        for (int gateway : gateways) {
            Entry entry = routeTable.get(gateway);
            int displayGateway = gateway + 1; // Convert to 1-indexed

            if (entry.cost == -1) {
                System.out.printf(" %d -1 -1%n", displayGateway);
            } else {
                // Format next hops: comma-separated, 1-indexed
                String hopsStr = formatNextHops(entry.nextHops);
                System.out.printf(" %d %d %s%n", displayGateway, entry.cost, hopsStr);
            }
        }
    }

    /**
     * Format next hops as comma-separated 1-indexed router IDs.
     */
    private String formatNextHops(List<Integer> hops) {
        StringBuilder sb = new StringBuilder();
        for (int i = 0; i < hops.size(); i++) {
            if (i > 0)
                sb.append(",");
            sb.append(hops.get(i) + 1); // Convert to 1-indexed
        }
        return sb.toString();
    }
}

// Main entry point for the packet‑passport routing program.
// Handles I/O, invokes graph algorithms, and prints forwarding tables.
public class RouteToGateway {
    public static void main(String[] args) {
        Scanner sc = new Scanner(System.in);

        // Read n (number of nodes)
        int n = sc.nextInt();

        // Create graph from adjacency matrix
        Graph graph = new Graph(n, sc);

        // Read gateway routers (1-indexed from input, convert to 0-indexed)
        List<Integer> gatewayList = new ArrayList<>();
        String gatewayLine = sc.nextLine(); // consume newline
        gatewayLine = sc.nextLine();
        String[] gatewayTokens = gatewayLine.trim().split("\\s+");
        for (String token : gatewayTokens) {
            if (!token.isEmpty()) {
                gatewayList.add(Integer.parseInt(token) - 1); // Convert to 0-indexed
            }
        }
        int[] gateways = gatewayList.stream().mapToInt(Integer::intValue).toArray();

        // Read SA index (1-indexed from input, convert to 0-indexed)
        int saIndex = sc.nextInt() - 1; // Convert to 0-indexed

        // Create a set of gateway IDs for quick lookup
        Set<Integer> gatewaySet = new HashSet<>();
        for (int gw : gateways) {
            gatewaySet.add(gw);
        }

        // Precompute backward Dijkstra from each gateway on transposed graph
        Graph transposed = graph.transpose();
        Map<Integer, Dijkstra.Result> backwardResults = new HashMap<>();
        for (int gateway : gateways) {
            backwardResults.put(gateway, Dijkstra.run(transposed, gateway));
        }
        // Also precompute backward from SA for routing logic
        backwardResults.put(saIndex, Dijkstra.run(transposed, saIndex));

        // For each non-gateway router, compute and print forwarding table
        for (int source = 0; source < n; source++) {
            if (gatewaySet.contains(source)) {
                continue; // Skip gateway routers
            }

            // Create forwarding table for this source
            ForwardingTable table = new ForwardingTable(
                    source, gateways, saIndex, graph, backwardResults);
            table.printTable();
        }

        sc.close();
    }
}
