# All targets
.PHONY: all clean skipped issue618 issue1015

# Tests that are currently passing
all: issue618 issue1015

# Tests that are currently not passing
skipped:

issue618:
	make -C issue618

issue1015:
	make -C issue1015

clean:
	make -C issue618 clean
	make -C issue1015 clean
