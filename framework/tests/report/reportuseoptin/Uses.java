public class Uses {
    // The default true reaches a direct subpackage.
    // :: error: (usage)
    reportuseoptin.inner.InInnerPackage inInnerPackage;

    // The default true also reaches transitively nested subpackages.
    // :: error: (usage)
    reportuseoptin.inner.nested.InNestedSubpackage inNestedSubpackage;
}
