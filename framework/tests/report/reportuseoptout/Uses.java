public class Uses {
    // The annotation always covers the package on which it is written.
    // :: error: (usage)
    reportuseoptout.InPackage inPackage;

    // applyToSubpackages=false also excludes transitively nested packages.
    reportuseoptout.sub.nested.InNestedSubpackage inNestedSubpackage;
}
