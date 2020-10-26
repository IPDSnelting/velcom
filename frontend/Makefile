.PHONY: clean

mode?=production

all: build

node_modules: package.json yarn.lock
	yarnpkg --color=always install
	touch node_modules

dist: $(shell find src -name "*.vue" -o -name "*.ts") node_modules .env.production
	yarnpkg --color=always build --mode="$(mode)"
	touch dist

build: dist
	@echo "Done :)"

clean:
	rm -rf dist

start-frontend:
	yarnpkg serve
