#ifndef __LIB_H__
#define __LIB_H__

#define NULL_VALUE 0
#define BOOLEAN_VALUE 1
#define INTEGER_VALUE 2
#define STRING_VALUE 3
#define PAIR_VALUE 4
#define VECTOR_VALUE 5
#define NATIVE_CLOSURE_VALUE 6
#define DYNAMIC_CLOSURE_VALUE 7

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

extern void _print_value(struct Value *value);
extern void _print_newline(void);

extern struct Value *_from_literal_int(int v);
extern struct Value *_from_literal_string(char *s);
extern struct Value *_mk_pair(struct Value *car, struct Value *cdr);
extern struct Value *_from_native_procedure(void *procedure, int number_arguments);
extern struct Value *_from_dynamic_procedure(void *procedure, int number_arguments, struct Value *frame);

extern void _assert_callable_closure(struct Value *closure, int number_arguments);
extern struct Value *_mk_frame(struct Value *parent, int size);
extern struct Value *_get_frame_value(struct Value *frame, int depth, int offset);
extern void _set_frame_value(struct Value *frame, int depth, int offset, struct Value *value);
extern struct Value *_get_frame(struct Value *frame, int depth);

extern struct Value *_call_closure_0(struct Value *closure);
extern struct Value *_call_closure_1(struct Value *closure, struct Value *a1);
extern struct Value *_call_closure_2(struct Value *closure, struct Value *a1, struct Value *a2);
extern struct Value *_call_closure_3(struct Value *closure, struct Value *a1, struct Value *a2, struct Value *a3);
extern struct Value *_call_closure_4(struct Value *closure, struct Value *a1, struct Value *a2, struct Value *a3, struct Value *a4);
extern struct Value *_call_closure_5(struct Value *closure, struct Value *a1, struct Value *a2, struct Value *a3, struct Value *a4, struct Value *a5);
extern struct Value *_call_closure_6(struct Value *closure, struct Value *a1, struct Value *a2, struct Value *a3, struct Value *a4, struct Value *a5, struct Value *a6);
extern struct Value *_call_closure_7(struct Value *closure, struct Value *a1, struct Value *a2, struct Value *a3, struct Value *a4, struct Value *a5, struct Value *a6, struct Value *a7);
extern struct Value *_call_closure_8(struct Value *closure, struct Value *a1, struct Value *a2, struct Value *a3, struct Value *a4, struct Value *a5, struct Value *a6, struct Value *a7, struct Value *a8);
extern struct Value *_call_closure_9(struct Value *closure, struct Value *a1, struct Value *a2, struct Value *a3, struct Value *a4, struct Value *a5, struct Value *a6, struct Value *a7, struct Value *a8, struct Value *a9);
extern struct Value *_call_closure_10(struct Value *closure, struct Value *a1, struct Value *a2, struct Value *a3, struct Value *a4, struct Value *a5, struct Value *a6, struct Value *a7, struct Value *a8, struct Value *a9, struct Value *a10);

extern struct Value *_plus(struct Value *op1, struct Value *op2);
extern struct Value *_minus(struct Value *op1, struct Value *op2);
extern struct Value *_multiply(struct Value *op1, struct Value *op2);
extern struct Value *_divide(struct Value *op1, struct Value *op2);
extern struct Value *_equals(struct Value *op1, struct Value *op2);
extern struct Value *_less_than(struct Value *op1, struct Value *op2);
extern struct Value *_greater_than(struct Value *op1, struct Value *op2);
extern struct Value *_pair_car(struct Value *pair);
extern struct Value *_pair_cdr(struct Value *pair);
extern struct Value *_nullp(struct Value *v);
extern struct Value *_booleanp(struct Value *v);
extern struct Value *_integerp(struct Value *v);
extern struct Value *_stringp(struct Value *v);
extern struct Value *_pairp(struct Value *v);

extern struct Value* _plus_variable(int num, ...);

#endif
