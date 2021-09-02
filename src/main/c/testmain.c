#include <stdio.h>
#include "lib.h"

extern int _main(int n);

int main(int argc, char *argv[]) {
  _initialise_lib();

 struct Value* (*pp)(int, ...) = &_plus_variable;

  _print_value(pp(3, _from_literal_int(10), _from_literal_int(20), _from_literal_int(30)));
  _print_newline();
}