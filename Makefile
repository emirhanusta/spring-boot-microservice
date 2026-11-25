COMPOSE = docker compose
ALL_PROFILES = --profile infra --profile obs --profile apps

.DEFAULT_GOAL := help
APP_SERVICES = gateway identity-service catalog-service review-service
JAVA25 = JAVA_HOME=$${JAVA_HOME_25:-$$HOME/.sdkman/candidates/java/25-tem}

.PHONY: help infra-up obs-up down clean ps logs build test dev-keys apps-build up smoke

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

dev-keys: ## Generate a local RSA key pair for identity-service's dev profile (.local/keys, git-ignored)
	mkdir -p .local/keys
	openssl genpkey -algorithm RSA -pkeyopt rsa_keygen_bits:2048 -out .local/keys/private.pem
	openssl pkey -in .local/keys/private.pem -pubout -out .local/keys/public.pem
	chmod 644 .local/keys/private.pem  # dev only: the non-root container user must be able to read the mounted key

apps-build: ## Build boot jars (Java 25), copy to services/<name>/build/docker/app.jar, build images
	$(JAVA25) ./gradlew $(foreach s,$(APP_SERVICES),:services:$(s):bootJar)
	@for s in $(APP_SERVICES); do \
	  mkdir -p services/$$s/build/docker; \
	  jar=$$(ls services/$$s/build/libs/$$s-*.jar | grep -v -- '-plain.jar' | head -1); \
	  cp "$$jar" services/$$s/build/docker/app.jar; \
	  cp infra/docker/service.Dockerfile services/$$s/build/docker/Dockerfile; \
	done
	$(COMPOSE) --profile infra --profile apps build

up: ## Start infra + apps (creates dev keys if missing; run make apps-build first)
	@test -f .local/keys/private.pem || $(MAKE) dev-keys
	@for s in $(APP_SERVICES); do test -f services/$$s/build/docker/app.jar || { echo "missing jar for $$s - run 'make apps-build'"; exit 1; }; done
	$(COMPOSE) --profile infra --profile apps up -d
	@echo "waiting for the gateway (it depends on every service being healthy)..."
	@for i in $$(seq 1 90); do \
	  curl -sf http://localhost:$${GATEWAY_PORT:-8080}/actuator/health/readiness >/dev/null && { echo "gateway is up"; exit 0; }; \
	  sleep 4; \
	done; echo "gateway did not become ready in 6 minutes - see 'make logs'"; exit 1

smoke: ## End-to-end smoke test through the gateway (http://localhost:8080)
	./scripts/smoke.sh
