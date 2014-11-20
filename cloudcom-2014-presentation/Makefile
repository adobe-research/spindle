SRC=cloudcom-pres.tex
FLAGS=-halt-on-error
PDF=$(SRC:.tex=.pdf)

all:
	latexmk --pdf $(SRC)
	latexmk -c
clean: ; latexmk -C
