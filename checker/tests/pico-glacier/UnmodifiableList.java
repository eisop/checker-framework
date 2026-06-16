package edu.cmu.cs.glacier.tests;

import java.util.List;

class ListProcessor {
    static <T> List<T> process(List<? extends T> c) {
        // ::warning: [unchecked] unchecked cast
        return (List<T>) c;
    }
}

class AClass {}

public class UnmodifiableList {
    private List<AClass> _list;

    public void foo() {

        // List<AClass> a = true ? _list : null;
        List<AClass> l = true ? ListProcessor.process(_list) : _list;
        // List<AClass> l = ListProcessor.process(_list);
    }
}
