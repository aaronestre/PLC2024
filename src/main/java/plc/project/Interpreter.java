package plc.project;

import java.math.BigDecimal;
import java.math.BigInteger;
import java.math.RoundingMode;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.stream.Collectors;

public class Interpreter implements Ast.Visitor<Environment.PlcObject> {

    private Scope scope = new Scope(null);

    public Interpreter(Scope parent) {
        scope = new Scope(parent);
        scope.defineFunction("print", 1, args -> {
            System.out.println(args.get(0).getValue());
            return Environment.NIL;
        });
    }

    public Scope getScope() {
        return scope;
    }

    @Override
    public Environment.PlcObject visit(Ast.Source ast) {

        List<Environment.PlcObject> args = new ArrayList<>();

        for ( Ast.Global global : ast.getGlobals() ) {

            visit(global);

        }

        for ( Ast.Function function : ast.getFunctions() ) {

            visit(function);

        }

        return scope.lookupFunction("main", 0).invoke(args);

    }

    @Override
    public Environment.PlcObject visit(Ast.Global ast) {

        Environment.PlcObject global = Environment.NIL;
        boolean isMutable = ast.getMutable();

        if ( ast.getValue().isPresent() ) {

            global = visit(ast.getValue().get());

        }
        scope.defineVariable(ast.getName(), isMutable, global);
        return Environment.NIL;

    }

    @Override
    public Environment.PlcObject visit(Ast.Function ast) {

        String name = ast.getName();
        int arity = ast.getParameters().size();

        scope.defineFunction(name, arity, args -> {

            try {

                scope = new Scope(scope);

                for ( int a = 0; a < arity; a++ ) {

                    scope.defineVariable(ast.getParameters().get(a), true, args.get(a));

                }

                for ( Ast.Statement statement : ast.getStatements() ) {

                    visit(statement);

                }

            }
            catch ( Return retrun ) {

                return retrun.value;

            }
            finally {

                scope = scope.getParent();

            }
            return Environment.NIL;

        });

        return Environment.NIL;


    }

    @Override
    public Environment.PlcObject visit(Ast.Statement.Expression ast) {

        visit(ast.getExpression());
        return Environment.NIL;

    }

    @Override
    public Environment.PlcObject visit(Ast.Statement.Declaration ast) {

        Optional optional = ast.getValue();
        Optional<Ast.Expression> op1 = ast.getValue();
        Boolean present = optional.isPresent();

        if ( present ) {

            Object obj = optional.get();

            Ast.Expression expr = (Ast.Expression)optional.get();
            Ast.Expression expr1 = (Ast.Expression)op1.get();

            scope.defineVariable(ast.getName(), true, visit(expr));

        }
        else {

            scope.defineVariable(ast.getName(), true, Environment.NIL);

        }

        return Environment.NIL;

    }

    @Override
    public Environment.PlcObject visit(Ast.Statement.Assignment ast) {

        if ( ast.getReceiver() instanceof Ast.Expression.Access ) {

            Ast.Expression.Access access = (Ast.Expression.Access)ast.getReceiver();

            if ( access.getOffset().isPresent() ) {

                Ast.Expression.Literal value = (Ast.Expression.Literal)ast.getValue();

                Environment.PlcObject lObj = visit(access);
                Environment.PlcObject offset = visit(access.getOffset().get());

                List<Object> _list = requireType(List.class, scope.lookupVariable(access.getName()).getValue());
                BigInteger off = (BigInteger) offset.getValue();

                _list.set(off.intValue(), value.getLiteral());
                Environment.PlcObject updated = Environment.create(_list);

                scope.lookupVariable(access.getName()).setValue(updated);

            }
            else {

                if ( scope.lookupVariable(access.getName()).getMutable() ) {

                    scope.lookupVariable(access.getName()).setValue(visit(ast.getValue()));

                }
                else {

                    throw new RuntimeException("Immutable Variable");

                }


            }

        }
        else {

            throw new RuntimeException("Not Access Type");

        }

        return Environment.NIL;

    }

    @Override
    public Environment.PlcObject visit(Ast.Statement.If ast) {

        if ( requireType(Boolean.class, visit(ast.getCondition())) ) {

            try {

                scope = new Scope(scope);

                for ( Ast.Statement statement : ast.getThenStatements() ) {

                    visit(statement);

                }



            }
            finally {

                scope = scope.getParent();

            }

        }
        else if ( !requireType(Boolean.class, visit(ast.getCondition())) ) {

            try {

                scope = new Scope(scope);

                for ( Ast.Statement statement : ast.getElseStatements() ) {

                    visit(statement);

                }

            }
            finally {

                scope = scope.getParent();

            }

        }
        else {

            throw new RuntimeException("No boolean expression");

        }

        return Environment.NIL;

    }

    @Override
    public Environment.PlcObject visit(Ast.Statement.Switch ast) {

        for ( Ast.Statement.Case _case : ast.getCases() ) {

            if ( _case.getValue().equals(ast.getCondition()) ) {

                visit(_case);
                return Environment.NIL;

            }

        }

        return Environment.NIL;

    }

    @Override
    public Environment.PlcObject visit(Ast.Statement.Case ast) {

        for ( Ast.Statement statement : ast.getStatements() ) {

            visit(statement);

        }

        return Environment.NIL;

    }

    @Override
    public Environment.PlcObject visit(Ast.Statement.While ast) {

        if ( requireType(Boolean.class, visit(ast.getCondition()))) {

            while ( requireType(Boolean.class, visit(ast.getCondition())) ) {

                try {

                    scope = new Scope(scope);

                    ast.getStatements().forEach(this::visit);

                }
                finally {

                    scope = scope.getParent();

                }

            }

        }
        else {

            throw new RuntimeException("No boolean expression");

        }

        return Environment.NIL;

    }

    @Override
    public Environment.PlcObject visit(Ast.Statement.Return ast) {

        throw new Return(visit(ast.getValue()));

    }

    @Override
    public Environment.PlcObject visit(Ast.Expression.Literal ast) {

        if ( ast.getLiteral() != null ) {

            return Environment.create(ast.getLiteral());

        }
        else {

            return Environment.NIL;

        }
    }

    @Override
    public Environment.PlcObject visit(Ast.Expression.Group ast) {

        return visit(ast.getExpression());

    }

    @Override
    public Environment.PlcObject visit(Ast.Expression.Binary ast) {

        String operator = ast.getOperator();
        Environment.PlcObject LHS = visit(ast.getLeft());

        if ( operator.equals("+") ) {

            Environment.PlcObject RHS = visit(ast.getRight());

            if ( LHS.getValue() instanceof String || RHS.getValue() instanceof String ) {

                return Environment.create(LHS.getValue().toString() + RHS.getValue().toString());

            }
            else if ( LHS.getValue() instanceof BigInteger && RHS.getValue() instanceof BigInteger ) {

                return Environment.create(((BigInteger) LHS.getValue()).add((BigInteger) RHS.getValue()));

            }
            else if ( LHS.getValue() instanceof BigDecimal && RHS.getValue() instanceof BigDecimal ) {

                return Environment.create(((BigDecimal) LHS.getValue()).add((BigDecimal) RHS.getValue()));

            }

        }
        else if ( operator.equals("-") ) {

            Environment.PlcObject RHS = visit(ast.getRight());

            if ( LHS.getValue() instanceof BigInteger && RHS.getValue() instanceof BigInteger ) {

                return Environment.create(BigInteger.class.cast(LHS.getValue()).subtract(BigInteger.class.cast(RHS.getValue())));

            }
            else if ( LHS.getValue() instanceof BigDecimal && RHS.getValue() instanceof BigDecimal ) {

                return Environment.create(BigDecimal.class.cast(LHS.getValue()).subtract(BigDecimal.class.cast(RHS.getValue())));

            }

        }
        else if ( operator.equals("*") ) {

            Environment.PlcObject RHS = visit(ast.getRight());

            if ( LHS.getValue() instanceof BigInteger && RHS.getValue() instanceof BigInteger ) {

                return Environment.create(BigInteger.class.cast(LHS.getValue()).multiply(BigInteger.class.cast(RHS.getValue())));

            }
            else if ( LHS.getValue() instanceof BigDecimal && RHS.getValue() instanceof BigDecimal ) {

                return Environment.create(BigDecimal.class.cast(LHS.getValue()).multiply(BigDecimal.class.cast(RHS.getValue())));

            }

        }
        else if ( operator.equals("/") ) {

            Environment.PlcObject RHS = visit(ast.getRight());

            if ( LHS.getValue() instanceof BigInteger && RHS.getValue() instanceof BigInteger ) {

                return Environment.create(BigInteger.class.cast(LHS.getValue()).divide(BigInteger.class.cast(RHS.getValue())));

            }
            else if ( LHS.getValue() instanceof BigDecimal && RHS.getValue() instanceof BigDecimal ) {

                return Environment.create(BigDecimal.class.cast(LHS.getValue()).divide(BigDecimal.class.cast(RHS.getValue()), RoundingMode.HALF_EVEN));

            }

        }
        else if ( operator.equals("^") ) {

            Environment.PlcObject RHS = visit(ast.getRight());

            if ( LHS.getValue() instanceof BigInteger && RHS.getValue() instanceof BigInteger ) {

                BigInteger result = BigInteger.class.cast(LHS.getValue());

                for ( int a = 0; a < ((BigInteger) RHS.getValue()).intValue(); a++ ) {

                    result.multiply(result);

                }

                return Environment.create(result);

            }

        }
        else if ( operator.equals("&&") ) {

            Environment.PlcObject RHS = visit(ast.getRight());

            if ( requireType(Boolean.class, LHS ) == false ) {

                return Environment.create(Boolean.FALSE);

            }

            if ( requireType(Boolean.class, LHS) == requireType(Boolean.class, RHS) ) {

                return LHS;

            }
            else {

                return Environment.create(Boolean.FALSE);

            }

        }
        else if ( operator.equals("||") ) {


            if ( requireType(Boolean.class, LHS) == Boolean.TRUE ) {

                return LHS;

            }
            else if ( requireType(Boolean.class, visit(ast.getRight())) == Boolean.TRUE) {

                return visit(ast.getRight());

            }
            else {

                return Environment.create(Boolean.FALSE);

            }

        }
        else if ( operator.equals("<") ) {

            Environment.PlcObject RHS = visit(ast.getRight());

            if ( LHS.getValue() instanceof Comparable<?> && RHS.getValue() instanceof Comparable<?>) {

                Comparable<Object> LC = (Comparable<Object>)LHS.getValue();
                Comparable<Object> RC = (Comparable<Object>)RHS.getValue();

                if ( LC.compareTo(RC) < 0 ) {

                    return Environment.create(Boolean.TRUE);

                }
                else {

                    return Environment.create(Boolean.FALSE);

                }

            }

        }
        else if ( operator.equals("<=") ) {

            Environment.PlcObject RHS = visit(ast.getRight());

            if ( LHS.getValue() instanceof Comparable<?> && RHS.getValue() instanceof Comparable<?>) {

                Comparable<Object> LC = (Comparable<Object>)LHS.getValue();
                Comparable<Object> RC = (Comparable<Object>)RHS.getValue();

                if ( LC.compareTo(RC) <= 0 ) {

                    return Environment.create(Boolean.TRUE);

                }
                else {

                    return Environment.create(Boolean.FALSE);

                }

            }

        }
        else if ( operator.equals(">") ) {

            Environment.PlcObject RHS = visit(ast.getRight());

            if ( LHS.getValue() instanceof Comparable<?> && RHS.getValue() instanceof Comparable<?>) {

                Comparable<Object> LC = (Comparable<Object>)LHS.getValue();
                Comparable<Object> RC = (Comparable<Object>)RHS.getValue();

                if ( LC.compareTo(RC) > 0 ) {

                    return Environment.create(Boolean.TRUE);

                }
                else {

                    return Environment.create(Boolean.FALSE);

                }

            }

        }
        else if ( operator.equals(">=") ) {

            Environment.PlcObject RHS = visit(ast.getRight());

            if ( LHS.getValue() instanceof Comparable<?> && RHS.getValue() instanceof Comparable<?>) {

                Comparable<Object> LC = (Comparable<Object>)LHS.getValue();
                Comparable<Object> RC = (Comparable<Object>)RHS.getValue();

                if ( LC.compareTo(RC) >= 0 ) {

                    return Environment.create(Boolean.TRUE);

                }
                else {

                    return Environment.create(Boolean.FALSE);

                }

            }

        }
        else if ( operator.equals("==") ) {

            Environment.PlcObject RHS = visit(ast.getRight());

            if ( LHS.getValue().equals(RHS.getValue()) ) {

                return Environment.create(Boolean.TRUE);

            }
            else {

                return Environment.create(Boolean.FALSE);

            }

        }
        else if ( operator.equals("!=") ) {

            Environment.PlcObject RHS = visit(ast.getRight());

            if ( LHS.getValue().equals(RHS.getValue()) ) {

                return Environment.create(Boolean.FALSE);

            }
            else {

                return Environment.create(Boolean.TRUE);

            }

        }

        throw new RuntimeException("Wrong types");

    }

    @Override
    public Environment.PlcObject visit(Ast.Expression.Access ast) {

        if ( ast.getOffset().isPresent() ) {

            Environment.PlcObject offset = visit(ast.getOffset().get());
            List<?> _list = requireType(List.class, scope.lookupVariable(ast.getName()).getValue());

            if ( offset.getValue() instanceof BigInteger ) {

                int index = ((BigInteger) offset.getValue()).intValue();

                if ( index >= 0 && index < _list.size() ) {

                    Environment.PlcObject ret = Environment.create(_list.get(index));
                    return ret;

                }

            }
            else {

                throw new RuntimeException("Not BigInteger offset");

            }

        }

        return scope.lookupVariable(ast.getName()).getValue();

    }

    @Override
    public Environment.PlcObject visit(Ast.Expression.Function ast) {

        List<Environment.PlcObject> args = new ArrayList<>();

        for ( Ast.Expression arg : ast.getArguments() ) {

            args.add(visit(arg));

        }

        Environment.Function func = scope.lookupFunction(ast.getName(), ast.getArguments().size());
        return func.invoke(args);

    }

    @Override
    public Environment.PlcObject visit(Ast.Expression.PlcList ast) {


        List<Object> values = new ArrayList<>();
        for ( int a = 0; a < ast.getValues().size(); a++ ) {

            Ast.Expression.Literal val = (Ast.Expression.Literal) ast.getValues().get(a);
            values.add(val.getLiteral());

        }

        return Environment.create(values);

    }

    /**
     * Helper function to ensure an object is of the appropriate type.
     */
    private static <T> T requireType(Class<T> type, Environment.PlcObject object) {
        if (type.isInstance(object.getValue())) {
            return type.cast(object.getValue());
        } else {
            throw new RuntimeException("Expected type " + type.getName() + ", received " + object.getValue().getClass().getName() + ".");
        }
    }

    /**
     * Exception class for returning values.
     */
    public static class Return extends RuntimeException {

        private final Environment.PlcObject value;

        private Return(Environment.PlcObject value) {
            this.value = value;
        }

    }

}
