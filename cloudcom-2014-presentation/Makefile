SRC=$(wildcard *.tex)
FLAGS=-halt-on-error
PDF=$(SRC:.tex=.pdf)

all:
	latexmk --pdf
	latexmk -c
clean: ; latexmk -C
