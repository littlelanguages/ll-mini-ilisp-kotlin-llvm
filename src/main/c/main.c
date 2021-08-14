#include <stdio.h>
#include "lib.h"

extern int _main(int n);

int main(int argc, char *argv[]) {
  _initialise_lib();
  _main(0);
}
