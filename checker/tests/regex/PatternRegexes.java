// Test case for typetools issue #7539:
// https://github.com/typetools/checker-framework/issues/7539

import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class PatternRegexes {
    void simple() {
        Pattern abc = Pattern.compile("abc");
        String str = abc.pattern();
        System.out.println("abc".replaceAll(str, ""));
    }

    void complex(String content) {
        Pattern g2 = Pattern.compile("a(b*)(c*)");
        String strPattern = g2.pattern();
        Pattern g2Pattern = Pattern.compile(strPattern);
        Matcher matPattern = g2Pattern.matcher(content);

        if (matPattern.matches()) {
            matPattern.group(2); // ok
            // :: error: (group.count.invalid)
            matPattern.group(3);
        }

        String strToString = g2.toString();
        Pattern g2ToString = Pattern.compile(strToString);
        Matcher matToString = g2ToString.matcher(content);
        if (matToString.matches()) {
            matToString.group(2); // ok
            // :: error: (group.count.invalid)
            matToString.group(3);
        }
    }
}
