all: lib.o main.o

lib.o: lib.c lib.h
	clang -c lib.c

main.o: main.c
	clang -c main.c

clean:
	rm -f lib.o main.o
