import org.checkerframework.checker.pico.qual.Immutable;

import java.util.Date;

/*
@Immutable class ImmutableDate {
	double secondsSinceEpoch;

	void setSeconds(double s) {
		secondsSinceEpoch = s; // Should error!
	}
}

public @Immutable class ImmutablePerson {
	public ImmutablePerson() {
		birthdate = new ImmutableDate();

	}

	ImmutableDate birthdate;

	public void test() {

	}

}

class Person {
	String name;
}
*/

@Immutable public class ImmutablePerson {
    // Date is mutable
    // :: error: glacier.mutable.invalid
    Date birthdate;

    public ImmutablePerson() {}

    public void aMethod() {}
}
