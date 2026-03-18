import java.util.*;

// Main entry point for the packet‑passport routing program.
// Handles I/O, invokes graph algorithms, and prints forwarding tables.
// This class remains public when merged for submission; helper classes are package-private.
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
