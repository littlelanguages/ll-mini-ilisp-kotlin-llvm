TARGETS=hello primes euler-001

all: $(TARGETS)

%: %.bc
	clang $< ../src/main/c/lib.o ../src/main/c/main.o -o $@

%.bc: %.mlsp
	../ll-mini-ilisp-kotlin-llvm/bin/ll-mini-ilisp-kotlin-llvm $<

clean:
	rm -f *.bc *.ll $(TARGETS)
