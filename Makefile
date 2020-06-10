.PHONY: frontend backend clean

USER_ID ?= 1003

all: frontend backend

frontend:
	@echo '#######################'
	@echo '## Building frontend ##'
	@echo '#######################'
	make -C frontend/ --trace

backend:
	@echo '######################'
	@echo '## Building backend ##'
	@echo '######################'
	make -C backend/ --trace

clean:
	@echo '#######################'
	@echo '## Cleaning frontend ##'
	@echo '#######################'
	make -C frontend/ clean
	@echo '######################'
	@echo '## Cleaning backend ##'
	@echo '######################'
	make -C backend/ clean
	@echo '######################'
	@echo '## Cleaning docker  ##'
	@echo '######################'
	rm -rf .docker


docker-build-server: backend frontend
	mkdir -p .docker
	cp backend/backend/target/backend.jar .docker
	cp -r frontend/dist .docker
	cp -r docs/* .docker
	cp Dockerfile .docker
	(cd .docker && sudo docker build -t velcom-server:latest --build-arg USER_ID=$(USER_ID) .)

docker-build-server-single-port: backend
	make -C frontend/ mode=production-single-port
	mkdir -p .docker
	cp backend/backend/target/backend.jar .docker
	cp -r frontend/dist .docker
	cp -r docs/* .docker
	cp Dockerfile-Single-Port .docker/Dockerfile
	(cd .docker && sudo docker build -t velcom-server:latest --build-arg USER_ID=$(USER_ID) .)

docker-github-ci:
	mkdir -p .docker
	cp backend.jar .docker
	cp -r dist .docker
	cp -r docs/* .docker
	cp Dockerfile-Single-Port .docker/Dockerfile
	(cd .docker && sudo docker build -t velcom-server:latest --build-arg USER_ID=$(USER_ID) .)
