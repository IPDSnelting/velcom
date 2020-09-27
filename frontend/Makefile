.PHONY: clean

mode?=production

all: build

node_modules: package.json yarn.lock
	yarnpkg install
	touch node_modules

dist: $(shell find src -name "*.vue" -o -name "*.ts") node_modules .env.production
	yarnpkg build --mode="$(mode)"
	touch dist

build: dist
	@echo "Done :)"

clean:
	rm -rf dist

start-frontend:
	yarnpkg serve
