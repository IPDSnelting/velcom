all: run-prod

data:
	bash apply_initial_sql.sh

backend.jar: data

build-backend: data backend.jar

run-backend: build-backend
	java  -jar backend.jar server ../backend/backend/src/main/resources/example_config.yml

build-frontend:
	(cd .. && ./scripts/build-frontend --env development)

run-frontend:
	python Frontend_Server.py ../frontend/dist 8080

run-dev:
	bash start_backend_and_frontend.sh development

run-prod:
	bash start_backend_and_frontend.sh prod

clean:
	rm -rf data cache tmp
	rm -rf .mypy_cache
	rm -f backend.jar

.PHONY: clean run
