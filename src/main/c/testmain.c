#include <stdio.h>
#include "lib.h"

extern int _main(int n);

void run_closure(struct Value *closure)
{
  _print_value(closure);
  _print_newline();

  _print_value(_call_closure_0(
      closure));
  _print_newline();

  _print_value(_call_closure_1(
      closure,
      _from_literal_int(1)));
  _print_newline();

  _print_value(_call_closure_2(
      closure,
      _from_literal_int(1),
      _from_literal_int(2)));
  _print_newline();

  _print_value(_call_closure_3(
      closure,
      _from_literal_int(1),
      _from_literal_int(2),
      _from_literal_int(3)));
  _print_newline();

  _print_value(_call_closure_4(
      closure,
      _from_literal_int(1),
      _from_literal_int(2),
      _from_literal_int(3),
      _from_literal_int(4)));
  _print_newline();

  _print_value(_call_closure_5(
      closure,
      _from_literal_int(1),
      _from_literal_int(2),
      _from_literal_int(3),
      _from_literal_int(4),
      _from_literal_int(5)));
  _print_newline();

  _print_value(_call_closure_6(
      closure,
      _from_literal_int(1),
      _from_literal_int(2),
      _from_literal_int(3),
      _from_literal_int(4),
      _from_literal_int(5),
      _from_literal_int(6)));
  _print_newline();

  _print_value(_call_closure_7(
      closure,
      _from_literal_int(1),
      _from_literal_int(2),
      _from_literal_int(3),
      _from_literal_int(4),
      _from_literal_int(5),
      _from_literal_int(6),
      _from_literal_int(7)));
  _print_newline();

  _print_value(_call_closure_8(
      closure,
      _from_literal_int(1),
      _from_literal_int(2),
      _from_literal_int(3),
      _from_literal_int(4),
      _from_literal_int(5),
      _from_literal_int(6),
      _from_literal_int(7),
      _from_literal_int(8)));
  _print_newline();

  _print_value(_call_closure_9(
      closure,
      _from_literal_int(1),
      _from_literal_int(2),
      _from_literal_int(3),
      _from_literal_int(4),
      _from_literal_int(5),
      _from_literal_int(6),
      _from_literal_int(7),
      _from_literal_int(8),
      _from_literal_int(9)));
  _print_newline();

  _print_value(_call_closure_10(
      closure,
      _from_literal_int(1),
      _from_literal_int(2),
      _from_literal_int(3),
      _from_literal_int(4),
      _from_literal_int(5),
      _from_literal_int(6),
      _from_literal_int(7),
      _from_literal_int(8),
      _from_literal_int(9),
      _from_literal_int(10)));
  _print_newline();
}

int main(int argc, char *argv[])
{
  _initialise_lib();

  struct Value *(*pp)(int, ...) = &_plus_variable;

  _print_value(pp(3, _from_literal_int(10), _from_literal_int(20), _from_literal_int(30)));
  _print_newline();

  run_closure(_from_native_var_arg_procedure(&_plus_variable));
  run_closure(_from_native_var_arg_procedure(&_multiply_variable));
  run_closure(_from_native_var_arg_procedure(&_minus_variable));
  run_closure(_from_native_var_arg_procedure(&_divide_variable));
  run_closure(_from_native_var_arg_procedure(&_println));
  run_closure(_from_native_var_arg_procedure(&_print));
}
