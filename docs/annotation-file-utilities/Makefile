
all: annotation-file-format.pdf annotation-file-format.html

.PHONY: figures
figures:
	${MAKE} -C figures

export BIBINPUTS = $BIBINPUTS:.:../manual/plume-bib

annotation-file-format.pdf: plume-bib figures
	latexmk -bibtex -pdf annotation-file-format.tex

annotation-file-format.html: annotation-file-format.pdf
	hevea -fix -exec xxdate.exe urlhref.hva annotation-file-format.tex

# The bibliography is shared with the Checker Framework manual.
plume-bib:
	${MAKE} -C ../manual plume-bib

# Ignore failure of latexmk, so that "gradlew clean" works where LaTeX is not installed.
clean:
	-latexmk -C annotation-file-format.tex
	make -C figures clean
