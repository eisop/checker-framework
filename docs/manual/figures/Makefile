# Put user-specific changes in your own Makefile.user.
# Make will silently continue if that file does not exist.
-include Makefile.user

PDFFILES = $(patsubst %.svg,%.pdf,$(wildcard *.svg))

RSVG_CONVERT_VERSION := $(shell rsvg-convert --version 2>/dev/null)
INKSCAPE_VERSION := $(shell inkscape --version 2>/dev/null)

all: check-reflection-inference-svg ${PDFFILES} clean-obsolete-files svg-copy

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
# The target records the hash of the files that the figure is made from in the SVG, and `all`
# fails when they no longer match it, so that the HTML manual does not show outdated rules.
REFLECTION_INFERENCE_HASH = $(shell cat ../reflection-inference-rules.tex reflection-inference.tex | sha256sum | cut -d' ' -f1)

.PHONY: reflection-inference-svg
reflection-inference-svg:
	pdflatex -interaction=nonstopmode reflection-inference.tex
	pdfcrop reflection-inference.pdf reflection-inference-crop.pdf
	pdftocairo -svg reflection-inference-crop.pdf reflection-inference.svg
	sed -i '1a <!-- source-sha256: ${REFLECTION_INFERENCE_HASH} -->' reflection-inference.svg
	rm -f reflection-inference.pdf reflection-inference-crop.pdf reflection-inference.aux reflection-inference.log

.PHONY: check-reflection-inference-svg
check-reflection-inference-svg:
	@grep -q 'source-sha256: ${REFLECTION_INFERENCE_HASH}' reflection-inference.svg \
	  || { echo "reflection-inference.svg is out of date: run 'make -C docs/manual/figures reflection-inference-svg' and commit the result."; exit 1; }

clean: clean-obsolete-files
	@\rm -f ${PDFFILES}

clean-obsolete-files:
	@\rm -f *.png
	@\rm -f *.eps
