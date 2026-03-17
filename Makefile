JC = javac
JVM = java
CP = .:lib/*
default: build

build:
	$(JC) -cp "$(CP)" -g -d . *.java

run:
	$(JVM) -cp "$(CP)" Tema1 $(ARGS)

clean:
	rm -f *.class