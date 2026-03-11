# CP372 – Computer Networks (Winter 2026)

## Assignment 3: Policy-Based Link State Routing

**Due:** Friday, March 27, 2026, at 11:59 pm  
**Language:** Java  
**Late Policy:** As stated in the course syllabus  
**Type:** Individual Assignment (No group work or group submissions permitted)

---

### 1. Overview

In this assignment, you will implement a core functionality of the Link State Intra‑AS routing algorithm for an
**autonomous system (AS) with a POLICY ENFORCEMENT**. This AS has a designated router (Security Agent, or **SA**)
that performs special security processing on every datagram that leaves the AS.

#### Policy Enforcement

- Every datagram leaving the AS must pass through the SA before it reaches a gateway.
- Therefore, a gateway **cannot** be an intermediate node on the way to the SA.
- Additionally, the SA is the inspection point; once a packet leaves the SA, the **first gateway it reaches** should be its exit point.

You are given a weighted directed graph with `n` vertices (labeled with `1, 2, …, n`) representing the topology of the AS.
You are also given a list of **gateway routers** `x₁, x₂, …, xₖ` (distinct numbers from `{1, …, n}`), and the index of the router SA.

The output consists of `n‑k` forwarding tables to gateway routers (one per non‑gateway router), which allows datagram
forwarding from a source router to a **destination gateway AS router along the shortest path**, which **must use an SA router**
as one of the intermediate points.

---

### 2. Input Format

The implementation must read input data from *standard input* and print the tables to *standard output*.  
The input starts with a single line containing the value of `n`, followed by `n` lines representing an adjacency matrix (weights).
Next comes a line with the list of gateway routers, then the last line contains the index of the SA.  

Example:

```
6
0 1 10 -1 -1 2
10 0 1 -1 -1 -1
1 10 0 -1 -1 -1
-1 -1 2 0 1 10
-1 -1 -1 10 0 1
-1 -1 -1 1 10 0
2 5
6
```

*Notes:*

- `-1` represents an infinite weight (absence of directed edge).
- The matrix is **not necessarily symmetric**.
- You may assume gateway routers are listed in ascending order; the list is non‑empty and `k < n`.

---

### 3. Output Format

Produce forwarding tables for each **non‑gateway router**.  Each table shows the cost and next hop to every gateway router
along the **policy‑compliant shortest path**.  If a gateway is unreachable or no valid path exists (through SA), show `-1` for
the cost and next hop.

**Sample output for the example input above:**

```
Forwarding Table for 1
 To Cost Next Hop
 2 6 6
 5 4 6
Forwarding Table for 3
 To Cost Next Hop
 2 8 1
 5 5 1
…

Forwarding Table for 6
 To Cost Next Hop
 2 5 4
 5 2 4
```

> Note that the route from 3 to 2 is `3->1->6->4->3->2` rather than `3->2` due to the policy.

*Additional behaviours:*

- If a gateway is unreachable from the source, print `-1` for both cost and next hop.
- If no shortest path exists that goes through SA, also print `-1` values.
- **No exception handling** is required; you can assume correct input.

---

### 4. Algorithmic Hint

We are dealing with a **directed graph** `G`. Consider `G'`, the transpose of `G` (flip every edge direction).
The shortest path `P` from `x` to `y` in `G` has the same weight as the shortest path `P'` from `y` to `x` in `G'` and uses
the same intermediate vertices in reverse order. Moreover, the predecessor of `x` in `P'` is the same as the “next hop” from `x`
in `P`.  This property allows you to compute necessary results in at most **two calls** to standard Dijkstra's algorithm.

> Dijkstra's algorithm can compute not only distance but also predecessors, which help construct the forwarding table.

---

### 5. Submission Instructions

Submit a **single Java file** named `RouteToGateway.java` containing your implementation.  
The file must use the default package (no `package` keyword).  It should compile via:

```bash
javac RouteToGateway.java
```

#### Bonus (optional)

Implement **alternative forwarding**: if there is an alternative path of the same weight from the source to a particular
destination, list multiple next hops separated by commas.  Bonus points may be awarded.

---

### 6. Testing Environment

An automated testing suite is available at the **RouteToGateway Testing Environment** repository:  
<https://github.com/MustafaDaraghmeh/RouteToGateway-TestingEnvironment>  
It contains various test cases to verify the routing implementation against the required security policy.

Below are sample test inputs and their expected outputs (as produced by `java RouteToGateway`):

#### test_asymmetric.txt

```
3
0 1 5
10 0 10
-1 2 0
2
3
```

```
Forwarding Table for 1
To Cost Next Hop
2 7 3
Forwarding Table for 3
To Cost Next Hop
2 2 2
```

#### test_bonus_alternative.txt

```
5
0 10 10 -1 -1
-1 0 -1 5 -1
-1 -1 0 5 -1
-1 -1 -1 0 2
-1 -1 -1 -1 0
5
4
```

```
Forwarding Table for 1
To Cost Next Hop
5 17 2,3
Forwarding Table for 2
To Cost Next Hop
5 7 4
5
Forwarding Table for 3
To Cost Next Hop
5 7 4
Forwarding Table for 4
To Cost Next Hop
5 2 5
```

#### test_disconnected.txt

```
4
0 1 2 -1
1 0 1 -1
2 1 0 -1
-1 -1 -1 0
2
3
```

```
Forwarding Table for 1
To Cost Next Hop
2 3 3
Forwarding Table for 3
To Cost Next Hop
2 1 2
Forwarding Table for 4
To Cost Next Hop
2 -1 -1
```

#### test_input.txt (original example)

```
6
0 1 10 -1 -1 2
10 0 1 -1 -1 -1
1 10 0 -1 -1 -1
6
-1 -1 2 0 1 10
-1 -1 -1 10 0 1
-1 -1 -1 1 10 0
2 5
6
```

```
Forwarding Table for 1
To Cost Next Hop
2 7 6
5 4 6
Forwarding Table for 3
To Cost Next Hop
2 8 1
5 5 1
Forwarding Table for 4
To Cost Next Hop
2 10 3
5 7 3
Forwarding Table for 6
To Cost Next Hop
2 5 4
5 2 4
```

#### test_tie_break.txt

```
4
0 1 1 -1
-1 0 -1 2
-1 -1 0 2
-1 -1 -1 0
4
2 3
```

```
Forwarding Table for 1
To Cost Next Hop
4 3 2
7
Forwarding Table for 2
To Cost Next Hop
4 2 4
Forwarding Table for 3
To Cost Next Hop
4 -1 -1
```

#### test_unreachable.txt

```
3
0 5 2
-1 0 -1
-1 -1 0
2
3
```

```
Forwarding Table for 1
To Cost Next Hop
2 -1 -1
Forwarding Table for 3
To Cost Next Hop
2 -1 -1
```

---

Good luck with the implementation!  Follow the policy strictly and make sure to handle unreachable cases correctly.
