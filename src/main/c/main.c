#include <stdio.h>

extern int _main(int n);

int main(int argc, char *argv[]) {
  _main(0);
  printf("42");
}
