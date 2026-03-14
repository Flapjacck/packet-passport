
/*
 * Simple directed weighted graph representation backed by an adjacency matrix.
 * Provides methods to read the matrix from standard input and to construct the
 * transposed graph (edges reversed) required by the policy logic.
 */
import java.util.Scanner;

class Graph {
    int n;
    int[][] adj;

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
}
