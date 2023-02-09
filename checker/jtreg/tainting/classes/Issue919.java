package classes;

import java.util.Set;

import classes.Issue919B.InnerClass;

public class Issue919 {
    private static void method(Set<InnerClass> innerClassSet2) throws Exception {
        Issue919B.otherMethod(innerClassSet2);
    }
}
