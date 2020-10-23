.PHONY: clean

all: build

build: backend/target/backend.jar

backend/target/backend.jar: $(shell find -name '*.java' -o -name '*.sql' -o -name pom.xml)
	@echo 'Building backend'
	# We want colors!
	MAVEN_OPTS="-Djansi.passthrough=true -Djansi.force=true" mvn -Dstyle.color=always package site jacoco:report-aggregate

clean: clean-backend-data
	mvn clean

clean-backend-data:
	rm -f backend/data/data.db
	rm -rf backend/data/repos/
	rm -rf backend/data/archives/
	rm -rf backend/target/data/

start-runner: build
	@echo 'Starting runner'
	cd runner/target/ && java -jar runner.jar ../src/main/resources/example_config.json

start-backend: build
	@echo 'Starting backend'
	cd backend/target/ && java -jar backend.jar server ../src/main/resources/example_config.yml
