package plc.project;

import java.math.BigDecimal;
import java.math.BigInteger;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Optional;
import java.util.stream.Collectors;

/**
 * See the specification for information about what the different visit
 * methods should do.
 */
public final class Analyzer implements Ast.Visitor<Void> {

    public Scope scope;
    private Ast.Function function;
    private Environment.Type ret;

    public Analyzer(Scope parent) {
        scope = new Scope(parent);
        scope.defineFunction("print", "System.out.println", Arrays.asList(Environment.Type.ANY), Environment.Type.NIL, args -> Environment.NIL);
    }

    public Scope getScope() {
        return scope;
    }

    @Override
    public Void visit(Ast.Source ast) {

        Environment.Function func = scope.lookupFunction("main", 0);

        if ( func != null && func.getReturnType() == Environment.Type.INTEGER && func.getArity() == 0 ) {

            for ( Ast.Global global : ast.getGlobals() ) {

                visit(global);

            }

            for ( Ast.Function function : ast.getFunctions() ) {

                visit(function);

            }

        }
        else {

            throw new RuntimeException("No/Invalid Main function");

        }

        return null;

    }

    @Override
    public Void visit(Ast.Global ast) {

        if ( ast.getValue().isPresent() ) {

            visit(ast.getValue().get());

            requireAssignable(Environment.getType(ast.getTypeName()), ast.getValue().get().getType());
            String name = ast.getName();
            Environment.Type tyoe = Environment.getType(ast.getTypeName());


            scope.defineVariable(name, name, tyoe,true,Environment.NIL);

        }
        else {

            scope.defineVariable(ast.getName(), ast.getName(), Environment.getType(ast.getTypeName()), true, Environment.NIL);

        }

        ast.setVariable(scope.lookupVariable(ast.getName()));
        return null;

    }

    @Override
    public Void visit(Ast.Function ast) {

        if ( ast.getReturnTypeName().isPresent() ) {

            ret = Environment.getType(ast.getReturnTypeName().get());

        }
        else {

            ret = Environment.Type.NIL;

        }

        List<Environment.Type> pTypes = new ArrayList<>();
        for ( int a = 0; a < pTypes.size(); a++ ) {

            pTypes.add(Environment.getType(ast.getParameterTypeNames().get(a)));

        }

        ast.setFunction(scope.defineFunction(ast.getName(), ast.getName(), pTypes, ret, args -> Environment.NIL));

        try {

            scope = new Scope(scope);

            List<String> params = ast.getParameters();

            for ( int a = 0; a < params.size(); a++ ) {

                scope.defineVariable(params.get(a), params.get(a), pTypes.get(a),true, Environment.NIL);

            }

            if ( !ast.getStatements().isEmpty() ) {

                for ( Ast.Statement statement : ast.getStatements() ) {

                    try {

                        scope = new Scope(scope);
                        visit(statement);

                    }
                    finally {

                        scope = scope.getParent();
                    }

                }

            }

            if ( !ast.getParameters().isEmpty() ) {

                for ( int a = 0; a < ast.getParameters().size(); a++ ) {

                    scope.defineVariable(ast.getParameters().get(a), ast.getParameters().get(a), pTypes.get(a), true, Environment.NIL);

                }

            }

        }
        finally {

            scope = scope.getParent();

        }

        return null;

    }

    @Override
    public Void visit(Ast.Statement.Expression ast) {

        if ( ast.getExpression() instanceof Ast.Expression.Function ) {

            visit(ast.getExpression());

        }
        else {

            throw new RuntimeException();

        }

        return null;

    }

    @Override
    public Void visit(Ast.Statement.Declaration ast) {

        if ( ast.getValue().isPresent() ) {

            try {

                visit(ast.getValue().get());

                Environment.Type type = Environment.Type.NIL;
                if ( ast.getTypeName().isPresent() ) {

                    type = Environment.getType(ast.getTypeName().get());

                }
                else {

                    type = ast.getValue().get().getType();

                }

                requireAssignable(type, ast.getValue().get().getType());
                ast.setVariable(scope.defineVariable(ast.getName(), ast.getName(), type,true, Environment.NIL));

            }
            catch (RuntimeException r) {

                throw r;

            }

        }
        else {


            ast.setVariable(scope.defineVariable(ast.getName(), ast.getName(), Environment.getType(ast.getTypeName().get()), true, Environment.NIL));

        }

        return null;

    }

    @Override
    public Void visit(Ast.Statement.Assignment ast) {
         

        if ( ast.getReceiver() instanceof Ast.Expression.Access ) {

            visit(ast.getReceiver());
            visit(ast.getValue());
            requireAssignable(ast.getReceiver().getType(), ast.getValue().getType());

        }
        else {

            throw new RuntimeException();

        }

        return null;

    }

    @Override
    public Void visit(Ast.Statement.If ast) {
        
        visit(ast.getCondition());
        requireAssignable(Environment.Type.BOOLEAN, ast.getCondition().getType());

        if ( ast.getCondition().getType() != Environment.Type.BOOLEAN ) {

            throw new RuntimeException();

        }
        else if ( ast.getThenStatements().size() <= 0 ) {

            throw new RuntimeException();

        }
        else {

            for ( Ast.Statement statement : ast.getThenStatements() ) {

                try {

                    scope = new Scope(scope);
                    visit(statement);

                }
                finally {

                    scope = scope.getParent();

                }

            }

            for ( Ast.Statement statement : ast.getElseStatements() ) {

                try {

                    scope = new Scope(scope);
                    visit(statement);

                }
                finally {

                    scope = scope.getParent();

                }

            }


        }

        return null;

    }

    @Override
    public Void visit(Ast.Statement.Switch ast) {
            
        visit(ast.getCondition());

        Environment.Type conditionType = ast.getCondition().getType();
        List<Ast.Statement.Case> cases = ast.getCases();

        if ( !ast.getCases().getLast().getValue().isEmpty() ) {

            throw new RuntimeException();

        }

        for ( int a = 0; a < cases.size(); a++ ) {

            try {

                scope = new Scope(scope);

                visit(cases.get(a));

                if ( cases.get(a) != ast.getCases().getLast() ) {

                    requireAssignable(conditionType, cases.get(a).getValue().get().getType());

                }


            }
            finally {

                scope = scope.getParent();

            }



        }

        return null;

    }

    @Override
    public Void visit(Ast.Statement.Case ast) {
            

        try {

            scope = new Scope(scope);

            if ( ast.getValue().isPresent() ) {

                visit(ast.getValue().get());

            }

            for ( Ast.Statement statement : ast.getStatements()) {

                visit(statement);

            }

        }
        finally {

            scope = scope.getParent();

        }

        return null;

    }

    @Override
    public Void visit(Ast.Statement.While ast) {
            

        visit(ast.getCondition());
        requireAssignable(Environment.Type.BOOLEAN, ast.getCondition().getType());

        try {

            scope = new Scope(scope);

            for ( Ast.Statement statement : ast.getStatements() ) {

                visit(statement);

            }

        }
        finally {

            scope = scope.getParent();

        }

        return null;

    }

    @Override
    public Void visit(Ast.Statement.Return ast) {
            

        try {

            visit(ast.getValue());
            requireAssignable(ret, ast.getValue().getType());

        }
        catch (RuntimeException r) {

            throw r;

        }

        return null;

    }

    @Override
    public Void visit(Ast.Expression.Literal ast) {
            

        if ( ast.getLiteral() == null ) {

            ast.setType(Environment.Type.NIL);

        }
        else if ( ast.getLiteral() instanceof Boolean ) {

            ast.setType(Environment.Type.BOOLEAN);

        }
        else if ( ast.getLiteral() instanceof Character ) {

            ast.setType(Environment.Type.CHARACTER);

        }
        else if ( ast.getLiteral() instanceof String ) {

            ast.setType(Environment.Type.STRING);

        }
        else if ( ast.getLiteral() instanceof BigInteger ) {

            BigInteger BI = (BigInteger) ast.getLiteral();
            BigInteger max = BigInteger.valueOf(Integer.MAX_VALUE);
            BigInteger min = BigInteger.valueOf(Integer.MIN_VALUE);

            if ( BI.compareTo(max) > 0 || BI.compareTo(min) < 0 ) {

                throw new RuntimeException();

            }
            else {

                ast.setType(Environment.Type.INTEGER);

            }

        }
        else if ( ast.getLiteral() instanceof BigDecimal ) {

            BigDecimal BD = (BigDecimal) ast.getLiteral();
            Double doub = BD.doubleValue();
            Double max = Double.MAX_VALUE;
            Double min = Double.MIN_VALUE;

            if (doub > max || doub < min ) {

                throw new RuntimeException();

            }
            else {

                ast.setType(Environment.Type.DECIMAL);

            }

        }

        return null;
    }

    @Override
    public Void visit(Ast.Expression.Group ast) {
            

        if ( ast.getExpression() instanceof Ast.Expression.Binary ) {

            visit(ast.getExpression());
            ast.setType(ast.getExpression().getType());

        }
        else {

            throw new RuntimeException();

        }

        return null;
    }

    @Override
    public Void visit(Ast.Expression.Binary ast) {
            

        String operator = ast.getOperator();

        visit(ast.getLeft());
        visit(ast.getRight());

        Ast.Expression LHS = ast.getLeft();
        Ast.Expression RHS = ast.getRight();

        if ( operator.equals("&&") || operator.equals("||") ) {

            requireAssignable(Environment.Type.BOOLEAN, LHS.getType());
            requireAssignable(Environment.Type.BOOLEAN, RHS.getType());
            ast.setType(Environment.Type.BOOLEAN);

        }
        else if ( operator.equals(">") || operator.equals(">=") || operator.equals("<") || operator.equals("<=") || operator.equals("!=") || operator.equals("==")) {

            requireAssignable(Environment.Type.COMPARABLE, LHS.getType());
            requireAssignable(Environment.Type.COMPARABLE, RHS.getType());
            requireAssignable(LHS.getType(), RHS.getType());

            ast.setType(Environment.Type.BOOLEAN);

        }
        else if ( operator.equals("+") ) {

            if ( LHS.getType() == Environment.Type.STRING || RHS.getType() == Environment.Type.STRING) {

                ast.setType(Environment.Type.STRING);

            }
            else if (LHS.getType() == Environment.Type.INTEGER && RHS.getType() == Environment.Type.INTEGER) {

                ast.setType(Environment.Type.INTEGER);

            }
            else if ( LHS.getType() == Environment.Type.DECIMAL && RHS.getType() == Environment.Type.DECIMAL ) {

                ast.setType(Environment.Type.DECIMAL);

            }
            else {

                throw new RuntimeException();

            }

        }
        else if ( operator.equals("-") || operator.equals("*") || operator.equals("/")) {

            if (LHS.getType() == Environment.Type.INTEGER && RHS.getType() == Environment.Type.INTEGER) {

                ast.setType(Environment.Type.INTEGER);

            }
            else if ( LHS.getType() == Environment.Type.DECIMAL && RHS.getType() == Environment.Type.DECIMAL ) {

                ast.setType(Environment.Type.DECIMAL);

            }
            else {

                throw new RuntimeException();

            }

        }
        else if ( operator.equals("^") ) {

            if (LHS.getType() == Environment.Type.INTEGER && RHS.getType() == Environment.Type.INTEGER) {

                ast.setType(Environment.Type.INTEGER);

            }
            else {

                throw new RuntimeException();

            }

        }
        else {

            throw new RuntimeException();

        }

        return null;
    }

    @Override
    public Void visit(Ast.Expression.Access ast) {
            

        if ( ast.getOffset().isPresent() ) {

            Ast.Expression expression = ast.getOffset().get();
            visit(expression);
            ast.setVariable(expression.getType().getGlobal(ast.getName()));

        }
        else {

            ast.setVariable(scope.lookupVariable(ast.getName()));

        }

        return null;
    }

    @Override
    public Void visit(Ast.Expression.Function ast) {
            

        List<Environment.Type> parameters = scope.lookupFunction(ast.getName(), ast.getArguments().size()).getParameterTypes();

        for ( int a = 0; a < ast.getArguments().size(); a++ ) {

            visit(ast.getArguments().get(a));
            requireAssignable(parameters.get(a), ast.getArguments().get(a).getType());

        }

        ast.setFunction(scope.lookupFunction(ast.getName(), ast.getArguments().size()));

        return null;
    }

    @Override
    public Void visit(Ast.Expression.PlcList ast) {

        for ( Ast.Expression expr : ast.getValues() ) {

            requireAssignable(ast.getType(), expr.getType());

        }

        return null;
    }

    public static void requireAssignable(Environment.Type target, Environment.Type type) {
            

        if ( target == type ) {

            return;

        }
        if ( target == Environment.Type.ANY ) {

            return;

        }
        if ( target == Environment.Type.COMPARABLE ) {

            return;

        }

        throw new RuntimeException();

    }

}
