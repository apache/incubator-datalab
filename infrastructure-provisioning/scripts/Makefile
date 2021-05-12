# Minimal makefile for Sphinx documentation

.PHONY: install-deps
install-deps:
	pip install --requirement ../../requirements.txt

.PHONY: doc
doc:
	set -e; make install-deps
	sphinx-build -T -c ./sphinx -a -b singlehtml . ./sphinx/docs

