
/*
 * Simple directed weighted graph representation backed by an adjacency matrix.
 *
 * The adjacency matrix uses -1 to denote the absence of an edge (infinite cost).
 * Node IDs in the input are 1..n; internal arrays are 0..n-1.
 */
import java.util.ArrayList;
import java.util.List;
import java.util.Scanner;

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
