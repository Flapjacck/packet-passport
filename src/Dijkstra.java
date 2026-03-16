
/*
 * Utility implementation of Dijkstra's algorithm on a directed weighted graph.
 * The run method returns both shortest distances and predecessor information.
 * Tracks all equal-cost predecessors to support the alternate next-hop bonus.
 *
 * This class is package-private and will be merged into RouteToGateway.java
 * for the final submission.
 */
import java.util.ArrayList;
import java.util.List;
import java.util.PriorityQueue;

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
