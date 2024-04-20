package plc.project;

import java.io.PrintWriter;
import java.math.BigDecimal;
import java.math.BigInteger;

public final class Generator implements Ast.Visitor<Void> {

    private final PrintWriter writer;
    private int indent = 0;

    public Generator(PrintWriter writer) {
        this.writer = writer;
    }

    private void print(Object... objects) {
        for (Object object : objects) {
            if (object instanceof Ast) {
                visit((Ast) object);
            } else {
                writer.write(object.toString());
            }
        }
    }

    private void newline(int indent) {
        writer.println();
        for (int i = 0; i < indent; i++) {
            writer.write("    ");
        }
    }

    @Override
    public Void visit(Ast.Source ast) {

        print("public class Main {");
        indent++;
        newline(0);

        if ( ast.getGlobals().size() > 0 ) {

            for ( Ast.Global glob : ast.getGlobals() ) {

                newline(indent);
                print(glob);

            }

            newline(0);

        }

        newline(indent);
        print("public static void main(String[] args) {");
        newline(++indent);

        print("System.exit(new Main().main());");
        newline(--indent);
        print("}");
        newline(0);

        if ( ast.getFunctions().size() > 0 ) {

                for ( Ast.Function func : ast.getFunctions() ) {

                    newline(indent);
                    print(func);

                }

        }

        newline(0);
        newline(--indent);

        print("}");

        return null;
    }

    @Override
    public Void visit(Ast.Global ast) {

        String type = Environment.getType(ast.getTypeName()).getJvmName();

        if ( ast.getValue().get() instanceof Ast.Expression.PlcList) {

            Ast.Expression.PlcList vals = (Ast.Expression.PlcList)ast.getValue().get();

            print(type);
            print("[]");
            print(" ");

            print(ast.getName());
            print(" ");
            print("= ");

            print("{");
            for ( int a = 0; a < vals.getValues().size(); a++ ) {

                print(vals.getValues().get(a));

                if ( a == vals.getValues().size() - 1 ) {

                    continue;

                }

                print(", ");

            }
            print("}");

            print(";");

        }
        else {

            if ( !ast.getMutable() ) {

                print("final");
                print(" ");

            }

            print(type);
            print(" ");
            print(ast.getName());

            if ( ast.getValue().isPresent() ) {

                print(" ");
                print("=");
                print(" ");
                print(ast.getValue().get());

            }

            print(";");


        }

        return null;
    }

    @Override
    public Void visit(Ast.Function ast) {

        String type = "";

        if ( ast.getReturnTypeName().isPresent() ) {

            type = Environment.getType(ast.getReturnTypeName().get()).getJvmName();

        }

        print(type);
        print(" ");
        print(ast.getName());
        print("(");

        for ( int a = 0; a < ast.getParameters().size(); a++ ) {

            print(ast.getParameterTypeNames().get(a));
            print(" ");
            print(ast.getParameters().get(a));

            if ( a == ast.getParameters().size() - 1 ) {

                continue;

            }

            print(", ");

        }

        print(") {");

        if ( !ast.getStatements().isEmpty() ) {

            indent++;

            for ( Ast.Statement statement : ast.getStatements() ) {

                newline(indent);
                print(statement);

            }

            newline(--indent);

        }

        print("}");

        return null;
    }

    @Override
    public Void visit(Ast.Statement.Expression ast) {

        print(ast.getExpression());
        print(";");

        return null;
    }

    @Override
    public Void visit(Ast.Statement.Declaration ast) {

        String type = ast.getVariable().getType().getJvmName();

        print(type);
        print(" ");
        print(ast.getName());

        if ( ast.getValue().isPresent() ) {

            print(" ");
            print("=");
            print(" ");
            print(ast.getValue().get());

        }

        print(";");

        return null;
    }

    @Override
    public Void visit(Ast.Statement.Assignment ast) {

        print(ast.getReceiver());
        print(" ");
        print("=");
        print(" ");
        print(ast.getValue());
        print(";");

        return null;
    }

    @Override
    public Void visit(Ast.Statement.If ast) {

        print("if (");
        print(ast.getCondition());
        print(") {");
        indent++;

        for ( Ast.Statement statement : ast.getThenStatements() ) {

            newline(indent);
            print(statement);

        }

        newline(--indent);
        print("}");

        if ( !ast.getElseStatements().isEmpty() ) {

            print(" else");
            print(" ");
            print("{");
            indent++;

            for ( Ast.Statement statement : ast.getElseStatements() ) {

                newline(indent);
                print(statement);

            }

            newline(--indent);
            print("}");

        }

        return null;
    }

    @Override
    public Void visit(Ast.Statement.Switch ast) {

        print("switch (");
        print(ast.getCondition());
        print(") {");

        indent++;
        for ( Ast.Statement.Case cas : ast.getCases() ) {

            newline(indent);
            print(cas);

        }

        newline(--indent);
        print("}");
        return null;
    }

    @Override
    public Void visit(Ast.Statement.Case ast) {

        if ( ast.getValue().isPresent() ) {

            print("case");
            print(" ");
            print(ast.getValue().get());
            print(":");

            indent++;

            for ( Ast.Statement statement : ast.getStatements() ) {

                newline(indent);
                print(statement);

            }

            newline(indent);
            print("break;");
            indent--;

            return null;

        }

        print("default:");

        indent++;

        for ( Ast.Statement statement : ast.getStatements() ) {

            newline(indent);
            print(statement);

        }

        indent--;

        return null;
    }

    @Override
    public Void visit(Ast.Statement.While ast) {

        print("while");
        print(" (");
        print(ast.getCondition());
        print(") {");

        if ( ast.getStatements().isEmpty() ) {

            print("}");
            return null;

        }

        indent++;
        for ( Ast.Statement statement : ast.getStatements() ) {

            newline(indent);
            print(statement);

        }

        newline(--indent);
        print("}");

        return null;
    }

    @Override
    public Void visit(Ast.Statement.Return ast) {

        print("return ");
        print(ast.getValue());
        print(";");

        return null;
    }

    @Override
    public Void visit(Ast.Expression.Literal ast) {

        if ( ast.getType() == Environment.Type.BOOLEAN ) {

            print((Boolean)ast.getLiteral());

        }
        else if ( ast.getType() == Environment.Type.CHARACTER ) {

            print("'");
            print(ast.getLiteral());
            print("'");

        }
        else if ( ast.getType() == Environment.Type.DECIMAL ) {

            print((BigDecimal)ast.getLiteral());


        }
        else if ( ast.getType() == Environment.Type.INTEGER ) {

            print((BigInteger)ast.getLiteral());

        }
        else if ( ast.getType() == Environment.Type.STRING ) {

            print("\"");
            print(ast.getLiteral());
            print("\"");

        }
        else {

            print(ast.getLiteral());

        }

        return null;
    }

    @Override
    public Void visit(Ast.Expression.Group ast) {

        print("(");
        print(ast.getExpression());
        print(")");

        return null;
    }

    @Override
    public Void visit(Ast.Expression.Binary ast) {

        if ( ast.getOperator().equals("^") ) {

            print("Math.pow(");
            print(ast.getLeft());
            print(", ");
            print(ast.getRight());
            print(")");

            return null;

        }

        print(ast.getLeft());
        print(" ");

        print(ast.getOperator());

        print(" ");
        print(ast.getRight());
        return null;
    }

    @Override
    public Void visit(Ast.Expression.Access ast) {

        String name = ast.getVariable().getJvmName();

        if( ast.getOffset().isPresent() ) {

            print(name);
            print("[");
            print(ast.getOffset().get());
            print("]");

            return null;

        }

        print(name);

        return null;
    }

    @Override
    public Void visit(Ast.Expression.Function ast) {

        print(ast.getFunction().getJvmName());
        print("(");

        for ( int a = 0; a < ast.getArguments().size(); a++ ) {

            print(ast.getArguments().get(a));

            if ( a == ast.getArguments().size() - 1 ) {

                continue;

            }

            print(", ");

        }

        print(")");


        return null;
    }

    @Override
    public Void visit(Ast.Expression.PlcList ast) {

        print("{");
        for ( int a = 0; a < ast.getValues().size(); a++ ) {

            print(ast.getValues().get(a));

            if ( a == ast.getValues().size() - 1 ) {

                continue;

            }

            print(", ");

        }
        print("}");

        print(";");

        return null;
    }

}
