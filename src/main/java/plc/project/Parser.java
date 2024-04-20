package plc.project;

import javax.swing.text.html.Option;
import java.util.List;
import java.util.ArrayList;
import java.math.BigDecimal;
import java.math.BigInteger;
import java.util.Optional;

/**
 * The parser takes the sequence of tokens emitted by the lexer and turns that
 * into a structured representation of the program, called the Abstract Syntax
 * Tree (AST).
 *
 * The parser has a similar architecture to the lexer, just with {@link Token}s
 * instead of characters. As before, {@link #peek(Object...)} and {@link
 * #match(Object...)} are helpers to make the implementation easier.
 *
 * This type of parser is called <em>recursive descent</em>. Each rule in our
 * grammar will have it's own function, and reference to other rules correspond
 * to calling that functions.
 */
public final class Parser {

    private final TokenStream tokens;

    public Parser(List<Token> tokens) {
        this.tokens = new TokenStream(tokens);
    }

    /**
     * Parses the {@code source} rule.
     */
    public Ast.Source parseSource() throws ParseException {

        List<Ast.Global> globals = new ArrayList<>();
        List<Ast.Function> functions = new ArrayList<>();

        try {

            if ( tokens.has(0) ) {

                while ( tokens.has(0) ) {

                    if  ( peek("LIST") || peek("VAR") || peek("VAL") ) {

                        globals.add(parseGlobal());

                    }
                    else if ( peek("FUN") ) {

                        functions.add(parseFunction());

                    }


                }

            }

            if ( !tokens.has(0) ) {

                return new Ast.Source(globals, functions);

            }
            else {

                throw new ParseException("Invalid ID", tokens.get(0).getIndex());

            }



        }
        catch ( ParseException p ) {

            throw new ParseException(p.getMessage(), p.getIndex());

        }

    }

    /**
     * Parses the {@code global} rule. This method should only be called if the
     * next tokens start a global, aka {@code LIST|VAL|VAR}.
     */
    public Ast.Global parseGlobal() throws ParseException {

        Ast.Global global;

        try {

            if ( peek("LIST") ) {

                global = parseList();

            }
            else if ( peek("VAR") ) {

                global = parseMutable();

            }
            else {

                global = parseImmutable();

            }

            return global;

        }
        catch ( ParseException p ) {

            throw new ParseException(p.getMessage(), p.getIndex());

        }
    }

    /**
     * Parses the {@code list} rule. This method should only be called if the
     * next token declares a list, aka {@code LIST}.
     */
    public Ast.Global parseList() throws ParseException {

        try {

            String name = "";
            String type = "";
            boolean mutable;

            match("LIST");
            mutable = true;

            if ( peek(Token.Type.IDENTIFIER) ) {

                name = tokens.get(0).getLiteral();
                match(Token.Type.IDENTIFIER);

            }

            if ( peek(":") ) {

                match(":");

            }

            if ( peek(Token.Type.IDENTIFIER) ) {

                type = tokens.get(0).getLiteral();
                match(Token.Type.IDENTIFIER);

            }

            match("=");

            if ( peek("[") ) {

                match("[");

                while ( !peek("]") ) {

                    parseExpression();
                    match(",");

                }

                if ( peek("]") ) {

                    match("]");

                }
                else {

                    throw new ParseException("Missing closing bracket", tokens.get(0).getIndex());

                }

            }


            if ( peek(";") ) {

                match(";");

            }
            else {

                throw new ParseException("Missing semicolon", tokens.get(0).getIndex());

            }

            return new Ast.Global(name, type, mutable, Optional.empty());

        }
        catch (ParseException p) {

            throw new ParseException(p.getMessage(), p.getIndex());

        }

    }

    /**
     * Parses the {@code mutable} rule. This method should only be called if the
     * next token declares a mutable global variable, aka {@code VAR}.
     */
    public Ast.Global parseMutable() throws ParseException {

        try {

            String name = "";
            String type = "";
            boolean mutable;
            Ast.Expression value;

            match("VAR");
            mutable = true;

            if ( peek(Token.Type.IDENTIFIER) ) {

                name = tokens.get(0).getLiteral();
                match(Token.Type.IDENTIFIER);

            }

            if ( peek(":") ) {

                match(":");

            }

            if ( peek(Token.Type.IDENTIFIER) ) {

                type = tokens.get(0).getLiteral();
                match(Token.Type.IDENTIFIER);

            }

            if ( peek("=") ) {

                match("=");
                value = parseExpression();
                if ( peek(";") ) {

                    match(";");

                }
                else {

                    throw new ParseException("Missing semicolon", tokens.get(0).getIndex());

                }
                return new Ast.Global(name, type, mutable, Optional.of(value));

            }

            if ( peek(";") ) {

                match(";");

            }
            else {

                throw new ParseException("Missing semicolon", tokens.get(0).getIndex());

            }
            return new Ast.Global(name, type, mutable, Optional.empty());

        }
        catch (ParseException p) {

            throw new ParseException(p.getMessage(), p.getIndex());

        }

    }

    /**
     * Parses the {@code immutable} rule. This method should only be called if the
     * next token declares an immutable global variable, aka {@code VAL}.
     */
    public Ast.Global parseImmutable() throws ParseException {

        try {

            String name = "";
            String type = "";
            boolean mutable;
            Ast.Expression value;

            match("VAL");
            mutable = false;

            if ( peek(Token.Type.IDENTIFIER) ) {

                name = tokens.get(0).getLiteral();
                match(Token.Type.IDENTIFIER);

            }

            if ( peek(":") ) {

                match(":");

            }

            if ( peek(Token.Type.IDENTIFIER) ) {

                type = tokens.get(0).getLiteral();
                match(Token.Type.IDENTIFIER);

            }

            match("=");
            value = parseExpression();
            if ( peek(";") ) {

                match(";");

            }
            else {

                throw new ParseException("Missing semicolon", tokens.get(0).getIndex());

            }

            return new Ast.Global(name, type, mutable, Optional.of(value));

        }
        catch (ParseException p) {

            throw new ParseException(p.getMessage(), p.getIndex());

        }

    }

    /**
     * Parses the {@code function} rule. This method should only be called if the
     * next tokens start a method, aka {@code FUN}.
     */
    public Ast.Function parseFunction() throws ParseException {

        try {

            String name = "";
            String type = "";
            List<String> parameterTypes = new ArrayList<>();
            List<String> parameters = new ArrayList<>();
            List<Ast.Statement> statements = new ArrayList<>();

            match("FUN");

            if ( peek(Token.Type.IDENTIFIER) ) {

                name = tokens.get(0).getLiteral();
                match(Token.Type.IDENTIFIER);

            }


            match("(");

            while ( !peek(")") ) {

                if ( peek(Token.Type.IDENTIFIER) ) {

                    parameters.add(tokens.get(0).getLiteral());
                    match(Token.Type.IDENTIFIER);

                }

                if ( peek(":") ) {

                    match(":");

                }

                if ( peek(Token.Type.IDENTIFIER) ) {

                    parameterTypes.add(tokens.get(0).getLiteral());
                    match(Token.Type.IDENTIFIER);

                }

                if ( peek(",") ) {

                    match(",");

                }

            }

            if ( peek(")") ) {

                match(")");

            }
            else {

                throw new ParseException("Closing Parentheses", tokens.get(0).getIndex());

            }

            if ( peek(":") ) {

                match(":");

            }

            if ( peek(Token.Type.IDENTIFIER) ) {

                type = tokens.get(0).getLiteral();
                match(Token.Type.IDENTIFIER);

            }

            if ( peek("DO") ) {

                match("DO");

            }
            else {

                throw new ParseException("Missing DO", tokens.get(0).getIndex());

            }

            statements = parseBlock();

            if ( peek("END") ) {

                match("END");

            }
            else {

                throw new ParseException("Missing END", tokens.get(0).getIndex());

            }

            return new Ast.Function(name, parameters, parameterTypes, Optional.of(type), statements);

        }
        catch (ParseException p) {

            throw new ParseException(p.getMessage(), p.getIndex());

        }

    }

    /**
     * Parses the {@code block} rule. This method should only be called if the
     * preceding token indicates the opening a block of statements.
     */
    public List<Ast.Statement> parseBlock() throws ParseException {

        try {

            List<Ast.Statement> statements = new ArrayList<>();

            while ( tokens.has(0) && !(peek("END") || peek("ELSE")) ) {

                statements.add(parseStatement());

            }

            return statements;

        }
        catch (ParseException p) {

            throw new ParseException(p.getMessage(), p.getIndex());

        }

    }

    /**
     * Parses the {@code statement} rule and delegates to the necessary method.
     * If the next tokens do not start a declaration, if, while, or return
     * statement, then it is an expression/assignment statement.
     */
    public Ast.Statement parseStatement() throws ParseException {

        try {

            Ast.Statement statement;

            if ( peek("LET") ) {

                statement = parseDeclarationStatement();

            }
            else if ( peek("RETURN") ) {

                statement = parseReturnStatement();

            }
            else if ( peek("SWITCH") ) {

                statement = parseSwitchStatement();

            }
            else if ( peek("WHILE") ) {

                statement = parseWhileStatement();

            }
            else if ( peek("IF") ) {

                statement = parseIfStatement();

            }
            else {

                Ast.Expression leftSide = parseExpression();
                Ast.Expression assignment = null;

                if ( peek("=") ) {

                    match("=");
                    assignment = parseExpression();

                    if ( peek(";" ) ) {

                        match(";");

                    }
                    else {

                        throw new ParseException("Missing semicolon", tokens.get(0).getIndex());

                    }

                    statement = new Ast.Statement.Assignment(leftSide, assignment);

                    return statement;

                }

                if ( peek(";" ) ) {

                    match(";");

                }
                else {

                    throw new ParseException("Missing semicolon", tokens.get(0).getIndex());

                }
                statement = new Ast.Statement.Expression(leftSide);

            }

            if ( peek(";" ) ) {

                match(";");

            }

            return statement;

        }
        catch (ParseException p) {

            throw new ParseException(p.getMessage(), p.getIndex());

        }

    }

    /**
     * Parses a declaration statement from the {@code statement} rule. This
     * method should only be called if the next tokens start a declaration
     * statement, aka {@code LET}.
     */
    public Ast.Statement.Declaration parseDeclarationStatement() throws ParseException {

        try {

            String name = "";
            String type = "";
            Optional<Ast.Expression> value = Optional.empty();
            match("LET");

            if ( peek(Token.Type.IDENTIFIER) ) {

                name = tokens.get(0).getLiteral();
                match(Token.Type.IDENTIFIER);

            }

            if ( peek(":") ) {

                match(":");

            }

            if ( peek(Token.Type.IDENTIFIER) ) {

                type = tokens.get(0).getLiteral();
                match(Token.Type.IDENTIFIER);

            }
            else {

                type = null;

            }

            if ( peek("=") ) {

                match("=");
                value = Optional.of(parseExpression());

            }

            if ( peek(";" ) ) {

                match(";");

            }
            else {

                throw new ParseException("Missing semicolon", tokens.get(0).getIndex());

            }


            if ( type != null ) {

                return new Ast.Statement.Declaration(name, Optional.of(type), value);

            }

            return new Ast.Statement.Declaration(name, Optional.empty(), value);


        }
        catch (ParseException p) {

            throw new ParseException(p.getMessage(), p.getIndex());

        }

    }

    /**
     * Parses an if statement from the {@code statement} rule. This method
     * should only be called if the next tokens start an if statement, aka
     * {@code IF}.
     */
    public Ast.Statement.If parseIfStatement() throws ParseException {

        try {

            Ast.Expression condition;
            List<Ast.Statement> thenStatements = new ArrayList<>();
            List<Ast.Statement> elseStatements = new ArrayList<>();
            match("IF");

            condition = parseExpression();

            if ( peek("DO") ) {

                match("DO");

                thenStatements = parseBlock();

                if ( peek("ELSE") ) {

                    match("ELSE");
                    elseStatements = parseBlock();

                }

                if ( peek("END") ) {

                    match("END");

                }
                else {

                    throw new ParseException("Missing END", tokens.get(0).getIndex());

                }

            }

            else {

                throw new ParseException("Missing DO", tokens.get(0).getIndex());

            }

            return new Ast.Statement.If(condition, thenStatements, elseStatements);

        }
        catch (ParseException p) {

            throw new ParseException(p.getMessage(), p.getIndex());

        }
    }

    /**
     * Parses a switch statement from the {@code statement} rule. This method
     * should only be called if the next tokens start a switch statement, aka
     * {@code SWITCH}.
     */
    public Ast.Statement.Switch parseSwitchStatement() throws ParseException {

        try {

            Ast.Expression condition;
            List<Ast.Statement.Case> cases = new ArrayList<>();
            match("SWITCH");

            condition = parseExpression();

            if ( peek("CASE" ) ) {

                while ( peek("CASE") ) {

                    cases.add(parseCaseStatement());

                }

            }

            cases.add(parseCaseStatement());
            if ( peek("END") ) {

                match("END");

            }
            else {

                throw new ParseException("Missing END", tokens.get(0).getIndex());

            }

            return new Ast.Statement.Switch(condition, cases);

        }
        catch (ParseException p) {

            throw new ParseException(p.getMessage(), p.getIndex());

        }

    }

    /**
     * Parses a case or default statement block from the {@code switch} rule. 
     * This method should only be called if the next tokens start the case or 
     * default block of a switch statement, aka {@code CASE} or {@code DEFAULT}.
     */
    public Ast.Statement.Case parseCaseStatement() throws ParseException {

        try {

            Optional<Ast.Expression> value = Optional.empty();
            List<Ast.Statement> statements =  new ArrayList<>();

            if ( peek("CASE") ) {

                match("CASE");

                value = Optional.of(parseExpression());

                match(":");

                statements = parseBlock();

            }
            else if ( peek("DEFAULT") ) {

                match("DEFAULT");
                statements = parseBlock();

            }

            return new Ast.Statement.Case(value, statements);

        }
        catch (ParseException p) {

            throw new ParseException(p.getMessage(), p.getIndex());

        }

    }

    /**
     * Parses a while statement from the {@code statement} rule. This method
     * should only be called if the next tokens start a while statement, aka
     * {@code WHILE}.
     */
    public Ast.Statement.While parseWhileStatement() throws ParseException {

        try {

            match("WHILE");

            Ast.Expression value;
            List<Ast.Statement> statements = new ArrayList<>();

            value = parseExpression();

            if ( peek("DO") ) {

                match("DO");
                statements = parseBlock();
                if ( peek("END") ) {

                    match("END");

                }
                else {

                    throw new ParseException("Missing END", tokens.get(0).getIndex());

                }

            }
            else {

                throw new ParseException("Missing DO", tokens.get(0).getIndex());

            }

            return new Ast.Statement.While(value, statements);

        }
        catch (ParseException p) {

            throw new ParseException(p.getMessage(), p.getIndex());

        }

    }

    /**
     * Parses a return statement from the {@code statement} rule. This method
     * should only be called if the next tokens start a return statement, aka
     * {@code RETURN}.
     */
    public Ast.Statement.Return parseReturnStatement() throws ParseException {

        try {

            Ast.Expression value;
            match("RETURN");

            value = parseExpression();

            return new Ast.Statement.Return(value);

        }
        catch (ParseException p) {

            throw new ParseException(p.getMessage(), p.getIndex());

        }

    }

    /**
     * Parses the {@code expression} rule.
     */
    public Ast.Expression parseExpression() throws ParseException {

        return parseLogicalExpression();

    }

    /**
     * Parses the {@code logical-expression} rule.
     */
    public Ast.Expression parseLogicalExpression() throws ParseException {


        try {

            Ast.Expression leftSide = parseComparisonExpression();

            while (peek("||") || peek("&&") ) {

                String operation = tokens.get(0).getLiteral();
                if ( peek("||") ) {

                    match("||");

                }
                else if ( peek("&&") ) {

                    match("&&");

                }
                Ast.Expression otherSide = parseComparisonExpression();

                leftSide = new Ast.Expression.Binary(operation, leftSide, otherSide);

            }

            return leftSide;

        }
        catch (ParseException p) {

            throw new ParseException(p.getMessage(), p.getIndex());

        }

    }

    /**
     * Parses the {@code comparison-expression} rule.
     */
    public Ast.Expression parseComparisonExpression() throws ParseException {

        try {

            Ast.Expression leftSide = parseAdditiveExpression();

            while ( peek("!=") || peek("==") || peek(">") || peek("<") ) {

                String operation = tokens.get(0).getLiteral();
                match(Token.Type.OPERATOR);
                Ast.Expression otherSide = parseAdditiveExpression();

                leftSide = new Ast.Expression.Binary(operation, leftSide, otherSide);

            }

            return leftSide;

        }
        catch (ParseException p) {

            throw new ParseException(p.getMessage(), p.getIndex());

        }

    }

    /**
     * Parses the {@code additive-expression} rule.
     */
    public Ast.Expression parseAdditiveExpression() throws ParseException {

        try {

            Ast.Expression leftSide = parseMultiplicativeExpression();

            while ( peek("+") || peek("-") ) {

                String operation = tokens.get(0).getLiteral();
                match(Token.Type.OPERATOR);
                Ast.Expression otherSide = parseMultiplicativeExpression();

                leftSide = new Ast.Expression.Binary(operation, leftSide, otherSide);

            }

            return leftSide;

        }
        catch (ParseException p) {

            throw new ParseException(p.getMessage(), p.getIndex());

        }

    }

    /**
     * Parses the {@code multiplicative-expression} rule.
     */
    public Ast.Expression parseMultiplicativeExpression() throws ParseException {

        try {

            Ast.Expression leftSide = parsePrimaryExpression();

            while ( peek("^") || peek("/") || peek("*") ) {

                String operation = tokens.get(0).getLiteral();
                match(Token.Type.OPERATOR);
                Ast.Expression otherSide = parsePrimaryExpression();

                leftSide = new Ast.Expression.Binary(operation, leftSide, otherSide);

            }

            return leftSide;

        }
        catch (ParseException p) {

            throw new ParseException(p.getMessage(), p.getIndex());

        }

    }

    /**
     * Parses the {@code primary-expression} rule. This is the top-level rule
     * for expressions and includes literal values, grouping, variables, and
     * functions. It may be helpful to break these up into other methods but is
     * not strictly necessary.
     */
    public Ast.Expression parsePrimaryExpression() throws ParseException {

        try {

            Ast.Expression.Literal result;

            if ( peek("NIL") ) {

                match("NIL");
                result = new Ast.Expression.Literal(null);

            }
            else if ( peek("TRUE") ) {

                match("TRUE");
                result = new Ast.Expression.Literal(true);

            }
            else if ( peek("FALSE") ) {

                match("FALSE");
                result = new Ast.Expression.Literal(false);

            }
            else if ( peek(Token.Type.INTEGER) ) {

                result = new Ast.Expression.Literal(new BigInteger(tokens.get(0).getLiteral()));
                match(Token.Type.INTEGER);


            }
            else if ( peek(Token.Type.DECIMAL) ) {

                result = new Ast.Expression.Literal(new BigDecimal(tokens.get(0).getLiteral()));
                match(Token.Type.DECIMAL);

            }
            else if ( peek(Token.Type.CHARACTER) ) {

                if ( tokens.get(0).getLiteral().length() >= 4 ) {

                    String escape = tokens.get(0).getLiteral();

                    escape = escape.replace("\\b", "\b");
                    escape = escape.replace("\\n", "\n");
                    escape = escape.replace("\\r", "\r");
                    escape = escape.replace("\\t", "\t");
                    escape = escape.replace("\\\"", "\"");
                    escape = escape.replace("\\\\", "\\");
                    escape = escape.replace("\\\'", "\'");

                    Character character = escape.charAt(1);
                    match(Token.Type.CHARACTER);

                    result = new Ast.Expression.Literal(character);

                }
                else {

                    Character character = tokens.get(0).getLiteral().charAt(1);
                    match(Token.Type.CHARACTER);

                    result = new Ast.Expression.Literal(character);

                }

            }
            else if ( peek(Token.Type.STRING) ) {

                String string = tokens.get(0).getLiteral();
                string = string.substring(1, string.length() - 1);
                match(Token.Type.STRING);

                if ( string.contains("\\") ) {

                    string = string.replace("\\b", "\b");
                    string = string.replace("\\n", "\n");
                    string = string.replace("\\r", "\r");
                    string = string.replace("\\t", "\t");
                    string = string.replace("\\\"", "\"");
                    string = string.replace("\\\\", "\\");
                    string = string.replace("\\\'", "\'");

                }

                result = new Ast.Expression.Literal(string);

            }
            else if ( peek(Token.Type.IDENTIFIER) ) {

                String name = tokens.get(0).getLiteral();
                match(Token.Type.IDENTIFIER);

                if ( peek("[") ) {

                    match("[");
                    Optional<Ast.Expression> offset = Optional.of(parseExpression());

                    if ( peek("]") ) {

                        match("]");

                    }
                    else {

                        throw new ParseException("No ]", tokens.get(0).getIndex());

                    }

                    return new Ast.Expression.Access(offset, name);

                }
                else if ( peek("(") ) {

                    match("(");
                    List<Ast.Expression> arguments = new ArrayList<>();
                    while ( !peek(")") ) {

                        arguments.add(parseExpression());

                        if ( peek(",") ) {

                            match(",");

                            if ( peek(")") ) {

                                throw new ParseException("Trailing comma", tokens.get(0).getIndex());

                            }

                        }

                    }

                    if ( peek(")") ) {

                        match(")");

                    }
                    else {

                        throw new ParseException("No )", tokens.get(0).getIndex());

                    }


                    return new Ast.Expression.Function(name, arguments);

                }
                else {

                    return new Ast.Expression.Access(Optional.empty(), name);

                }

            }
            else if ( peek("(") ) {

                match("(");
                Ast.Expression expression = parseExpression();

                if ( peek(")") ) {

                    match(")");

                }
                else {

                    throw new ParseException("Expected )", tokens.get(0).getIndex());

                }

                return new Ast.Expression.Group(expression);

            }
            else {

                throw new ParseException("Invalid primary expression", tokens.get(0).getIndex());

            }

            return result;

        }
        catch (ParseException p) {

            throw new ParseException(p.getMessage(), p.getIndex());

        }

    }

    /**
     * As in the lexer, returns {@code true} if the current sequence of tokens
     * matches the given patterns. Unlike the lexer, the pattern is not a regex;
     * instead it is either a {@link Token.Type}, which matches if the token's
     * type is the same, or a {@link String}, which matches if the token's
     * literal is the same.
     *
     * In other words, {@code Token(IDENTIFIER, "literal")} is matched by both
     * {@code peek(Token.Type.IDENTIFIER)} and {@code peek("literal")}.
     */
    private boolean peek(Object... patterns) {

        for ( int i = 0; i < patterns.length; i++ ) {

            if ( !tokens.has(i) ) {

                return false;

            }
            else if ( patterns[i] instanceof Token.Type ) {

                if ( patterns[i] != tokens.get(i).getType() ) {

                    return false;

                }

            }
            else if ( patterns[i] instanceof  String ) {

                if ( !patterns[i].equals(tokens.get(i).getLiteral()) ) {

                    return false;

                }

            }
            else {

                throw new AssertionError("Invalid pattern object: " + patterns[i].getClass());

            }

        }
        return true;

    }

    /**
     * As in the lexer, returns {@code true} if {@link #peek(Object...)} is true
     * and advances the token stream.
     */
    private boolean match(Object... patterns) {

        boolean peek = peek(patterns);

        if ( peek ) {

            for ( int i = 0; i < patterns.length; i++ ) {

                tokens.advance();

            }

        }

        return peek;

    }

    private static final class TokenStream {

        private final List<Token> tokens;
        private int index = 0;

        private TokenStream(List<Token> tokens) {
            this.tokens = tokens;
        }

        /**
         * Returns true if there is a token at index + offset.
         */
        public boolean has(int offset) {
            return index + offset < tokens.size();
        }

        /**
         * Gets the token at index + offset.
         */
        public Token get(int offset) {
            return tokens.get(index + offset);
        }

        /**
         * Advances to the next token, incrementing the index.
         */
        public void advance() {
            index++;
        }

    }

}
