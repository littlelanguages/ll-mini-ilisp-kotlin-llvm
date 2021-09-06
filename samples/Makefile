all: hello


hello: hello.bc
	clang hello.bc ../src/main/c/lib.o ../src/main/c/main.o -o hello


%.bc: %.mlsp
	../ll-mini-ilisp-kotlin-llvm/bin/ll-mini-ilisp-kotlin-llvm $<

clean:
	rm -i *.bc
