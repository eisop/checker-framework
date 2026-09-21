import org.checkerframework.checker.sqlquotes.qual.SqlEvenQuotes;
import org.checkerframework.checker.sqlquotes.qual.SqlOddQuotes;

public class SqlQuotesRegex {

    void oddQuotes() {
        // :: error: (assignment.type.incompatible)
        @SqlOddQuotes String none = "asdf";
        @SqlOddQuotes String one = "asdf'asdf";
        // :: error: (assignment.type.incompatible)
        @SqlOddQuotes String two = "'asdf'";
        @SqlOddQuotes String three = "'asdf'asdf'";
        // :: error: (assignment.type.incompatible)
        @SqlOddQuotes String manyEven = "'asdf''asdf'asdf'asdf'''";
        @SqlOddQuotes String manyOdd = "''asdf'asdf'''asdf'asdf''";
    }

    void evenQuotes() {
        @SqlEvenQuotes String none = "";
        // :: error: (assignment.type.incompatible)
        @SqlEvenQuotes String one = "'asdf";
        @SqlEvenQuotes String two = "''asdf";
        // :: error: (assignment.type.incompatible)
        @SqlEvenQuotes String three = "asdf'asdf''";
        @SqlEvenQuotes String manyEven = "''asdf''asdf'asdf''asdf'asdf";
        // :: error: (assignment.type.incompatible)
        @SqlEvenQuotes String manyOdd = "asdf''''asdf'asdf'asdf'asdf";
    }
}
