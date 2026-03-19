# RouteToGateway Testing Environment

This repository includes an automated testing suite to verify the Link-State routing implementation against the required security policy.

## 🚀 Recommended Approach: Automated Script
A new script `test.sh` has been created for a cleaner, colorized, and robust testing experience.

### How to use:
1. Ensure the script is executable:
   ```bash
   chmod +x test.sh
   ```
2. Run all tests:
   ```bash
   ./test.sh
   ```

### What `test.sh` does:
*   **Safety Check**: Stops immediately if your Java code fails to compile.
*   **Visual Clarity**: Uses colors to distinguish between headers, input, and output.
*   **Automatic Discovery**: Finds all `.txt` files in the directory and uses them as test inputs.
*   **Input Context**: Prints the contents of each test file before showing results.
*   **Results Logging**: Automatically saves all terminal output to `test_results.log` for future reference.

---

## 🛠️ Improved One-Liner (Standard Command)
If you prefer a single command without using the script file, here is a professional version of your original command:

```bash
javac RouteToGateway.java && for f in *.txt; do printf "\n\033[1;34m>>> Testing: %s <<<\033[0m\n" "$f"; echo -e "\033[0;90m[Input]\033[0m"; cat "$f"; echo -e "\n\033[0;90m[Output]\033[0m"; java RouteToGateway < "$f"; printf "\033[1;34m%s\033[0m\n" "---------------------------------------"; done
```

---

## 📂 Test Case Descriptions
| File | Focus Area |
| :--- | :--- |
| `test_input.txt` | General functional test with 6 routers and 2 gateways. |
| `test_bonus_alternative.txt` | Tests **Load Balancing** (multiple next hops for same cost). |
| `test_disconnected.txt` | Verifies behavior when the network is not fully connected. |
| `test_unreachable.txt` | Ensures the `-1` policy is applied for out-of-reach nodes. |
| `test_asymmetric.txt` | Confirms that directed links (different costs each way) are handled. |
| `test_tie_break.txt` | Tests deterministic behavior when multiple paths exist. |