JAVA_VER := $(shell java -version 2>&1 | head -1 | cut -d'"' -f2 | sed '/^1\./s///' | cut -d'.' -f1 | sed 's/-ea//')

.PHONY: all clean

all:
	- ../../../gradlew build > Out.txt 2>&1
	grep -qF "Demo.java:16: warning: [NullAway] passing @Nullable parameter 'null' where @NonNull is required" Out.txt
	grep -qF "Demo.java:16: error: [argument.type.incompatible] incompatible argument for parameter name of Demo.generateGreeting." Out.txt

clean:
	-./gradlew -b ./docs/examples/NullAwayWithEISOP/build.gradle clean
	rm -f Out.txt
