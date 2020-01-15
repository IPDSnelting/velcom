.PHONY: build clean

all: build

build:
	@echo 'Building backend'
	mvn package

clean:
	@echo 'Removing backend files'
	rm -f backend/data/data.db
	rm -rf backend/data/repos
	@echo 'Removing maven files'
	mvn clean
