COMPOSE = docker compose
ALL_PROFILES = --profile infra --profile obs --profile apps

.DEFAULT_GOAL := help
.PHONY: help infra-up obs-up down clean ps logs build test

help: ## Show this help
	@grep -E '^[a-z-]+:.*##' $(MAKEFILE_LIST) | awk -F':.*## ' '{printf "  %-10s %s\n", $$1, $$2}'

infra-up: ## Start Postgres, Kafka, Schema Registry, MinIO, kafka-ui
	$(COMPOSE) --profile infra up -d

obs-up: ## Start Jaeger, Prometheus, Grafana
	$(COMPOSE) --profile obs up -d

down: ## Stop all profiles (keep volumes)
	$(COMPOSE) $(ALL_PROFILES) down

clean: ## Stop all profiles and delete volumes
	$(COMPOSE) $(ALL_PROFILES) down -v

ps: ## List containers
	$(COMPOSE) $(ALL_PROFILES) ps

logs: ## Follow logs (make logs s=kafka for one service)
	$(COMPOSE) $(ALL_PROFILES) logs -f $(s)

build: ## Gradle build with Java 25
	JAVA_HOME=$${JAVA_HOME_25:-$$HOME/.sdkman/candidates/java/25-tem} ./gradlew build

test: ## Gradle tests with Java 25
	JAVA_HOME=$${JAVA_HOME_25:-$$HOME/.sdkman/candidates/java/25-tem} ./gradlew test
