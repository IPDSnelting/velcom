.PHONY: clean

node_modules:
	yarn install

dist: $(find src -name "*.vue" -o -name "*.ts") node_modules
	yarn build
	touch dist

build: dist
	@echo "Done :)"

clean:
	rm -rf dist
