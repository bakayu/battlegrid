# Makefile for the BattleGrid Game

# Define the main classes for server and client
SERVER_CLASS=game.server.ServerRunner
CLIENT_CLASS=game.client.GameClient

# Define the Maven command
MVN=mvn

# Use .PHONY to declare targets that are not files
.PHONY: all sync server client clean

# Default target that runs when you just type 'make'
all: sync

# Target to update dependencies and build the project
# This cleans the project, resolves dependencies, and builds the JAR
sync:
	@echo "--- Syncing dependencies and building project... ---"
	$(MVN) clean install

# Target to run the server
# It depends on 'sync' to ensure the project is built first
server: sync
	@echo "--- Starting server... ---"
	$(MVN) exec:java -Dexec.mainClass="$(SERVER_CLASS)"

# Target to run the client
# It also depends on 'sync'
client: sync
	@echo "--- Starting client... ---"
	$(MVN) exec:java -Dexec.mainClass="$(CLIENT_CLASS)"

# Target to clean the project build artifacts
clean:
	@echo "--- Cleaning project... ---"
	$(MVN) clean