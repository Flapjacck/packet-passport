import java.util.*;

/*
 * Builds and prints forwarding tables for non-gateway routers.  Uses the
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
     * @param forward  Dijkstra result from source on original graph
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
     * Uses backward Dijkstra to determine which neighbors maintain the shortest
     * path.
     */
    private List<Integer> buildDirectNextHops(int gateway) {
        // For each neighbor N of source, check if: edge(source, N) + distance(N to
        // gateway) == distance(source to gateway)
        // Backward distances give us distance from each node TO the gateway (in
        // original graph direction)
        Set<Integer> nextHopsSet = new LinkedHashSet<>();
        int shortestDist = forward.distances[gateway];

        // Check all potential neighbors (all nodes with edges from source)
        for (int neighbor : graph.outgoingNeighbors(source)) {
            if (neighbor == source)
                continue; // Skip self-loops

            int edgeWeight = graph.weight(source, neighbor);
            if (edgeWeight == Graph.NO_EDGE)
                continue;

            // Distance from neighbor to gateway via shortest paths
            int distFromNeighbor = backward.get(gateway).distances[neighbor];
            if (distFromNeighbor == Integer.MAX_VALUE)
                continue; // Unreachable

            int totalDist = edgeWeight + distFromNeighbor;
            if (totalDist == shortestDist) {
                nextHopsSet.add(neighbor);
            }
        }

        // Convert to sorted list for consistent output
        List<Integer> hops = new ArrayList<>(nextHopsSet);
        Collections.sort(hops);
        return hops;
    }

    /**
     * Build next hop(s) for a gateway when source != SA.
     * Returns all immediate neighbors of source that are on a shortest path to SA.
     * Supports bonus: multiple equal-cost next hops.
     */
    private List<Integer> buildNextHopsForGateway(int gateway) {
        // The next hop is a neighbor N of source such that:
        // edge(source, N) + distance(N to SA) == distance(source to SA)
        // This ensures we follow a shortest path towards SA.
        Set<Integer> nextHopsSet = new LinkedHashSet<>();
        int shortestDistToSA = forward.distances[saIndex];

        // Get backward results from SA (distances to SA in original graph)
        Dijkstra.Result backwardFromSA = backward.get(saIndex);
        if (backwardFromSA == null) {
            return new ArrayList<>(); // SA not in backward map (shouldn't happen)
        }

        // Check all neighbors of source
        for (int neighbor : graph.outgoingNeighbors(source)) {
            if (neighbor == source)
                continue; // Skip self-loops

            int edgeWeight = graph.weight(source, neighbor);
            if (edgeWeight == Graph.NO_EDGE)
                continue;

            // Distance from neighbor to SA (via shortest path in original)
            int distFromNeighborToSA = backwardFromSA.distances[neighbor];
            if (distFromNeighborToSA == Integer.MAX_VALUE)
                continue; // Unreachable

            int totalDist = edgeWeight + distFromNeighborToSA;
            if (totalDist == shortestDistToSA) {
                nextHopsSet.add(neighbor);
            }
        }

        // Convert to sorted list for consistent output
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
