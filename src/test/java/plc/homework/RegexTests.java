package plc.homework;

import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;

import java.util.regex.Pattern;
import java.util.stream.Stream;

/**
 * Contains JUnit tests for {@link Regex}. A framework of the test structure 
 * is provided, you will fill in the remaining pieces.
 *
 * To run tests, either click the run icon on the left margin, which can be used
 * to run all tests or only a specific test. You should make sure your tests are
 * run through IntelliJ (File > Settings > Build, Execution, Deployment > Build
 * Tools > Gradle > Run tests using <em>IntelliJ IDEA</em>). This ensures the
 * name and inputs for the tests are displayed correctly in the run window.
 */
public class RegexTests {

    /**
     * This is a parameterized test for the {@link Regex#EMAIL} regex. The
     * {@link ParameterizedTest} annotation defines this method as a
     * parameterized test, and {@link MethodSource} tells JUnit to look for the
     * static method {@link #testEmailRegex()}.
     *
     * For personal preference, I include a test name as the first parameter
     * which describes what that test should be testing - this is visible in
     * IntelliJ when running the tests (see above note if not working).
     */
    @ParameterizedTest
    @MethodSource
    public void testEmailRegex(String test, String input, boolean success) {
        test(input, Regex.EMAIL, success);
    }

    /**
     * This is the factory method providing test cases for the parameterized
     * test above - note that it is static, takes no arguments, and has the same
     * name as the test. The {@link Arguments} object contains the arguments for
     * each test to be passed to the function above.
     */
    public static Stream<Arguments> testEmailRegex() {
        return Stream.of(
                Arguments.of("Alphanumeric", "thelegend27@gmail.com", true),
                Arguments.of("UF Domain", "otherdomain@ufl.edu", true),
                Arguments.of("More than one dot", "the.legend.27@gmail.com", true),
                Arguments.of("Underscore", "the_legend_27@gmail.com", true),
                Arguments.of("Upper Case", "THELEGEND@gmail.com", true),
                Arguments.of("Upper Case in domain", "thelegend27@GMAIL.com", true),
                Arguments.of("Missing Domain Dot", "missingdot@gmailcom", false),
                Arguments.of("Symbols", "symbols#$%@gmail.com", false),
                Arguments.of("Missing At", "thelegend27gmail.com", false),
                Arguments.of("More than three domain", "otherdomain@ufl.ufledu", false),
                Arguments.of("Less than two name", "a@gmailcom", false),
                Arguments.of("Symbols in domain", "thelegend27@#$%.com", false),
                Arguments.of("Symbols in domain 2", "thelegend27@gmail.@#$", false),
                Arguments.of("More than one dot", "thelegend27@gmail..com", false),
                Arguments.of("Upper Case in domain", "thelegend27@gmail.COM", false),
                Arguments.of("Numbers in domain", "thelegend27@gmail.123", false)
        );
    }

    @ParameterizedTest
    @MethodSource
    public void testOddStringsRegex(String test, String input, boolean success) {
        test(input, Regex.ODD_STRINGS, success);
    }

    public static Stream<Arguments> testOddStringsRegex() {
        return Stream.of(
                // what have eleven letters and starts with gas?
                Arguments.of("11 Characters", "automobiles", true),
                Arguments.of("13 Characters", "i<3pancakes13", true),
                Arguments.of("19 Characters", "abcdefghijklmnopqrs", true),
                Arguments.of("Mixed Characters", "ab#$cd$%aaa", true),
                Arguments.of("Special Characters", "!@#$%!@#$%!@#", true),
                Arguments.of("10 Characters", "ababababab", true),
                Arguments.of("20 Characters", "abababababababababab", true),
                Arguments.of("5 Characters", "5five", false),
                Arguments.of("14 Characters", "i<3pancakes14!", false),
                Arguments.of("21 Characters", "abcabcabcabcabcabcabc", false),
                Arguments.of("Empty String", "", false),
                Arguments.of("Even Special Characters", "!@#$%^&*()_+", false),
                Arguments.of("String with spaces", "very odd strings", false)
        );
    }

    @ParameterizedTest
    @MethodSource
    public void testCharacterListRegex(String test, String input, boolean success) {
        test(input, Regex.CHARACTER_LIST, success);
    }

    public static Stream<Arguments> testCharacterListRegex() {
        return Stream.of(
                Arguments.of("Single Element", "['a']", true),
                Arguments.of("Multiple Elements", "['a','b','c']", true),
                Arguments.of("Numbers", "['1','2','3']", true),
                Arguments.of("Numbers and Characters", "['a','b','c','1','2','3']", true),
                Arguments.of("Empty List", "[]", true),
                Arguments.of("Escape Characters", "['\\b', '\\r', '\\n', '\\t', '\\f', '\\u000B']", true),
                Arguments.of("White space", "[' ', ' ']", true),
                Arguments.of("Unicode", "['\\u1000']", true),
                Arguments.of("Missing Brackets", "'a','b','c'", false),
                Arguments.of("Missing Commas", "['a' 'b' 'c']", false),
                Arguments.of("Not single characters", "['abc','def']", false),
                Arguments.of("Mixed character and integer", "['a','b',123]", false),
                Arguments.of("Mixed missing comma", "['a','b''c']", false),
                Arguments.of("Empty String", "['','']", false),
                Arguments.of("Character outside of list left", "a['a','b','c']", false),
                Arguments.of("Character outside of list Right", "['a','b','c']a", false),
                Arguments.of("Missing left bracket", "'a','b','c']", false),
                Arguments.of("Missing right bracket", "['a','b','c'", false),
                Arguments.of("Trailing Comma", "['a','b',]", false),
                Arguments.of("Invalid unicode", "['\\u000a']", false),
                Arguments.of("Valid invalid unicode mix", "['\\u0000','\\u000a']", false)
        );
    }

    @ParameterizedTest
    @MethodSource
    public void testDecimalRegex(String test, String input, boolean success) {
        test(input, Regex.DECIMAL, success);
    }

    public static Stream<Arguments> testDecimalRegex() {
        return Stream.of(
                Arguments.of("Numeric", "1.1", true),
                Arguments.of("Negative", "-1.0", true),
                Arguments.of("Trailing zeroes", "1.010000", true),
                Arguments.of("Lead 0", "0.12", true),
                Arguments.of("Long Decimal", "9999999999.999999999", true),
                Arguments.of("Multiple numbers before dot", "123456789.0", true),
                Arguments.of("Nothing after dot", "1", false),
                Arguments.of("No leading zero", ".5", false),
                Arguments.of("Too many leading zeroes", "001.015", false),
                Arguments.of("Negative with no leading number", "-.25", false),
                Arguments.of("Multiple dots", "12.34.56", false),
                Arguments.of("No number after dot", "100.", false),
                Arguments.of("Zero", "0", false),
                Arguments.of("Multiple zeroes", "000.000", false)
        );
    }

    @ParameterizedTest
    @MethodSource
    public void testStringRegex(String test, String input, boolean success) {
        test(input, Regex.STRING, success);
    }

    public static Stream<Arguments> testStringRegex() {
        return Stream.of(
                Arguments.of("Empty String", "\"\"", true),
                Arguments.of("Normal String", "\"Hello, World!\"", true),
                Arguments.of("Escape Character", "\"1\\t2\"", true),
                Arguments.of("Space", "\" \"", true),
                Arguments.of("New Line", "\"\n\"", true),
                Arguments.of("Single Quote", "\"'\"", true),
                Arguments.of("Double Quote", "\"\"\"", true),
                Arguments.of("Slash", "\"\\\\\"", true),
                Arguments.of("Backspace", "\"\b\"", true),
                Arguments.of("Carriage", "\"\r\"", true),
                Arguments.of("Tab", "\"\t\"", true),
                Arguments.of("Multiple escapes", "\"\\n\\n\\n\"", true),
                Arguments.of("Unterminated", "\"unterminated", false),
                Arguments.of("Invalid Escape", "\"invalid\\escape\"", false),
                Arguments.of("No quotes", "Hello, World", false),
                Arguments.of("Triple backslash", "\"backslash\\\\\\\"", false),
                Arguments.of("Missing right quote", "\"hello", false),
                Arguments.of("Missing left quote", "hello\"", false)
        );
    }

    /**
     * Asserts that the input matches the given pattern. This method doesn't do
     * much now, but you will see this concept in future assignments.
     */
    private static void test(String input, Pattern pattern, boolean success) {
        Assertions.assertEquals(success, pattern.matcher(input).matches());
    }

}
