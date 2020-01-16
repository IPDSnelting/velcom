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
