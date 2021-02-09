SHELL:=bash
S3_READY_REGEX=^Ready\.$

default: help

.PHONY: help
help:
	@grep -E '^[a-zA-Z_-]+:.*?## .*$$' $(MAKEFILE_LIST) | sort | awk 'BEGIN {FS = ":.*?## "}; {printf "\033[36m%-30s\033[0m %s\n", $$1, $$2}'

.PHONY: bootstrap
bootstrap: ## Bootstrap local environment for first use
	make git-hooks

.PHONY: git-hooks
git-hooks: ## Set up hooks in .git/hooks
	@{ \
		HOOK_DIR=.git/hooks; \
		for hook in $(shell ls .githooks); do \
			if [ ! -h $${HOOK_DIR}/$${hook} -a -x $${HOOK_DIR}/$${hook} ]; then \
				mv $${HOOK_DIR}/$${hook} $${HOOK_DIR}/$${hook}.local; \
				echo "moved existing $${hook} to $${hook}.local"; \
			fi; \
			ln -s -f ../../.githooks/$${hook} $${HOOK_DIR}/$${hook}; \
		done \
	}

certificates:
	./generate-certificates.sh

service-prometheus:
	docker-compose up -d prometheus

service-localstack:
	docker-compose up -d localstack
	@{ \
		while ! docker logs localstack 2> /dev/null | grep -q $(S3_READY_REGEX); do \
			echo Waiting for localstack.; \
			sleep 2; \
		done; \
	}
	docker-compose up localstack-init

services: service-prometheus service-localstack

dks: services
	docker-compose up -d dks
	@{ \
		while ! docker logs dks | fgrep -q "Started DataKeyServiceApplication"; do \
		  echo "Waiting for dks"; \
		  sleep 2; \
		done; \
	}

integration-tests: dks
	docker-compose up integration-tests

restart-prometheus:
	docker stop prometheus
	docker rm prometheus
	docker-compose build prometheus
	docker-compose up -d prometheus

datakey:
	curl -sS --insecure --cert certificate.pem:changeit --key key.pem https://localhost:8443/datakey | jq .

metrics:
	curl --insecure --cert certificate.pem:changeit --key key.pem https://localhost:8443/actuator/prometheus

