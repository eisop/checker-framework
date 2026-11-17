public class LhsExceptionTypeVar {
    <S> S id(S s) {
        return s;
    }

    <T> void crash(T p) {
        T throwInLhsNoTypeVarDefault = id(p);
    }
}
