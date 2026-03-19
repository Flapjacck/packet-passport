#!/bin/bash

# --- Color Constants ---
BLUE='\033[1;34m'
CYAN='\033[1;36m'
GREEN='\033[0;32m'
GRAY='\033[0;90m'
YELLOW='\033[1;33m'
RED='\033[0;31m'
NC='\033[0m' # No Color

# --- Logging Setup ---
LOG_FILE="test_results.log"
# Clear existing log file
> "$LOG_FILE"
# Redirect all output to both terminal and log file
exec > >(tee -a "$LOG_FILE") 2>&1


# --- 1. Compilation ---
echo -e "${BLUE}===> Compiling RouteToGateway.java...${NC}"
javac RouteToGateway.java

if [ $? -ne 0 ]; then
    echo -e "${RED}[ERROR] Compilation failed!${NC}"
    exit 1
fi
echo -e "${GREEN}[SUCCESS] Compiled successfully.${NC}\n"

# --- 2. Test Execution ---
TEST_FILES=$(ls *.txt 2>/dev/null)

if [ -z "$TEST_FILES" ]; then
    echo -e "${YELLOW}[WARNING] No .txt test files found in the current directory.${NC}"
    exit 0
fi

for f in $TEST_FILES; do
    echo -e "${CYAN}====================================================${NC}"
    echo -e "${CYAN}  TEST CASE: $f${NC}"
    echo -e "${CYAN}====================================================${NC}"
    
    echo -e "${GRAY}[Input File Contents]${NC}"
    cat "$f"
    
    echo -e "\n${GRAY}[Execution Output]${NC}"
    # Adding a separator for clarity
    echo -e "----------------------------------------------------"
    java RouteToGateway < "$f"
    echo -e "----------------------------------------------------"
    
    echo -e "\n"
done

echo -e "${BLUE}===> All tests completed.${NC}"
