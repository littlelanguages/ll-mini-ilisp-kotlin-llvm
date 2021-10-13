#include <stdio.h>
#include <setjmp.h>

#include "lib.h"

extern int _main(int n);

int main(int argc, char *argv[]) {
  _initialise_lib();

//  _print_value(_plus_variable(3, _from_literal_int(10), _from_literal_int(20), _from_literal_int(30)));
//  _print_newline();

  _exception_try_block_idx += 1;
  _exception_try_blocks[_exception_try_block_idx].exception = _VNull;
  if (setjmp(_exception_try_blocks[_exception_try_block_idx].jmp))
  {
    printf("Unhandled Exception: ");
    _print_value("", 0, _exception_try_blocks[_exception_try_block_idx].exception);
    _print_newline();
    _exception_try_block_idx -= 1;
    return 1;
  }
  else
  {
    _main(0);
    _exception_try_block_idx -=1;
    return 0;
  }
}
