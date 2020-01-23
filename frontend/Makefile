.PHONY: clean

all: build

node_modules: package.json yarn.lock
	yarn install
	touch node_modules

dist: $(shell find src -name "*.vue" -o -name "*.ts") node_modules
	yarn build
	touch dist

build: dist
	@echo "Done :)"

clean:
	rm -rf dist

start-frontend:
	yarn serve
