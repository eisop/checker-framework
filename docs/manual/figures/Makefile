# Put user-specific changes in your own Makefile.user.
# Make will silently continue if that file does not exist.
-include Makefile.user

PDFFILES = $(patsubst %.svg,%.pdf,$(wildcard *.svg))

RSVG_CONVERT_VERSION := $(shell rsvg-convert --version 2>/dev/null)
INKSCAPE_VERSION := $(shell inkscape --version 2>/dev/null)

all: ${PDFFILES} clean-obsolete-files svg-copy

svg-copy:
	-chmod -f u+w ../*.svg
	cp -pf *.svg ..
	chmod -f a-w ../*.svg

%.pdf : %.svg
ifdef RSVG_CONVERT_VERSION
	rsvg-convert -f pdf -o $@ $<
else
  ifdef INKSCAPE_VERSION
	inkscape $< --export-filename=$@
  else
	convert $< $@
  endif
endif

# reflection-inference.svg is checked in: it is the HTML manual's rendering of
# ../reflection-inference-rules.tex, which the PDF manual typesets itself.  It is not a
# prerequisite of `all`, so that building the manual does not need the tools below.
# Run this target after changing the rules, and commit the result.
# pdfcrop is in texlive-extra-utils and pdftocairo in poppler-utils.  The glyphs become paths, so
# the figure needs no fonts.
.PHONY: reflection-inference-svg
reflection-inference-svg:
	pdflatex -interaction=nonstopmode reflection-inference.tex
	pdfcrop reflection-inference.pdf reflection-inference-crop.pdf
	pdftocairo -svg reflection-inference-crop.pdf reflection-inference.svg
	rm -f reflection-inference.pdf reflection-inference-crop.pdf reflection-inference.aux reflection-inference.log

clean: clean-obsolete-files
	@\rm -f ${PDFFILES}

clean-obsolete-files:
	@\rm -f *.png
	@\rm -f *.eps
