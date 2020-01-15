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
