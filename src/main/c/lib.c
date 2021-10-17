/* Library to link into compiled code
 */

#include <stdarg.h>
#include <stdio.h>
#include <stdlib.h>
#include <string.h>

#include "./lib.h"
#include "../../../bdwgc/include/gc.h"

struct Value *_VNull;
struct Value *_VTrue;
struct Value *_VFalse;

void _initialise_lib()
{
    _VNull = (struct Value *)GC_MALLOC(sizeof(struct Value));
    _VNull->tag = NULL_VALUE;

    _VTrue = (struct Value *)GC_MALLOC(sizeof(struct Value));
    _VTrue->tag = BOOLEAN_VALUE;
    _VTrue->boolean = (1 == 1);

    _VFalse = (struct Value *)GC_MALLOC(sizeof(struct Value));
    _VFalse->tag = BOOLEAN_VALUE;
    _VFalse->boolean = (1 == 0);
}

void _print_value(char *file_name, int line_number, struct Value *value)
{
    switch (value->tag)
    {
    case NULL_VALUE:
        printf("()");
        break;
    case BOOLEAN_VALUE:
        if (value->boolean)
            printf("#t");
        else
            printf("#f");
        break;
    case INTEGER_VALUE:
        printf("%d", value->integer);
        break;
    case STRING_VALUE:
        printf("%s", value->string);
        break;
    case PAIR_VALUE:
    {
        printf("(");
        _print_value(file_name, line_number, value->pair.car);

        struct Value *runner = value->pair.cdr;

        while (1)
        {
            if (runner->tag == PAIR_VALUE)
            {
                printf(" ");
                _print_value(file_name, line_number, runner->pair.car);
                runner = runner->pair.cdr;
            }
            else if (runner->tag == NULL_VALUE)
                break;
            else
            {
                printf(" . ");
                _print_value(file_name, line_number, runner);
                break;
            }
        }
        printf(")");
        break;
    }
    case NATIVE_CLOSURE_VALUE:
        printf("#NATIVE_CLOSURE/%d", value->native_closure.number_arguments);
        break;
    case NATIVE_VAR_ARG_CLOSURE_VALUE:
        printf("#NATIVE_VAR_ARG_CLOSURE");
        break;
    case NATIVE_VAR_ARG_CLOSURE_POSITION_VALUE:
        printf("#NATIVE_VAR_ARG_POSITION_CLOSURE/%s/%d", value->native_var_arg_closure_position.file_name, value->native_var_arg_closure_position.line_number);
        break;
    case DYNAMIC_CLOSURE_VALUE:
        printf("#DYNAMIC_CLOSURE/%d", value->dynamic_closure.number_arguments);
        break;
    default:
        _exception_throw(file_name, line_number,
                         _mk_pair(
                             _from_literal_string("InternalError"),
                             _mk_pair(
                                 _mk_pair(_from_literal_string("reason"), _from_literal_string("UnknownTag")),
                                 _mk_pair(
                                     _mk_pair(_from_literal_string("tag"), _from_literal_int(value->tag)),
                                     _VNull))));
    }
}

struct Value *_from_literal_int(int v)
{
    struct Value *r = (struct Value *)GC_MALLOC(sizeof(struct Value));
    r->tag = INTEGER_VALUE;
    r->integer = v;
    return r;
}

struct Value *_from_literal_string(char *s)
{
    struct Value *r = (struct Value *)GC_MALLOC(sizeof(struct Value));
    r->tag = STRING_VALUE;
    r->string = strdup(s);
    return r;
}

struct Value *_wrap_native_0(void *native_procedure)
{
    struct Value *(*f)() = native_procedure;
    return f();
}

struct Value *_wrap_native_1(void *native_procedure, struct Value *a1)
{
    struct Value *(*f)(struct Value *) = native_procedure;
    return f(a1);
}

struct Value *_wrap_native_2(void *native_procedure, struct Value *a1, struct Value *a2)
{
    struct Value *(*f)(struct Value *, struct Value *) = native_procedure;
    return f(a1, a2);
}

struct Value *_wrap_native_3(void *native_procedure, struct Value *a1, struct Value *a2, struct Value *a3)
{
    struct Value *(*f)(struct Value *, struct Value *, struct Value *) = native_procedure;
    return f(a1, a2, a3);
}

struct Value *_wrap_native_4(void *native_procedure, struct Value *a1, struct Value *a2, struct Value *a3, struct Value *a4)
{
    struct Value *(*f)(struct Value *, struct Value *, struct Value *, struct Value *) = native_procedure;
    return f(a1, a2, a3, a4);
}

struct Value *_wrap_native_5(void *native_procedure, struct Value *a1, struct Value *a2, struct Value *a3, struct Value *a4, struct Value *a5)
{
    struct Value *(*f)(struct Value *, struct Value *, struct Value *, struct Value *, struct Value *) = native_procedure;
    return f(a1, a2, a3, a4, a5);
}

struct Value *_wrap_native_6(void *native_procedure, struct Value *a1, struct Value *a2, struct Value *a3, struct Value *a4, struct Value *a5, struct Value *a6)
{
    struct Value *(*f)(struct Value *, struct Value *, struct Value *, struct Value *, struct Value *, struct Value *) = native_procedure;
    return f(a1, a2, a3, a4, a5, a6);
}

struct Value *_wrap_native_7(void *native_procedure, struct Value *a1, struct Value *a2, struct Value *a3, struct Value *a4, struct Value *a5, struct Value *a6, struct Value *a7)
{
    struct Value *(*f)(struct Value *, struct Value *, struct Value *, struct Value *, struct Value *, struct Value *, struct Value *) = native_procedure;
    return f(a1, a2, a3, a4, a5, a6, a7);
}

struct Value *_wrap_native_8(void *native_procedure, struct Value *a1, struct Value *a2, struct Value *a3, struct Value *a4, struct Value *a5, struct Value *a6, struct Value *a7, struct Value *a8)
{
    struct Value *(*f)(struct Value *, struct Value *, struct Value *, struct Value *, struct Value *, struct Value *, struct Value *, struct Value *) = native_procedure;
    return f(a1, a2, a3, a4, a5, a6, a7, a8);
}

struct Value *_wrap_native_9(void *native_procedure, struct Value *a1, struct Value *a2, struct Value *a3, struct Value *a4, struct Value *a5, struct Value *a6, struct Value *a7, struct Value *a8, struct Value *a9)
{
    struct Value *(*f)(struct Value *, struct Value *, struct Value *, struct Value *, struct Value *, struct Value *, struct Value *, struct Value *, struct Value *) = native_procedure;
    return f(a1, a2, a3, a4, a5, a6, a7, a8, a9);
}

struct Value *_wrap_native_10(void *native_procedure, struct Value *a1, struct Value *a2, struct Value *a3, struct Value *a4, struct Value *a5, struct Value *a6, struct Value *a7, struct Value *a8, struct Value *a9, struct Value *a10)
{
    struct Value *(*f)(struct Value *, struct Value *, struct Value *, struct Value *, struct Value *, struct Value *, struct Value *, struct Value *, struct Value *, struct Value *) = native_procedure;
    return f(a1, a2, a3, a4, a5, a6, a7, a8, a9, a10);
}

struct Value *_from_native_var_arg_procedure(void *procedure)
{
    struct Value *r = (struct Value *)GC_MALLOC(sizeof(struct Value));
    r->tag = NATIVE_VAR_ARG_CLOSURE_VALUE;
    r->native_var_arg_closure.native_procedure = procedure;

    return r;
}

struct Value *_from_native_var_arg_position_procedure(char *file_name, int line_number, void *procedure)
{
    struct Value *r = (struct Value *)GC_MALLOC(sizeof(struct Value));
    r->tag = NATIVE_VAR_ARG_CLOSURE_POSITION_VALUE;
    r->native_var_arg_closure_position.native_procedure = procedure;
    r->native_var_arg_closure_position.file_name = file_name;
    r->native_var_arg_closure_position.line_number = line_number;

    return r;
}

struct Value *_from_native_procedure(char *file_name, int line_number, void *procedure, int number_arguments)
{
    struct Value *r = (struct Value *)GC_MALLOC(sizeof(struct Value));
    r->tag = NATIVE_CLOSURE_VALUE;

    switch (number_arguments)
    {
    case 0:
        r->native_closure.procedure = &_wrap_native_0;
        break;
    case 1:
        r->native_closure.procedure = &_wrap_native_1;
        break;
    case 2:
        r->native_closure.procedure = &_wrap_native_2;
        break;
    case 3:
        r->native_closure.procedure = &_wrap_native_3;
        break;
    case 4:
        r->native_closure.procedure = &_wrap_native_4;
        break;
    case 5:
        r->native_closure.procedure = &_wrap_native_5;
        break;
    case 6:
        r->native_closure.procedure = &_wrap_native_6;
        break;
    case 7:
        r->native_closure.procedure = &_wrap_native_7;
        break;
    case 8:
        r->native_closure.procedure = &_wrap_native_8;
        break;
    case 9:
        r->native_closure.procedure = &_wrap_native_9;
        break;
    case 10:
        r->native_closure.procedure = &_wrap_native_10;
        break;
    default:
        _exception_throw(file_name, line_number,
                         _mk_pair(
                             _from_literal_string("InternalError"),
                             _mk_pair(
                                 _mk_pair(_from_literal_string("reason"), _from_literal_string("UnableToWrapNative")),
                                 _mk_pair(
                                     _mk_pair(_from_literal_string("number-of-arguments"), _from_literal_int(number_arguments)),
                                     _VNull))));
    }

    r->native_closure.number_arguments = number_arguments;
    r->native_closure.native_procedure = procedure;

    return r;
}

struct Value *_from_dynamic_procedure(void *procedure, int number_arguments, struct Value *frame)
{
    struct Value *r = (struct Value *)GC_MALLOC(sizeof(struct Value));
    r->tag = DYNAMIC_CLOSURE_VALUE;
    r->dynamic_closure.procedure = procedure;
    r->dynamic_closure.number_arguments = number_arguments;
    r->dynamic_closure.frame = frame;

    return r;
}

void _assert_callable_closure(char *file_name, int line_number, struct Value *closure, int number_arguments)
{
    if (closure->tag != NATIVE_CLOSURE_VALUE && closure->tag != DYNAMIC_CLOSURE_VALUE)
    {
        _exception_throw(file_name, line_number,
                         _mk_pair(
                             _from_literal_string("NotClosure"),
                             _mk_pair(
                                 _mk_pair(_from_literal_string("reason"), _from_literal_string("Attempt to call value as if a closure")),
                                 _mk_pair(
                                     _mk_pair(_from_literal_string("tag"), _from_literal_int(closure->tag)),
                                     _VNull))));
    }
    if (closure->native_closure.number_arguments != number_arguments)
    {
        _exception_throw(file_name, line_number,
                         _mk_pair(
                             _from_literal_string("ArgumentCountMismatch"),
                             _mk_pair(
                                 _mk_pair(_from_literal_string("reason"), _from_literal_string("Argument mismatch")),
                                 _mk_pair(
                                     _mk_pair(_from_literal_string("received"), _from_literal_int(number_arguments)),
                                     _mk_pair(
                                         _mk_pair(_from_literal_string("expected"), _from_literal_int(closure->native_closure.number_arguments)),
                                         _VNull)))));
    }
}

struct Value *_mk_frame(struct Value *parent, int size)
{
    struct Value *frame = (struct Value *)GC_MALLOC(sizeof(struct Value));
    frame->tag = VECTOR_VALUE;
    frame->vector.length = 1;
    frame->vector.items = (struct Value **)GC_MALLOC(sizeof(struct Value *) * (1 + size));
    frame->vector.items[0] = parent;

    while (size > 0)
    {
        frame->vector.items[size] = _VNull;
        size -= 1;
    }

    return frame;
}

struct Value *_get_frame(struct Value *frame, int depth)
{
    while (depth > 0)
    {
        frame = frame->vector.items[0];
        depth -= 1;
    }
    return frame;
}

struct Value *_get_frame_value(struct Value *frame, int depth, int offset)
{
    return _get_frame(frame, depth)->vector.items[offset];
}

void _set_frame_value(struct Value *frame, int depth, int offset, struct Value *value)
{
    while (depth > 0)
    {
        frame = frame->vector.items[0];
        depth -= 1;
    }
    frame->vector.items[offset] = value;
}

struct Value *_call_closure_0(char *file_name, int line_number, struct Value *closure)
{
    if (closure->tag == NATIVE_VAR_ARG_CLOSURE_VALUE)
    {
        struct Value *(*f)(int, ...) = closure->native_var_arg_closure.native_procedure;
        return f(0);
    }
    else if (closure->tag == NATIVE_VAR_ARG_CLOSURE_POSITION_VALUE)
    {
        struct Value *(*f)(char *, int, int, ...) = closure->native_var_arg_closure_position.native_procedure;
        return f(closure->native_var_arg_closure_position.file_name, closure->native_var_arg_closure_position.line_number, 0);
    }
    else
    {
        _assert_callable_closure(file_name, line_number, closure, 0);

        struct Value *(*f)(struct Value *) = closure->dynamic_closure.procedure;

        return f(closure->dynamic_closure.frame);
    }
}

struct Value *_call_closure_1(char *file_name, int line_number, struct Value *closure, struct Value *a1)
{
    if (closure->tag == NATIVE_VAR_ARG_CLOSURE_VALUE)
    {
        struct Value *(*f)(int, ...) = closure->native_var_arg_closure.native_procedure;
        return f(1, a1);
    }
    else if (closure->tag == NATIVE_VAR_ARG_CLOSURE_POSITION_VALUE)
    {
        struct Value *(*f)(char *, int, int, ...) = closure->native_var_arg_closure_position.native_procedure;
        return f(closure->native_var_arg_closure_position.file_name, closure->native_var_arg_closure_position.line_number, 1, a1);
    }
    else
    {
        _assert_callable_closure(file_name, line_number, closure, 1);

        struct Value *(*f)(struct Value *, struct Value *) = closure->dynamic_closure.procedure;

        return f(closure->dynamic_closure.frame, a1);
    }
}

struct Value *_call_closure_2(char *file_name, int line_number, struct Value *closure, struct Value *a1, struct Value *a2)
{
    if (closure->tag == NATIVE_VAR_ARG_CLOSURE_VALUE)
    {
        struct Value *(*f)(int, ...) = closure->native_var_arg_closure.native_procedure;
        return f(2, a1, a2);
    }
    else if (closure->tag == NATIVE_VAR_ARG_CLOSURE_POSITION_VALUE)
    {
        struct Value *(*f)(char *, int, int, ...) = closure->native_var_arg_closure_position.native_procedure;
        return f(closure->native_var_arg_closure_position.file_name, closure->native_var_arg_closure_position.line_number, 2, a1, a2);
    }
    else
    {
        _assert_callable_closure(file_name, line_number, closure, 2);

        struct Value *(*f)(struct Value *, struct Value *, struct Value *) = closure->dynamic_closure.procedure;

        return f(closure->dynamic_closure.frame, a1, a2);
    }
}

struct Value *_call_closure_3(char *file_name, int line_number, struct Value *closure, struct Value *a1, struct Value *a2, struct Value *a3)
{
    if (closure->tag == NATIVE_VAR_ARG_CLOSURE_VALUE)
    {
        struct Value *(*f)(int, ...) = closure->native_var_arg_closure.native_procedure;
        return f(3, a1, a2, a3);
    }
    else if (closure->tag == NATIVE_VAR_ARG_CLOSURE_POSITION_VALUE)
    {
        struct Value *(*f)(char *, int, int, ...) = closure->native_var_arg_closure_position.native_procedure;
        return f(closure->native_var_arg_closure_position.file_name, closure->native_var_arg_closure_position.line_number, 3, a1, a2, a3);
    }
    else
    {
        _assert_callable_closure(file_name, line_number, closure, 3);

        struct Value *(*f)(struct Value *, struct Value *, struct Value *, struct Value *) = closure->dynamic_closure.procedure;

        return f(closure->dynamic_closure.frame, a1, a2, a3);
    }
}

struct Value *_call_closure_4(char *file_name, int line_number, struct Value *closure, struct Value *a1, struct Value *a2, struct Value *a3, struct Value *a4)
{
    if (closure->tag == NATIVE_VAR_ARG_CLOSURE_VALUE)
    {
        struct Value *(*f)(int, ...) = closure->native_var_arg_closure.native_procedure;
        return f(4, a1, a2, a3, a4);
    }
    else if (closure->tag == NATIVE_VAR_ARG_CLOSURE_POSITION_VALUE)
    {
        struct Value *(*f)(char *, int, int, ...) = closure->native_var_arg_closure_position.native_procedure;
        return f(closure->native_var_arg_closure_position.file_name, closure->native_var_arg_closure_position.line_number, 4, a1, a2, a3, a4);
    }
    else
    {
        _assert_callable_closure(file_name, line_number, closure, 4);

        struct Value *(*f)(struct Value *, struct Value *, struct Value *, struct Value *, struct Value *) = closure->dynamic_closure.procedure;

        return f(closure->dynamic_closure.frame, a1, a2, a3, a4);
    }
}

struct Value *_call_closure_5(char *file_name, int line_number, struct Value *closure, struct Value *a1, struct Value *a2, struct Value *a3, struct Value *a4, struct Value *a5)
{
    if (closure->tag == NATIVE_VAR_ARG_CLOSURE_VALUE)
    {
        struct Value *(*f)(int, ...) = closure->native_var_arg_closure.native_procedure;
        return f(5, a1, a2, a3, a4, a5);
    }
    else if (closure->tag == NATIVE_VAR_ARG_CLOSURE_POSITION_VALUE)
    {
        struct Value *(*f)(char *, int, int, ...) = closure->native_var_arg_closure_position.native_procedure;
        return f(closure->native_var_arg_closure_position.file_name, closure->native_var_arg_closure_position.line_number, 5, a1, a2, a3, a4, a5);
    }
    else
    {
        _assert_callable_closure(file_name, line_number, closure, 5);

        struct Value *(*f)(struct Value *, struct Value *, struct Value *, struct Value *, struct Value *, struct Value *) = closure->dynamic_closure.procedure;

        return f(closure->dynamic_closure.frame, a1, a2, a3, a4, a5);
    }
}

struct Value *_call_closure_6(char *file_name, int line_number, struct Value *closure, struct Value *a1, struct Value *a2, struct Value *a3, struct Value *a4, struct Value *a5, struct Value *a6)
{
    if (closure->tag == NATIVE_VAR_ARG_CLOSURE_VALUE)
    {
        struct Value *(*f)(int, ...) = closure->native_var_arg_closure.native_procedure;
        return f(6, a1, a2, a3, a4, a5, a6);
    }
    else if (closure->tag == NATIVE_VAR_ARG_CLOSURE_POSITION_VALUE)
    {
        struct Value *(*f)(char *, int, int, ...) = closure->native_var_arg_closure_position.native_procedure;
        return f(closure->native_var_arg_closure_position.file_name, closure->native_var_arg_closure_position.line_number, 6, a1, a2, a3, a4, a5, a6);
    }
    else
    {
        _assert_callable_closure(file_name, line_number, closure, 6);

        struct Value *(*f)(struct Value *, struct Value *, struct Value *, struct Value *, struct Value *, struct Value *, struct Value *) = closure->dynamic_closure.procedure;

        return f(closure->dynamic_closure.frame, a1, a2, a3, a4, a5, a6);
    }
}

struct Value *_call_closure_7(char *file_name, int line_number, struct Value *closure, struct Value *a1, struct Value *a2, struct Value *a3, struct Value *a4, struct Value *a5, struct Value *a6, struct Value *a7)
{
    if (closure->tag == NATIVE_VAR_ARG_CLOSURE_VALUE)
    {
        struct Value *(*f)(int, ...) = closure->native_var_arg_closure.native_procedure;
        return f(7, a1, a2, a3, a4, a5, a6, a7);
    }
    else if (closure->tag == NATIVE_VAR_ARG_CLOSURE_POSITION_VALUE)
    {
        struct Value *(*f)(char *, int, int, ...) = closure->native_var_arg_closure_position.native_procedure;
        return f(closure->native_var_arg_closure_position.file_name, closure->native_var_arg_closure_position.line_number, 7, a1, a2, a3, a4, a5, a6, a7);
    }
    else
    {
        _assert_callable_closure(file_name, line_number, closure, 7);

        struct Value *(*f)(struct Value *, struct Value *, struct Value *, struct Value *, struct Value *, struct Value *, struct Value *, struct Value *) = closure->dynamic_closure.procedure;

        return f(closure->dynamic_closure.frame, a1, a2, a3, a4, a5, a6, a7);
    }
}

struct Value *_call_closure_8(char *file_name, int line_number, struct Value *closure, struct Value *a1, struct Value *a2, struct Value *a3, struct Value *a4, struct Value *a5, struct Value *a6, struct Value *a7, struct Value *a8)
{
    if (closure->tag == NATIVE_VAR_ARG_CLOSURE_VALUE)
    {
        struct Value *(*f)(int, ...) = closure->native_var_arg_closure.native_procedure;
        return f(8, a1, a2, a3, a4, a5, a6, a7, a8);
    }
    else if (closure->tag == NATIVE_VAR_ARG_CLOSURE_POSITION_VALUE)
    {
        struct Value *(*f)(char *, int, int, ...) = closure->native_var_arg_closure_position.native_procedure;
        return f(closure->native_var_arg_closure_position.file_name, closure->native_var_arg_closure_position.line_number, 8, a1, a2, a3, a4, a5, a6, a7, a8);
    }
    else
    {
        _assert_callable_closure(file_name, line_number, closure, 8);

        struct Value *(*f)(struct Value *, struct Value *, struct Value *, struct Value *, struct Value *, struct Value *, struct Value *, struct Value *, struct Value *) = closure->dynamic_closure.procedure;

        return f(closure->dynamic_closure.frame, a1, a2, a3, a4, a5, a6, a7, a8);
    }
}

struct Value *_call_closure_9(char *file_name, int line_number, struct Value *closure, struct Value *a1, struct Value *a2, struct Value *a3, struct Value *a4, struct Value *a5, struct Value *a6, struct Value *a7, struct Value *a8, struct Value *a9)
{
    if (closure->tag == NATIVE_VAR_ARG_CLOSURE_VALUE)
    {
        struct Value *(*f)(int, ...) = closure->native_var_arg_closure.native_procedure;
        return f(9, a1, a2, a3, a4, a5, a6, a7, a8, a9);
    }
    else if (closure->tag == NATIVE_VAR_ARG_CLOSURE_POSITION_VALUE)
    {
        struct Value *(*f)(char *, int, int, ...) = closure->native_var_arg_closure_position.native_procedure;
        return f(closure->native_var_arg_closure_position.file_name, closure->native_var_arg_closure_position.line_number, 9, a1, a2, a3, a4, a5, a6, a7, a8, a9);
    }
    else
    {
        _assert_callable_closure(file_name, line_number, closure, 9);

        struct Value *(*f)(struct Value *, struct Value *, struct Value *, struct Value *, struct Value *, struct Value *, struct Value *, struct Value *, struct Value *, struct Value *) = closure->dynamic_closure.procedure;

        return f(closure->dynamic_closure.frame, a1, a2, a3, a4, a5, a6, a7, a8, a9);
    }
}

struct Value *_call_closure_10(char *file_name, int line_number, struct Value *closure, struct Value *a1, struct Value *a2, struct Value *a3, struct Value *a4, struct Value *a5, struct Value *a6, struct Value *a7, struct Value *a8, struct Value *a9, struct Value *a10)
{
    if (closure->tag == NATIVE_VAR_ARG_CLOSURE_VALUE)
    {
        struct Value *(*f)(int, ...) = closure->native_var_arg_closure.native_procedure;
        return f(10, a1, a2, a3, a4, a5, a6, a7, a8, a9, a10);
    }
    else if (closure->tag == NATIVE_VAR_ARG_CLOSURE_POSITION_VALUE)
    {
        struct Value *(*f)(char *, int, int, ...) = closure->native_var_arg_closure_position.native_procedure;
        return f(closure->native_var_arg_closure_position.file_name, closure->native_var_arg_closure_position.line_number, 10, a1, a2, a3, a4, a5, a6, a7, a8, a9, a10);
    }
    else
    {
        _assert_callable_closure(file_name, line_number, closure, 10);

        struct Value *(*f)(struct Value *, struct Value *, struct Value *, struct Value *, struct Value *, struct Value *, struct Value *, struct Value *, struct Value *, struct Value *, struct Value *) = closure->dynamic_closure.procedure;

        return f(closure->dynamic_closure.frame, a1, a2, a3, a4, a5, a6, a7, a8, a9, a10);
    }
}

struct Value *_mk_pair(struct Value *car, struct Value *cdr)
{
    struct Value *r = (struct Value *)GC_MALLOC(sizeof(struct Value));
    r->tag = PAIR_VALUE;
    r->pair.car = car;
    r->pair.cdr = cdr;
    return r;
}

void _print_newline(void)
{
    printf("\n");
}

struct Value *_plus(struct Value *op1, struct Value *op2)
{
    int v1 = op1->tag == INTEGER_VALUE ? op1->integer : 0;

    if (v1 == 0)
        return op2;
    else
    {
        int v2 = op2->tag == INTEGER_VALUE ? op2->integer : 0;
        if (v2 == 0)
            return op1;
        else
            return _from_literal_int(v1 + v2);
    }
}

struct Value *_minus(struct Value *op1, struct Value *op2)
{
    int v2 = op2->tag == INTEGER_VALUE ? op2->integer : 0;
    if (v2 == 0)
        return op1;
    else
    {
        int v1 = op1->tag == INTEGER_VALUE ? op1->integer : 0;
        return _from_literal_int(v1 - v2);
    }
}

struct Value *_multiply(struct Value *op1, struct Value *op2)
{
    int v1 = op1->tag == INTEGER_VALUE ? op1->integer : 0;

    if (v1 == 0)
        return _from_literal_int(0);
    else
    {
        int v2 = op2->tag == INTEGER_VALUE ? op2->integer : 0;
        if (v2 == 0)
            return _from_literal_int(0);
        else
            return _from_literal_int(v1 * v2);
    }
}

struct Value *_divide(char *file_name, int line_number, struct Value *op1, struct Value *op2)
{
    int v1 = op1->tag == INTEGER_VALUE ? op1->integer : 0;
    int v2 = op2->tag == INTEGER_VALUE ? op2->integer : 0;

    if (v2 == 0)
    {
        _exception_throw(file_name, line_number, _from_literal_string("DivideByZero"));
    }

    return _from_literal_int((int)(v1 / v2));
}

struct Value *_equals(struct Value *op1, struct Value *op2)
{
    if (op1->tag != op2->tag)
        return _VFalse;

    switch (op1->tag)
    {
    case NULL_VALUE:
        return _VTrue;
    case BOOLEAN_VALUE:
        return (op1->boolean == op2->boolean) ? _VTrue : _VFalse;
    case INTEGER_VALUE:
        return (op1->integer == op2->integer) ? _VTrue : _VFalse;
    case STRING_VALUE:
        return (strcmp(op1->string, op2->string) == 0) ? _VTrue : _VFalse;
    case PAIR_VALUE:
        return _equals(op1->pair.car, op2->pair.car) == _VTrue && _equals(op1->pair.cdr, op2->pair.cdr) == _VTrue ? _VTrue : _VFalse;
    default:
        return _VFalse;
    }
}

struct Value *_less_than(struct Value *op1, struct Value *op2)
{
    if (op1->tag != op2->tag)
        return _VFalse;

    switch (op1->tag)
    {
    case BOOLEAN_VALUE:
        return (op1->boolean < op2->boolean) ? _VTrue : _VFalse;
    case INTEGER_VALUE:
        return (op1->integer < op2->integer) ? _VTrue : _VFalse;
    case STRING_VALUE:
        return (strcmp(op1->string, op2->string) < 0) ? _VTrue : _VFalse;
    default:
        return _VFalse;
    }
}

struct Value *_greater_than(struct Value *op1, struct Value *op2)
{
    if (op1->tag != op2->tag)
        return _VFalse;

    switch (op1->tag)
    {
    case BOOLEAN_VALUE:
        return (op1->boolean > op2->boolean) ? _VTrue : _VFalse;
    case INTEGER_VALUE:
        return (op1->integer > op2->integer) ? _VTrue : _VFalse;
    case STRING_VALUE:
        return (strcmp(op1->string, op2->string) > 0) ? _VTrue : _VFalse;
    default:
        return _VFalse;
    }
}

static char *_value_type_name(int tag)
{
    switch (tag)
    {
    case NULL_VALUE:
        return "()";
    case BOOLEAN_VALUE:
        return "boolean";
    case INTEGER_VALUE:
        return "integer";
    case STRING_VALUE:
        return "string";
    case PAIR_VALUE:
        return "pair";
    default:
        return "unknown";
    }
}

struct Value *_pair_car(char *file_name, int line_number, struct Value *pair)
{
    if (pair->tag == PAIR_VALUE)
        return pair->pair.car;

    _exception_throw(file_name, line_number,
                     _mk_pair(
                         _from_literal_string("EmptyList"),
                         _mk_pair(
                             _mk_pair(_from_literal_string("reason"), _from_literal_string("Attempt to call car on empty list")),
                             _VNull)));

    return _VNull;
}

struct Value *_pair_cdr(char *file_name, int line_number, struct Value *pair)
{
    if (pair->tag == PAIR_VALUE)
        return pair->pair.cdr;

    _exception_throw(file_name, line_number,
                     _mk_pair(
                         _from_literal_string("EmptyList"),
                         _mk_pair(
                             _mk_pair(_from_literal_string("reason"), _from_literal_string("Attempt to call cdr on empty list")),
                             _VNull)));

    return _VNull;
}

struct Value *_nullp(struct Value *v)
{
    return v->tag == NULL_VALUE ? _VTrue : _VFalse;
}

struct Value *_booleanp(struct Value *v)
{
    return v->tag == BOOLEAN_VALUE ? _VTrue : _VFalse;
}

struct Value *_integerp(struct Value *v)
{
    return v->tag == INTEGER_VALUE ? _VTrue : _VFalse;
}

struct Value *_stringp(struct Value *v)
{
    return v->tag == STRING_VALUE ? _VTrue : _VFalse;
}

struct Value *_pairp(struct Value *v)
{
    return v->tag == PAIR_VALUE ? _VTrue : _VFalse;
}

void _fail(char *file_name, int line_number, struct Value *msg)
{
    _exception_throw(file_name, line_number,
                     _mk_pair(
                         _from_literal_string("Exit"),
                         _mk_pair(
                             _mk_pair(_from_literal_string("message"), msg),
                             _VNull)));
}

struct Value *_plus_variable(int num, ...)
{
    if (num == 0)
        return _from_literal_int(0);
    else
    {
        va_list arguments;

        va_start(arguments, num);
        struct Value *result = va_arg(arguments, struct Value *);
        for (int i = 1; i < num; i++)
        {
            result = _plus(result, va_arg(arguments, struct Value *));
        }
        va_end(arguments);

        return result;
    }
}

struct Value *_multiply_variable(int num, ...)
{
    if (num == 0)
        return _from_literal_int(1);
    else
    {
        va_list arguments;

        va_start(arguments, num);
        struct Value *result = va_arg(arguments, struct Value *);
        for (int i = 1; i < num; i++)
        {
            result = _multiply(result, va_arg(arguments, struct Value *));
        }
        va_end(arguments);

        return result;
    }
}

struct Value *_minus_variable(int num, ...)
{
    switch (num)
    {
    case 0:
        return _from_literal_int(0);
    case 1:
    {
        va_list arguments;

        struct Value *value;
        va_start(arguments, num);
        struct Value *result = _minus(_from_literal_int(0), va_arg(arguments, struct Value *));
        va_end(arguments);

        return result;
    }
    default:
    {
        va_list arguments;

        va_start(arguments, num);
        struct Value *result = va_arg(arguments, struct Value *);
        for (int i = 1; i < num; i++)
        {
            result = _minus(result, va_arg(arguments, struct Value *));
        }
        va_end(arguments);

        return result;
    }
    }
}

struct Value *_divide_variable(char *file_name, int line_number, int num, ...)
{
    switch (num)
    {
    case 0:
        return _from_literal_int(1);
    case 1:
    {
        va_list arguments;

        struct Value *value;
        va_start(arguments, num);
        struct Value *result = _divide(file_name, line_number, _from_literal_int(1), va_arg(arguments, struct Value *));
        va_end(arguments);

        return result;
    }
    default:
    {
        va_list arguments;

        va_start(arguments, num);
        struct Value *result = va_arg(arguments, struct Value *);
        for (int i = 1; i < num; i++)
        {
            result = _divide(file_name, line_number, result, va_arg(arguments, struct Value *));
        }
        va_end(arguments);

        return result;
    }
    }
}

struct Value *_println(char *file_name, int line_number, int num, ...)
{
    va_list arguments;

    va_start(arguments, num);
    for (int i = 0; i < num; i++)
    {
        _print_value(file_name, line_number, va_arg(arguments, struct Value *));
    }
    va_end(arguments);
    printf("\n");

    return _VNull;
}

struct Value *_print(char *file_name, int line_number, int num, ...)
{
    va_list arguments;

    va_start(arguments, num);
    for (int i = 0; i < num; i++)
    {
        _print_value(file_name, line_number, va_arg(arguments, struct Value *));
    }
    va_end(arguments);

    return _VNull;
}

struct ExceptionTryBlock _exception_try_blocks[100];
int _exception_try_block_idx;

struct Value *_exception_try(char *file_name, int line_number, struct Value *body, struct Value *handler)
{
    _exception_try_block_idx += 1;
    _exception_try_blocks[_exception_try_block_idx].exception = _VNull;
    if (setjmp(_exception_try_blocks[_exception_try_block_idx].jmp))
    {
        struct Value *exception = _exception_try_blocks[_exception_try_block_idx].exception;
        _exception_try_block_idx -= 1;
        return _call_closure_1(file_name, line_number, handler, exception);
    }
    else
    {
        struct Value *result = _call_closure_0(file_name, line_number, body);
        _exception_try_block_idx -= 1;
        return result;
    }
}

void _exception_throw(char *file_name, int line_number, struct Value *exception)
{
    struct Value *exception_value =
        _mk_pair(
            exception,
            _mk_pair(
                _from_literal_string(file_name),
                _mk_pair(
                    _from_literal_int(line_number),
                    _VNull)));

    _exception_try_blocks[_exception_try_block_idx].exception = exception_value;
    longjmp(_exception_try_blocks[_exception_try_block_idx].jmp, 0);
}
