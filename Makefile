.PHONY: frontend backend clean

all: frontend backend

frontend:
	@echo '#######################'
	@echo '## Building frontend ##'
	@echo '#######################'
	make -C frontend/

backend:
	@echo '######################'
	@echo '## Building backend ##'
	@echo '######################'
	make -C backend/

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
	(cd .docker && sudo docker build -t velcom-server:latest .)
