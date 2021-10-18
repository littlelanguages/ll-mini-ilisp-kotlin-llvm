TARGETS=hello primes euler-001 divide-by-zero

all: $(TARGETS)

%: %.bc
	clang $< ../src/main/c/lib.o ../bdwgc/gc.a ../src/main/c/main.o -o $@

%.bc: %.mlsp
	../ll-mini-ilisp-kotlin-llvm/bin/ll-mini-ilisp-kotlin-llvm $<
	opt -O3 $@ > tmp
	mv tmp $@
	llvm-dis $@

clean:
	rm -f *.bc *.ll $(TARGETS)
