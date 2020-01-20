.PHONY: clean

all: build

build: backend/target/backend.jar

backend/target/backend.jar: $(find -name '*.java' -o -name pom.xml)
	@echo 'Building backend'
	mvn package

clean:
	@echo 'Removing backend files'
	rm -f backend/data/data.db
	rm -rf backend/data/repos
	@echo 'Removing maven files'
	mvn clean

start-runner:
	cd runner/target/ && java -jar runner.jar ../src/main/resources/example_config.json

start-backend:
	cd backend/target && java -jar backend.jar server ../src/main/resources/example_config.yml
