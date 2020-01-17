.PHONY: clean

all: build

node_modules:
	@echo "Node modules..."
	@echo "$(file node_modules) - $(ls node_modules)"
	yarn install
	touch node_modules

dist: $(find src -name "*.vue" -o -name "*.ts") node_modules package.json
	yarn build
	touch dist

build: dist
	@echo "Done :)"

clean:
	rm -rf dist
