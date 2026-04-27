#!/bin/bash
echo "[INFO] Stopping any existing Java processes on port 8080..."
fuser -k 8080/tcp 2>/dev/null
echo "[INFO] Starting Spring Boot application..."
./gradlew bootRun
