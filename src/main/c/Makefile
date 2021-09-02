all: lib.o main.o

lib.o: lib.c lib.h
	clang -c lib.c

main.o: main.c
	clang -c main.c

testmain.o: testmain.c
	clang -c testmain.c

testmain: testmain.o lib.o
	clang testmain.o lib.o -o testmain

testmain.ll: testmain.c
	clang -c testmain.c -emit-llvm -o testmain.bc
	llvm-dis testmain.bc

clean:
	rm -f *.o *.bc *.ll testmain
