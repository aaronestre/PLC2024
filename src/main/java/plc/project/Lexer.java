
package plc.project;

import java.util.ArrayList;
import java.util.List;

/**
 * The lexer works through three main functions:
 *
 *  - {@link #lex()}, which repeatedly calls lexToken() and skips whitespace
 *  - {@link #lexToken()}, which lexes the next token
 *  - {@link CharStream}, which manages the state of the lexer and literals
 *
 * If the lexer fails to parse something (such as an unterminated string) you
 * should throw a {@link ParseException} with an index at the character which is
 * invalid.
 *
 * The {@link #peek(String...)} and {@link #match(String...)} functions are * helpers you need to use, they will make the implementation a lot easier. */
public final class Lexer {

    private final CharStream chars;

    public Lexer(String input) {
        chars = new CharStream(input);
    }

    /**
     * Repeatedly lexes the input using {@link #lexToken()}, also skipping over
     * whitespace where appropriate.
     */
    public List<Token> lex() {


        List<Token> tokens = new ArrayList<>();

        while ( chars.has(0) ) {

            if ( !match("[ \b\n\r\t]") ) {

                tokens.add(lexToken());

            }
            else {

                chars.skip();

            }

        }

        return tokens;

    }

    /**
     * This method determines the type of the next token, delegating to the
     * appropriate lex method. As such, it is best for this method to not change
     * the state of the char stream (thus, use peek not match).
     *
     * The next character should start a valid token since whitespace is handled
     * by {@link #lex()}
     */
    public Token lexToken() {

        Token token;

        String identifier = "[@A-Za-z]";
        String number = "[0-9]";
        String character = "'";
        String string = "\"";

        if ( peek(identifier) ) {

            token = lexIdentifier();

        }
        else if ( peek(number) ) {

            token = lexNumber();

        }
        else if ( peek(number) || peek("-", number)) {

            token = lexNumber();

        }
        else if ( peek(character) ) {

            token = lexCharacter();

        }
        else if ( peek(string) ) {

            token = lexString();

        }
        else {

            token = lexOperator();

        }

        return token;

    }

    public Token lexIdentifier() {

        // Matches the first character
        match("[@A-Za-z]");

        // Matches the rest of the identifier so long as it is valid
        while ( peek("[A-Za-z0-9_-]") ) {

            match("[A-Za-z0-9_-]");

        }

        return chars.emit(Token.Type.IDENTIFIER);

    }

    public Token lexNumber() {

        // No leading zeroes unless it is a decimal
        if ( peek("[0]") ) {

            match("[0]");

            // Checks if it is a decimal
            if ( peek("\\.") ) {

                match("\\.");

                while ( peek("[0-9]") ) {

                    match("[0-9]");

                }

                return chars.emit(Token.Type.DECIMAL);

            }

            // If not a decimal then no leading zeroes are allowed
            return chars.emit(Token.Type.INTEGER);

        }
        if ( peek("-") ) { // Checks if there is a negative sign, and if there is a zero that follows

            match("-");

            if ( peek("0") ) {

                match("0");
                match("\\.");

                while ( peek("[0-9]") ) {

                    match("[0-9]");

                }

                return chars.emit(Token.Type.DECIMAL);

            }

        }

        // Matches numbers until it reaches a potential decimal, but it has to start with a nonzero number
        while( peek("[1-9]") ) {

            match("[1-9]");

        }
        while( peek("[0-9]") ) {

            match("[0-9]");

        }
        // If there is a decimal then matches that then matches the rest of the decimal
        if ( peek("\\.", "[0-9]")) {

            match(("\\."));

            while ( peek("[0-9]") ) {

                match("[0-9]");

            }

            return chars.emit(Token.Type.DECIMAL);

        }

        return chars.emit((Token.Type.INTEGER));

    }

    public Token lexCharacter() {

        // Match opening quote
        match("'");

        // Checks for escape character
        if ( peek("\\\\") ) {

            lexEscape();

        }
        else if ( peek("[^'\\n\\r]") ) { // Matches any character as long as it is legal

            match("[^'\\n\\r]");

        }
        else {

            throw new ParseException("Illegal Character", chars.index);

        }

        // Checks for closing single quote
        if ( peek("'") ) {

            match("'");
            return chars.emit(Token.Type.CHARACTER);

        }

        // If none then throws exception
        throw new ParseException("Illegal Character", chars.index);

    }

    public Token lexString() {

        // Match opening quote
        match("\"");

        // Matches as long as there is no illegal characters
        while( peek("[^\"\\n\\r]") ) {

            if ( peek("\\\\") ) {

                lexEscape();

            }
            else {

                match("[^\"\\n\\r]");

            }


        }

        // Checks for closing character
        if ( peek("\"") ) {

            match("\"");
            return chars.emit(Token.Type.STRING);

        }

        throw new ParseException("Invalid String", chars.index);

    }

    public void lexEscape() {

        // Checks for the inital \ then checks for a valid escape then matches
        if ( peek("\\\\", "[bnrt'\"\\\\]") ) {

            match("\\\\");
            match("[bnrt'\"\\\\]");

        }
        else {

            // If not then throws an error
            throw new ParseException("Invalid escape", chars.index);

        }

    }

    public Token lexOperator() {

        // First check for comparison operators
        if ( peek("[!=]") ) {

            match("[!=]");

            if ( peek("=") ) {

                match("=");

            }

            return chars.emit(Token.Type.OPERATOR);

        }

        // Then checks for compound operators
        if ( peek("&", "&") ) {

            match("&", "&");

        }
        else if ( peek("[|]", "[|]") ) {

            match("[|]", "[|]");

        }
        else {

            // Then matches any other character that was not matched from any other token type
            match(".");

        }

        return chars.emit(Token.Type.OPERATOR);

    }

    /**
     * Returns true if the next sequence of characters match the given patterns,
     * which should be a regex. For example, {@code peek("a", "b", "c")} would
     * return true if the next characters are {@code 'a', 'b', 'c'}.
     */
    public boolean peek(String... patterns) {

        for ( int i = 0; i < patterns.length; i++ ) {

            if ( !chars.has(i) || !String.valueOf(chars.get(i)).matches(patterns[i])) {

                return false;

            }

        }

        return true;

    }

    /**
     * Returns true in the same way as {@link #peek(String...)}, but also
     * advances the character stream past all matched characters if peek returns
     * true. Hint - it's easiest to have this method simply call peek.
     */
    public boolean match(String... patterns) {

        boolean peek = peek(patterns);

        if ( peek ) {

            for ( int i = 0; i < patterns.length; i++ ) {

                chars.advance();

            }

        }

        return peek;

    }

    /**
     * A helper class maintaining the input string, current index of the char
     * stream, and the current length of the token being matched.
     *
     * You should rely on peek/match for state management in nearly all cases.
     * The only field you need to access is {@link #index} for any {@link
     * ParseException} which is thrown.
     */
    public static final class CharStream {

        private final String input;
        private int index = 0;
        private int length = 0;

        public CharStream(String input) {
            this.input = input;
        }

        public boolean has(int offset) {
            return index + offset < input.length();
        }

        public char get(int offset) {
            return input.charAt(index + offset);
        }

        public void advance() {
            index++;
            length++;
        }

        public void skip() {
            length = 0;
        }

        public Token emit(Token.Type type) {
            int start = index - length;
            skip();
            return new Token(type, input.substring(start, index), start);
        }

    }

}
