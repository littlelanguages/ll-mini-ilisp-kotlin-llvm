/* Library to link into compiled code
 */

#include <stdio.h>
#include <stdlib.h>
#include <string.h>

#include "./lib.h"

struct Value *_VNull;
struct Value *_VTrue;
struct Value *_VFalse;

void _initialise_lib()
{
    _VNull = (struct Value *)malloc(sizeof(struct Value));
    _VNull->tag = NULL_VALUE;

    _VTrue = (struct Value *)malloc(sizeof(struct Value));
    _VTrue->tag = BOOLEAN_VALUE;
    _VTrue->boolean = (1 == 1);

    _VFalse = (struct Value *)malloc(sizeof(struct Value));
    _VFalse->tag = BOOLEAN_VALUE;
    _VFalse->boolean = (1 == 0);
}

void _print_value(struct Value *value)
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
        _print_value(value->pair.car);

        struct Value *runner = value->pair.cdr;

        while (1)
        {
            if (runner->tag == PAIR_VALUE)
            {
                printf(" ");
                _print_value(runner->pair.car);
                runner = runner->pair.cdr;
            }
            else if (runner->tag == NULL_VALUE)
                break;
            else
            {
                printf(" . ");
                _print_value(runner);
                break;
            }
        }
        printf(")");
        break;
    }
    default:
        fprintf(stderr, "Error: _print_value: Unknown Tag: %d\n", value->tag);
        exit(-1);
    }
}

struct Value *_from_literal_int(int v)
{
    struct Value *r = (struct Value *)malloc(sizeof(struct Value));
    r->tag = INTEGER_VALUE;
    r->integer = v;
    return r;
}

struct Value *_from_literal_string(char *s)
{
    struct Value *r = (struct Value *)malloc(sizeof(struct Value));
    r->tag = STRING_VALUE;
    r->string = strdup(s);
    return r;
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

struct Value *_from_native_procedure(void *procedure, int number_arguments)
{
    struct Value *r = (struct Value *)malloc(sizeof(struct Value));
    r->tag = NATIVE_CLOSURE_VALUE;

    switch (number_arguments)
    {
      case 1:
        r->native_closure.procedure = &_wrap_native_1;
        break;
      case 2:
        r->native_closure.procedure = &_wrap_native_2;
        break;
      default:
        fprintf(stderr, "Error: _from_native_procedure: Unable to wrap native with %d arguments\n", number_arguments);
        exit(-1);
    }

    r->native_closure.number_arguments = number_arguments;
    r->native_closure.native_procedure = procedure;

    return r;
}

struct Value *_from_dynamic_procedure(void *procedure, int number_arguments, struct Value *frame)
{
    struct Value *r = (struct Value *)malloc(sizeof(struct Value));
    r->tag = DYNAMIC_CLOSURE_VALUE;
    r->dynamic_closure.procedure = procedure;
    r->dynamic_closure.number_arguments = number_arguments;
    r->dynamic_closure.frame = frame;

    return r;
}

void _assert_callable_closure(struct Value *closure, int number_arguments)
{
    if (closure->tag != NATIVE_CLOSURE_VALUE && closure->tag != DYNAMIC_CLOSURE_VALUE)
    {
        fprintf(stderr, "Error: call closure: Attempt to call value as if a closure: %d\n", closure->tag);
        exit(-1);
    }
    if (closure->native_closure.number_arguments != number_arguments)
    {
        fprintf(stderr, "Error: call closure: Expected %d arguments but received %d\n", number_arguments, closure->native_closure.number_arguments);
        exit(-1);
    }
}

struct Value *_mk_frame(struct Value *parent, int size)
{
    struct Value *frame = (struct Value *)malloc(sizeof(struct Value));
    frame->tag = VECTOR_VALUE;
    frame->vector.length = 1;
    frame->vector.items = (struct Value **)malloc(sizeof(struct Value *) * (1 + size));
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

struct Value *_call_closure_0(struct Value *closure)
{
    _assert_callable_closure(closure, 0);

    struct Value *(*f)(struct Value *) = closure->dynamic_closure.procedure;

    return f(closure->dynamic_closure.frame);
}

struct Value *_call_closure_1(struct Value *closure, struct Value *a1)
{
    _assert_callable_closure(closure, 1);

    struct Value *(*f)(struct Value *, struct Value *) = closure->dynamic_closure.procedure;

    return f(closure->dynamic_closure.frame, a1);
}

struct Value *_call_closure_2(struct Value *closure, struct Value *a1, struct Value *a2)
{
    _assert_callable_closure(closure, 2);

//    printf("_call_closure_2:\n");
//    printf("  v1: ");
//    _print_value(a1);
//    printf("\n");
//    printf("  v2: ");
//    _print_value(a2);
//    printf("\n");

    struct Value *(*f)(struct Value *, struct Value *, struct Value *) = closure->dynamic_closure.procedure;

//    struct Value *result = f(closure->dynamic_closure.frame, a1, a2);

    return f(closure->dynamic_closure.frame, a1, a2);

//    printf("  result: ");
//    _print_value(result);
//    printf("\n");
//
//    return result;
}

struct Value *_mk_pair(struct Value *car, struct Value *cdr)
{
    struct Value *r = (struct Value *)malloc(sizeof(struct Value));
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

struct Value *_divide(struct Value *op1, struct Value *op2)
{
    int v1 = op1->tag == INTEGER_VALUE ? op1->integer : 0;
    int v2 = op2->tag == INTEGER_VALUE ? op2->integer : 0;

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

struct Value *_pair_car(struct Value *pair)
{
    if (pair->tag == PAIR_VALUE)
        return pair->pair.car;

    fprintf(stderr, "Error: car: Unable to get car of %s value.\n", _value_type_name(pair->tag));
    exit(-1);
}

struct Value *_pair_cdr(struct Value *pair)
{
    if (pair->tag == PAIR_VALUE)
        return pair->pair.cdr;

    fprintf(stderr, "Error: cdr: Unable to get cdr of %s value.\n", _value_type_name(pair->tag));
    exit(-1);
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

void _assert_eq(struct Value *msg, struct Value *v1, struct Value *v2)
{
    if (_equals(v1, v2) != _VTrue)
    {
        printf("x = %s\n", msg->string);
        exit(1);
    }
    else
    {
        printf("- = %s\n", msg->string);
    }
}

void _assert_neq(struct Value *msg, struct Value *v1, struct Value *v2)
{
    if (_equals(v1, v2) != _VFalse)
    {
        printf("x = %s\n", msg->string);
        exit(1);
    }
    else
    {
        printf("- = %s\n", msg->string);
    }
}

void _assert_true(struct Value *msg, struct Value *v)
{
    if (_equals(v, _VTrue) != _VTrue)
    {
        printf("x = %s\n", msg->string);
        exit(1);
    }
    else
    {
        printf("- = %s\n", msg->string);
    }
}

void _assert_false(struct Value *msg, struct Value *v)
{
    if (_equals(v, _VFalse) != _VTrue)
    {
        printf("x = %s\n", msg->string);
        exit(1);
    }
    else
    {
        printf("- = %s\n", msg->string);
    }
}

void _fail(struct Value *msg)
{
    printf("x = %s\n", msg->string);
    exit(1);
}
