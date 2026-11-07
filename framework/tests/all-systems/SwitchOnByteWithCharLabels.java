// The selector and the case labels have primitive types without a greatest lower bound.
public class SwitchOnByteWithCharLabels {
    int read(byte tag) {
        switch (tag) {
            case 'Z':
                return 0;
            case 'C':
                return 1;
            default:
                return 2;
        }
    }
}
