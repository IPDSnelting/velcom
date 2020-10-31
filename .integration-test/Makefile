all: run

data:
	bash apply_initial_sql.sh

backend.jar: data

run-backend: data backend.jar
	java  -jar backend.jar server ../backend/backend/src/main/resources/example_config.yml

run-frontend:
	cd ../frontend && yarn serve --mode=development

run:
	bash start_backend_and_frontend.sh

clean:
	rm -rf data
	rm -f backend.jar

.PHONY: clean run
