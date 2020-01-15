.PHONY: frontend backend clean

all: frontend backend

frontend:
	@echo '#######################'
	@echo '## Building frontend ##'
	@echo '#######################'
	@echo '  Nothing to see here (yet)'

backend:
	@echo '######################'
	@echo '## Building backend ##'
	@echo '######################'
	make -C backend/

clean:
	@echo '#######################'
	@echo '## Cleaning frontend ##'
	@echo '#######################'
	@echo '  Nothing to see here (yet)'
	@echo '######################'
	@echo '## Cleaning backend ##'
	@echo '######################'
	make -C backend/ clean
