#include <stdio.h>
#include <setjmp.h>

#include "../../../bdwgc/include/gc.h"

#include "lib.h"

extern int _main(int n);

int main(int argc, char *argv[])
{
  GC_INIT();

  _initialise_lib();

  int result = 0;

  _exception_try_block_idx += 1;
  _exception_try_blocks[_exception_try_block_idx].exception = _VNull;
  if (setjmp(_exception_try_blocks[_exception_try_block_idx].jmp))
  {
    printf("Unhandled Exception: ");
    _print_value("", 0, _exception_try_blocks[_exception_try_block_idx].exception);
    _print_newline();
    _exception_try_block_idx -= 1;
    result = 1;
  }
  else
  {
    _main(0);
    _exception_try_block_idx -= 1;
    result = 0;
  }

  // struct GC_prof_stats_s stats;
  // GC_get_prof_stats(&stats, 0);

  // printf("GC Stats:\n");
  // printf("  Heap size:                 %lu\n", stats.heapsize_full);
  // printf("  Free bytes:                %lu\n", stats.free_bytes_full);
  // printf("  Unmapped bytes:            %lu\n", stats.unmapped_bytes);
  // printf("  Bytes allocated since GC:  %lu\n", stats.bytes_allocd_since_gc);
  // printf("  Bytes allocated before GC: %lu\n", stats.allocd_bytes_before_gc);
  // printf("  Non GC bytes:              %lu\n", stats.non_gc_bytes);
  // printf("  GC cycle no:               %lu\n", stats.gc_no);
  // printf("  No of marker threads:      %lu\n", stats.markers_m1);
  // printf("  Bytes reclaimed since GC:  %lu\n", stats.bytes_reclaimed_since_gc);
  // printf("  Bytes reclaimed before GC: %lu\n", stats.reclaimed_bytes_before_gc);
  // printf("  Explicitly freed since GC: %lu\n", stats.expl_freed_bytes_since_gc);
  // printf("  Memory from OS:            %lu\n", stats.obtained_from_os_bytes);

  GC_deinit();

  return result;
}
