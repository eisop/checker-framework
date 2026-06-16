interface AnInterface {}

public class InterfaceSupertype {

    public void takeMutable(Object o) {}

    public void takeImmutable(Object o) {}

    public void doStuff() {
        AnInterface obj = null;

        takeMutable(obj);
        takeImmutable(obj);
    }

    public boolean equals(Object o1, Object o2) {
        if (o1 == o2) return true;
        return o1 != null ? o1.equals(o2) : false;
    }
}
