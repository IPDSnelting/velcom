all: run-backend

data:
	bash apply_initial_sql.sh

backend.jar: data

run-backend: data backend.jar
	java  -jar backend.jar server ../backend/backend/src/main/resources/example_config.yml

clean:
	rm -rf data
	rm -f backend.jar

.PHONY: clean run
