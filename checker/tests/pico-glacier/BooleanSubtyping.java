public class BooleanSubtyping {
    public enum CellType {
        BLANK(3),
        NUMBER(0);

        private int value;

        private CellType(int value) {
            this.value = value;
        }
    }

    public boolean foo() {
        CellType cellType = CellType.BLANK;
        return cellType == CellType.NUMBER;
    }
}
