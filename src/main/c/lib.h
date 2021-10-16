#ifndef __LIB_H__
#define __LIB_H__

#include <setjmp.h>

#define NULL_VALUE 0
#define BOOLEAN_VALUE 1
#define INTEGER_VALUE 2
#define STRING_VALUE 3
#define PAIR_VALUE 4
#define VECTOR_VALUE 5
#define NATIVE_CLOSURE_VALUE 6
#define NATIVE_VAR_ARG_CLOSURE_VALUE 7
#define NATIVE_VAR_ARG_CLOSURE_POSITION_VALUE 8
#define DYNAMIC_CLOSURE_VALUE 9

struct Value
{
    int tag;
    union
    {
        int boolean;
        int integer;
        char *string;
        struct Pair
        {
            struct Value *car;
            struct Value *cdr;
        } pair;
        struct Vector
        {
            int length;
            struct Value **items;
        } vector;
        struct NativeClosure
        {
            void *procedure;
            int number_arguments;
            void *native_procedure;
        } native_closure;
        struct NativeVarArgClosure
        {
            void *native_procedure;
        } native_var_arg_closure;
        struct NativeVarArgClosurePosition
        {
            void *native_procedure;
            char *file_name;
            int line_number;
        } native_var_arg_closure_position;
        struct DynamicClosure
        {
            void *procedure;
            int number_arguments;
            struct Value *frame;
        } dynamic_closure;
    };
};

extern struct Value *_VNull;
extern struct Value *_VTrue;
extern struct Value *_VFalse;

extern void _initialise_lib();

extern void _print_value(char *file_name, int line_number, struct Value *value);
extern void _print_newline(void);

extern struct Value *_from_literal_int(int v);
extern struct Value *_from_literal_string(char *s);
extern struct Value *_mk_pair(struct Value *car, struct Value *cdr);
extern struct Value *_from_native_var_arg_procedure(void *procedure);
extern struct Value *_from_native_var_arg_position_procedure(char *file_name, int line_number, void *procedure);
extern struct Value *_from_native_procedure(char *file_name, int line_number, void *procedure, int number_arguments);
extern struct Value *_from_dynamic_procedure(void *procedure, int number_arguments, struct Value *frame);

extern void _assert_callable_closure(char *file_name, int line_number, struct Value *closure, int number_arguments);
extern struct Value *_mk_frame(struct Value *parent, int size);
extern struct Value *_get_frame_value(struct Value *frame, int depth, int offset);
extern void _set_frame_value(struct Value *frame, int depth, int offset, struct Value *value);
extern struct Value *_get_frame(struct Value *frame, int depth);

extern struct Value *_call_closure_0(char *file_name, int line_number, struct Value *closure);
extern struct Value *_call_closure_1(char *file_name, int line_number, struct Value *closure, struct Value *a1);
extern struct Value *_call_closure_2(char *file_name, int line_number, struct Value *closure, struct Value *a1, struct Value *a2);
extern struct Value *_call_closure_3(char *file_name, int line_number, struct Value *closure, struct Value *a1, struct Value *a2, struct Value *a3);
extern struct Value *_call_closure_4(char *file_name, int line_number, struct Value *closure, struct Value *a1, struct Value *a2, struct Value *a3, struct Value *a4);
extern struct Value *_call_closure_5(char *file_name, int line_number, struct Value *closure, struct Value *a1, struct Value *a2, struct Value *a3, struct Value *a4, struct Value *a5);
extern struct Value *_call_closure_6(char *file_name, int line_number, struct Value *closure, struct Value *a1, struct Value *a2, struct Value *a3, struct Value *a4, struct Value *a5, struct Value *a6);
extern struct Value *_call_closure_7(char *file_name, int line_number, struct Value *closure, struct Value *a1, struct Value *a2, struct Value *a3, struct Value *a4, struct Value *a5, struct Value *a6, struct Value *a7);
extern struct Value *_call_closure_8(char *file_name, int line_number, struct Value *closure, struct Value *a1, struct Value *a2, struct Value *a3, struct Value *a4, struct Value *a5, struct Value *a6, struct Value *a7, struct Value *a8);
extern struct Value *_call_closure_9(char *file_name, int line_number, struct Value *closure, struct Value *a1, struct Value *a2, struct Value *a3, struct Value *a4, struct Value *a5, struct Value *a6, struct Value *a7, struct Value *a8, struct Value *a9);
extern struct Value *_call_closure_10(char *file_name, int line_number, struct Value *closure, struct Value *a1, struct Value *a2, struct Value *a3, struct Value *a4, struct Value *a5, struct Value *a6, struct Value *a7, struct Value *a8, struct Value *a9, struct Value *a10);

extern struct Value *_plus(struct Value *op1, struct Value *op2);
extern struct Value *_minus(struct Value *op1, struct Value *op2);
extern struct Value *_multiply(struct Value *op1, struct Value *op2);
extern struct Value *_divide(char *file_name, int line_number, struct Value *op1, struct Value *op2);
extern struct Value *_equals(struct Value *op1, struct Value *op2);
extern struct Value *_less_than(struct Value *op1, struct Value *op2);
extern struct Value *_greater_than(struct Value *op1, struct Value *op2);
extern struct Value *_pair_car(char *file_name, int line_number, struct Value *pair);
extern struct Value *_pair_cdr(char *file_name, int line_number, struct Value *pair);
extern struct Value *_nullp(struct Value *v);
extern struct Value *_booleanp(struct Value *v);
extern struct Value *_integerp(struct Value *v);
extern struct Value *_stringp(struct Value *v);
extern struct Value *_pairp(struct Value *v);

extern struct Value* _plus_variable(int num, ...);
extern struct Value* _multiply_variable(int num, ...);
extern struct Value* _minus_variable(int num, ...);
extern struct Value* _divide_variable(char *file_name, int line_number, int num, ...);
extern struct Value* _println(char *file_name, int line_number, int num, ...);
extern struct Value* _print(char *file_name, int line_number, int num, ...);

struct ExceptionTryBlock {
  jmp_buf jmp;
  struct Value *exception;
};

extern struct ExceptionTryBlock _exception_try_blocks[];
extern int _exception_try_block_idx;

extern struct Value *_exception_try(char *file_name, int line_number, struct Value *body, struct Value *handler);
extern void _exception_throw(char *file_name, int line_number, struct Value *exception);

#endif
