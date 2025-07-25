Version 3.49.5-eisop1 (July ??, 2025)
-------------------------------------

**User-visible changes:**

The Nullness Checker now has more fine-grained prefix options to suppress warnings:
- `@SuppressWarnings("nullness")` is used to suppress warnings from the Nullness, Initialization, and KeyFor Checkers.
- `@SuppressWarnings("nullnesskeyfor")` is used to suppress warnings from the Nullness and KeyFor Checkers,
   warnings from the Initialization Checker are not suppressed.
  `@SuppressWarnings("nullnessnoinit")` has the same effect as `@SuppressWarnings("nullnesskeyfor")`.
- `@SuppressWarnings("nullnessinitialization")` is used to suppress warnings from the Nullness and Initialization Checkers,
   warnings from the KeyFor Checker are not suppressed.
- `@SuppressWarnings("nullnessonly")` is used to suppress warnings from the Nullness Checker only,
   warnings from the Initialization and KeyFor Checkers are not suppressed.
- `@SuppressWarnings("initialization")` is used to suppress warnings from the Initialization Checker only,
   warnings from the Nullness and KeyFor Checkers are not suppressed.
- `@SuppressWarnings("keyfor")` is used to suppress warnings from the KeyFor Checker only,
   warnings from the Nullness and Initialization Checkers are not suppressed.

The EISOP Checker Framework now use `NullType` instead `Void` to denote the bottom type in the Java type hierarchy.
It is visible in error messages with type variable's or wildcard's lower bounds.
The type of the `null` literal in the Nullness Checker is now displayed as `@Nullable NullType` instead of the earlier `null (NullType)`.
This change makes the Checker Framework consistent with the Java language specification.

The format of error messages for type variables and wildcards has been improved to be consistent when printing both bounds.

The `instanceof.unsafe` and `instanceof.pattern.unsafe` warnings in the Checker Framework are now controlled by lint options.
They are enabled by default and can be disabled using `-Alint=-instanceof.unsafe` or `-Alint=-instanceof`.

**Implementation details:**

**Closed issues:**

eisop#1247, eisop#1263, typetools#7096.


Version 3.49.5 (June 30, 2025)
-----------------------------

**User-visible changes:**

The Checker Framework runs under JDK 25 -- that is, it runs on a version 25 JVM.
(EISOP note: this already worked in Version 3.49.3-eisop1.)

**Closed issues:**

#7093.


Version 3.49.4 (June 2, 2025)
-----------------------------

**Closed issues:**

#6740, #7013, #7038, #7070, #7082.


Version 3.49.3-eisop1 (May 6, 2025)
-----------------------------------

**User-visible changes:**

The Checker Framework runs under JDK 25 -- that is, it runs on a version 25 JVM.

**Implementation details:**

Gradle should now be run with at least JDK 17.
The `ORG_GRADLE_PROJECT_useJdkVersion` environment variable can be used to
select a different JDK for the actual compilation and testing.

**Closed issues:**

eisop#1051, eisop#1115, eisop#1180.


Version 3.49.3 (May 2, 2025)
----------------------------

**User-visible changes:**

The Checker Framework runs under JDK 24 -- that is, it runs on a version 24 JVM.
(EISOP note: this has been working for a while already.)

**Closed issues:**

#6520, #6671, #6750, #6762, #6887, #7001, #7019, #7024, #7029, #7053.


Version 3.49.2 (April 1, 2025)
------------------------------

**Closed issues:**

#6747, #6755, #6789, #6891, #6963, #6996, #7001, #7008, #7014.


Version 3.49.1-eisop1 (March 17, 2025)
--------------------------------------

**User-visible changes:**

The Nullness Checker now reports an error if any instanceof pattern variables
are annotated with `@Nullable` and a redundant warning if they are annotated
with `@NonNull`.

**Implementation details:**

Fixed intersection of wildcards with extends bounds, to ensure the correct
bounds are used.

**Closed issues:**

eisop#1003, eisop#1022, eisop#1033, eisop#1058.


Version 3.49.1 (March 3, 2025)
------------------------------

**Closed issues:**

#6970, #6974.


Version 3.49.0 (February 3, 2025)
---------------------------------

**User-visible changes:**

The Optional Checker is more precise for `Optional` values resulting from
operations on container types (e.g., `List`, `Map`, `Iterable`).  It supports
two new annotations:
 * `@NonEmpty`
 * `@UnknownNonEmpty`

The Signature Checker no longer supports `@BinaryNameWithoutPackage` because
it is equivalent to `@Identifier`; use `@Identifier` instead.

The JavaStubifier implementation now appears in package
`org.checkerframework.framework.stubifier.JavaStubifier`.

**Closed issues:**

#6935, #6936, #6939.


Version 3.48.4 (January 2, 2025)
--------------------------------

**Closed issues:**

#6919, #6630.


Version 3.48.3 (December 2, 2024)
---------------------------------

**Closed issues:**

#6886.


Version 3.48.2 (November 1, 2024)
---------------------------------

**Closed issues:**

#6371, #6867.


Version 3.48.1 (October 11, 2024)
---------------------------------

**User-visible changes:**

The Returns Receiver sub-checker is now disabled by default when running
the Resource Leak Checker, as usually it is not needed and it adds overhead.
To enable it, use the new `-AenableReturnsReceiverForRlc` command-line argument.

**Closed issues:**

#6434, #6810, #6839, #6842, #6856.


Version 3.48.0 (October 2, 2024)
--------------------------------

**User-visible changes:**

The new SQL Quotes Checker prevents errors in quoting in SQL queries.  It
prevents injection attacks that exploit quoting errors.

Aggregate Checkers now interleave error messages so that all errors about a line
of code appear together.
(EISOP note: some signatures changed from `BaseTypeChecker` to `SourceChecker`,
which might require adaptation in checkers.)

**Closed issues:**

#3568, #6725, #6753, #6769, #6770, #6780, #6785, #6795, #6804, #6811, #6825.


Version 3.47.0 (September 3, 2024)
----------------------------------

**User-visible changes:**

The Checker Framework runs under JDK 22 -- that is, it runs on a version 22 JVM.
The Checker Framework runs under JDK 23 -- that is, it runs on a version 23 JVM.
(EISOP note: this has been working for a while already, this just cleaned up
compiler warnings.)

The Optional Checker no longer supports the `@OptionalBottom` annotation.

**Implementation details:**

Removed annotations:
 * `@OptionalBottom`

**Closed issues:**

#6510, #6704, #6743, #6749, #6760, #6761.


Version 3.46.0 (August 1, 2024)
-------------------------------

**User-visible changes:**

Renamed `@EnsuresCalledMethodsVarArgs`to `@EnsuresCalledMethodsVarargs`.

**Implementation details:**

Many symbols that contained `VarArgs` were similarly renamed to use `Varargs`,
e.g. `AnnotatedTypeMirror.isVarargs()`.

**Closed issues:**

#4923, #6420, #6469, #6652, #6664.


Version 3.45.0 (July 1, 2024)
-----------------------------

**Implementation details:**

Added a `Tree` argument to `AnnotatedTypes.adaptParameters()`

Deprecated methods:
 * `TreeUtils.isVarArgs()` => `isVarargsCall()`
 * `TreeUtils.isVarArgMethodCall()` => `isVarargsCall()`

**Closed issues:**

#152, #5575, #6630, #6641, #6648, #6676.


Version 3.44.0 (June 3, 2024)
-----------------------------

**Implementation details:**

Removed methods:
 * `AbstractAnalysis.readFromStore()`:  use `Map.get()`

Renamed methods:
 * `CFAbstractStore.methodValues()` => `methodCallExpressions()`
 * `AbstractCFGVisualizer.format()` => `escapeString()`

Renamed fields:
 * `AnalysisResult.stores` => `inputs`

Deprecated methods:
 * `AbstractAnalysis.getContainingMethod()` => `getEnclosingMethod()`
 * `AbstractAnalysis.getContainingClass()` => `getEnclosingMethod()`
 * `ControlFlowGraph.getContainingMethod()` => `getEnclosingMethod()`
 * `ControlFlowGraph.getContainingClass()` => `getEnclosingClass()`
 * `JavaExpression.isUnassignableByOtherCode()` => `isAssignableByOtherCode()`
 * `JavaExpression.isUnmodifiableByOtherCode()` => `isModifiableByOtherCode()`

`BaseTypeVisitor#visitMethod(MethodTree, Void)` is now `final`.
Subclasses should override `BaseTypeVisitor#processMethodTree(MethodTree)`.

**Closed issues:**

#802, #2676, #2780, #2926, #3378, #3612, #3764, #4007, #4964, #5070, #5176,
#5237, #5541, #6046, #6382, #6388, #6566, #6568, #6570, #6576, #6577, #6631,
#6635, #6636, #6644.


Version 3.43.0 (May 1, 2024)
----------------------------

**User-visible changes:**

Method, constructor, lambda, and method reference type inference has been
greatly improved.  The `-AconservativeUninferredTypeArguments` option is
no longer necessary and has been removed.

Renamed command-line arguments:
 * `-AskipDirs` has been renamed to `-AskipFiles`.
   `-AskipDirs` will continue to work for the time being.

New command-line arguments:
 * `-AonlyFiles` complements `-AskipFiles`

A specialized inference algorithm for the Resource Leak Checker runs
automatically as part of whole-program inference.

**Implementation details:**

Deprecated `ObjectCreationNode#getConstructor` in favor of new
`ObjectCreationNode#getTypeToInstantiate()`.
(EISOP note: this already happened in Version 3.39.0-eisop1 on
October 22, 2023.)

Renamed `AbstractCFGVisualizer.visualizeBlockHelper()` to
`visualizeBlockWithSeparator()`.

Moved methods from `TreeUtils` to subclasses of `TreeUtilsAfterJava11`:
 * isConstantCaseLabelTree
 * isDefaultCaseLabelTree
 * isPatternCaseLabelTree

Renamed `BaseTypeVisitor.checkForPolymorphicQualifiers()` to
`warnInvalidPolymorphicQualifier()`.

**Closed issues:**

#979, #4559, #4593, #5058, #5734, #5781, #6071, #6093, #6239, #6297, #6317,
#6322, #6346, #6373, #6376, #6378, #6379, #6380, #6389, #6393, #6396, #6402,
#6406, #6407, #6417, #6421, #6430, #6433, #6438, #6442, #6473, #6480, #6507,
#6531, #6535.


Version 3.42.0-eisop5 (December 20, 2024)
-----------------------------------------

**User-visible changes:**

Removed support for the `-Anocheckjdk` option, which was deprecated in version 3.1.1.
Use `-ApermitMissingJdk` instead.

The Nullness Checker now reports an error if an array or object creation is annotated
with `@Nullable`, as array and object creations are intrinsically non-null.

**Implementation details:**

Changed `org.checkerframework.framework.util.ContractsFromMethod` to an interface.
Use `DefaultContractsFromMethod` to get the default behavior or use the new
`NoContractsFromMethod` if you want no support for contracts.

Make `SourceChecker#suppressWarningsString` protected to allow adaptation in subclasses.

**Closed issues:**

eisop#413, eisop#782, eisop#815, eisop#826, eisop#860, eisop#873, eisop#875, eisop#927,
eisop#982, eisop#1012.


Version 3.42.0-eisop4 (July 12, 2024)
-------------------------------------

**Implementation details:**

New method `GenericAnnotatedTypeFactory#addComputedTypeAnnotationsWithoutFlow(Tree, AnnotatedTypeMirror)`
that sets `useFlow` to `false` before calling `addComputedTypeAnnotations`. Subclasses should override
method `GenericAnnotatedTypeFactory#addComputedTypeAnnotations(Tree, AnnotatedTypeMirror)` instead.
Deprecated the `GenericAnnotatedTypeFactory#addComputedTypeAnnotations(Tree, AnnotatedTypeMirror, boolean)`
overload.

Changed the return type of `AnnotatedTypeFactory#getEnumConstructorQualifiers` from `Set<AnnotationMirror>`
to `AnnotationMirrorSet`.

Field `AnnotatedTypeFactory#root` is now private and can only be accessed through `getRoot`/`setRoot`.

framework-test:
 * Improvements to more consistently handle tests that do not use `-Anomsgtext`.
 * Added new class `DetailedTestDiagnostic` to directly represent test diagnostics when
   `-Adetailedmsgtext` is used.

**Closed issues:**

eisop#742, eisop#777, eisop#795, typetools#6704.


Version 3.42.0-eisop3 (March 1, 2024)
-------------------------------------

**User-visible changes:**

Performance improvements in the Nullness Checker.

**Implementation details:**

Support separate defaults for wildcard and type variable upper bounds.
Add support for defaults for type variable uses.
See changes in `TypeUseLocation`, `QualiferDefaults`, and `QualifierHierarchy`,
as well as the new `ParametricTypeVariableUseQualifier` meta-annotation.

Refactored the `TypeInformationPresenter` into several classes in the new
`org.checkerframework.framework.util.visualize` package.

**Closed issues:**

eisop#703, typetools#6433, typetools#6438.


Version 3.42.0-eisop2 (January 9, 2024)
---------------------------------------

**Implementation details:**

Moved `ErrorTypeKindException` from `org.checkerframework.framework.util.element.ElementAnnotationUtil` to
`org.checkerframework.framework.type.AnnotatedTypeMirror`. Properly raise these errors in more cases.

Deprecated `AnnotationUtils#isDeclarationAnnotation` and added the clearer `AnnotationUtils#isTypeUseAnnotation`.

Removed the dependency on the classgraph library, which added over 500kB to `checker.jar`.
It is easy to add the dependency for debugging.

**Closed issues:**

eisop#666, eisop#673.


Version 3.42.0-eisop1 (January 2, 2024)
---------------------------------------

**Closed issues:**

typetools#6373, typetools#6374.


Version 3.42.0 (December 15, 2023)
----------------------------------

**User-visible changes:**

Method annotation `@AssertMethod` indicates that a method checks a value and
possibly throws an assertion.  Using it can make flow-sensitive type refinement
more effective.

In `org.checkerframework.common.util.debug`, renamed `EmptyProcessor` to `DoNothingProcessor`.
Removed `org.checkerframework.common.util.report.DoNothingChecker`; use `DoNothingProcessor`.
Moved `ReportChecker` from `org.checkerframework.common.util.report` to `org.checkerframework.common.util.count.report`.
(EISOP note: we did not follow this renaming - if anything, `counting` could be a special case of `reporting`, not
the other way around.)


Version 3.41.0-eisop1 (December 5, 2023)
----------------------------------------

**User-visible changes:**

The Nullness Checker now warns about redundant null cases in switch statements and expressions when
using the `-Alint=redundantNullComparison` command-line argument.

**Closed issues:**

eisop#628, eisop#635, eisop#640, eisop#641.


Version 3.41.0 (December 4, 2023)
---------------------------------

**User-visible changes:**

New command-line options:
 * `-AassumePureGetters`: Unsoundly assume that every getter method is pure.

**Implementation details:**

Added method `isDeterministic()` to the `AnnotationProvider` interface.

`CFAbstractValue#leastUpperBound` and `CFAbstractValue#widenUpperBound` are now
final.  Subclasses should override method `CFAbstractValue#upperBound(V,
TypeMirror, boolean)` instead.

(EISOP note: typetools added the new method annotation `org.checkerframework.dataflow.qual.AssertMethod`
to treat such methods like assert statements. EISOP might change the implementation of this feature
in a future release.)

**Closed issues:**

#1497, #3345, #6037, #6204, #6276, #6282, #6290, #6296, #6319, #6327.


Version 3.40.0-eisop2 (November 24, 2023)
-----------------------------------------

**Implementation details:**

Always use reflective access for `TreeMaker#Select`, to allow artifacts built with
Java 21+ to be executed on Java <21.


Version 3.40.0-eisop1 (November 24, 2023)
-----------------------------------------

**User-visible changes:**

Improvements to initialization type frames in the Initialization Checker.

**Implementation details:**

New method `TreeUtils#isEnhancedSwitchStatement` to determine if a switch statement tree
is an enhanced switch statement.

**Closed issues:**

eisop#609, eisop#610, eisop#612.


Version 3.40.0 (November 1, 2023)
---------------------------------

**User-visible changes:**

Optional Checker:  `checker-util.jar` defines `OptionalUtil.castPresent()` for
suppressing false positive warnings from the Optional Checker.

**Closed issues:**

#4947, #6179, #6215, #6218, #6222, #6247, #6259, #6260.


Version 3.39.0-eisop1 (October 22, 2023)
----------------------------------------

**User-visible changes:**

The Initialization Checker is now separated from the Nullness Checker.
To unsoundly use the Nullness Checker without initialization checking, use the new `-AassumeInitialized`
command-line argument.
Error messages will now be either from the Initialization Checker or the Nullness Checker, which
simplifies the types in error messages.
`@SuppressWarnings("initialization")` should be used to suppress initialization warnings.
In this release, `nullness` continues to suppress warnings from the Initialization Checker, while
`nullnessnoinit` may be used to suppress warnings from the Nullness Checker only. A future release
will make suppression behavior consistent with other checkers.

The Initialization Checker supports the new qualifier `@PolyInitialized` to express qualifier polymorphism.

Fixed a bug in the Nullness Checker where an instance receiver is incorrectly marked non-null after
a static method or field access. This could lead to new nullness errors. The static access should be
changed to be through a class name.

Checkers now enforce `@TargetLocations` meta-annotations: if a qualifier is declared with the
meta-annotation `@TargetLocations({TypeUseLocation...})`, the qualifier should only be applied to
these type use locations.
The new command-line argument `-AignoreTargetLocations` disables validating the target locations
of qualifiers. This option is not enabled by default. With this flag, the checker ignores all
`@TargetLocations` meta-annotations and allows all qualifiers to be applied to every type use.

**Implementation details:**

Corrected the arguments to an `ObjectCreationNode` when the node refers to an
anonymous constructor invocation with an explicit enclosing expression in Java 11+.
Now the first argument is not treated as an enclosing expression if it is not.

Deprecated `ObjectCreationNode#getConstructor` in favor of new `ObjectCreationNode#getTypeToInstantiate()`.

Removed class `StringConcatenateAssignmentNode` and its last usages.
The class was deprecated in release 3.21.3-eisop1 (March 23, 2022) and no longer used in CFGs.

Changed the return types of
 * `BaseTypeChecker#getImmediateSubcheckerClasses()` and overrides to
   `Set<Class<? extends BaseTypeChecker>>`,
 * `AnalysisResult#getFinalLocalValues()` to `Map<VariableElement, V>`, and
 * `GenericAnnotatedTypeFactory#getFinalLocalValues()` to `Map<VariableElement, Value>`.

**Closed issues:**

eisop#297, eisop#376, eisop#400, eisop#519, eisop#532, eisop#533, typetools#1590, typetools#1919.


Version 3.39.0 (October 2, 2023)
--------------------------------

**User-visible changes:**

The Checker Framework runs on a version 21 JVM.
It does not yet soundly check all new Java 21 language features, but it does not
crash when compiling them.

**Implementation details:**

Dataflow supports all the new Java 21 language features.
 * A new node, `DeconstructorPatternNode`, was added, so any implementation of
   `NodeVisitor` must be updated.
 * Method `InstanceOfNode.getBindingVariable()` is deprecated; use
   `getPatternNode()` or `getBindingVariables()` instead.

WPI uses 1-based indexing for formal parameters and arguments.

**Closed issues:**

#5911, #5967, #6155, #6173, #6201.


Version 3.38.0 (September 1, 2023)
----------------------------------

**User-visible changes:**

Eliminated the `@SignedPositiveFromUnsigned` annotation, which users were
advised against using.

**Implementation details:**

Renamed `SourceChecker.processArg()` to `processErrorMessageArg()`.

**Closed issues:**

#2156, #5672, #6110, #6111, #6116, #6125, #6129, #6136.


Version 3.37.0 (August 1, 2023)
-------------------------------

**User-visible changes:**

Removed support for deprecated option `-AuseDefaultsForUncheckedCode`.

The Signedness Checker no longer allows (nor needs) `@UnknownSignedness`
to be written on a non-integral type.

**Implementation details:**

`QualifierHierarchy`:
 * The constructor takes an `AnnotatedTypeFactory`.
 * Changes to `isSubtype()`:
    * `isSubtype()` has been renamed to `isSubypeQualifiers()` and made protected.
      Clients that are not in a qualifier hierarchy should call `isSubtypeShallow()`
      or, rarely, new method `isSubtypeQualifiersOnly()`.
    * New public method `isSubtypeShallow()` that takes two more arguments than
      `isSubypeQualifiers()`.
 * Similar changes to `greatestLowerBound()` and `leastUpperBound()`.

**Closed issues:**

#6076, #6077, #6078, #6098, #6100, #6104, #6113.


Version 3.36.0 (July 3, 2023)
-----------------------------

**User-visible changes:**

The Initialization Checker issues a `cast.unsafe` warning instead of an
`initialization.cast` error.

The Resource Leak Checker now issues a `required.method.not.known` error
when an expression with type `@MustCallUnknown` has a must-call obligation
(e.g., because it is a parameter annotated as `@Owning`).

The Resource Leak Checker's default MustCall type for type variables has been
changed from `@MustCallUnknown` to `@MustCall({})`.  This change reduces the
number of false positive warnings in code that uses type variables but not
resources.  However, it makes some code that uses type variables and resources
unverifiable with any annotation.

**Implementation details:**

Deprecated `ElementUtils.getSimpleNameOrDescription()` in favor of `getSimpleDescription()`.

Renamed methods in `AnnotatedTypeMirror`.
The old versions are deprecated.  Because the `*PrimaryAnnotation*` methods
might not return an annotation of a type variable or wildcard, it is better to
call `getEffectiveAnnotation*` or `hasEffectiveAnnotation*` instead.
 * `clearAnnotations*()` => `clearPrimaryAnnotations()`
 * `getAnnotation*()` => `getPrimaryAnnotation*()`.
 * `hasAnnotation*()` => `hasPrimaryAnnotation()`.
 * `removeAnnotation*()` => `removePrimaryAnnotation*()`.
 * `isAnnotatedInHierarchy()` => `hasPrimaryAnnotationInHierarchy()`
 * `removeNonTopAnnotationInHierarchy()` should not be used.
(EISOP note: these renamings break javac convention and are inconsistently applied.
Only the last two changes are retained.)

Dataflow Framework:
 * New `ExpressionStatementNode` marks an expression that is used as a statement.
 * Removed class `StringConcatenateAssignmentNode`, which is now desugared.
(EISOP note: these were performed in 3.21.2-eisop1 and 3.21.3-eisop1, respectively.)

`GenericAnnotatedTypeFactory`:
 * Renamed `getTypeFactoryOfSubchecker()` to `getTypeFactoryOfSubcheckerOrNull`.
 * Added new `getTypeFactoryOfSubchecker()` that never returns null.

Return types changed:
 * `GenericAnnotatedTypeFactory.getFinalLocalValues()` return type changed to
   `Map`, though the returned value is still a `HashMap`.
 * `BaseTypeChecker.getImmediateSubcheckerClasses()` return type changed to
   `Set`, though the returned value is still a `LinkedHashSet`.

Renamed methods in `CFAbstractValue`:
 * `combineOneAnnotation()` => `combineAnnotationWithTypeVar()`
 * `combineNoAnnotations()` => `combineTwoTypeVars()`

**Closed issues:**
#5908, #5936, #5971, #6019, #6025, #6028, #6030, #6039, #6053, #6060, #6069.


Version 3.35.0 (June 1, 2023)
-----------------------------

**User-visible changes:**

The Checker Framework no longer issues `type.checking.not.run` errors.
This reduces clutter in the output.

Signedness Checker:
 * The receiver type of `Object.hashCode()` is now `@UnknownSignedness`.

**Implementation details:**

Instead of overriding `isRelevant()`, a type factory implementation should
override `isRelevantImpl()`.  Clients should continue to call `isRelevant()`;
never call `isRelevantImpl()` except as `super.isRelevantImpl()`.

Methods that now return a `boolean` rather than `void`:
 * `commonAssignmentCheck()`
 * `checkArrayInitialization()`
 * `checkLock()`
 * `checkLockOfThisOrTree()`
 * `ensureExpressionIsEffectivelyFinal()`

Methods that now return `AnnotationMirrorSet` instead of `Set<? extends AnnotationMirror>`:
 * `getTopAnnotations()`
 * `getBottomAnnotations()`
 * `getDefaultTypeDeclarationBounds()`
 * `getExceptionParameterLowerBoundAnnotations()`

Renamed `BaseTypeVisitor.checkExtendsImplements()` to `checkExtendsAndImplements()`.

Class `FieldInvariants`:
 * constructor now takes an `AnnotatedTypeFactory`
 * `isSuperInvariant()` has been renamed to `isStrongerThan()` and
   no longer takes an `AnnotatedTypeFactory`

`CFAbstractValue.validateSet()` takes a type factory rather than a `QualifierHierarchy`.

Removed methods that have been deprecated for over two years.

**Closed issues:**

#4170, #5722, #5777, #5807, #5821, #5826, #5829, #5837, #5930.


Version 3.34.0-eisop1 (May 9, 2023)
-----------------------------------

**User-visible changes:**

There is now a dedicated website for the EISOP Framework at https://eisop.github.io/ .

The new command-line arguments `-AaliasedTypeAnnos={aliases}` and `-AaliasedDeclAnnos={aliases}`
define custom type and declaration annotation aliases for the canonical annotations of a checker.
`aliases` is in the format
`FQN.canonical.Qualifier1:FQN.alias1.Qual1,FQN.alias2.Qual1;FQN.canonical.Qualifier2:FQN.alias1.Qual2`.

**Implementation details:**

The EISOP Framework continues to build and run on JDK 8.

Improvements to `-AwarnRedundantAnnotations` with type variables and the Interning Checker.

Refactored handling of test options and fixed the interaction between the `detailedmsgtext` and
`nomsgtext` options.

New `CFGVisualizeOptions` class for handling command-line arguments, making the
dataflow demo `Playground` applications much easier to use.


Version 3.34.0 (May 2, 2023)
----------------------------

**User-visible changes:**

The Checker Framework runs under JDK 20 -- that is, it runs on a version 20 JVM.

Explicit lambda parameters are defaulted the same as method parameters.  For
example, in `(String s) -> {...}` the type of `s` is `@NonNull String`.

**Implementation details:**

Renamings in `AnnotatedTypeFactory`:
 * `prepareCompilationUnitForWriting()` => `wpiPrepareCompilationUnitForWriting()`
 * `prepareClassForWriting()` => `wpiPrepareClassForWriting()`
 * `prepareMethodForWriting()` => `wpiPrepareMethodForWriting()`
   and changed its signature by adding two formal parameters

**Closed issues:**

#803, #5739, #5749, #5767, #5781, #5787.


Version 3.33.0 (April 3, 2023)
------------------------------

**User-visible changes:**

The new command-line argument `-AwarnRedundantAnnotations` warns about redundant
annotations.  With this flag, a warning is issued if an explicitly written
annotation on a type is the same as the default annotation.  This feature does
not warn about all redundant annotations, only some.
(EISOP note: this was implemented in Version 3.27.0-eisop1.)

The Value Checker is cognizant of signedness annotations.  This eliminates some
false positive warnings.

**Implementation details:**

The Checker Framework no longer builds under JDK 8.
However, you can still run the Checker Framework under JDK 8.
(EISOP note: the EISOP Framework continues to build and run on JDK 8.)

**Closed issues:**

#3785, #5436, #5708, #5717, #5720, #5721, #5727, #5732.


Version 3.32.0-eisop1 (March 9, 2023)
-------------------------------------

**User-visible changes:**

The new command-line argument `-AcheckEnclosingExpr` enables type checking for
enclosing expression types of inner class instantiations. This fixes an
unsoundness, in particular for the Nullness Initialization Checker, which did
not detect the use of an uninitialized outer class for an inner class
instantiation.
The option is off by default to avoid many false-positive errors.

**Implementation details:**

Added method `AnnotatedExecutableType.getVarargType` to access the vararg type
of a method/constructor.
This allows us to remove usages of `AnnotatedTypes.adaptParameters()`.

A `VariableDeclarationNode` is now correctly added to the CFG for the binding
variable in a `BindingPatternTree`.

Remove the `fastAssemble` task which is subsumed by `assembleForJavac`.

Successfully compiles with Java 20 and 21.

**Closed issues:**

eisop#282, eisop#310, eisop#312, typetools#5672.


Version 3.32.0 (March 2, 2023)
------------------------------

**User-visible changes:**

Fixed a bug in the Nullness Checker where a call to a side-effecting method did
not make some formal parameters possibly-null.  The Nullness Checker is likely
to issue more warnings for your code.  For ways to eliminate the new warnings,
see <https://eisop.github.io/cf/manual/#type-refinement-side-effects>.

If you supply the `-AinvocationPreservesArgumentNullness` command-line
option, the Nullness Checker unsoundly assumes that arguments passed to
non-null parameters in an invocation remain non-null after the invocation.
This assumption is unsound in general, but it holds for most code.

(EISOP note: contrary to this description, one needs to use
`-AinvocationPreservesArgumentNullness=false` to get the unsound behavior.
EISOP keeps only the `-AconservativeArgumentNullnessAfterInvocation` option,
introduced in version 3.25.0-eisop1, which this typetools option is based on.)

**Implementation details:**

Moved `TreeUtils.isAutoGeneratedRecordMember(Element)` to `ElementUtils`.
(EISOP note: originally introduced the method in the correct location in Version 3.27.0-eisop1.)

Renamed `TreeUtils.instanceOfGetPattern()` to `TreeUtils.instanceOfTreeGetPattern()`.
(EISOP note: EISOP performed this renaming in Version 3.21.2-eisop1.)

Deprecated `AnnotatedTypes#isExplicitlySuperBounded` and `AnnotatedTypes#isExplicitlyExtendsBounded`
because they are duplicates of `#hasExplicitSuperBound` and `#hasExplicitExtendsBound`.


Version 3.31.0 (February 17, 2023)
----------------------------------

**User-visible changes:**

Command-line argument `-AshowPrefixInWarningMessages` puts the checker name
on the first line of each warning and error message.

Signedness Checker changes:
 * Cast expressions are not subject to type refinement.  When a programmer
   writes a cast such as `(@Signed int) 2`, it is not refined to
   `@SignednessGlb` and cannot be used in an unsigned context.
 * When incompatible arguments are passed to `@PolySigned` formal parameters,
   the error is expressed in terms of `@SignednessBottom` rather than the
   greatest lower bound of the argument types.

**Implementation details:**

Moved `AnnotationMirrorSet` and `AnnotationMirrorMap` from
`org.checkerframework.framework.util` to `org.checkerframework.javacutil`.
Changed uses of `Set<AnnotationMirror>` to `AnnotationMirrorSet` including in APIs.
Removed methods from AnnotationUtils that are no longer useful:
`createAnnotationMap`, `createAnnotationSet`, `createUnmodifiableAnnotationSet`.

**Closed issues:**

#5597.


Version 3.30.0 (February 2, 2023)
---------------------------------

**Implementation details:**

`getQualifierKind()` throws an exception rather than returning null.
(EISOP note: this method is in `ElementQualifierHierarchy` and `QualifierKindHierarchy`.)

Renamed Gradle task `copyJarsToDist` to `assembleForJavac`.

**Closed issues:**

#5402, #5486, #5489, #5519, #5524, #5526.


Version 3.29.0 (January 5, 2023)
--------------------------------

**User-visible changes:**

Dropped support for `-ApermitUnsupportedJdkVersion` command-line argument.
You can now run the Checker Framework under any JDK version, without a warning.
(EISOP note: a note is however still issued. Use the EISOP option
`-AnoJreVersionCheck` to also suppress the note.)

Pass `-Astubs=permit-nullness-assertion-exception.astub` to not be warned about null
pointer exceptions within nullness assertion methods like `Objects.requireNonNull`.

Pass `-Astubs=sometimes-nullable.astub` to unsoundly permit passing null to
calls if null is sometimes but not always permitted.

**Closed issues:**

#5412, #5431, #5435, #5438, #5447, #5450, #5453, #5471, #5472, #5487.


Version 3.28.0-eisop1 (December 7, 2022)
----------------------------------------

**User-visible changes:**

Support JSpecify annotations in the `org.jspecify.annotations` package.

**Implementation details:**

Remove duplicate code in `AnnotatedTypeFactory` and `javacutil`.


Version 3.28.0 (December 1, 2022)
---------------------------------

**User-visible changes:**

The Checker Framework runs under JDK 19 -- that is, it runs on a version 19 JVM.

**Implementation details:**

Renamed `TryFinallyScopeCell` to `LabelCell`.

Renamed `TreeUtils.isEnumSuper` to `isEnumSuperCall`.

**Closed issues:**

#5390, #5399, #5390.


Version 3.27.0-eisop1 (November 6, 2022)
----------------------------------------

**User-visible changes:**

The new command-line argument `-AwarnRedundantAnnotations` warns about redundant annotations.
With this flag, a warning is issued if an explicitly written annotation on a type is the same
as the default annotation for this type and location.

Support additional Nullness Checker annotation aliases from:
 * `io.micronaut.core.annotation`
 * `io.vertx.codegen.annotations`
 * `jakarta.annotation`
 * `net.bytebuddy[.agent].utility.nullability`

**Implementation details:**

When reporting issues on an artificial tree (generated by the compiler), always
try to find the closest non-artificial parent in the AST path to provide position
information.

Formatting rules for `*.ajava` files are now consistent with the ones for `*.java` files.
Imports are now ignored when parsing `ajava` files.

Moved method `isAutoGeneratedRecordMember(Element e)`, which was added in 3.27.0,
from `TreeUtils` to the more appropriate `ElementUtils`.

Refined the return types of several `TreeUtils` `elementFromDeclaration` methods
to be `@NonNull`.

**Closed issues:**

eisop#244, eisop#360.


Version 3.27.0 (November 1, 2022)
---------------------------------

**User-visible changes:**

The Constant Value Checker supports new annotation `@DoesNotMatchRegex`.

**Closed issues:**

#5238, #5360, #5362, #5387.


Version 3.26.0-eisop1 (October 13, 2022)
----------------------------------------

**Implementation details:**

Documentation improvements and various code fixes.

**Closed issues:**

eisop#333, eisop#348.


Version 3.26.0 (October 3, 2022)
--------------------------------

**User-visible changes:**

The Checker Framework runs under JDK 18 -- that is, it runs on a version 18 JVM.
(It worked before, but gave a warning that it was not tested.)

Annotations are available for some new JDK 17 APIs (some of those
introduced since JDK 11).

Added `-AnoWarnMemoryConstraints` to change the "Memory constraints are impeding
performance; please increase max heap size" message from a warning to a note.

'unneeded.suppression' warnings can now themeselves be suppressed.

**Implementation details:**

Deprecated `TreeUtils.constructor()` in favor of `TreeUtils.elementFromUse()`.

Added method `isSideEffectFree()` to the `AnnotationProvider` interface.

Deprecated `CFAbstractStore.isSideEffectFree()` in favor of new method
`AnnotationProvider.isSideEffectFree()`.  Note the different contracts of
`PurityUtils.isSideEffectFree()` and `AnnotationProvider.isSideEffectFree()`.

Use `TreeUtils.elementFromDeclaration` and `TreeUtils.elementFromUse` in
preference to `TreeUtils.elementFromTree`, when possible.

For code formatting, use `./gradlew spotlessCheck` and `./gradlew spotlessApply`.
The `checkFormat` and `reformat` Gradle tasks have been removed.

Removed variable `BaseTypeVisitor.inferPurity`.

**Closed issues:**

#5081, #5159, #5245, #5302, #5319, #5323.


Version 3.25.0-eisop1 (September 3, 2022)
-----------------------------------------

**User-visible changes:**

The new command-line argument `-AconservativeArgumentNullnessAfterInvocation` improves
the soundness of the Nullness Checker. In previous versions and without supplying the
new flag, the receiver and arguments that are passed to non-null parameters in a method call
or constructor invocation are assumed to be non-null after the invocation.
This assumption is unsound in general, but holds for most code.
Use the new flag to soundly handle the nullness of the receiver and arguments in an invocation.
In a future version, we might change the default for this option.

Support the JSpecify NonNull annotation as an alias in the Nullness Checker.

Fixed ordering of command-line and JDK stubs.

**Closed issues:**

eisop#300, eisop#321.


Version 3.25.0 (September 1, 2022)
----------------------------------

**User-visible changes:**

Make `mustcall.not.inheritable` a warning rather than an error.

The Property File Checker, Internationalization Checker, and Compiler
Message Checker use `File.pathSeparator` to separate property file paths in
`-Apropfiles`, rather than ':'.

Added `DoNothingChecker` that does nothing.

**Closed issues:**

#5216, #5240, #5256, #5273.


Version 3.24.0-eisop1 (August 5, 2022)
--------------------------------------

**User-visible changes:**

Postconditions on the parameters of a constructor are now used at new object creations.


Version 3.24.0 (August 3, 2022)
-------------------------------

**User-visible changes:**

Performance improvements.

Minor bug fixes and enhancements.

**Implementation details:**

Prefer `SystemUtil.jreVersion` to `SystemUtil.getJreVersion()`.

**Closed issues:**

#5200, #5216.


Version 3.23.0-eisop2 (July 22, 2022)
-------------------------------------

**Implementation details:**

Improved defaulting in stub files:
As an extension to the fix for eisop#270, we now allow internally parsing
multiple stub files at the same time. This should make `AnnotatedTypeFactory.getDeclAnnotations`
return the expected declaration annotations for all kinds of elements,
even if it is parsing a different stub file.

**Closed issues:**

eisop#308.


Version 3.23.0-eisop1 (July 14, 2022)
-------------------------------------

**Implementation details:**

Added support for viewpoint adaptation of types via the added
ViewpointAdapter interface. This support is experimental and the API
will change, in particular if the feature is fully integrated with
the DependentTypesHelper.

Improved defaulting in stub files:
Method `AnnotatedTypeFactory.getDeclAnnotations` now returns the
annotations for a package element. Previously, it returned an empty set
when parsing another file. (eisop#270)

Method `CFAbstractTransfer.visitMethodInvocation` now only creates a
`ConditionalTransferResult` when the method return type is boolean or
Boolean. This avoids unnecessary duplication of many stores, reducing
memory consumption.

Improved the CFG type of implicit this receivers. (typetools#5174)

**Closed issues:**

eisop#270, eisop#281, typetools#5174, typetools#5189.


Version 3.23.0 (July 11, 2022)
------------------------------

**User-visible changes:**

By default, command-line argument `-AstubWarnIfNotFound` is treated as true
for stub files provided on the command line and false for built-in stub
files.  Use `-AstubWarnIfNotFound` to enable it for all stub files, and use
new `-AstubNoWarnIfNotFound` to disable it for all stub files.

New command-line argument `-ApermitStaticOwning` suppresses Resource Leak
Checker warnings related to static owning fields.

New command-line argument `-ApermitInitializationLeak` suppresses Resource Leak
Checker warnings related to field initialization.

**Closed issues:**

#4855, #5151, #5166, #5172, #5175, #5181, #5189.


Version 3.22.2 (June 14, 2022)
------------------------------

**Implementation details:**

Expose CFG APIs to allow inserting jumps and throws.


Version 3.22.1-eisop1 (June 3, 2022)
------------------------------------

**User-visible changes:**

Type parameters with explicit j.l.Object upper bounds and
unannotated, unbounded wildcards now behave the same in .astub
files and in .java files.

**Implementation details:**

In `PropagationTreeAnnotator.visitBinary`, we now consider the two cases where
the resulting Java type of a binary operation can be different from the operands'
types: string concatenation and binary comparison. We apply the declaration
bounds of the resulting Java type to ensure annotations in the ATM are valid.

Deprecated `AnnotatedTypeFactory.binaryTreeArgTypes(AnnotatedTypeMirror, AnnotatedTypeMirror)` in favor of
`AnnotatedTypeFactory.binaryTreeArgTypes(BinaryTree)` and
`AnnotatedTypeFactory.compoundAssignmentTreeArgTypes(CompoundAssignmentTree)`.

**Closed issues:**

typetools#3025, typetools#3030, typetools#3236.

Test cases for issues that already pass:
typetools#2722, typetools#2995, typetools#3015, typetools#3027.

typetools#58 was closed in error. See
https://github.com/eisop/checker-framework/issues/242
for follow-up discussions.


Version 3.22.1 (June 1, 2022)
-----------------------------

**Closed issues:**
#58, #5136, #5138, #5142, #5143.


Version 3.22.0-eisop1 (May 6, 2022)
-----------------------------------

**User-visible changes:**

Added reaching definitions and very busy expressions analysis demos.

**Implementation details:**

Fixed the types of `MethodInvocationNode#arguments` and
`ObjectCreationNode#arguments` in CFGs. Previously, argument nodes are created
using the types from the method declaration, which means some nodes are using
type variables that are not substituted by type arguments at the call site.
For example, we used to observe `new T[]{"a", "b"}` instead of
`new String[]{"a", "b"}`, while the second one makes more sense.

Added a new gradle task `fastAssemble` to quickly rebuild the Checker
Framework for local development. This command will assemble the jar
files without generating any Javadoc or sources.jar files, thus it is
faster than the gradle assemble task.

Type system test drivers no longer need to pass `-Anomsgtext`.
The Checker Framework test driver (in `TypecheckExecutor.compile`) now always
passes the `-Anomsgtext` option.

Moved the `-AajavaChecks` option from `CheckerFrameworkPerDirectoryTest` to
`TypecheckExecutor.compile` to ensure the option is used for all tests.

**Closed issues:**
eisop#210, eisop#215.


Version 3.22.0 (May 2, 2022)
----------------------------

**User-visible changes:**

The Signedness Checker now checks calls to `equals()` as well as to `==`.  When
two formal parameter types are annotated with @PolySigned, the two arguments at
a call site must have the same signedness type annotation. (This differs from
the standard rule for polymorphic qualifiers.)

**Implementation details:**

When passed a NewClassTree that creates an anonymous constructor,
AnnotatedTypeFactory#constructorFormUse now returns the type of the anonymous
constructor rather than the type of the super constructor invoked in the
anonymous classes constructor.  If the super constructor has explicit
annotations, they are copied to the anonymous classes constructor.

**Closed issues:**
#5113.


Version 3.21.4-eisop1 (April 4, 2022)
-------------------------------------

**Closed issues:**
eisop#199, eisop#204.


Version 3.21.4 (April 1, 2022)
------------------------------

**Closed issues:**
#5086.


Version 3.21.3-eisop1 (March 23, 2022)
--------------------------------------

**User-visible changes:**

If you supply the new `-AjspecifyNullMarkedAlias=false` command-line
option, then the Nullness Checker will not treat
`org.jspecify.nullness.NullMarked` as a defaulting annotation.
By default the `NullMarked` annotation continues to be recognized.

**Implementation details:**

Changed `AnnotatedTypeFactory.initializeAtm` from public to package
private visibility. Nobody outside the package should call this method.

Changed `CFAbstractTransfer.insertIntoStores` from public to protected
visibility. It is only meant as a utility method for use within a
transfer function.

Deprecated class `StringConcatenateAssignmentNode` and its usages.
String concatenate assignments are now desugared to an assignment and
a concatenation node instead.
This avoids error prone duplication of logic.

**Closed issues:**
typetools#5075.


Version 3.21.3 (March 1, 2022)
------------------------------

**Closed issues:**
#2847, #4965, #5039, #5042, #5047.


Version 3.21.2-eisop1 (February 2, 2022)
----------------------------------------

**User-visible changes:**

Improved support for `NullMarked` default annotation.

`DefaultQualifier` supports the new `applyToSubpackages` annotation attribute
to decide whether a default should also apply to subpackages. To preserve the
current behavior the default is `true`.

**Implementation details:**

Moved files AnnotationFormatter.java and DefaultAnnotationFormatter.java from
javacutil/src/main/java/org/checkerframework/javacutil/ to
framework/src/main/java/org/checkerframework/framework/util/.
typetools PR 3821 incorrectly moved these files, without adapting their
packages, leading to framework classes in javacutil.
The AnnotationFormatter depends on the InvisibleQualifier framework
annotation, so should be in that project.
Added additional toStringSimple methods to AnnotationUtils to format
AnnotationMirrors without depending on the framework project.

AnnotatedTypeFactory: removed field `artificialTreeToEnclosingElementMap` and
final methods `getEnclosingElementForArtificialTree` and
`setEnclosingElementForArtificialTree`. The new final method
`setPathForArtificialTree` is used by `CFCFGBuilder` to update the mapping. Now
all trees, including artificial trees, have a correct path and enclosing
element.

Dataflow Framework: new `ExpressionStatementNode` marks an expression that is
used as a statement.

To correctly handle ternary expressions, support synthetic AssignmentNodes that
do not merge stores. These synthetic assignments are used for the assignments
to the synthetic variables in a ternary expression.
(typetools PR #5000 48f2652bc8bf4801b2e750cd92325583939f2f52 added synthetic
variables for ternary expressions to the CFG. This broke how the Nullness
Checker handles ternary expressions, leading to false positives.)

**Closed issues:**
typetools#3281.


Version 3.21.2 (February 1, 2022)
---------------------------------

**User-visible changes:**

The `wpi.sh` script supports non-standard names for build system compile targets
via the new `-c` command-line option.

The Checker Framework now more precisely computes and checks the type of the
pattern variable in a pattern match instanceof.

**Implementation details:**

Deprecated CFGLambda.getMethod{Name} in favor of getEnclosingMethod{Name}.

**Closed issues:**
#4615, #4993, #5006, #5007, #5008, #5013, #5016, #5021.


Version 3.21.1 (January 7, 2022)
--------------------------------

**User-visible changes:**

The Checker Framework Gradle Plugin now works incrementally:  if you change just
one source file, then Gradle will recompile just that file rather than all
files.

**Closed issues:**
#2401, #4994, #4995, #4996.


Version 3.21.0 (December 17, 2021)
----------------------------------

**User-visible changes:**

The Checker Framework now more precisely computes the type of a switch expression.

**Implementation details:**

The Dataflow Framework now analyzes switch expressions and switch statements
that use the new `->` case syntax. To do so, a new node, SwitchExpressionNode,
was added.

**Closed issues:**
#2373, #4934, #4977, #4979, #4987.


Version 3.20.0 (December 6, 2021)
---------------------------------

**User-visible changes:**

The Checker Framework now runs on code that contains switch expressions and
switch statements that use the new `->` case syntax, but treats them
conservatively. A future version will improve precision.

**Implementation details:**

The Dataflow Framework can be run on code that contains switch expressions and
switch statements that use the new `->` case syntax, but it does not yet
analyze the cases in a switch expression and it treats `->` as `:`. A future
version will do so.

Removed methods and classes that have been deprecated for more than one year:
 * Old way of constructing qualifier hierarchies
 * `@SuppressWarningsKeys`
 * `RegularBlock.getContents()`
 * `TestUtilities.testBooleanProperty()`
 * `CFAbstractTransfer.getValueWithSameAnnotations()`

**Closed issues:**
#4911, #4948, #4965.


Version 3.19.0-eisop1 (November 4, 2021)
----------------------------------------

**User-visible changes:**

Avoid shading of string literals which broke some annotation aliasing.
Add more nullness annotation aliases.

**Implementation details:**

Remove the unsound "BOTH-TO-THEN", "BOTH-TO-ELSE" logic from the Dataflow
Framework.

Small improvements and code-style clean-ups in the Dataflow Framework and
in the core Checker Framework "framework" package.

**Closed issues:**
eisop#121, typetools#4923.


Version 3.19.0 (November 1, 2021)
---------------------------------

**User-visible changes:**

The Checker Framework runs under JDK 17 -- that is, it runs on a version 17 JVM.
The Checker Framework also continues to run under JDK 8 and JDK 11.  New
command-line argument `-ApermitUnsupportedJdkVersion` lets you run the Checker
Framework on any JDK (version 8 or greater) without a warning about an
unsupported JDK version.  The Checker Framework does not yet run on code that
contains switch expressions.

**Implementation details:**

Removed `org.checkerframework.framework.type.VisitorState`
Removed `AnnotatedTypeFactory#postTypeVarSubstitution`

Deprecated methods in AnnotatedTypeFactory:
* `getCurrentClassTree`
* `getCurrentMethodReceiver`

**Closed issues:**
#4932, #4924, #4908, #3014.


Version 3.18.1-eisop1 (October 7, 2021)
---------------------------------------

**User-visible changes:**

Add more aliases for nullness annotations; fix manual formatting (#105).


Version 3.18.1 (October 4, 2021)
--------------------------------

**Closed issues:**
#4902 and #4903.


Version 3.18.0-eisop1 (September 23, 2021)
------------------------------------------

The new `-AnoJreVersionCheck` command-line argument can be used to not get
a warning about running the Checker Framework on an unsupported JRE version.

JAR files are minimized to only include required classes.

Temporarily remove support for "Whole Program Inference" - the -Ainfer option and
related scripts.

**Implementation details:**

Changes to `AnnotatedTypeMirror`:
 * Rename `clearPrimaryAnnotations()` back to `clearAnnotations()` to be consistent
   with other method names. Undoes change in typetools 3.16.0.
 * Remove `getAnnotation()` method. `getAnnotationInHierarchy` should be used instead.
   Undoes change in typetools #3691.


Version 3.18.0 (September 1, 2021)
----------------------------------

**User-visible changes:**

Java records are type-checked.  Thanks to Neil Brown.

**Closed issues:**
#4838, #4843, #4852, #4853, #4861, #4876, #4877, #4878, #4878, #4889, #4889.


Version 3.17.0 (August 3, 2021)
-------------------------------

**User-visible changes:**

`-Ainfer` can now infer postcondition annotations that reference formal parameters
(e.g. `"#1"`, `"#2"`) and the receiver (`"this"`).

**Implementation details:**

Method renamings and signature changes (old methods are removed) in `GenericAnnotatedTypeFactory`:
* `getPreconditionAnnotation(VariableElement, AnnotatedTypeMirror)` => `getPreconditionAnnotations(String, AnnotatedTypeMirror, AnnotatedTypeMirror)`
* `getPostconditionAnnotation(VariableElement, AnnotatedTypeMirror, List<AnnotationMirror>)` => `getPostconditionAnnotations(String, AnnotatedTypeMirror, AnnotatedTypeMirror, List<AnnotationMirror>)`
* `getPreOrPostconditionAnnotation(VariableElement, AnnotatedTypeMirror, Analysis.BeforeOrAfter, List<AnnotationMirror>)` => `getPreOrPostconditionAnnotations(String, AnnotatedTypeMirror, AnnotatedTypeMirror, Analysis.BeforeOrAfter, List<AnnotationMirror>)`
* `requiresOrEnsuresQualifierAnno(VariableElement, AnnotationMirror, Analysis.BeforeOrAfter)` => `createRequiresOrEnsuresQualifier(String, AnnotationMirror, AnnotatedTypeMirror, Analysis.BeforeOrAfter, List<AnnotationMirror>)`

Method renamings and signature changes (old method is removed) in `WholeProgramInferenceStorage`:
* `getPreOrPostconditionsForField(Analysis.BeforeOrAfter, ExecutableElement, VariableElement, AnnotatedTypeFactory)` =>  `getPreOrPostconditions(Analysis.BeforeOrAfter, ExecutableElement, String, AnnotatedTypeMirror, AnnotatedTypeFactory)`

Method renamings:
 * `CFAbstractAnalysis.getFieldValues` => `getFieldInitialValues`

The following methods no longer take a `fieldValues` parameter:
 * `GenericAnnotatedTypeFactory#createFlowAnalysis`
 * `CFAnalysis` construtor
 * `CFAbstractAnalysis#performAnalysis`
 * `CFAbstractAnalysis` constructors

**Closed issues:**
#4685, #4689, #4785, #4805, #4806, #4815, #4829, #4849.


Version 3.16.0 (July 13, 2021)
------------------------------

**User-visible changes:**

You can run the Checker Framework on a JDK 16 JVM.  You can pass the `--release
16` command-line argument to the compiler.  You may need to add additional
command-line options, such as `--add-opens`; see the Checker Framework manual.
New syntax, such as records and switch expressions, is not yet supported or
type-checked; that will be added in a future release.  Thanks to Neil Brown for
the JDK 16 support.

The Lock Checker supports a new type, `@NewObject`, for the result of a
constructor invocation.

The `-Ainfer` command-line argument now outputs purity annotations even if
neither `-AsuggestPureMethods` nor `-AcheckPurityAnnotations` is supplied
on the command line.

**Implementation details:**

Method renamings (the old methods remain but are deprecated):
 * `AnnotationFileElementTypes.getDeclAnnotation` => `getDeclAnnotations`

Method renamings (the old methods were removed):
 * `AnnotatedTypeMirror.clearAnnotations => `clearPrimaryAnnotations`

Method renamings in `DefaultTypeHierarchy` (the old methods were removed):
 * `visitIntersectionSupertype` => `visitIntersectionSupertype`
 * `visitIntersectionSubtype` => `visitIntersection_Type`
 * `visitUnionSubtype` => `visitUnion_Type`
 * `visitTypevarSubtype` => `visitTypevar_Type`
 * `visitTypevarSupertype` => `visitType_Typevar`
 * `visitWildcardSubtype` => `visitWildcard_Type`
 * `visitWildcardSupertype` => `visitType_Wildcard`

Method renamings in `AnnotatedTypes` (the old methods were removed):
 * `expandVarArgs` => `expandVarArgsParameters`
 * `expandVarArgsFromTypes` => `expandVarArgsParametersFromTypes`

**Closed issues:**
#3013, #3754, #3791, #3845, #4523, #4767.

Version 3.15.0 (June 18, 2021)
----------------------------

**User-visible changes:**

The Resource Leak Checker ensures that certain methods are called on an
object before it is de-allocated. By default, it enforces that `close()` is
called on any expression whose compile-time type implements `java.io.Closeable`.

**Implementation details:**

Method renamings (the old methods remain but are deprecated):
 * `AnnotatedDeclaredType#wasRaw` => `isUnderlyingTypeRaw`
 * `AnnotatedDeclaredType#setWasRaw` => `setIsUnderlyingTypeRaw`

**Closed issues:**
#4549, #4646, #4684, and #4699.


Version 3.14.0 (June 1, 2021)
----------------------------

**User-visible changes:**

The Units Checker supports new qualifiers (thanks to Rene Kraneis):
 * `@Volume`, `@m3`, `@mm3`, `@km3`
 * `@Force`, `@N`, `@kN`
 * `@t` (metric ton, a unit of mass)

Stub files can now override declaration annotations in the annotated JDK.
Previously, stub files only overrode type annotations in the annotated JDK.

Command-line argument `-AstubWarnIfNotFound` is treated as true for stub
files provided on the command line.

**Implementation details:**

Method `SourceChecker.getProperties` takes a third formal parameter `permitNonExisting`.

Method `TreeUtils.getMethodName()` returns a `String` rather than a `Name`.

Removed CheckerDevelMain.

**Closed issues:**
#3993, #4116, #4586, #4598, #4612, #4614.


Version 3.13.0 (May 3, 2021)
----------------------------

**Survey:**

If you use the Checker Framework, please answer a 3-question survey about what
version of Java you use.  It will take less than 1 minute to complete.  Please
answer it at
<https://docs.google.com/forms/d/1Bbt34c_3nDItHsBnmEfumoyrR-Zxhvo3VTHucXwfMcQ>.
Thanks!

**User-visible changes:**

Command-line argument -AassumeKeyFor makes the Nullness Checker and Map Key
Checker unsoundly assume that the argument to `Map.get` is a key for the
receiver map.

Not included in eisop:
Warning message keys are shorter.  This reduces clutter in error messages and in
`@SuppressWarnings` annotations.  Most ".type.invalid", ".type.incompatible",
".invalid", and ".not.satisfied" suffixes and "type.invalid." prefixes have been
removed, and most ".invalid." substrings have been changed to ".".

The Checker Framework no longer crashes on code that contains binding
variables (introduced in Java 14 for `instanceof` pattern matching), and
such variables are reflected in the control flow graph (CFG).  Thanks to
Chris Day for this change.  However, note that the Checker Framework only
has full support for Java 8 and Java 11.

New command-line argument `-AstubWarnNote` makes stub file warnings notes
rather than warnings.

Removed the StubGenerator section from the manual, because changes in JDK 11
have broken the StubGenerator program.

**Implementation details:**

Method renamings:
 * `DependentTypesHelper.atReturnType` => `atMethodBody`

**Closed issues:**
#1268, #3039, #4410, #4550, #4558, #4563, #4566, #4567, #4571, #4584, #4591,
#4594, #4600.


Version 3.12.0 (April 1, 2021)
------------------------------

**User-visible changes:**

New FAQ item "How should I annotate code that uses generics?" gives
examples of annotations on type variables, together with their meaning.

`-Ainfer=ajava` uses ajava files (rather than jaif files or stub files)
internally during whole-program inference.

The Optional Checker supports a new annotation `@OptionalBottom` that
stands for (only) the `null` value.

The `value` element/argument to `@EnumVal` is now required.  Previously it
defaulted to an empty array.

**Implementation details:**

A precondition or normal postcondition annotation's `value` element must have
type `String[]`, not `String`.  A conditional postcondition annotation's
`expression` element must have type `String[]`, not `String`.  These changes
will not affect users (any programmer-written annotation that was legal before
will still be legal), but it may affect checker implementations.

`JavaExpressionParseUtil`:
`JavaExpressionParseUtil#parse` no longer viewpoint-adapts Java expressions. It
just converts the expression `String` to a `JavaExpression`.  To that end,
`JavaExpressionParseUtil.JavaExpressionContext` has been removed and
`JavaExpressionParseUtil#parse` no longer takes a context object.  Most calls to
`JavaExpressionParseUtil#parse` should be replaced with a call to one of the
methods in `StringToJavaExpressions`.

Renamed `AnnotatedTypeComparer` to `DoubleAnnotatedTypeScanner`. In the new
class, the method `compare` was renamed `defaultAction`. The method `combineRs`
was replaced by `reduce`.

Removed methods:
 * `AnnotationUtils.getElementValueArrayOrSingleton`
 * `DependentTypesHelper.standardizeNewClassTree`: use `atExpression` instead
 * `DependentTypesHelper.standardizeString`: override one of the methods
   explained in the Javadoc of `convertAnnotationMirror`

Method renamings:
 * `DefaultQualifierForUseTypeAnnotator.getSupportAnnosFromDefaultQualifierForUses` => `getDefaultQualifierForUses`
 * In `DependentTypesHelper`:
    * `check*` => `check*ForErrorExpressions`
    * `viewpointAdaptConstructor` => `atConstructorInvocation`
    * `viewpointAdaptMethod` => `atMethodInvocation`
    * `viewpointAdaptTypeVariableBounds` => `atParameterizedTypeUse`
    * `standardizeClass` =>  `atTypeDecl`
    * `standardizeExpression` => `atExpression`
    * `standardizeFieldAccess` => `atFieldAccess`
    * `standardizeReturnType` => `atReturnType`
    * `standardizeVariable` => `atVariableDeclaration`

Deprecated some overloads in `AnnotationUtils` that take a `CharSequence`
(use an overload that takes an `ExecutablElement`):
 * `getElementValueArray`
 * `getElementValueClassName`
 * `getElementValueClassNames`
 * `getElementValueEnumArray`
 * `getElementValueEnum`
 * `getElementValue`
 * `getElementValuesWithDefaults`

Deprecated methods in `AnnotationUtils`:
 * `areSameByClass`: use `areSameByName`
 * `getElementValuesWithDefaults`: use a `getElementValue*` method

Removed deprecated `PluginUtil` class.

**Closed issues:**
#1376, #3740, #3970, #4041, #4254, #4346, #4355, #4358, #4372, #4381, #4384,
#4417, #4449, #4452, #4480.


Version 3.11.0 (March 1, 2021)
------------------------------

**User-visible changes:**

In a stub file for a class C, you may write a declaration for a method that is
inherited by C but not defined by it.  Previously, such stub declarations were
ignored.  For more information, see the manual's documentation of "fake
overrides".

Nullness Checker error message key changes:
 * `known.nonnull` => `nulltest.redundant`
 * `initialization.static.fields.uninitialized` => `initialization.static.field.uninitialized`,
   and it is now issued on the field rather than on the class
 * new `initialization.field.uninitialized` is issued on the field instead of
   `initialization.fields.uninitialized` on the class, if there is no
   explicitly-written constructor.

Signature Checker supports two new type qualifiers:
 * `@CanonicalNameAndBinaryName`
 * `@CanonicalNameOrPrimitiveType`

**Implementation details:**

You can make a variable's default type depend on its name, or a method
return type default depend on the method's name.  To support this feature,
`@DefaultFor` has new elements `names` and `namesExceptions`.

Changes to protected fields in `OverrideChecker`:
 * Removed `overriderMeth`, `overriderTyp`, `overriddenMeth`, `overriddenTyp`
 * Renamed `methodReference` => `isMethodReference`
 * Renamed `overridingType` => `overriderType`
 * Renamed `overridingReturnType` => `overriderReturnType`

Changes to JavaExpression parsing:
 * The signatures of these methods changed; see Javadoc.
    * `JavaExpressionParseUtil#parse`
    * `DependentTypesHelper#standardizeString`
 * These methods moved:
    * `GenericAnnotatedTypeFactory#standardizeAnnotationFromContract` => `DependentTypesHelper`
    * `JavaExpressionParseUtil#fromVariableTree` => `JavaExpression`

Changes to JavaExpressionContext:
 * New method JavaExpressionContext#buildContextForMethodDeclaration(MethodTree, SourceChecker)
   replaces all overloads of buildContextForMethodDeclaration.

Parsing a Java expression no longer requires the formal parameters
`AnnotationProvider provider` or `boolean allowNonDeterministic`.  Methods
in `JavaExpression` with simplified signatures include
 * `fromArrayAccess`
 * `fromNodeFieldAccess`
 * `fromNode`
 * `fromTree`
 * `getParametersOfEnclosingMethod`
 * `getReceiver`

`CFAbstractStore.insertValue` does nothing if passed a nondeterministic
expression.  Use new method `CFAbstractStore.insertValuePermitNondeterministic`
to map a nondeterministic expression to a value.

**Closed issues:**
#862, #3631, #3991, #4031, #4206, #4207, #4226, #4231, #4248, #4263, #4265,
#4279, #4286, #4289.


Version 3.10.0 (February 1, 2021)
---------------------------------

**User-visible changes:**

Moved utility classes from `checker-qual.jar` to the new `checker-util.jar`.
Also, added `util` to the end of all the packages of the utility classes.

In Maven Central, `checker.jar` no longer contains duplicates of qualifiers in
`checker-qual.jar`, but rather uses a Maven dependency. A fat jar file with all
the dependencies (like the old `checker.jar`) is available in Maven Central with
the classifier "all".

When supplying the `-Ainfer=...` command-line argument, you must also supply `-Awarns`.

Replaced several error message keys:
 * `contracts.precondition.expression.parameter.name`
 * `contracts.postcondition.expression.parameter.name`
 * `contracts.conditional.postcondition.expression.parameter.name`
 * `method.declaration.expression.parameter.name`
by new message keys:
 * `expression.parameter.name.invalid`
 * `expression.parameter.name.shadows.field`

**Implementation details:**

Deprecated `ElementUtils.enclosingClass`; use `ElementUtils.enclosingTypeElement`.

Removed classes (use `SourceChecker` instead):
 * `BaseTypeContext`
 * `CFContext`
 * `BaseContext`

Removed methods:
 * `SourceChecker.getContext()`: it returned the receiver
 * `SourceChecker.getChecker()`: it returned the receiver
 * `AnnotatedTypeFactory.getContext()`: use `getChecker()`
 * methods on `TreePath`s from class 'TreeUtils`; use the versions in `TreePathUtil`.

Moved class:
 * org.checkerframework.framework.util.PurityUnqualified to
   org.checkerframework.framework.qual.PurityUnqualified

Renamed methods:
 * `AnnotatedTypeMirror.directSuperTypes` => `directSupertypes` (note
   capitalization) for consistency with `javax.lang.model.util.Types`
 * `AnnotatedTypeMirror.removeAnnotation(Class)` => `removeAnnotationByClass`
 * `MethodCall.getParameters` => `getArguments`
 * `MethodCall.containsSyntacticEqualParameter` => `containsSyntacticEqualArgument`
 * `ArrayAccess.getReceiver` => `getArray`

**Closed issues:**
#3325 , #3474.


Version 3.9.1 (January 13, 2021)
--------------------------------

**Implementation details:**

Copied methods on `TreePath`s from class 'TreeUtils` to new class `TreePathUtil`.
(The methods in TreePath will be deleted in the next release.)
 * `TreeUtils.enclosingClass` => `TreePathUtil.enclosingClass`
 * `TreeUtils.enclosingMethod` => `TreePathUtil.enclosingMethod`
 * `TreeUtils.enclosingMethodOrLambda` => `TreePathUtil.enclosingMethodOrLambda`
 * `TreeUtils.enclosingNonParen` => `TreePathUtil.enclosingNonParen`
 * `TreeUtils.enclosingOfClass` => `TreePathUtil.enclosingOfClass`
 * `TreeUtils.enclosingOfKind` => `TreePathUtil.enclosingOfKind`
 * `TreeUtils.enclosingTopLevelBlock` => `TreePathUtil.enclosingTopLevelBlock`
 * `TreeUtils.enclosingVariable` => `TreePathUtil.enclosingVariable`
 * `TreeUtils.getAssignmentContext` => `TreePathUtil.getAssignmentContext`
 * `TreeUtils.inConstructor` => `TreePathUtil.inConstructor`
 * `TreeUtils.isTreeInStaticScope` => `TreePathUtil.isTreeInStaticScope`
 * `TreeUtils.pathTillClass` => `TreePathUtil.pathTillClass`
 * `TreeUtils.pathTillOfKind` => `TreePathUtil.pathTillOfKind`

**Closed issues:**
#789, #3202, #4071, #4083, #4114, #4115.


Version 3.9.0 (January 4, 2021)
-------------------------------

**User-visible changes:**

New scripts `checker/bin/wpi.sh` and `checker/bin/wpi-many.sh` run whole-program
inference, without modifying the source code of the target programs.

The `-Ainfer` command-line argument now infers
 * method preconditions (`@RequiresQualifiers`, `@RequiresNonNull`)
 * method postconditions (`@EnsuresQualifiers`, `@EnsuresNonNull`)
 * `@MonotonicNonNull`

The Called Methods Checker supports the -AdisableReturnsReceiver command-line option.

The Format String Checker recognizes Error Prone's `@FormatMethod` annotation.

Use of `@SuppressWarnings("fbc")` to suppress initialization warnings is deprecated.

**Implementation details:**

Class renamings:
 * `StubParser` => `AnnotationFileParser`
 * `Receiver` => `JavaExpression`
   Also related class and method renamings, such as
    * `FlowExpressions.internalReprOf` => `JavaExpressions.fromNode`
 * In the Dataflow Framework:
    * `ThisLiteralNode` => `ThisNode`
    * `ExplicitThisLiteralNode` => `ExplicitThisNode`
    * `ImplicitThisLiteralNode` => `ImplicitThisNode`

Method deprecations:
 * Deprecated `AnnotatedTypeFactory.addAliasedAnnotation`; use `addAliasedTypeAnnotation`

**Closed issues:**
#765, #2452, #2953, #3377, #3496, #3499, #3826, #3956, #3971, #3974, #3994,
#4004, #4005, #4018, #4032, #4068, #4070.


Version 3.8.0 (December 1, 2020)
--------------------------------

**User-visible changes:**

The Initialized Fields Checker warns when a field is not initialized by a
constructor.  This is more general than the Initialization Checker, which
only checks that `@NonNull` fields are initialized.

The manual describes how to modify an sbt build file to run the Checker
Framework.

The -AwarnUnneededSuppressions command-line option warns only about
suppression strings that contain a checker name.

The -AwarnUnneededSuppressionsExceptions=REGEX command-line option
partially disables -AwarnUnneededSuppressions.  Most users don't need this.

**Implementation details:**

Added classes `SubtypeIsSubsetQualifierHierarchy` and
`SubtypeIsSupersetQualifierHierarchy`.

Moved the `contractsUtils` field from the visitor to the type factory.

Class renamings:
 * `ContractsUtils` => `ContractsFromMethod`

Method renamings:
 * `ElementUtils.getVerboseName` => `ElementUtils.getQualifiedName`
 * `ElementUtils.getSimpleName` => `ElementUtils.getSimpleSignature`

Field renamings:
 * `AnnotatedTypeMirror.actualType` => `AnnotatedTypeMirror.underlyingType`

Added a formal parameter to methods in `MostlyNoElementQualifierHierarchy`:
 * `leastUpperBoundWithElements`
 * `greatestLowerBoundWithElements`

Removed a formal parameter from methods in `BaseTypeVisitor`:
 * `checkPostcondition`
 * `checkConditionalPostcondition`

In `Analysis.runAnalysisFor()`, changed `boolean` parameter to enum `BeforeOrAfter`.

Removed `org.checkerframework.framework.util.AnnotatedTypes#getIteratedType`; use
`AnnotatedTypeFactory#getIterableElementType(ExpressionTree)` instead.

**Closed issues:**
#3287, #3390, #3681, #3839, #3850, #3851, #3862, #3871, #3884, #3888, #3908,
#3929, #3932, #3935.


Version 3.7.1 (November 2, 2020)
--------------------------------

**User-visible changes:**

The Constant Value Checker supports two new annotations: @EnumVal and @MatchesRegex.

The Nullness Checker supports annotation org.jspecify.annotations.NullnessUnspecified.

**Implementation details:**

AnnotatedIntersectionType#directSuperTypes now returns
List<? extends AnnotatedTypeMirror>.

The @RelevantJavaTypes annotation is now enforced:  a checker issues a warning
if the programmer writes a type annotation on a type that is not listed.

Deprecated CFAbstractTransfer.getValueWithSameAnnotations(), which is no
longer used.  Added new methods getWidenedValue() and getNarrowedValue().

Renamed TestUtilities.assertResultsAreValid() to TestUtilities.assertTestDidNotFail().

Renamed BaseTypeValidator.isValidType() to BaseTypeValidator.isValidStructurally().

New method BaseTypeVisitor#visitAnnotatedType(List, Tree) centralizes checking
of user-written type annotations, even when parsed in declaration locations.

**Closed issues:**
#868, #1908, #2075, #3349, #3362, #3569, #3614, #3637, #3709, #3710, #3711,
#3720, #3730, #3742, #3760, #3770, #3775, #3776, #3792, #3793, #3794, #3819,
#3831.


Version 3.7.0 (October 1, 2020)
-------------------------------

**User-visible changes:**

The new Called Methods Checker tracks methods that have definitely been
called on an object. It automatically supports detecting mis-uses of the
builder pattern in code that uses Lombok or AutoValue.

Accumulation analysis is now supported via a generic Accumulation Checker.
An accumulation analysis is a restricted form of typestate analysis that does
not require a precise alias analysis for soundness. The Called Methods Checker
is an accumulation analysis.

The Nullness Checker supports annotations
org.codehaus.commons.nullanalysis.NotNull,
org.codehaus.commons.nullanalysis.Nullable, and
org.jspecify.annotations.Nullable.

The Signature Checker supports annotations @CanonicalName and @CanonicalNameOrEmpty.
The Signature Checker treats jdk.jfr.Unsigned as an alias for its own @Unsigned annotation.

The shorthand syntax for the -processor command-line argument applies to
utility checkers, such as the Constant Value Checker.

**Implementation details:**

A checker implementation may override AnnotatedTypeFactory.getWidenedAnnotations
to provide special behavior for primitive widening conversions.

Deprecated org.checkerframework.framework.util.MultiGraphQualifierHierarchy and
org.checkerframework.framework.util.GraphQualifierHierarchy.  Removed
AnnotatedTypeFactory#createQualifierHierarchy(MultiGraphFactory) and
AnnotatedTypeFactory#createQualifierHierarchyFactory.  See Javadoc of
MultiGraphQualifierHierarchy for instructions on how to use the new classes and
methods.

Renamed methods:
 * NumberUtils.isFloatingPoint => TypesUtils.isFloatingPoint
 * NumberUtils.isIntegral => TypesUtils.isIntegralPrimitiveOrBoxed
 * NumberUtils.isPrimitiveFloatingPoint => TypeKindUtils.isFloatingPoint
 * NumberUtils.isPrimitiveIntegral => TypeKindUtils.isIntegral
 * NumberUtils.unboxPrimitive => TypeKindUtils.primitiveOrBoxedToTypeKind
 * TypeKindUtils.widenedNumericType => TypeKindUtils.widenedNumericType
 * TypesUtils.isFloating => TypesUtils.isFloatingPrimitive
 * TypesUtils.isIntegral => TypesUtils.isIntegralPrimitive

The CFStore copy constructor now takes only one argument.

**Closed issues:**
#352, #354, #553, #722, #762, #2208, #2239, #3033, #3105, #3266, #3275, #3408,
#3561, #3616, #3619, #3622, #3625, #3630, #3632, #3648, #3650, #3667, #3668,
#3669, #3700, #3701.


Version 3.6.1 (September 2, 2020)
---------------------------------

Documented that the Checker Framework can issue false positive warnings in
dead code.

Documented when the Signedness Checker permits right shift operations.

**Closed issues:**
#3484, #3562, #3565, #3566, #3570, #3584, #3594, #3597, #3598.


Version 3.6.0 (August 3, 2020)
------------------------------

**User-visible changes:**

The Interning Checker supports method annotations @EqualsMethod and
@CompareToMethod.  Place them on methods like equals(), compareTo(), and
compare() to permit certain uses of == on non-interned values.

Added an overloaded version of NullnessUtil.castNonNull that takes an error message.

Added a new option `-Aversion` to print the version of the Checker Framework.

New CFGVisualizeLauncher command-line arguments:
 * `--outputdir`: directory in which to write output files
 * `--string`: print the control flow graph in the terminal
All CFGVisualizeLauncher command-line arguments now start with `--` instead of `-`.

**Implementation details:**

`commonAssignmentCheck()` now takes an additional argument.  Type system
authors must update their overriding implementations.

Renamed methods:
 * GenericAnnotatedTypeFactory#addAnnotationsFromDefaultQualifierForUse => #addAnnotationsFromDefaultForType and
 * BaseTypeValidator#shouldCheckTopLevelDeclaredType => #shouldCheckTopLevelDeclaredOrPrimitiveType

Removed org.checkerframework.framework.test.FrameworkPer(Directory/File)Test classes.
Use CheckerFrameworkPer(Directory/File)Test instead.

**Closed issues:**

#1395, #2483, #3207, #3223, #3224, #3313, #3381, #3422, #3424, #3428, #3429,
#3438, #3442, #3443, #3447, #3449, #3461, #3482, #3485, #3495, #3500, #3528.


Version 3.5.0 (July 1, 2020)
----------------------------

**User-visible changes:**

Use "allcheckers:" instead of "all:" as a prefix in a warning suppression string.
Writing `@SuppressWarnings("allcheckers")` means the same thing as
`@SuppressWarnings("all")`, unless the `-ArequirePrefixInWarningSuppressions`
command-line argument is supplied.  See the manual for details.

It is no longer necessary to pass -Astubs=checker.jar/javadoc.astub when
compiling a program that uses Javadoc classes.

Renamed command-line arguments:
 * -AshowSuppressWarningKeys to -AshowSuppressWarningsStrings

The Signature Checker no longer considers Java keywords to be identifiers.
Renamed Signature Checker annotations:
 * @BinaryNameInUnnamedPackage => @BinaryNameWithoutPackage
 * @FieldDescriptorForPrimitiveOrArrayInUnnamedPackage => @FieldDescriptorWithoutPackage
 * @IdentifierOrArray => @ArrayWithoutPackage
Added new Signature Checker annotations:
 * @BinaryNameOrPrimitiveType
 * @DotSeparatedIdentifiersOrPrimitiveType
 * @IdentifierOrPrimitiveType

The Nullness Checker now treats `System.getProperty()` soundly.  Use
`-Alint=permitClearProperty` to disable special treatment of
`System.getProperty()` and to permit undefining built-in system properties.

Class qualifier parameters:  When a generic class represents a collection,
a user can write a type qualifier on the type argument, as in
`List<@Tainted Character>` versus `List<@Untainted Character>`.  When a
non-generic class represents a collection with a hard-coded type (as
`StringBuffer` hard-codes `Character`), you can use the new class qualifier
parameter feature to distinguish `StringBuffer`s that contain different
types of characters.

The Dataflow Framework supports backward analysis.  See its manual.

**Implementation details:**

Changed the types of some fields and methods from array to List:
 * QualifierDefaults.validLocationsForUncheckedCodeDefaults()
 * QualifierDefaults.STANDARD_CLIMB_DEFAULTS_TOP
 * QualifierDefaults.STANDARD_CLIMB_DEFAULTS_BOTTOM
 * QualifierDefaults.STANDARD_UNCHECKED_DEFAULTS_TOP
 * QualifierDefaults.STANDARD_UNCHECKED_DEFAULTS_BOTTOM

Dataflow Framework: Analysis is now an interface.  Added AbstractAnalysis,
ForwardAnalysis, ForwardTransferFunction, ForwardAnalysisImpl,
BackwardAnalysis, BackwardTransferFunction, and BackwardAnalysisImpl.
To adapt existing code:
 * `extends Analysis<V, S, T>` => `extends ForwardAnalysisImpl<V, S, T>`
 * `implements TransferFunction<V, S>` => `implements ForwardTransferFunction<V, S>`

In AbstractQualifierPolymorphism, use AnnotationMirrors instead of sets of
annotation mirrors.

Renamed meta-annotation SuppressWarningsKeys to SuppressWarningsPrefix.
Renamed SourceChecker#getSuppressWarningsKeys(...) to getSuppressWarningsPrefixes.
Renamed SubtypingChecker#getSuppressWarningsKeys to getSuppressWarningsPrefixes.

Added GenericAnnotatedTypeFactory#postAnalyze, changed signature of
GenericAnnotatedTypeFactory#handleCFGViz, and removed CFAbstractAnalysis#visualizeCFG.

Removed methods and classes marked deprecated in release 3.3.0 or earlier.

**Closed issues:**
#1362, #1727, #2632, #3249, #3296, #3300, #3356, #3357, #3358, #3359, #3380.


Version 3.4.1 (June 1, 2020)
----------------------------

-Ainfer now takes an argument:
 * -Ainfer=jaifs uses .jaif files to store the results of whole-program inference.
 * -Ainfer=stubs uses .astub files to store the results of whole-program inference.
 * -Ainfer is deprecated but is the same as -Ainfer=jaifs, for backwards compatibility.

New command-line option:
  -AmergeStubsWithSource If both a stub file and a source file are available, use both.

**Closed issues:**
#2893, #3021, #3128, #3160, #3232, #3277, #3285, #3289, #3295, #3302, #3305,
#3307, #3310, #3316, #3318, #3329.


Version 3.4.0 (May 3, 2020)
---------------------------

The annotated jdk8.jar is no longer used.  You should remove any occurrence of
  -Xbootclasspath/p:.../jdk8.jar
from your build scripts.  Annotations for JDK 8 are included in checker.jar.

The Returns Receiver Checker enables documenting and checking that a method
returns its receiver (i.e., the `this` parameter).

**Closed issues:**
#3267, #3263, #3217, #3212, #3201, #3111, #3010, #2943, #2930.


Version 3.3.0 (April 1, 2020)
-----------------------------

**User-visible changes:**

New command-line options:
  `-Alint=trustArrayLenZero` trust `@ArrayLen(0)` annotations when determining
  the type of Collections.toArray.

Renamings:
  `-AuseDefaultsForUncheckedCode` to `-AuseConservativeDefaultsForUncheckedCode`
    The old name works temporarily but will be removed in a future release.

For collection methods with `Object` formal parameter type, such as
contains, indexOf, and remove, the annotated JDK now forbids null as an
argument.  To make the Nullness Checker permit null, pass
`-Astubs=collection-object-parameters-may-be-null.astub`.

The argument to @SuppressWarnings can be a substring of a message key that
extends at each end to a period or an end of the key.  (Previously, any
substring worked, including the empty string which suppressed all warnings.
Use "all" to suppress all warnings.)

All postcondition annotations are repeatable (e.g., `@EnsuresNonNull`,
`@EnsuresNonNullIf`, ...).

Renamed wrapper annotations (which users should not write):
 * `@DefaultQualifiers` => `@DefaultQualifier.List`
 * `@EnsuresQualifiersIf` => `@EnsuresQualifierIf.List`
 * `@EnsuresQualifiers` => `@EnsuresQualifier.List`
 * `@RequiresQualifiers` => `@RequiresQualifier.List`

**Implementation details:**

Removed `@DefaultInUncheckedCodeFor` and
`@DefaultQualifierInHierarchyInUncheckedCode`.

Renamings:
 * applyUncheckedCodeDefaults() to applyConservativeDefaults()
 * useUncheckedCodeDefault() to useConservativeDefault()
 * AnnotatedTypeReplacer to AnnotatedTypeCopierWithReplacement
 * AnnotatedTypeMerger to AnnotatedTypeReplacer

Deprecated the `framework.source.Result` class; use `DiagMessage` or
`List<DiagMessage>` instead.  If you were creating a `Result` just to
pass it to `report`, then call new methods `reportError` and
`reportWarning` instead.

AbstractTypeProcessor#typeProcessingOver() always gets called.

**Closed issues:**
#1307, #1881, #1929, #2432, #2793, #3040, #3046, #3050, #3056, #3083, #3124,
#3126, #3129, #3132, #3139, #3149, #3150, #3167, #3189.


Version 3.2.0 (March 2, 2020)
-----------------------------

@SuppressWarnings("initialization") suppresses only warnings whose key
contains "initialization".  Previously, it suppressed all warnings issued
by the Nullness Checker or the Initialization Checker.

**Closed issues:**
#2719, #3001, #3020, #3069, #3093, #3120.


Version 3.1.1 (February 3, 2020)
--------------------------------

New command-line options:
  -AassumeDeterministic Unsoundly assume that every method is deterministic
  -AassumePure Unsoundly assume that every method is pure

Renamed -Anocheckjdk to -ApermitMissingJdk.
The old version still works, for backward compatibility.

Renamed -Alint=forbidnonnullarraycomponents to
-Alint=soundArrayCreationNullness.  The old version still works, for
backward compatibility.

Implementation details:
 * Deprecated QualifierHierarchy#getTypeQualifiers.
 * Deprecated Analysis#Analysis(ProcessingEnvironment) and Analysis#Analysis(T,
   int, ProcessingEnvironment); use Analysis#Analysis(), Analysis#Analysis(int),
   Analysis#Analysis(T), and Analysis#Analysis(T, int) instead.
 * Renamed SourceChecker#getMessages to getMessagesProperties.
 * Renamed one overload of SourceChecker.printMessages to printOrStoreMessage.

**Closed issues:**
#2181, #2975, #3018, #3022, #3032, #3036, #3037, #3038, #3041, #3049, #3055,
#3076.


Version 3.1.0 (January 3, 2020)
-------------------------------

Command-line option -AprintGitProperties prints information about the git
repository from which the Checker Framework was compiled.

**Implementation details:**
 * Removed static cache in AnnotationUtils#areSameByClass and added
   AnnotatedTypeFactory#areSameByClass that uses an instance cache.
 * Removed static cache in AnnotationBuilder#fromName and #fromClass.
 * ContractsUtils#getPreconditions takes an ExecutableElement as an argument.
 * ContractsUtils#getContracts returns a Set.
 * Moved ContractUtils.Contract to outer level.
 * Renamed ConditionalPostcondition#annoResult to ConditionalPostcondition#resultValue.

**Closed issues:**
#2867, #2897, #2972.


Version 3.0.1 (December 2, 2019)
--------------------------------

New command-line option for the Constant Value Checker
`-AnoNullStringsConcatenation` unsoundly assumes that every operand of a String
concatenation is non-null.

**Implementation details:**
 * Moved AnnotatedTypes#hasTypeQualifierElementTypes to AnnotationUtils.
 * Deprecated AnnotatedTypes#isTypeAnnotation and AnnotatedTypes#hasTypeQualifierElementTypes.

**Closed issues:**
#945, #1224, #2024, #2744, #2809, #2815, #2818, #2830, #2840, #2853, #2854,
#2865, #2873, #2874, #2878, #2880, #2886, #2888, #2900, #2905, #2919, #2923.


Version 3.0.0 (November 1, 2019)
--------------------------------

The Checker Framework works on both JDK 8 and JDK 11.
 * Type annotations for JDK 8 remain in jdk8.jar.
 * Type annotations for JDK 11 appear in stub files in checker.jar.

Removed the @PolyAll annotation.

**Implementation details:**
 * Removed all previously deprecated methods.
 * AnnotatedTypeFactory#getFnInterfaceFromTree now returns an AnnotatedExecutableType.
 * AnnotationUtils#areSame and #areSameByName now only accept non-null
   AnnotationMirrors

**Closed issues:**
#1169, #1654, #2081, #2703, #2739, #2749, #2779, #2781, #2798, #2820, #2824,
#2829, #2842, #2845, #2848.


Version 2.11.1 (October 1, 2019)
--------------------------------

The manual links to the Object Construction Checker.

**Closed issues:**
#1635, #2718, #2767.


Version 2.11.0 (August 30, 2019)
--------------------------------

The Checker Framework now uses the Java 9 javac API. The manual describes
how to satisfy this dependency, in a way that works on a Java 8 JVM.
Running the Checker Framework on a Java 9 JVM is not yet supported.


Version 2.10.1 (August 22, 2019)
--------------------------------

**Closed issues:**
#1152, #1614, #2031, #2482, #2543, #2587, #2678, #2686, #2690, #2712, #2717,
#2713, #2721, #2725, #2729.


Version 2.10.0 (August 1, 2019)
-------------------------------

Removed the NullnessRawnessChecker.  Use the NullnessChecker instead.

**Closed issues:**
#435, #939, #1430, #1687, #1771, #1902, #2173, #2345, #2470, #2534, #2606,
#2613, #2619, #2633, #2638.


Version 2.9.0 (July 3, 2019)
----------------------------

Renamed the Signedness Checker's @Constant annotation to @SignednessGlb.
Introduced an alias, @SignedPositive, for use by programmers.

Annotated the first argument of Opt.get and Opt.orElseThrow as @NonNull.

Removed meta-annotation @ImplicitFor:
 * Use the new meta-annotation @QualifierForLiteral to replace
   @ImplicitFor(literals, stringpatterns).
 * Use the meta-annotation @DefaultFor to replace @ImplicitFor(typeKinds,
   types).
 * Use the new meta-annotation @UpperBoundFor to specify a qualifier upper
   bound for certain types.
 * You can completely remove
     @ImplicitFor(typeNames = Void.class, literals = LiteralKind.NULL)
   on bottom qualifiers.
     @DefaultFor(types = Void.class)
   and
     @QualifierForLiterals(literals = LiteralKind.NULL)
   are added to the bottom qualifier by default.

Add @DefaultQualifierOnUse and @NoDefaultQualifierOnUse type declaration annotations

New/changed error message keys:
 * initialization.static.fields.uninitialized for uninitialized static fields
 * unary.increment.type.incompatible and unary.decrement.type.incompatible
   replace some occurrences of compound.assignment.type.incompatible

**Implementation details:**
 * Renamed QualifierPolymorphism#annotate methods to resolve
 * Renamed ImplicitsTreeAnnotator to LiteralTreeAnnotator
 * Renamed ImplicitsTypeAnnotator to DefaultForTypeAnnotator
 * Removed TypeUseLocation.TYPE_DECLARATION
 * Removed InheritedFromClassAnnotator, replace with DefaultQualifierForUseTypeAnnotator
 * Rename TreeUtils.isSuperCall and TreeUtils.isThisCall to
 isSuperConstructorCall and isThisConstructorCall

**Closed issues:**
#2247, #2391, #2409, #2434, #2451, #2457, #2468, #2484, #2485, #2493, #2505,
#2536, #2537, #2540, #2541, #2564, #2565, #2585.


Version 2.8.2 (June 3, 2019)
----------------------------

The Signature Checker supports a new type, @FqBinaryName.

Added a template for a repository that you can use to write a custom checker.

Linked to the Checker Framework Gradle plugin, which makes it easy to run
a checker on a project that is built using the Gradle build tool.

Implementation detail: deprecated TreeUtils.skipParens in favor of
TreeUtils.withoutParens which has the same specification.

**Closed issues:**
#2291, #2406, #2469, #2477, #2479, #2480, #2494, #2499.


Version 3.0.0-b1 (May 1, 2019)
------------------------------

First release of artifacts suitable for Java 9--12.
There is no checker artifact, because no replacement for the
annotated JDK mechanism exists yet.


Version 2.8.1 (May 1, 2019)
---------------------------

Moved text about the Purity Checker into its own chapter in the manual.

**Closed issues:**
#660, #2030, #2223, #2240, #2244, #2375, #2407, #2410, #2415, #2420, #2421,
#2446, #2447, #2460, #2462.


Version 2.8.0 (April 3, 2019)
-----------------------------

Support `androidx.annotation.RecentlyNonNull` and `RecentlyNullable` (as of
2.6.0, but not previously documented).

The following qualifiers are now repeatable:  `@DefaultQualifier`
`@EnsuresQualifierIf` `@EnsuresQualifier` `@RequiresQualifier`.  Therefore,
users generally do not need to write the following wrapper annotations:
`@DefaultQualifiers` `@EnsuresQualifiersIf` `@EnsuresQualifiers`
`@RequiresQualifiers`.

New command-line option `-ArequirePrefixInWarningSuppressions` makes
`@SuppressWarnings` recognize warning keys of the form
"checkername:key.about.problem" but ignore warning keys of the form
"key.about.problem" without the checker name as a prefix.

New CONSTRUCTOR_RESULT enum constant in TypeUseLocation makes it possible to
set default annotations for constructor results.

Clarified the semantics of annotations on class and constructor declarations.
See Section 25.5 "Annotations on classes and constructors" in the manual.

Interface changes:
 * Added protected methods to BaseTypeVisitor so that checkers can change the
   checks for annotations on classes, constructor declarations, and constructor
   invocations.
 * Removed BaseTypeVisitor#checkAssignability and BaseTypeVisitor#isAssignable
   methods.
 * Renamed AnnotatedTypeFactory#getEnclosingMethod to
   AnnotatedTypeFactory#getEnclosingElementForArtificialTree

**Closed issues:**
#2159, #2230, #2318, #2324, #2330, #2334, #2343, #2344, #2353, #2366, #2367,
#2370, #2371, #2385.


Version 2.7.0 (March 1, 2019)
-----------------------------

The manual links to the AWS crypto policy compliance checker, which enforces
that no weak cipher algorithms are used with the Java crypto API.

The Nullness Checker supports RxJava annotations
io.reactivex.annotations.NonNull and io.reactivex.annotations.Nullable.

The checker-qual artifact (jar file) contains an OSGi manifest.

New TYPE_DECLARATION enum constant in TypeUseLocation makes it possible to
(for example) set defaults annotations for class/interface definitions.

Interface changes:
 * Renamed the "value" element of the @HasSubsequence annotation to
   "subsequence".
 * Renamed @PolySignedness to @PolySigned.
 * Renamed AnnotatedTypeFactory.ParameterizedMethodType to
   ParameterizedExecutableType.

Added missing checks regarding annotations on classes, constructor
declarations, and constructor invocations.  You may see new warnings.

**Closed issues:**
#788, #1751, #2147, #2163, #2186, #2235, #2243, #2263, #2264, #2286, #2302,
#2326, #2327.


Version 2.6.0 (February 3, 2019)
--------------------------------

The manual includes a section about how to use Lombok and the Checker
Framework simultaneously.

Commons CSV has been added to the annotated libraries on Maven Central.

Some error messages have been changed to improve comprehensibility,
such as by adjusting wording or adding additional information.

Relevant to type system implementers:
Renamed method areSameIgnoringValues to areSameByName.

**Closed issues:**
#2008, #2166, #2185, #2187, #2221, #2224, #2229, #2234, #2248.
Also fixed false negatives in handling of Map.get().


Version 2.5.8 (December 5, 2018)
--------------------------------

The manual now links to the AWS KMS compliance checker, which enforces
that calls to AWS KMS only generate 256-bit keys.

**Closed issues:**
#372, #1678, #2207, #2212, #2217.


Version 2.5.7 (November 4, 2018)
--------------------------------

New @EnsuresKeyFor and @EnsuresKeyForIf method annotations permit
specifying the postcondition that a method gives some value a @KeyFor type.

The manual links to the Rx Thread & Effect Checker, which enforces
UI Thread safety properties for stream-based Android applications.

**Closed issues:**
#1014, #2151, #2178, #2180, #2183, #2188, #2190, #2195, #2196, #2198, #2199.


Version 2.5.6 (October 3, 2018)
-------------------------------

Introduce checker-qual-android artifact that is just like the checker-qual
artifact, but the qualifiers have classfile retention.  This is useful for
Android projects.

Removed the code for the checker-compat-qual artifact.  It was only useful
for Java 7, which the Checker Framework no longer supports.  The
checker-compat-qual artifact remains available on Maven Central, with
versions 2.5.5 and earlier.

**Closed issues:**
#2135, #2157, #2158, #2164, #2171.


Version 2.5.5 (August 30, 2018)
-------------------------------

Implicit imports (deprecated in November 2014) are no longer supported.

Renamed the testlib Maven artifact to framework-test.

Removed command-line option -AprintErrorStack, which is now the default.
Added -AnoPrintErrorStack to disable it (which should be rare).

Replaced ErrorReporter class with BugInCF and UserError exceptions.

**Closed issues:**
#1999, #2008, #2023, #2029, #2074, #2088, #2098, #2099, #2102, #2107.


Version 2.5.4 (August 1, 2018)
------------------------------

**Closed issues:**
#2030, #2048, #2052, #2059, #2065, #2067, #2073, #2082.


Version 2.5.3 (July 2, 2018)
----------------------------

**Closed issues:**
#266, #1248, #1678, #2010, #2011, #2018, #2020, #2046, #2047, #2054.


Version 2.5.2 (June 1, 2018)
----------------------------

In the Map Key Checker, null is now @UnknownKeyFor.  See the "Map Key Checker"
chapter in the manual for more details.

**Closed issues:**
#370, #469, #1701, #1916, #1922, #1959, #1976, #1978, #1981, #1983, #1984, #1991, #1992.


Version 2.5.1 (May 1, 2018)
---------------------------

Added a Maven artifact of the Checker Framework testing library, testlib.

**Closed issues:**
#849, #1739, #1838, #1847, #1890, #1901, #1911, #1912, #1913, #1934, #1936,
#1941, #1942, #1945, #1946, #1948, #1949, #1952, #1953, #1956, #1958.


Version 2.5.0 (April 2, 2018)
-----------------------------

Declaration annotations that are aliases for type annotations are now treated
as if they apply to the top-level type.  See "Declaration annotations" section
in the "Warnings" chapter in the manual for more details.

Ended support for annotations in comments.  See "Migrating away from
annotations in comments" section in the "Handling legacy code" chapter in the
manual for instructions on how to remove annotations from comments.

**Closed issues:**
#515, #1667, #1739, #1776, #1819, #1863, #1864, #1865, #1866, #1867, #1870,
#1876, #1879, #1882, #1898, #1903, #1905, #1906, #1910, #1914, #1915, #1920.


Version 2.4.0 (March 1, 2018)
-----------------------------

Added the Index Checker, which eliminates ArrayIndexOutOfBoundsException.

Added the Optional Checker, which verifies uses of Java 8's Optional class.

Removed the Linear Checker, whose implementation was inconsistent with its
documentation.

Added a @QualifierArgument annotation to be used on pre- and postcondition
  annotations created by @PreconditionAnnotation, @PostconditionAnnotation,
  and @ConditionalPostconditionAnnotation. This allows qualifiers with
  arguments to be used in pre- and postconditions.

Added new type @InternalFormForNonArray to the Signature Checker

Moved annotated libraries from checker/lib/*.jar to the Maven Central Repository:
<https://search.maven.org/#search%7Cga%7C1%7Cg%3A%22org.checkerframework.annotatedlib%22>

Moved the Javadoc stub file from checker/lib/javadoc.astub to
checker/resources/javadoc.astub.

Simplified the instructions for running the Checker Framework with Gradle.

The Checker Framework Eclipse plugin is no longer released nor supported.

**Closed issues:**
#65, #66, #100, #108, #175, #184, #190, #194, #209, #239, #260, #270, #274,
#293, #302, #303, #306, #321, #325, #341, #356, #360, #361, #371, #383, #385,
#391, #397, #398, #410, #423, #424, #431, #430, #432, #548, #1131, #1148,
#1213, #1455, #1504, #1642, #1685, #1770, #1796, #1797, #1801, #1809, #1810,
#1815, #1817, #1818, #1823, #1831, #1837, #1839, #1850, #1851, #1852, #1861.


Version 2.3.2 (February 1, 2018)
--------------------------------

**Closed issues:**
#946, #1133, #1232, #1319, #1625, #1633, #1696, #1709, #1712, #1734, #1738,
#1749, #1754, #1760, #1761, #1768, #1769, #1781.


Version 2.3.1 (January 2, 2018)
-------------------------------

**Closed issues:**
#1695, #1696, #1697, #1698, #1705, #1708, #1711, #1714, #1715, #1724.


Version 2.3.0 (December 1, 2017)
--------------------------------

Removed the deprecated @LazyNonNull type qualifier.
Deprecated most methods in InternalUtils and moved them to either
TreeUtils or TypesUtils. Adapted a few method names and parameter
orders for consistency.

**Closed issues:**
#951, #1356, #1495, #1602, #1605, #1623, #1628, #1636, #1641, #1653, #1655,
#1664, #1665, #1681, #1684, #1688, #1690.


Version 2.2.2 (November 2, 2017)
--------------------------------

The Interning Checker supports a new annotation, @InternedDistinct, which
indicates that the value is not equals() to any other value.

An annotated version of the Commons IO library appears in checker/lib/ .

Closed issue #1586, which required re-opening issues 293 and 341 until
proper fixes for those are implemented.

**Closed issues:**
#1386, #1389, #1423, #1520, #1529, #1530, #1531, #1546, #1553, #1555, #1565,
#1570, #1579, #1580, #1582, #1585, #1586, #1587, #1598, #1609, #1615, #1617.


Version 2.2.1 (September 29, 2017)
----------------------------------

Deprecated some methods in AnnotatedTypeMirror and AnnotationUtils, to
be removed after the 2.2.1 release.

The qualifiers and utility classes in checker-qual.jar are compiled to Java 8
byte code. A new jar, checker-qual7.jar, includes the qualifiers and utility
classes compiled to Java 7 byte code.

**Closed issues:**
#724, #1431, #1442, #1459, #1464, #1482, #1496, #1499, #1500, #1506, #1507,
#1510, #1512, #1522, #1526, #1528, #1532, #1535, #1542, #1543.


Version 2.2.0 (September 5, 2017)
---------------------------------

A Java 8 JVM is required to run the Checker Framework.
You can still typecheck and compile Java 7 (or earlier) code.
With the "-target 7" flag, the resulting .class files still run with JDK 7.

The stub file format has changed to be more similar to regular Java syntax.
Most notably, receiver annotations are written using standard Java 8 syntax
(a special first formal paramter named "this") and inner classes are written
using standard Java syntax (rather than at the top level using a name that
contains "$". You need to update your stub files to conform to the new syntax.

**Closed issues:**
#220, #293, #297, #341, #375, #407, #536, #571, #798, #867, #1180, #1214, #1218,
#1371, #1411, #1427, #1428, #1435, #1438, #1450, #1456, #1460, #1466, #1473,
#1474.


Version 2.1.14 (3 August 2017)
------------------------------

Nullness Checker change to annotated JDK:  The type argument to the Class,
Constructor, and Optional classes may now be annotated as @Nullable or
@NonNull.  The nullness of the type argument doesn't matter, but this
enables easier integration with generic clients.

Many crashes and false positives associated with uninferred method type
arguments have been correct. By default, uninferred method type arguments,
which can happen with Java 8 style target type contexts, are silently ignored.
Use the option -AconservativeUninferredTypeArguments to see warnings about
method calls where the Checker Framework fails to infer type arguments.

**Closed issues:**
#753, #804, #961, #1032, #1062, #1066, #1098, #1209, #1280, #1316, #1329, #1355,
#1365, #1366, #1367, #1377, #1379, #1382, #1384, #1397, #1398, #1399, #1402,
#1404, #1406, #1407.


Version 2.1.13 (3 July 2017)
----------------------------

Verified that the Checker Framework builds from source on Windows Subsystem
for Linux, on Windows 10 Creators Edition.

The manual explains how to configure Android projects that use Android Studio
3.0 and Android Gradle Plugin 3.0.0, which support type annotations.

**Closed issues:**
#146, #1264, #1275, #1290, #1303, #1308, #1310, #1312, #1313, #1315, #1323,
#1324, #1331, #1332, #1333, #1334, #1347, #1357, #1372.


Version 2.1.12 (1 June 2017)
----------------------------

The manual links to Glacier, a class immutability checker.

The stubparser license has been updated.  You can now use stubparser under
either the LGPL or the Apache license, whichever you prefer.

**Closed issues:**
#254, #1201, #1229, #1236, #1239, #1240, #1257, #1265, #1270, #1271, #1272,
#1274, #1288, #1291, #1299, #1304, #1305.


Version 2.1.11 (1 May 2017)
---------------------------

The manual contains new FAQ (frequently asked questions) sections about
false positive warnings and about inference for field types.

**Closed issues:**
#989, #1096, #1136, #1228.


Version 2.1.10 (3 April 2017)
-----------------------------

The Constant Value Checker, which performs constant propagation, has been
extended to perform interval analysis -- that is, it determines, for each
expression, a statically-known lower and upper bound.  Use the new
@IntRange annotation to express this.  Thanks to Jiasen (Jason) Xu for this
feature.

**Closed issues:**
#134, #216, #227, #307, #334, #437, #445, #718, #1044, #1045, #1051, #1052,
#1054, #1055, #1059, #1077, #1087, #1102, #1108, #1110, #1111, #1120, #1124,
#1127, #1132.


Version 2.1.9 (1 March 2017)
----------------------------

By default, uninferred method type arguments, which can happen with Java 8
style target type contexts, are silently ignored, removing many false
positives.  The new option -AconservativeUninferredTypeArguments can be used to
get the conservative behavior.

**Closed issues:**
#1006, #1011, #1015, #1027, #1035, #1036, #1037, #1039, #1043, #1046, #1049,
#1053, #1072, #1084.


Version 2.1.8 (20 January 2017)
-------------------------------

The Checker Framework webpage has moved to <https://checkerframework.org/>.
Old URLs should redirect to the new one, but please update your links
and let us know if any old links are broken rather than redirecting.

The documentation has been reorganized in the Checker Framework repository.
The manual, tutorial, and webpages now appear under checker-framework/docs/.

**Closed issues:**
#770, #1003, #1012.


Version 2.1.7 (3 January 2017)
------------------------------

Manual improvements:
 * Added a link to jOOQ's SQL checker.
 * Documented the `-AprintVerboseGenerics` command-line option.
 * Better explanation of relationship between Fake Enum and Subtyping Checkers.

**Closed issues:**
#154, #322, #402, #404, #433, #531, #578, #720, #795, #916, #953, #973, #974,
#975, #976, #980, #988, #1000.


Version 2.1.6 (1 December 2016)
-------------------------------

**Closed issues:**
#412, #475.


Version 2.1.5 (2 November 2016)
-------------------------------

The new class org.checkerframework.checker.nullness.Opt provides every
method in Java 8's java.util.Optional class, but written for possibly-null
references rather than for the Optional type.  This can shorten code that
manipulates possibly-null references.

In bytecode, type variable upper bounds of type Object may or may not have
been explicitly written.  The Checker Framework now assumes they were not
written explicitly in source code and defaults them as implicit upper bounds.

The manual describes how to run a checker within the NetBeans IDE.

The manual describes two approaches to creating a type alias or typedef.

**Closed issues:**
#643, #775, #887, #906, #941.


Version 2.1.4 (3 October 2016)
------------------------------

**Closed issues:**
#885, #886, #919.


Version 2.1.3 (16 September 2016)
---------------------------------

**Closed issues:**
#122, #488, #495, #580, #618, #647, #713, #764, #818, #872, #893, #894, #901,
#902, #903, #905, #913.


Version 2.1.2 (1 September 2016)
--------------------------------

**Closed issues:**
#182, #367, #712, #811, #846, #857, #858, #863, #870, #871, #878, #883, #888.


Version 2.1.1 (1 August 2016)
-----------------------------

The codebase conforms to a consistent coding style, which is enforced by
a git pre-commit hook.

AnnotatedTypeFactory#createSupportedTypeQualifiers() must now return a mutable
list.  Checkers that override this method will have to be changed.

**Closed issues:**
#384, #590, #681, #790, #805, #809, #810, #820, #824, #826, #829, #838, #845,
#850, #856.


Version 2.1.0 (1 July 2016)
---------------------------

The new Signedness Checker prevents mixing of unsigned and signed
values and prevents meaningless operations on unsigned values.

The Lock Checker expresses the annotated variable as `<self>`;
previously it used `itself`, which may conflict with an identifier.

**Closed issues:**
#166, #273, #358, #408, #471, #484, #594, #625, #692, #700, #701, #711, #717,
#752, #756, #759, #763, #767, #779, #783, #794, #807, #808.


Version 2.0.1 (1 June 2016)
---------------------------

We renamed method annotateImplicit to addComputedTypeAnnotations.  If you
have implemented a checker, you need to change occurrences of
annotateImplicit to addComputedTypeAnnotations.

The Checker Framework (checker.jar) is now placed on the processorpath
during compilation.  Previously, it was placed on the classpath.  The
qualifiers (checker-qual.jar) remain on the classpath.  This change should
reduce conflicts between your code and the Checker Framework.  If your code
depends on classes in the Checker Framework, then you should add those
classes to the classpath when you run the compiler.

**Closed issues:**
#171, #250, #291, #523, #577, #672, #680, #688, #689, #690, #691, #695, #696,
#698, #702, #704, #705, #706, #707, #720, #721, #723, #728, #736, #738, #740.


Version 2.0.0 (2 May 2016)
--------------------------

Inference:

 * The infer-and-annotate.sh script infers annotations and inserts them in
   your source code.  This can reduce the burden of writing annotations and
   let you get started using a type system more quickly.  See the
   "Whole-program inference" section in the manual for details.

Type systems:

 * The Lock Checker has been replaced by a new implementation that provides
   a stronger guarantee.  The old Lock Checker prevented two threads from
   simultaneously using a given variable, but race conditions were still
   possible due to aliases.  The new Lock Checker prevents two threads from
   simultaneously dereferencing a given value, and thus prevents race
   conditions.  For details, see the "Lock Checker" chapter in the manual,
   which has been rewritten to describe the new semantics.

 * The top type qualifier for the Signature String type system has been
   renamed from @UnannotatedString to @SignatureUnknown.  You shouldn't
   ever write this annotation, but if you perform separate compilation (for
   instance, if you do type-checking with the Signature String Checker
   against a library that is annotated with Signature String annotations),
   then you need to re-compile the library.

 * The IGJ, OIGJ, and Javari Checkers are no longer distributed with the
   Checker Framework.  If you wish to use them, install version 1.9.13 of
   the Checker Framework.  The implementations have been removed because
   they were not being maintained.  The type systems are valuable, but the
   type-checkers should be rewritten from scratch.

Documentation improvements:

 * New manual section "Tips for creating a checker" shows how to break down
   the implementation of a type system into small, manageable pieces.

 * Improved instructions for using Maven and Gradle, including for Android
   code.

Tool changes:

 * The Checker Framework Live Demo webpage lets you try the Checker
   Framework without installing it:  <http://eisop.uwaterloo.ca/live/>

 * New command-line arguments -Acfgviz and -Averbosecfg enable better
   debugging of the control-flow-graph generation step of type-checking.

 * New command-line argument -Ainfer is used by the infer-and-annotate.sh
   script that performs type inference.

**Closed issues:**
#69, #86, #199, #299, #329, #421, #428, #557, #564, #573, #579, #665, #668, #669,
#670, #671.


Version 1.9.13 (1 April 2016)
-----------------------------

Documentation:
 * Clarified Maven documentation about use of annotations in comments.
 * Added FAQ about annotating fully-qualified type names.

**Closed issues:**
#438, #572, #579, #607, #624, #631.


Version 1.9.12 (1 March 2016)
-----------------------------

The Checker Framework distribution contains annotated versions
of libraries in directory checker-framework/checker/lib/.
During type-checking, you should put these versions first on your classpath,
to obtain more precise type-checking with fewer false positive warnings.

tools.jar is no longer required to be on the classpath when using
checker-qual.jar

The Signature String Checker supports two new string representations of a
Java type: @InternalForm and @ClassGetSimpleName.

The manual documents how to run a pluggable type-checker in IntelliJ IDEA.

The instructions on how to run a type-checker in Gradle have been updated to
use the artifacts in Maven Central. Examples using the instructions have been
added under checker-framework/docs/examples/GradleExamples/.

Renamed enum DefaultLocation to TypeUseLocation.

**Closed issues:**
#130, #263, #345, #458, #559, #559, #574, #582, #596.


Version 1.9.11 (1 February 2016)
--------------------------------

Renamed and merged -AuseSafeDefaultsForUnannotatedSourceCode and
-AsafeDefaultsForUnannotatedBytecode command-line options to
-AuseDefaultsForUncheckedCode that takes arguments source and bytecode.

For type-system developers:

* The previously deprecated
  org.checkerframework.framework.qual.TypeQualifier{s} annotations
  were removed.
* Every type system uses the CLIMB-to-top defaulting scheme, unless it
  explicitly specifies a different one.  Previously a type system needed
  to explicitly request CLIMB-to-top, but now it is the default.

**Closed issues:**
#524, #563, #568.


Version 1.9.10 (4 January 2016)
-------------------------------

The Checker Framework distribution files now contain a version number:
for example, checker-framework-1.9.9.zip rather than checker-framework.zip.

The Nullness Checker supports the org.eclipse.jgit.annotations.Nullable and
NonNull annotations.

Buildfiles do less unnecessary recomputation.

Documentation:
 * Documented how to initialize circular data structures in the
   Initialization type system.
 * Linked to David Bürgin's Nullness Checker tutorial at
   <https://github.com/glts/safer-spring-petclinic/wiki>
 * Acknowledged more contributors in the manual.

For type-system developers:
 * The org.checkerframework.framework.qual.TypeQualifier{s} annotations are
   now deprecated.  To indicate which annotations a checker supports, see
   <https://eisop.github.io/cf/manual/#creating-indicating-supported-annotations>.
   Support for TypeQualifier{s} will be removed in the next release.
 * Renamed
   `org.checkerframework.framework.qual.Default{,Qualifier}ForUnannotatedCode` to
   `DefaultInUncheckedCodeFor and DefaultQualifierInHierarchyInUncheckedCode`.

**Closed issues:**
#169, #363, #448, #478, #496, #516, #529.


Version 1.9.9 (1 December 2015)
-------------------------------

Fixed issues:  #511, #513, #514, #455, #527.

Removed the javac_maven script and batch file,
which had been previously deprecated.


Version 1.9.8 (9 November 2015)
-------------------------------

Field initialization warnings can now be suppressed for a single field at a
time, by placing @SuppressWarnings("initialization") on the field declaration.

Updated Maven instructions to no longer require a script.
Added an example of how to use the instructions under
docs/examples/MavenExample.

The javac_maven script (and batch file) are deprecated and will be
removed as of December 2015.

Fixed issues:  #487, #500, #502.


Version 1.9.7 (24 October 2015)
-------------------------------

Fixed issues:  #291, #474.


Version 1.9.6 (8 October 2015)
------------------------------

Fixed issue:  #460.


Version 1.9.5 (1 September 2015)
--------------------------------

Test Framework Updates:
  * The test framework has been refactored to improve extensibility.
  * Tests that previously extended ParameterizedCheckerTest or
    CheckerTest should extend either CheckerFrameworkTest or nothing.
  * If a test used methods that were previously found on
    CheckerTest, you may find them in TestUtilities.

Fixed issues:  #438, #457, #459.


Version 1.9.4 (4 August 2015)
-----------------------------

Documented the notion of a compound checker, which depends on other checkers
  and automatically runs them.

Renamed -AuseConservativeDefaultsForUnannotatedSourceCode command-line
  option to -AuseSafeDefaultsForUnannotatedSourceCode

Moved the Checker Framework version control repository from Google Code to
GitHub, and from the Mercurial version control system to Git.  If you have
cloned the old repository, then discard your old clone and create a new one
using this command:
```
  git clone https://github.com/typetools/checker-framework.git
```

Fixed issues:  #427, #429, #434, #442, #450.


Version 1.9.3 (1 July 2015)
---------------------------

New command-line options:
 * -AsafeDefaultsForUnannotatedBytecode causes a checker to use conservative
   defaults for .class files that were compiled without running the given
   checker.  Without this option, type-checking is unsound (that is, there
   might be errors at run time even though the checker issues no warnings).
 * -AuseConservativeDefaultsForUnannotatedSourceCode uses conservative
   annotations for unannotated type uses.  Use this when compiling a library in
   which some but not all classes are annotated.

Various bug fixes and documentation improvements.

Fixed issues: #436.


Version 1.9.2 (1 June 2015)
---------------------------

Internationalization Format String Checker:
This new type-checker prevents use of incorrect internationalization
format strings.

Fixed issues: #434.


Version 1.9.1 (1 May 2015)
--------------------------

New FAQ entry:
  "How does the Checker Framework compare with Eclipse's null analysis?"


Version 1.9.0 (17 April 2015)
-----------------------------

Bug fixes for generics, especially type parameters:
   * Manual chapter 21 "Generics and polymorphism" has been expanded,
     and it gives more information on annotating type parameters.
   * The qualifier on a type parameter (e.g. <@HERE T> ) only applies
     to the lower bound of that type parameter.  Previously it also
     applied to the upper bound.
   * Unannotated, unbounded wildcards are now qualified with the
     annotations of the type parameter to which they are an argument.
     See the new manual section 23.3.4 for more details.
   * Warning "bound.type.incompatible" is issued if the lower bound of
     a type parameter or wildcard is a supertype of its upper bound,
     e.g.  <@Nullable T extends @NonNull Object>
   * Method type argument inference has been improved. Fewer warnings
     should be issued when method invocations omit type arguments.
   * Added command-line option -AprintVerboseGenerics to print more
     information about type parameters and wildcards when they appear
     in warning messages.

Reflection resolution:
If you supply the -AresolveReflection command-line option, the Checker
Framework attempts to resolve reflection.  This reduces the number of
false positive warnings caused by reflection.

The documentation for the Map Key Checker has been moved into its own
chapter in the manual.

Fixed issues: #221, #241, #313, #314, #328, #335, #337, #338, #339, #355, #369,
              #376, #378, #386, #388, #389, #393, #403, #404, #413, #414, #415,
              #417, #418, #420, #421, #422, #426.


Version 1.8.11 (2 March 2015)
-----------------------------

Fixed issues: #396, #400, #401.


Version 1.8.10 (30 January 2015)
--------------------------------

Fixed issues: #37, #127, #350, #364, #365, #387, #392, #395.


Version 1.8.9 (19 December 2014)
--------------------------------

Aliasing Checker:
This new type-checker ensures that an expression has no aliases.

Fixed issues: #362, #380, #382.


Version 1.8.8 (26 November 2014)
--------------------------------

@SuppressWarnings("all") suppresses all Checker Framework warnings.

Implicit imports are deprecated, including the jsr308_imports environment
variable and the -jsr308_imports ... and -Djsr308.imports=... command-line
options.

For checkers bundled with the Checker Framework, package names may now
be omitted when running from the command line.
E.g.
    javac -processor NullnessChecker MyFile.java

The Nullness checker supports Android annotations
android.support.annotation.NonNull and android.support.annotation.Nullable.

Fixed issues: #366, #379.


Version 1.8.7 (30 October 2014)
-------------------------------

Fix performance regression introduced in release 1.8.6.

Nullness Checker:
  * Updated Nullness annotations in the annotated JDK.
    See issues: #336, #340, #374.
  * String concatenations with null literals are now @NonNull
    rather than @Nullable.  See issue #357.

Fixed issues:  #200, #300, #332, #336, #340, #357, #359, #373, #374.


Version 1.8.6 (25 September 2014)
---------------------------------

Method Reference and Lambda Expression Support:
The Checker Framework now supports type-checking method references
and lambda expressions to ensure they are congruent with the
functional interface they are assigned to. The bodies of lambda expressions
are also now type-checked similarly to regular method bodies.

Dataflow:
 * Handling of the following language features has been improved:
   boxed Booleans, finally blocks, switch statements, type casts, enhanced
   for loops
 * Performance improvements

Annotations:
The checker-compat-qual.jar is now included with the Checker Framework
release.  It can also be found in Maven Central at the coordinates:
org.checkerframework:checker-compat-qual
Annotations in checker-compat-qual.jar do not require Java 8 but
can only be placed in annotation locations valid in Java 7.


Version 1.8.5 (29 August 2014)
------------------------------

Eclipse Plugin:
All checkers in the Checker Framework manual now appear in the
Eclipse plugin by default.  Users no longer have to include
checker.jar on their classpath to run any of the built-in checkers.

Improved Java 7 compatibility and introduced Java 7 compliant
annotations for the Nullness Checker.  Please see the section on
"Class-file compatibility with Java 7" in the manual for more details.

Fixed issue #347.


Version 1.8.4 (1 August 2014)
-----------------------------

The new Constant Value Checker is a constant propagation analysis:  it
determines which variable values can be known at compile time.

Overriding methods now inherit declaration annotations from methods they
override, if the declaration annotation is meta-annotate with
@InheritedAnnotation.  In particular, the purity annotations @SideEffectFree,
@Deterministic, and @Pure are inherited.

Command-line options:
 * Renamed the -AenablePurity command-line flag to -AcheckPurityAnnotations.
 * Added a command-line option -AoutputArgsToFile to output all command-line
   options passed to the compiler to a file.  This is especially useful when
   debugging Maven compilation.

Annotations:
These changes are relevant only to people who wish to use pluggable
type-checking with a standard Java 7 toolset.  (If you are not having
trouble with your Java 7 JVM, then you don't care about them.)
 * Made clean-room reimplementations of nullness-related annotations
   compatible with Java 7 JVMs, by removing TYPE_USE as a target.
 * Added a new set of Java 7 compatibility annotations for the Nullness Checker
   in the org.checkerframework.checker.nullness.compatqual package. These
   annotations do not require Java 8 but can only be placed in annotation
   locations valid in Java 7.

Java 8 support:
The Checker Framework no longer crashes when type-checking code with lambda
expressions, but it does issue a lambda.unsupported warning when
type-checking code containing lambda expressions.  Full support for
type-checking lambda expressions will appear in a future release.

Fixed issue #343.


Version 1.8.3 (1 July 2014)
---------------------------

Updated the Initialization Checker section in the manual with
a new introduction paragraph.

Removed the Maven plugin section from the manual as the plugin is
no longer maintained and the final release was on June 2, #2014.
The javac_maven script (and batch file) are available to use
the Checker Framework from Maven.

Fixed issue #331.


Version 1.8.2 (2 Jun 2014)
--------------------------

Converted from using rt.jar to ct.sym for creating the annotated jdk.
Using the annotated jdk on the bootclasspath of a VM will cause the
vm to crash immediately.

The Lock Checker has been rewritten to support dataflow analysis.
It can now understand conditional expressions, for example, and
knows that "lock" is held in the body of statements like
"if (lock.tryLock()) { ... }"
The Lock Checker chapter in the manual has been updated accordingly
and describes the new Lock Checker features in detail.

Provided a javac_maven script (and batch file) to make it simpler
to use the Checker Framework from Maven.  The Maven plug-in is deprecated
and will be removed as of July 1, 2014. Added an explanation of how
to use the script in the Maven section of the manual.

The Checker Framework installation instructions in the manual have
been updated.

Fixed issues: #312, #315, #316, #318, #319, #324, #326, #327.


Version 1.8.1 (1 May 2014)
--------------------------

Support to directly use the Java 8 javac in addition to jsr308-langtools.
Added docs/examples directory to checker-framework.zip.
New section in the manual describing the contents of checker-framework.zip.

Fixed issues: #204, #304, #320.


Version 1.8.0 (2 April 2014)
----------------------------

Added the GUI Effect Checker, which prevents "invalid thread access" errors
when a background thread in a GUI attempts to access the UI.

Changed the Java package of all type-checkers and qualifiers.  The package
"checkers" has been renamed to "org.checkerframeork.checker".  This
requires you to change your import statements, such as from
  import checkers.nullness.quals.*;
to
  import org.checkerframework.checker.nullness.qual.*;
It also requires you to change command-line invocations of javac, such as from
  javac -processor checkers.nullness.NullnessChecker ...
to
  javac -processor org.checkerframework.checker.nullness.NullnessChecker ...

Restructured the Checker Framework project and package layout,
using the org.checkerframework prefix.


Version 1.7.5 (5 March 2014)
----------------------------

Minor improvements to documentation and demos.
Support a few new units in the UnitsChecker.


Version 1.7.4 (19 February 2014)
--------------------------------

Error messages now display the error key that can be used in
SuppressWarnings annotations. Use -AshowSuppressWarningKeys to
show additional keys.

Defaulted type qualifiers are now stored in the Element and written
to the final bytecode.

Reduce special treatment of checkers.quals.Unqualified.

Fixed issues: #170, #240, #265, #281.


Version 1.7.3 (4 February 2014)
-------------------------------

Fixes for Issues #210, #253, #280, #288.

Manual:
   Improved discussion of checker guarantees.

Maven Plugin:
   Added option useJavacOutput to display exact compiler output.

Eclipse Plugin:
   Added the Format String Checker to the list of built-in checkers.


Version 1.7.2 (2 January 2014)
------------------------------

Fixed issues: #289, #292, #295, #296, #298.


Version 1.7.1 (9 December 2013)
-------------------------------

Fixes for Issues #141, #145, #257, #261, #269, #267, #275, #278, #282, #283, #284, #285.

**Implementation details:**

Renamed AbstractBasicAnnotatedTypeFactory to GenericAnnotatedTypeFactory


Version 1.7.0 (23 October 2013)
-------------------------------

Format String Checker:
  This new type-checker ensures that format methods, such as
  System.out.printf, are invoked with correct arguments.

Renamed the Basic Checker to the Subtyping Checker.

Reimplemented the dataflow analysis that performs flow-sensitive type
  refinement.  This fixes many bugs, improves precision, and adds features.
  Many more Java expressions can be written as annotation arguments.

Initialization Checker:
  This new abstract type-checker verifies initialization properties.  It
  needs to be combined with another type system whose proper initialization
  should be checked.  This is the new default initialzation checker for the
  Nullness Checker.  It is based on the "Freedom Before Commitment" approach.

Renamed method annotations used by the Nullness Checker:
  @AssertNonNullAfter => @EnsuresNonNull
  @NonNullOnEntry => @RequiresNonNull
  @AssertNonNullIfTrue(...) => @IfMethodReturnsFalseEnsuresNonNull
  @AssertNonNullIfFalse(...) => @IfMethodReturnsFalseEnsuresNonNull
  @LazyNonNull => @MonotonicNonNull
  @AssertParametersNonNull => [no replacement]
Removed annotations used by the Nullness Checker:
  @AssertParametersNonNull
Renamed type annotations used by the Initialization Checker:
  @NonRaw => @Initialized
  @Raw => @UnknownInitialization
  new annotation @UnderInitialization
The old Initialization Checker (that uses @Raw and @NonRaw) can be invoked
  by invoking the NullnessRawnessChecker rather than the NullnessChecker.

Purity (side effect) analysis uses new annotations @SideEffectFree,
  @Deterministic, and @TerminatesExecution; @Pure means both @SideEffectFree
  and @Deterministic.

Pre- and postconditions about type qualifiers are available for any type system
  through @RequiresQualifier, @EnsuresQualifier and @EnsuresQualifierIf.  The
  contract annotations for the Nullness Checker (e.g. @EnsuresNonNull) are now
  only a special case of these general purpose annotations.
  The meta-annotations @PreconditionAnnotation, @PostconditionAnnotation, and
  @ConditionalPostconditionAnnotation can be used to create more special-case
  annotations for other type systems.

Renamed assertion comment string used by all checkers:
  @SuppressWarnings => @AssumeAssertion

To use an assert statement to suppress warnings, the assertion message must
  include the string "@AssumeAssertion(warningkey)".  Previously, just the
  warning key sufficed, but the string @SuppressWarnings(warningkey) was
  recommended.

New command-line options:
  -AonlyDefs and -AonlyUses complement existing -AskipDefs and -AskipUses
  -AsuppressWarnings Suppress warnings matching the given key
  -AassumeSideEffectFree Unsoundly assume that every method is side-effect-free
  -AignoreRawTypeArguments Ignore subtype tests for type arguments that
    were inferred for a raw type
  -AenablePurity Check the bodies of methods marked as pure
    (@SideEffectFree or @Deterministic)
  -AsuggestPureMethods Suggest methods that could be marked as pure
  -AassumeAssertionsAreEnabled, -AassumeAssertionsAreDisabled Whether to
    assume that assertions are enabled or disabled
  -AconcurrentSemantics Whether to assume concurrent semantics
  -Anocheckjdk Don't err if no annotated JDK can be found
  -Aflowdotdir Create an image of the control flow graph
  -AinvariantArrays replaces -Alint=arrays:invariant
  -AcheckCastElementType replaces -Alint=cast:strict

Manual:
  New manual section about array types.
  New FAQ entries:  "Which checker should I start with?", "How can I handle
    typestate, or phases of my program with different data properties?",
    "What is the meaning of a type qualifier at a class declaration?"
  Reorganized FAQ chapter into sections.
  Many other improvements.


Version 1.6.7 (28 August 2013)
------------------------------

User-visible framework improvements:
  Improve the error message produced by -Adetailedmsgtext

Bug fixes:
  Fix issue #245: anonymous classes were skipped by default


Version 1.6.6 (01 August 2013)
------------------------------

Documentation:
  The Checker Framework manual has been improved.  Changes include:
more troubleshooting tips to the Checker Framework manual, an improved
discussion on qualifier bounds, more examples, improved formatting, and more.
  An FAQ entry has been added to discuss JSR305.
  Minor clarifications have been added to the Checker Framework tutorial.


Version 1.6.5 (01 July 2013)
----------------------------

User-visible framework improvements:
  Stub files now support static imports.

Maven plugin:
  Maven plugin will now issue a warning rather than quit when zero checkers are specified in a project's pom.xml.

Documentation:
  Improved the Maven plugin instructions in the Checker Framework manual.
  Added documentation for the -XDTA:noannotationsincomments compiler flag.

Internal framework improvements:
  Improved Maven-plugin developer documentation.


Version 1.6.4 (01 June 2013)
----------------------------

User-visible framework improvements:
    StubGenerator now generates stubs that can be read by the StubParser.

Maven plugin:
    The Maven plugin no longer requires the Maven project's output directory to exist in order to run the Checker Framework.  However, if you ask the Checker Framework to generate class files then the output directory will be created.

Documentation:
  Improved the Maven plugin instructions in the Checker Framework manual.
  Improved the discussion of why to define both a bottom and a top qualifier in the Checker Framework manual.
  Update FAQ to discuss that some other tools incorrectly interpret array declarations.


Version 1.6.3 (01 May 2013)
---------------------------

Eclipse plugin bug fixes:
  The javac argument files used by the Eclipse plugin now properly escape file paths.  Windows users should no longer encounter errors about missing built-in checkers.

Documentation:
  Add FAQ "What is the meaning of an annotation after a type?"


Version 1.6.2 (04 Apr 2013)
---------------------------

Eclipse plugin:
  The "Additional compiler parameters" text field has now been replaced by a list.  Parameters in this list may be activated/deactivated via checkbox.

Eclipse plugin bug fixes:
   Classpaths and source files should now be correctly quoted when they contain spaces.

Internal framework improvements:
  Update pom files to use the same update-version code as the Checker Framework "web" ant task.  Remove pom specific update-version code.
  Update build ant tasks to avoid re-running targets when executing tests from the release script.


Version 1.6.1 (01 Mar 2013)
---------------------------

User-visible framework improvements:
  A number of error messages have been clarified.
  Stub file now supports type annotations in front and after method type variable declarations.
  You may now specify custom paths to javac.jar and jdk7.jar on the command line for non-standard installations.

Internal framework improvements:
  Add shouldBeApplied method to avoid unnecessary scans in DefaultApplier and avoid annotating void types.
  Add createQualifierDefaults and createQualifierPolymorphism factory methods.

Maven plugin:
  Put Checker Framework jars at the beginning of classpath.
  Added option to compile code in order to support checking for multi-module projects.
  The plugin no longer copies the various Checker Framework maven artifacts to one location but instead takes advantage of the new custom path options for javac.jar and jdk7.jar.
  The maven plugin no longer attempts to resolve jdk6.jar

Eclipse plugin:
  Put Checker Framework jars at the beginning of classpath.
  All files selected from a single project can now be checked.  The previous behavior only checked the entire project or one file depending on the type of the first file selected.

Documentation:
  Fixed broken links and incomplete URLs in the Checker Framework Manual.
  Update FAQ to discuss that some other tools incorrectly interpret array declarations.

Bug fixes


Version 1.6.0 (1 Feb 2013)
--------------------------

User-visible framework improvements:
  It is possible to use enum constants in stub files without requiring the fully qualified name, as was previously necessary.
  Support build on a stock Java 8 OpenJDK.

Adapt to underlying jsr308-langtools changes.
  The most visible change is syntax for fully-qualified types, from @A java.lang.Object to java.lang.@A Object.
  JDK 7 is now required.  The Checker Framework does not build or run on JDK 6.

Documentation:
  A new tutorial is available at <https://eisop.github.io/cf/tutorial/>.


Version 1.5.0 (14 Jan 2013)
---------------------------

User-visible framework improvements:
  To invoke the Checker Framework, call the main method of class
    CheckerMain, which is a drop-in replacement for javac.  This replaces
    all previous techniques for invoking the Checker Framework.  Users
    should no longer provide any Checker Framework jars on the classpath or
    bootclasspath.  jsr308-all.jar has been removed.
  The Checker Framework now works with both JDK 6 and JDK 7, without need
    for user customization.  The Checker Framework determines the
    appropriate annotated JDK to use.
  All jar files now reside in checker-framework/checkers/binary/.

Maven plugin:
  Individual pom files (and artifacts in the Maven repository) for all
    Checker Framework jar files.
  Avoid too-long command lines on Windows.
  See the Maven section of the manual for more details.

Eclipse plugin:
  Avoid too-long command lines on Windows.
  Other bug fixes and interface improvements.

Other framework improvements:
  New -Adetailedmsgtext command-line option, intended for use by IDE plugins.


Version 1.4.4 (1 Dec 2012)
--------------------------

Internal framework improvements:
  Add shutdown hook mechanism and use it for -AresourceStats resource
    statistics flag.
  Add -AstubWarnIfNotFound and -AstubDebug options to improve
    warnings and debug information from the stub file parsing.
  Ignore case when comparing error suppression keys.
  Support the bottom type as subtype of any wildcard type.

Tool Integration Changes
  The Maven plugin id has been changed to reflect standard Maven
    naming conventions.
  Eclipse and Maven plugin version numbers will now
    track the Checker Framework version numbers.

Bug fixes.


Version 1.4.3 (1 Nov 2012)
--------------------------

Clarify license:
  The Checker Framework is licensed under the GPL2.  More permissive
    licenses apply to annotations, tool plugins (Maven, Eclipse),
    external libraries included with the Checker Framework, and examples in
    the Checker Framework Manual.
  Replaced all third-party annotations by cleanroom implementations, to
    avoid any potential problems or confusion with licensing.

Aliased annotations:
  Clarified that there is no need to rewrite your program.  The Checker
    Framework recognizes dozens of annotations used by other tools.

Improved documentation of Units Checker and Gradle Integration.
Improved developer documentation of Eclipse and Maven plugins.

Bug fixes.


Version 1.4.2 (16 Oct 2012)
---------------------------

External tool support:
  Eclipse plug-in now works properly, due to many fixes

Regex Checker:
  New CheckedPatternSyntaxException added to RegexUtil

Support new foreign annotations:
  org.eclipse.jdt.annotation.Nullable
  org.eclipse.jdt.annotation.NonNull

New FAQ: "What is a receiver?"

Make annotations use 1-based numbering for formal parameters:
  Previously, due to a bug the annotations used 0-based numbering.
  This change means that you need to rewrite annotations in the following ways:
    @KeyFor("#3")  =>  @KeyFor("#4")
    @AssertNonNullIfTrue("#0")  =>  @AssertNonNullIfTrue("#1")
    @AssertNonNullIfTrue({"#0", "#1"})  =>  @AssertNonNullIfTrue({"#1", "#2"})
    @AssertNonNullAfter("get(#2)")  =>  @AssertNonNullAfter("get(#3)")
  This command:
    find . -type f -print | xargs perl -pi -e 's/("#)([0-9])(")/$1.($2+1).$3/eg'
  handles the first two cases, which account for most uses.  You would need
  to handle any annotations like the last two cases in a different way,
  such as by running
    grep -r -n -E '\("[^"]+#[0-9][^A-Za-z]|\("#[0-9][^"]' .
  and making manual changes to the matching lines.  (It is possible to
  provide a command that handles all cases, but it would be more likely to
  make undesired changes.)
  Whenever making automated changes, it is wise to save a copy of your
  codebase, then compare it to the modified version so you can undo any
  undesired changes.  Also, avoid running the automated command over version
  control files such as your .hg, .git, .svn, or CVS directory.


Version 1.4.1 (29 Sep 2012)
---------------------------

User-visible framework improvements:
  Support stub files contained in .jar files.
  Support aliasing for declaration annotations.
  Updated the Maven plugin.

Code refactoring:
  Make AnnotationUtils and AnnotatedTypes into stateless utility classes.
    Instead, provide the necessary parameters for particular methods.
  Make class AnnotationBuilder independent of AnnotationUtils.
  Remove the ProcessingEnvironment from AnnotatedTypeMirror, which was
    hardly used and can be replaced easily.
  Used more consistent naming for a few more fields.
  Moved AnnotatedTypes from package checkers.types to checkers.utils.
    this required making a few methods in AnnotatedTypeFactory public,
    which might require changes in downstream code.

Internal framework improvements:
  Fixed Issues #136, #139, #142, #156.
  Bug fixes and documentation improvements.


Version 1.4.0 (11 Sep 2012)
---------------------------

User-visible framework improvements:
  Defaulting:
    @DefaultQualifier annotations now use a Class instead of a String,
      preventing simple typo errors.
    @DefaultLocation extended with more constants.
    TreeAnnotator propagates the least-upper-bound of the operands of
      binary/compound operations, instead of taking the default qualifier.
  Stub files now ignore the return type, allowing for files automatically
    generated from other formats.
  Type factories and type hierarchies:
    Simplify AnnotatedTypeFactory constructors.
    Add a GeneralAnnotatedTypeFactory that supports multiple type systems.
    Improvements to QualifierHierarchy construction.
  Type-checking improvements:
    Propagate annotations from the sub-expression of a cast to its result.
    Better handling of assignment context and improved inference of
      array creation expressions.
  Optional stricter checking of casts to array and generic types using
    the new -Alint=cast:strict flag.
    This will become the default in the future.
  Code reorganization:
    SourceChecker.initChecker no longer has a ProcessingEnvironment
      parameter. The environment can now be accessed using the standard
      processingEnv field (instead of the previous env field).
    Classes com.sun.source.util.AbstractTypeProcessor and
      checkers.util.AggregateChecker are now in package checkers.source.
    Move isAssignable from the BaseTypeChecker to the BaseTypeVisitor; now
      the Checker only consists of factories and logic is contained in the
      Visitor.
  Warning and error messages:
    Issue a warning if an unsupported -Alint option is provided.
    Improved error messages.
  Maven plugin now works.

Nullness Checker:
  Only allow creation of (implicitly) non-null objects.
  Optionally forbid creation of arrays with @NonNull component type,
    when flag -Alint=arrays:forbidnonnullcomponents is supplied.
    This will become the default in the future.

Internal framework improvements:
  Enable assertion checking.
  Improve handling of annotated type variables.
  Assignment context is now a type, not a tree.
  Fix all compiler warnings.


Version 1.3.1 (21 Jul 2012)
---------------------------

Installation:
  Clarify installation instructions for Windows.  Remove javac.bat, which
  worked for running distributed checkers but not for creating new checkers.

User-visible framework improvements:
  Implement @PolyAll qualifier to vary over multiple type systems.
  The Checker Framework is unsound due to Java's covariant array subtyping.
    You can enable invariant array subtyping (for qualifiers only, not for
    base Java types) with the command-line option -Alint=arrays:invariant.
    This will become the default in the future.

Internal framework improvements:
  Improve defaulting for multiple qualifier hierarchies.
  Big refactoring of how qualifier hierarchies are built up.
  Improvements to error handling output for unexpected exceptions.
  Bug fixes and documentation improvements.


Version 1.3.0 (3 Jul 2012)
--------------------------

Annotation syntax changes, as mandated by the latest Type Annotations
(JSR 308) specification.  The most important ones are:
- New receiver syntax, using "this" as a formal parameter name:
    ReturnType methodname(@ReceiverAnnotation MyClass this, ...) { ... }
- Changed @Target default to be the Java 1.5 values
- UW extension: in addition to annotations in comments, support
    special /*>>> */ comments to hide multiple tokens.
    This is useful for the new receiver syntax and for import statements.

Framework improvements:
  Adapt to annotation storage changes in jsr308-langtools 1.3.0.
  Move type validation methods from the BaseTypeChecker to BaseTypeVisitor.


Version 1.2.7 (14 May 2012)
---------------------------

Regex Checker:
  Add basic support for the concatenation of two non-regular expressions
    that produce a valid regular expression.
  Support "isRegex" in flow inference.

Framework improvements:
  New @StubFiles annotation declaratively adds stub files to a checker.

Internal bug fixes:
  Respect skipDefs and skipUses in NullnessFlow.
  Support package annotations in stub files.
  Better support for enums in annotation attributes.
  Cleanups to how implicit receivers are determined.


Version 1.2.6 (18 Mar 2012)
---------------------------

Nullness Checker:
  Correctly handle unboxing in more contexts (if, switch (Issue 129),
    while loops, ...)

Regex Checker:
  Add capturing groups parameter to Regex qualifier.
    Count groups in String literals and String concatenation.
    Verify group number to method calls that take a capturing group
      number.
    Update RegexUtil methods to take optional groups parameter.
    Modify regex qualifier hierarchy to support groups parameter.
  Add special case for Pattern.compile when called with Pattern.LITERAL flag.

Internal bug fixes:
  Improve flow's support of annotations with parameters.
  Fix generics corner cases (Issues #131, #132, #133, #135).
  Support type annotations in annotations and type-check annotations.
  Improve reflective look-up of visitors and factories.
  Small cleanups.


Version 1.2.5.1 (06 Feb 2012)
-----------------------------

Nullness Checker:
  Correct the annotations on ThreadLocal and InheritableThreadLocal.

Internal bug fixes:
  Expand release tests.
  Compile release with JDK 6 to work on both JDK 6 and JDK 7.


Version 1.2.5 (3 Feb 2012)
--------------------------

Don't put classpath on the bootclasspath when invoking javac.  This
prevents problems if, for example, android.jar is on the classpath.

New -jsr308_imports ... and -Djsr308.imports=... command-line options, for
specifying implicit imports from the command line.  This is needed by Maven.

New -Aignorejdkastub option makes the checker not load the jdk.astub
file. Files from the "stubs" option are still loaded.

Regex Checker:
  Support concatenation of PolyRegex strings.
  Improve examples of use of RegexUtil methods.

Signature Checker:
  Add new @ClassGetName annotation, for a 4th string representation of a
    class that is used by the JDK.  Add supporting annotations to make the
    type hierarchy a complete lattice.
  Add PolySignature annotation.

Internal bug fixes:
  Improve method type argument inference.
  Handle type variables whose upper bound is a type variable.
  Fix bug in least upper bound computation for anonymous classes.
  Improve handling of annotations inherited from superclasses.
  Fix design problem with Nullness Checker and primitive types.
  Ensure that overriding methods respect pre- and postconditions.
  Correctly resolve references to an enclosing this.
  Improve handling of Java source that contains compilation errors.


Version 1.2.4 (15 Dec 2011)
---------------------------

All checkers:
- @Target(TYPE_USE) meta-annotation is properly handled.

Nullness Checker:
- Do not allow nullness annotations on primitive types.
- Improvements to rawness (initialization) checks.
- Special-case known keys for System.getProperty.
- The -Alint=uninitialized command-line option now defaults to off, and
  applies only to initialization of primitive and @Nullable fields.  It is
  not possible to disable, from the command line, the check that all
  @NonNull fields are initialized.  Such warnings must be suppressed
  explicitly, for example by using @SuppressWarnings.

Regex Checker:
- Improved RegexUtil class.

Manual:
- Add FAQ item "Is the Checker Framework an official part of Java?"
- Trim down README.txt; users should read the manual instead.
- Improvements throughout, especially to Nullness and Regex Checker sections.

**Implementation details:**
- Add a new @InvisibleQualifier meta-annotation for type qualifiers.
  Instead of special-casing @Unqualified in the AnnotatedTypeMirror it
  now looks for this meta-annotation. This also allows type systems to
  hide type qualifiers it doesn't want visible, which we now use in the
  Nullness Checker to hide the @Primitive annotation.
- Nullness Checker:  Introduce a new internal qualifier @Primitive that is
  used for primitive types.
- Be stricter about qualifiers being present on all types. If you get
  errors about missing qualifiers, check your defaulting rules.
  This helped in fixing small bugs in corner cases of the type
  hierarchy and type factory.
- Unify decoding type annotations from trees and elements.
- Improve handling of annotations on type variables and upper bounds.
- Support checkers that use multiple, disjoint qualifier hierarchies.
- Many bug fixes.


Version 1.2.3 (1 Nov 2011)
--------------------------

Regex Checker:
- Add @PolyRegex polymorphic annotation
- Add more stub library annotations

**Implementation details:**
- Do not use "null" for unqualified types. Explicitly use @Unqualified
  and be strict about correct usage. If this causes trouble for you,
  check your @ImplicitFor and @DefaultQualifierInHierarchy
  meta-annotations and ensure correct defaulting in your
  AnnotatedTypeFactory.

Bug fixes:
- Correctly handle f-bounded polymorphism. AnnotatedTypeMirror now has
  methods to query the "effective" annotations on a type, which
  handles type variable and wildcard bounds correctly. Also, terminate
  recursions by not doing lazy-initialization of bounds during defaulting.
- Many other small bug fixes and documentation updates.


Version 1.2.2 (1 Oct 2011)
--------------------------

Be less restrictive about when to start type processing when errors
already exist.
Add -AskipDefs command-line option to not type-check some class
definitions.
Documentation improvements.


Version 1.2.1 (20 Sep 2011)
---------------------------

Fix issues #109, #110, #111 and various other cleanups.
Improvements to the release process.
Documentation improvements.


Version 1.2.0.1 (4 Sep 2011)
----------------------------

New version number to stay in sync with JSR 308 compiler bugfix.
No significant changes.


Version 1.2.0 (2 Sep 2011)
--------------------------

Updated to JDK 8. Use -source 8 (the new default) for type annotations.
Documentation improvements
Bug fixes all over

Nullness Checker:
- Correct the upper bounds of all Collection subtypes


Version 1.1.5 (22 Jul 2011)
---------------------------

**User-visible changes:**

Units Checker:
  Instead of conversion routines, provide unit constants, with which
  to multiply unqualified values. This is easier to type and the
  multiplication gets optimized away by the compiler.

Fenum Checker:
  Ensure that the switch statement expression is a supertype of all
  the case expressions.

**Implementation details:**

- Parse declaration annotations in stub files

- Output error messages instead of raising exceptions. This change
  required us to introduce method "initChecker" in class
  SourceChecker, which should be used instead of "init". This allows
  us to handle the calls to initChecker within the framework.
  Use method "errorAbort" to output an error message and abort
  processing.


Version 1.1.4 (8 Jul 2011)
--------------------------

**User-visible changes:**

Units Checker (new):
  Ensures operations are performed on variables of correct units of
  measurement (e.g., miles vs. kilometers vs. kilograms).

Changed -AskipClasses command-line option to -AskipUses

**Implementation details:**

- Improve support for type qualifiers with enum attributes


Version 1.1.3 (17 Jun 2011)
---------------------------

**User-visible changes:**

Interning:
- Add @UsesObjectEquals annotation

Manual:
- Signature Checker is now documented
- Fenum Checker documentation improved
- Small improvements to other sections

**Implementation details:**

- Updates to the web-site build process

- The BaseTypeVisitor used to provide the same two type parameters as
  class SourceVisitor. However, all subtypes of BaseTypeVisitor were
  instantiated as <Void, Void>. We decided to directly instantiate the
  SourceVisitor as <Void, Void> and removed this complexity.
  Instead, the BaseTypeVisitor is now parameterized by the subtype of
  BaseTypeChecker that should be used. This gives a more concrete type
  to field "checker" and is similar to BasicAnnotatedTypeFactory.

- Added method AnnotatedTypeFactory.typeVariablesFromUse to allow
  type-checkers to adapt the upper bounds of a type variable depending on
  the type instantiation.

- Method type argument inference:
  Changed AnnotatedTypeFactory.methodFromUse to return a Pair consisting
  of the method and the inferred or explicit method type arguments.
  If you override this method, you will need to update your version.
  See this change set for a simple example:
  <https://github.com/typetools/checker-framework/source/detail?r=8381a213a4>

- Testing framework:
  Support for multiple expected errors using the "// :: A :: B :: C" syntax.

Many small updates and fixes.


Version 1.1.2 (12 Jan 2011)
---------------------------

Fake Enum Checker (new):
  A "fake enumeration" is a set of integers rather than a proper Java enum.
  They are used in legacy code and for efficiency (e.g., in Android).  The
  Fake Enum Checker gives them the same safety guarantees as a proper Java
  enum.

Property File Checker (new):
  Ensures that valid keys are used for property files and resource bundles.
  Also includes a checker that code is properly internationalized and a
  checker for compiler message keys as used in the Checker Framework.

Signature Checker (new):
  Ensures that different string representations of a Java type (e.g.,
  `"pakkage.Outer.Inner"` vs. `"pakkage.Outer$Inner"` vs. `"Lpakkage/Outer$Inner;"`)
  are not misused.

Interning Checker enhancements:
  Issues fewer false positives for code like "a==b || a.equals(b)"

Foreign annotations:
  The Checker Framework supports more non-Checker-Framework annotations.
  This means that it can check already-annotated code without requiring you
  to rewrite your annotations.
    Add as an alias for checkers.interning.quals.Interned:
      com.sun.istack.Interned
    Add as aliases for checkers.nullness.quals.NonNull:
      com.sun.istack.NotNull
      org.netbeans.api.annotations.common.NonNull
    Add as aliases for checkers.nullness.quals.Nullable:
      com.sun.istack.Nullable
      javax.validation.constraints.NotNull
      org.netbeans.api.annotations.common.CheckForNull
      org.netbeans.api.annotations.common.NullAllowed
      org.netbeans.api.annotations.common.NullUnknown

Manual improvements:
  Improve installation instructions
  Rewrite section on generics (thanks to Bert Fernandez and David Cok)
    Also refactor the generics section into its own chapter
  Rewrite section on @Unused and @Dependent
  New manual section: Writing Java expressions as annotation arguments
  Better explanation of warning suppression
  JSR 308 is planned for Java 8, not Java 7

Stub files:
  Support nested classes by expressing them at top level in binary form: A$B
  Improved error reporting when parsing stub files

Annotated JDK:
  New way of generating annotated JDK
  jdk.jar file no longer appears in repository
  Warning if you are not using the annotated JDK.

Miscellaneous:
  Warn if -source command-line argument does not support type annotations

Many bug fixes
  There are too many to list, but some notable ones are to local type
  inference, generics, pre- and post-conditions (e.g., @NonNullOnEntry,
  @AssertNonNull*), and map keys (@KeyFor).  In particular, preconditions
  and map key annotations are now checked, and if they cannot be verified,
  an error is raised; previously, they were not verified, just unsoundly
  trusted.


Version 1.1.1 (18 Sep 2010)
---------------------------

Eclipse support:
  Removed the obsolete Eclipse plug-in from repository.  The new one uses a
  different repository
  (http://code.google.com/a/eclipselabs.org/p/checker-plugin/) but a user
  obtains it from the same URL as before:
  https://checkerframework.org/eclipse/

Property Key Checker:
  The property key checker allows multiple resource bundles and the
  simultaneous use of both resource bundles and property files.

Javari Checker:
  Added Javari stub classes for more JDK classes.

Distribution:
  Changed directory structure (top level is "checker-framework"; "checkers"
  is a under that) for consistency with version control repository.

Many documentation improvements and minor bugfixes.


Version 1.1.0b, 16 Jun 2010
---------------------------

Fixed a bug related to running binary release in JDK 6


Version 1.1.0 (13 Jun 2010)
---------------------------

Checkers
  Introduced a new simple mechanism for running a checker
  Added one annotated JDK for all checkers

Nullness Checker
  Fixed bugs related to map.get() and KeyFor annotation
  Fixed bugs related to AssertNonNull* and parameters
  Minor updates to the annotated JDK, especially to java.io.File

Manual
  Updated installation instructions
  Clarified section regarding fields and type inference


Version 1.0.9 (25 May 2010)
---------------------------

Nullness Checker:
  Improved Javadocs and manual documentation
  Added two new annotations: AssertNonNullAfter, KeyFor
  Fixed a bug related to AssertNonNullIfFalse and assert statements
  Renamed NonNullVariable to NonNullOnEntry

Checkers:
  Interning: Skipping equality check, if either operands should be skipped
  Fixed a bug related to annotations targeting array fields found in classfile
  Fixed a bug related to method invocation generic type inference
    in static methods

Manual
  Added a section on nullness method annotations
  Revised the Nullness Checker section
  Updated Ant usage instructions


Version 1.0.8 (15 May 2010)
---------------------------

Checkers
  Changed behavior of flow type refinement when annotation is explicit
  Handle array initializer trees (without explicit type)
  Handle the case of Vector.copyInto
  Include javax classes in the distributed jdk jar files

Interning Checker
  Handle interning inference of string concatenation
  Add 20+ @Interned annotations to the JDK
  Add an option, checkclass, to validate the interning
    of specific classes only

Bug fixes
  Fix a bug related to array implicit types
  Lock Checker: Treat null as a bottom type

Manual
  Added a new section about Flow inference and fields


Version 1.0.7 (12 Apr 2010)
---------------------------

Checkers
  Distributed a Maven repository
  Updated stub parser project to latest version (javaparser 1.0.8)
  Fixed bugs related to iterable wildcards and type parameter types


Version 1.0.6 (24 Feb 2009)
---------------------------

Nullness Checker
  Added support for new annotations:
    Pure - indicates that the method, given the same parameters, return the
            same values
    AssertNonNullIfFalse - indicates that a field is NonNull if the method
            returns false
  Renamed AssertNonNull to AssertParametersNonNull
  Updated the annotated jdk

Javari Checker
  Fixed many bugs:
    handle implicit dereferencing of this (e.g. `field` in place of
      `this.field`)
    apply default annotations to method parameters


Version 1.0.5 (12 Jan 2009)
---------------------------

Checkers
  Added support for annotated jdk jars
  Improved readability of some failure messages
  Added AssertNonNullIfTrue support for method parameter references
  Fixed a bug related to LazyNonNull and array fields
  Fixed a bug related to inference and compound assignments (e.g. +=)
  nullness: permit the type of @NonNull Void

Manual
  Updated annotating-libraries chapter regarding annotated jdk


Version 1.0.4 (19 Dec 2009)
---------------------------

Bug Fixes
  wildcards not recognized as subtypes of type variables
    e.g. '? extends A' and 'A'
  PolyNull methods not accepting null literal value arguments
  spurious unexpected Raw warnings

Manual
  Clarified FAQ item regarding why List's type parameter is
    "extends @NonNull Object"


Version 1.0.3 (5 Dec 2009)
--------------------------

Checkers
  New location UPPER_BOUND for DefaultQualifier permits setting the default
    for upper bounds, such as Object in "? extends Object".
  @DefaultQualifier accepts simple names, like @DefaultQualifier("Nullable"),
    rather than requiring @DefaultQualifier("checkers.nullness.quals.Nullable").
  Local variable type inference has improved support for array accesses.
  The repository contains Eclipse project and launch configuration files.
    This is helpful too people who want to build a checker, not to people
    who merely want to run a checker.
  Many bug fixes, including:
    handling wildcard subtyping rules
    stub files and vararg methods being ignored
    nullness and spurious rawness errors
    uses of array clone method (e.g. String[].clone())
    multibound type parameters (e.g. <T extends @A Number & @B Cloneable>)

Manual
  Documented the behavior of annotations on type parameter declarations.
  New FAQ item:
    How to collect warnings from multiple files
    Why a qualifier shouldn't apply to both types and declarations


Version 1.0.2 (16 Nov 2009)
---------------------------

Checkers
  Renamed Regex Checker's @ValidRegex annotation to @Regex
  Improved Collection.toArray() heuristics to be more sound

Bug fixes
  Fixed the annotated JDK to match OpenJDK 6
    - Added missing methods and corrected class hierarchy
  Fixed a crash related to intersection types


Version 1.0.1 (1 Nov 2009)
--------------------------

Checkers
  Added new checkers:
    RegEx checker to detect invalid regular expression use
    Internationalization (I18n) checker to detect internationalization errors

Functionality
  Added more performance optimizations
  nullness: Added support for netbeans nullness annotations
  nullness: better semantics for redundant nullness tests
    related to redundant tests in assertions
  lock: Added support for JCIP annotation in the Lock Checker
  tainting: Added support for polymorphism
  Lock Checker supports the JCIP GuardedBy annotation

Bug fixes
  Fixed a crashing bug related to interaction between
    generic types and wildcards
  Fixed a bug in stub file parser related to vararg annotations
  Fixed few bugs in skeleton file generators

Manual
  Tweak installation instructions
  Reference Units Checker
  Added new sections for new checkers
    RegEx checker (S 10)
    Internationalization Checker (S 11)


Version 1.0.0 (30 Sep 2009)
---------------------------

Functionality
  Added Linear Checker to restrict aliasing

Bug fixes
  Fixed flow erros related to loop controls and break/continue

Manual
  Adopt new term, "Declaration Annotation" instead of non-type annotations
  Added new sections:
    Linear Checker (S 9)
    Inexpressible types (S 14.3)
    How to get started annotating legacy code (S 2.4.4)
  Expanded Tainting Checker section


Version 0.9.9 (4 Sep 2009)
--------------------------

Functionality
  Added more optional lint checks (cast:unsafe, all)
  Nullness Checker supports @SuppressWarnings("nullness:generic.argument"),
    for suppressing warnings related to misuse of generic type arguments.
    This was already supported and documented, but had not been mentioned
    in the changelog.

Bug fixes
  Fixed many bugs related to Stub files causing parser to ignore
    bodiless constructors
    annotated arrays annotations
    type parameter and wildcard bounds annotations

Manual
  Rewrote 'javac implementation survival guide' (S 13.9)
  Restructured 'Using a checker' (S 2)
  Added 'Integration with external tools' (S 14)
  Added new questions to the FAQ (S 15)


Version 0.9.8 (21 Aug 2009)
---------------------------

Functionality
  Added a Tainting Checker
  Added support for conditional nonnull checking
  Added optional check for redundant nullness tests
  Updated stub parser to latest libraries

Bug fixes
  Fixed a bug related to int[] treated as Object when passed to vararg T...
  Fixed a crash related to intersection types
  Fixed a bug related to -AskipClasses not being honored
  Fixed a bug related to flow

Manual
  Added new sections
    8 Tainting Checker
    3.2.3 Conditional nullness


Version 0.9.7 (12 Aug 2009)
---------------------------

Functionality
  Changed swNonNull to castNonNull
  nullness: Improved flow to infer nullness based on method invocations
  locking: Permitted @Holding to appear on constructors

Bug fixes
  Fixed a bug related to typevar and wildcard extends clauses


Version 0.9.6 (29 Jul 2009)
---------------------------

Functionality
  Changed 'jsr308.skipClasses' property with '-AskipClasses' option
  Locking checker
    - Add subtype checking for Holding
    - Treat constructors as synchronized methods

Bug fixes
  Added some missing nullness annotations in the jdk
  Fixed some bugs related to reading stub files

Manual
  Added a new section
    2.10  Tips about writing annotations
  Updated sections of
    2.6   Unused fields and dependent types
    3.1.1 Rawness annotation hierarchy


Version 0.9.5 (13 Jul 2009)
---------------------------

Functionality
  Added support for Findbugs, JSR305, and IntelliJ nullness annotations
  Added an Aggregate Checker base-class
  Added support for a form of field access control

Bug fixes
  Added check for arguments in super() calls in constructors

Manual
  Added new sections:
    Fields access control
    Other tools for nullness checking
    Bundling multiple checkers


Version 0.9.4 (30 Jun 2009)
---------------------------

Functionality
  Added Lock Checker

Bug fixes
  Handle more patterns for determining Map.get() return type

Manual Documentations
  Improved installation instructions
  Added the following sections
    2.6 Dependent types
    3.1 subsection for LazyNonNull
    10.9 When to use (and not to use) type qualifiers


Version 0.9.3 (23 Jun 2009)
---------------------------

Functionality
  Added support DefaultQualifier on packages
  Added support for Dependent qualifier types
    see checkers.quals.Dependent
  Added an option to treat checker errors as warnings
  Improved flow handling of boolean logic

Manual Documentations
  Improved installation instructions
  Improved discussion of effective and implicit qualifiers and defaults
  Added a discussion about the need for bottom qualifiers
  Added sections for how-to
    . suppress Basic Checker warnings
    . troubleshoot skeleton files


Version 0.9.2 (2 Jun 2009)
--------------------------

Functionality
  Added pre-liminary support for lazy initialization in nullness
    see LazyNonNull

Bug fixes
  Corrected method declarations in JDK skeleton files
    - bug resulted in a runtime error

Documentations
  Updated qualifier javadoc documentations
  Corrected a reference on passing qualifiers to javac


Version 0.9.1 (19 May 2009)
---------------------------

Bug fixes
  Eliminated unexpected compiler errors when using checkers
  Fixed bug related to reading annotations in skeleton files

API Changes
  Renamed SourceChecker.process() to .typeProcess()

Manual
  Updated troubleshooting info
    info for annotations in skeleton files


Version 0.9b, 22 Apr 2009
-------------------------

No visible changes


Version 0.9 (16 Apr 2009)
-------------------------

Framework
  More space and performance optimizations
  Handle raw type with multiple type var level
    e.g. class Pair<X, Y extends X> { ... }

Manual
  Improve installation instructions
  Update references to command line arguments


Version 0.8.9 (28 Mar 2009)
---------------------------

Framework
  Introduce Space (and minor performance) optimizations
  Type-check constructor invocation receiver type
  Fixed bug related to try-catch flow sensitivity analysis
  Fixed bugs when type-checking annotations and enums
    - bug results in null-pointer exception


Version 0.8.8 (13 Mar 2009)
---------------------------

Nullness Checker
  Support for custom nullness assertion via @AssertNonNull
  Support for meta-annotation AssertNonNull
  Support for Collection.toArray() method
    Infer the nullness of the returned type
  Corrected some JDK Collection API annotations

Framework
  Fixed bugs related to assignments expressions in Flow
  Fixed bugs related to enum and annotation type hierarchy
  Fixed bugs related to default annotations on wildcard bounds


Version 0.8.7 (27 Feb 2009)
---------------------------

Framework
  Support annotations on type parameters
  Fixed bugs related to polymorphic types/annotations
  Fixed bugs related to stub fixes

Manual
  Specify annotation defaults settings for IGJ
  Update Known Problems section

Version 0.8.6 (3 Feb 2009)
--------------------------

Framework
  Fixed bugs related to flow sensitivity analysis related to
    . for loop and do while loops
    . multiple iterations of a loop
    . complement of logical conditions
  Declarative syntax for string literal type introduction rules
  Support for specifying stub file directories


Version 0.8.5 (17 Jan 2009)
---------------------------

Framework
  Fixed bugs related to flow sensitivity analysis
  Fixed bugs related to annotations on type parameters


Version 0.8.4 (17 Dec 2008)
---------------------------

Distribution
  Included checkers-quals.jar which contains the qualifiers only

Framework
  Fixed bugs related to inner classes
  Fixed a bug related to resolving polymorphic qualifiers
    within static methods

Manual
  Added 'Distributing your annotated project'


Version 0.8.3 (7 Dec 2008)
--------------------------

Framework
  Fixed bugs related to inner classes
  Changed cast semantics
    Unqualified casts don't change cast away (or in) any qualifiers
  Refactored AnnotationBuilder to ease building annotations
  Added support for Object += String new behavior
  Added a type validation check for method return types

Nullness
  Added inference of field initialization
    Suppress false warnings due to method invocations within constructors

IGJ
  Added proper support for AssignsFields and inner classes interactions

Manual
  Updated 'Known Problems' section


Version 0.8.2 (14 Nov 2008)
---------------------------

Framework
  Included a binary distribution in the releases
  Added support for annotations on type parameters
  Fixed bugs related to casts

Nullness
  Improved error messages readability
  Added partial support for Map.get() detection

Manual
  Improved installation instructions


Version 0.8.1 (1 Nov 2008)
--------------------------

Framework
  Added support for array initializers
  Fixed many bugs related to generics and generic type inference

Documentations
  Added 'Getting Started' guide


Version 0.8 (27 Sep 2008)
-------------------------

Framework
  Added support for newly specified array syntax
  Refactored code for annotating supertypes
  Fixed AnnotationBuilder AnnotationMirror string representation
  Fixed AnnotatedTypeMirror hashCode

Manual
  Reorganized 'Annotating Libraries' section


Version 0.7.9 (19 Sep 2008)
---------------------------

Framework
  Added support for stub files/classes
  Fixed bugs related to anonymous classes
  Fixed bugs related to qualifier polymorphism

Manual
  Updated 'Annotating Libraries' section to describe stub files

Tests
  Added support for Windows
  Fixed a bug causing IGJ tests to fail on Windows


Version 0.7.8 (12 Sep 2008)
---------------------------

Framework
  Improved support for anonymous classes
  Included refactorings to ease extensibility
  Fixed some minor bugs

Nullness
  Fix some errors in annotated JDK


Version 0.7.7 (29 Aug 2008)
---------------------------

Framework
  Fixed bugs related to polymorphic qualifiers
  Fixed bugs related to elements array convention
  Add implicit type arguments to raw types

Interning
  Suppress cast warnings for interned classes

Manual
  Removed discussion of non-standard array syntax alternatives


Version 0.7.6 (12 Aug 2008)
---------------------------

Framework
  Changed default array syntax to ARRAYS-PRE, per the JSR 308 specification
  Added an optional check for qualifier unsafe casts
  Added support for running multiple checkers at once
  Fixed bugs related array syntax
  Fixed bugs related to accessing outer classes with-in inner classes

Manual
  Added a new subsection about Checker Auto-Discovery
    2.2.1 Checker Auto-discovery


Version 0.7.5 (2 Aug 2008)
--------------------------

Framework
  Added support for ARRAYS-PRE and ELTS-PRE array syntax
  Added a check for unsafe casts
  Some improvements to the AnnotationBuilder API

Nullness Checker
  Added a check for synchronized objects
  Added a check for (un)boxing conversions

Javari Checker
  Fixed some JDK annotated classes


Version 0.7.4 (11 July 2008)
----------------------------

Framework
  Added support for annotations found in classfiles
  Added support for the ARRAY-IN array syntax
  Added AnnotationBuilder, to create AnotationMirrors with values
  Improved the readability of recursive types string representation

Nullness Checker
  Added a check for thrown Throwable nullability

IGJ Checker
  Treat enums as mutable by default, like regular classes

Manual
  Added a new subsection about array syntax proposals:
    2.1.2 Annotating Arrays


Version 0.7.3 ( 4 July 2008)
----------------------------

Javari Checker
  Converted JDK files into stubs

Nullness Checker
  Fixed java.lang.Number declaration in the annotated jdk

Framework
  Fixed a bug causing crashes related to primitive type boxing
  Renamed DAGQualifierHierarchy to GraphQualifierHierarchy


Version 0.7.2 (26 June 2008)
----------------------------

IGJ Checker
  Supports flow-sensitive type refinement

Framework
  Renamed Default annotation to DefaultQualifier
  Added DefaultQualifiers annotation
  Fixed bugs related to flow-sensitive type refinement
  Fixed an error in the build script in Windows

Manual
  Added a new section
    9.2  javac implementation survival guide
  Added hyperlinks to Javadocs of the referenced classes


Version 0.7.1 (20 June 2008)
----------------------------

Nullness Checker
  Made NNEL the default qualifier scheme

Basic Checker
  Moved to its own checkers.basic package

Framework
  Enhanced type-checking within qualifier-polymorphic method bodies
  Fixed a bug causing StackOverflowError when type-checking wildcards
  Fixed a bug causing a NullPointerException when type-checking
    compound assignments, in the form of +=

Class Skeleton Generator
  Distributed in compiled form (no more special installation instructions)
  Added required asmx.jar library to lib/

Manual
  Added new sections
    2.2.1 Ant tasks
    2.2.2 Eclipse plugin
    2.6   The effective qualifier on a type
  Rewrote section 8 on annotating libraries
    Added reference to the new Eclipse plug-in
    Deleted installation instructions

Javari Checker
  Fixed bugs causing a NullPointerException when type-checking
    primitive arrays

IGJ Checker
  Fixed bugs related to uses of raw types

API Changes
  Moved AnnotationFactory functionality to AnnotationUtils
  Removed .root and .inConflict from DAGQualifierHierarchy


Version 0.7 (14 June 2008)
--------------------------

Installation
  New, very simple installation instructions for Linux.  For other
    operating systems, you should continue to use the old instructions.

Nullness Checker
  Renamed from "NonNull Checker" to "Nullness Checker".
    Renamed package from checkers.nonnull to checkers.nullness.
    The annotation names remain the same.
  Added PolyNull, a polymorphic type qualifier for nullness.

Interning Checker
  Renamed from "Interned Checker" to "Interning Checker".
    Renamed package from checkers.interned to checkers.interning.
    The annotation names remain the same.
  Added PolyInterned, a polymorphic type qualifier for Interning.
  Added support for @Default annotation.

Framework
  Qualifiers
    @PolymorphicQualifier was not previously documented in the manual.
    Moved meta-qualifiers from checkers.metaquals package to checkers.quals.
    Removed @VariableQualifier and @RootQualifier meta-qualifiers.
  Added BasicAnnotatedTypeFactory, a factory that handles implicitFor,
    defaults, flow-sensitive type inference.
  Deprecated GraphQualifierHierarchy; DAGQualifierHierarchy replaces it.
  Renamed methods in QualifierHierarchy.

Manual
  Rewrote several manual sections, most notably:
    2.1.1  Writing annotations in comments for backward compatibility
      (note new -Xspacesincomments argument to javac)
    2.3  Checking partially-annotated programs: handling unannotated code
    2.6  Default qualifier for unannotated types
    2.7  Implicitly refined types (flow-sensitive type qualifier inference)
    8  Annotating libraries
    9  How to create a new checker plugin
  Javadoc for the Checker Framework is included in its distribution and is
    available online at <https://eisop.github.io/cf/api/>.


Version 0.6.4 (9 June 2008)
---------------------------

All Framework
  Updated the distributed JDK and examples to the new location of qualifiers

Javari Checker
  Improved documentation on polymorphism resolution
  Removed redundant code now added to the framework from JavariVisitor,
    JavariChecker and JavariAnnotatedTypeFactory
  Refactored method polymorphism into JavariAnnotatedTypeFactory
  Fixed bug on obtaining type from NewClassTree, annotations at constructor
    invocation are not ignored now
  Refactored polymorphism resolution, now all annotations on parameters and
    receivers are replaced, not only on the return type
  Refactored and renamed internal annotator classes in
    JavariAnnotatedTypeFactory
  Added more constructor tests
  Moved Javari annotations to checkers.javari.quals package


Version 0.6.3 (6 June 2008)
---------------------------

Checker Framework
  Improved documentation and manual
  Treat qualifiers on extends clauses of type variables and wildcard types as
    if present on type variable itself
  Renamed AnnotationRelations to QualifierHierarchy
  Renamed GraphAnnotationRelations to GraphQualifierHierarchy
  Renamed TypeRelations to TypeHierarchy
  Added flow as a supported lint option for all checkers
  Determined the suppress warning key reflectively

Interned Checker
  Moved @Interned annotation to checkers.interned.quals package

NonNull Checker
  Moved nonnull annotations to checkers.nonnull.quals package

Miscellaneous
  Included Javadocs in the release
  Improved documentation for all checkers


Version 0.6.2 (30 May 2008)
---------------------------

Checker Framework API
  Added support for @Default annotation via TreeAnnotator
  Added support for PolymorphicQualifier meta-annotation
  Disallow the use of @SupportedAnnotationTypes on checkers
  Fixed bugs related to wildcards with super clauses
  Improved flow-sensitive analysis for fields

Javari Checker
  Moved Javari qualifiers from checkers.quals to checkers.javari.quals
  Fixed bugs causing null pointer exceptions

NonNull Checker
  Fixed bugs related to nonnull flow
  Added new tests to test suite

Basic Checker
  Renamed Custom Checker to Basic Checker


Version 0.6.1 (26 Apr 2008)
---------------------------

Checker Framework API
  Added support for @ImplicitFor meta-annotations via the new TypeAnnotator
    and TreeAnnotator classes
  Improved documentation and specifications
  Fixed a bug related to getting supertypes of wildcards
  Fixed a crash on class literals of primitive and array types
  Framework ignores annotations that are not part of a type system
  Fixed several minor bugs in the flow-sensitive inference implementation.

IGJ Checker
  Updated the checker to use AnnotationRelations and TypeRelations

Javari Checker
  Changing RoMaybe annotation to PolyRead
  Updated checker to use AnnotationRelations and TypeRelations
  Updated the JDK
  Fixed bugs related to QReadOnly and type argument subtyping
  Fixed bugs related to this-mutable fields in methods with @ReadOnly receiver
  Fixed bugs related to primitive type casts
  Added new tests to test suit

NonNull Checker
  Updated the annotated JDK
  Fixed bugs in which default annotations were not correctly applied
  Added @Raw types to handle partial object initialization.
  Fixed several minor bugs in the checker implementation.

Custom Checker
  Updated checker to use hierarchy meta-annotations, via -Aquals argument


Version 0.6 (11 Apr 2008)
-------------------------

Checker Framework API
  Introduced AnnotationRelations and TypeRelations, more robust classes to
    represent type and annotation hierarchies, and deprecated
    SimpleSubtypeRelation
  Add support for meta-annotations to declare type qualifiers subtype relations
  Re-factored AnnotatedTypes and AnnotatedTypeFactory
  Added a default implementation of SourceChecker.getSuppressWarningsKey()
    that reads the @SuppressWarningsKey class annotation
  Improved support for multidimensional arrays and new array expressions
  Fixed a bug in which implicit annotations were not being applied to
    parenthesized expressions
  Framework ignores annotations on a type that do not have @TypeQualifier
  Moved error/warning messages into "messages.properties" files in each
    checker package
  Fixed a bug in which annotations were inferred to liberally by
    checkers.flow.Flow

Interned Checker
  Added heuristics that suppress warnings for certain comparisons (namely in
    methods that override Comparator.compareTo and Object.equals)
  The Interned checker uses flow-sensitive inference by default

IGJ Checker
  Fixed bugs related to resolving immutability variable in method invocation
  Fixed a bug related to reassignability of fields
  Add more tests

Javari Checker
  Added placeholder annotation for ThisMutable mutability
  Re-factored JavariAnnotatedTypeFactory
  Fixed self-type resolution for method receivers for readonly classes
  Fixed annotations on parameters of readonly methods
  Fixed type validation for arrays of primitives
  Added more tests
  Renamed @RoMaybe annotation to @PolyRead

NonNull Checker
  Removed deprecated checkers.nonnull.flow package
  Fixed a bug in which default annotations were not applied correctly

Miscellaneous
  Improved Javadocs
  Added FactoryTestChecker, a more modular tester for the annotated type
    factory
  Simplify error output for some types by stripping package names


Version 0.5.1 (21 Mar 2008)
---------------------------

Checker Framework API
  Added support for conditional expression
  Added checks for type validity and assignability
  Added support for per-checker customization of asMemberOf
  Added support for type parameters in method invocation,
    including type inference
  Enhanced performance of AnnotatedTypeFactory
  Checkers run only when no errors are found by Javac
  Fixed bugs related AnnotationUtils.deepCopy()
  Fixed support for annotated class type parameters
  Fixed some support for annotated type variable bounds
  Added enhancements to flow-sensitive qualifier inference
  Added checks for type parameter bounds

Interned Checker
  Fixed some failing test cases
  Fixed a bug related to autoboxing/unboxing
  Added experimental flow-sensitive qualifier inference (use
    "-Alint=flow" to enable)
  Improved subtype testing, removing some spurious errors

IGJ Checker
  Deleted IGJVisitor!
  Fixed some bugs related to immutability type variable resolution

Javari Checker
  Removed redundant methods from JavariVisitor in the new framework
  Added support to constructor receivers
  Added support to parenthesized expressions
  Fixed a bug related to resolving RoMaybe constructors
  Fixed a bug related to parsing conditional expressions
  Added parsing of parenthesized expressions
  Replaced checkers.javari.VisitorState with
    checkers.types.VisitorState, present in BaseTypeVisitor
  Modified JavariVisitor type parameters (it now extends
    BaseTypeVisitor<Void, Void>, not BaseTypeVisitor<Void,
    checkers.javari.VisitorState>)
  Modified JavariAnnotatedTypeFactory TreePreAnnotator to mutate a
    AnnotatedTypeMirror parameter instead of returning a
    List<AnnotationMirror>, in accordance with other parts of the
    framework design
  Modified test output format
  Added tests to test suite

NonNull Checker
  Fixed a bug related to errors produced on package declarations
  Exception parameters are now treated as NonNull by default
  Added better support for complex conditionals in NonNull-specific
    flow-sensitive inference
  Fixed some failing test cases
  Improved subtype testing, removing some spurious errors

Custom Checker
  Added a new type-checker for type systems with no special semantics, for
    which annotations can be provided via the command line

Miscellaneous
  Made corrections and added more links to Javadocs
  A platform-independent binary version of the checkers and framework
    (checkers.jar) is now included in this release


Version 0.5 (7 Mar 2008)
------------------------

Checker Framework API
  Enhanced the supertype finder to take annotations on extends and
    implements clauses of a class type
  Fixed a bug related to checking an empty array initializer ("{}")
  Fixed a bug related to missing type information when multiple
    top-level classes are defined in a single file
  Fixed infinite recursion when checking expressions like "Enum<E
    extends Enum<E>>"
  Fixed a crash in checkers.flow.Flow related to multiple top-level
    classes in a single file
  Added better support for annotated wildcard type bounds
  Added AnnotatedTypeFactory.annotateImplicit() methods to replace
    overriding the getAnnotatedType() methods directly
  Fixed a bug in which constructor arguments were not checked

Interned Checker
  Fixed a bug related to auto-unboxing of classes for primitives
  Added checks for calling methods with an @Interned receiver

IGJ Checker
  Implemented the immutability inference for self-type (type of
    'this') properly
  Enhanced the implicit annotations to make an un-annotated code
    type-check
  Fixed bugs related to invoking methods based on a method's receiver
    annotations

Javari Checker
  Restored in this version, after porting to the new framework

NonNull Checker
  Fixed a bug in which primitive types were considered possibly null
  Improvements to support for @Default annotations

Miscellaneous
  Improved error message display for all checkers


Version 0.4.1 (22 Feb 2008)
---------------------------

Checker Framework API
  Introduced AnnotatedTypeFactory.directSupertypes() which finds the
    supertypes as annotated types, which can be used by the framework.
  Introduced default error messages analogous to javac's error messages.
  Fixed bugs related to handling array access and enhanced-for-loop type
    testing.
  Fixed several bugs that are due AnnotationMirror not overriding .equals()
    and .hashCode().
  Improved Javadocs for various classes and methods.
  Fixed several bugs that caused crashes in the checkers.
  Fixed a bug where varargs annotations were not handled correctly.

IGJ Checker
  Restored in this version, after porting the checker to the new framework.

NonNull Checker
  Fixed a bug where static field accesses were not handled correctly.
  Improved error messages for the NonNull checker.
  Added the NNEL (NonNull Except Locals) annotation default.

Interned Checker
  Fixed a bug where annotations on type parameter bounds were not handled
    correctly.
  Improved error messages for the Interned checker.


Version 0.4 (11 Feb 2008)
-------------------------

Checker Framework API
  Added checkers.flow, an improved and generalized flow-sensitive type
    qualifier inference, and removed redundant parts from
    checkers.nonnull.flow.
  Fixed a bug that prevented AnnotatedTypeMirror.removeAnnotation from working
    correctly.
  Fixed incorrect behavior in checkers.util.SimpleSubtypeRelation.

NonNull Checker
  Adopted the new checkers.flow.Flow type qualifier inference.
  Clarifications and improvements to Javadocs.


Version 0.3.99 (20 Nov 2007)
----------------------------

Checker Framework API
  Deprecated AnnotatedClassType, AnnotatedMethodType, and AnnotationLocation
    in favor of AnnotatedTypeMirror (a new representation of annotated types
    based on the javax.lang.model.type hierarchy).
  Added checkers.basetype, which provides simple assignment and
    pseudo-assignment checking.
  Deprecated checkers.subtype in favor of checkers.basetype.
  Added options for debugging output from checkers: -Afilenames, -Ashowchecks

Interned Checker
  Adopted the new Checker Framework API.
  Fixed a bug in which "new" expressions had an incorrect type.

NonNull Checker
  Adopted the new Checker Framework API.

Javari Checker
IGJ Checker
  Removed in this version, to be restored in a future version pending
    completion of updates to these checkers with respect to the new framework
    API.


Version 0.3 (1 Oct 2007)
------------------------

Miscellaneous Changes
  Consolidated HTML documentation into a single user manual (see the "manual"
    directory in the distribution).

IGJ Checker
  New features:
    Added a test suite.
    Added annotations (skeleton files) for parts of java.util and java.lang.

NonNull Checker
  New features:
    @SuppressWarnings("nonnull") annotation suppresses checker warnings.
    @Default annotation can make NonNull (not Nullable) the default.
    Added annotations (skeleton classes) for parts of java.util and java.lang.
    NonNull checker skips no classes by default (previously skipped JDK).
    Improved error messages: checker reports expected and found types.

  Bug fixes:
    Fixed a null-pointer exception when checking certain array accesses.
    Improved checking for field dereferences.

Interned Checker
  New features:
    @SuppressWarnings("interned") annotation suppresses checker warnings.
    The checker warns when two @Interned objects are compared with .equals

  Bug fixes:
    The checker honors @Interned annotations on method receivers.
    java.lang.Class types are treated as @Interned.

Checker Framework API
  New features:
    Added support for default annotations and warning suppression in checkers


Version 0.2.3 (30 Aug 2007)
---------------------------

IGJ Checker
  New features:
    changed @W(int) annotation to @I(String) to improve readability
    improved readability of error messages
    added a test for validity of types (testing @Mutable String)

  Bug fixes:
    fixed resolving of @I on fields on receiver type
    fixed assignment checking assignment validity for enhanced for loop
    added check for constructor invocation parameters

Interned Checker
  added the Interned checker, for verifying the absence of equality testing
    errors; see "interned-checker.html" for more information

Javari Checker
  New features:
    added skeleton classes for parts of java.util and java.lang with Javari
      annotations

  Bug fixes:
    fixed readonly inner class bug on Javari Checker

NonNull Checker
  New features:
    flow-sensitive analysis for assignments from a known @NonNull type (e.g.,
      when the right-hand of an assignment is @NonNull, the left-hand is
      considered @NonNull from the assignment to the next possible
      reassignment)
    flow-sensitive analysis within conditional checks

  Bug fixes:
    fixed several sources of null-pointer errors in the NonNull checker
    fixed a bug in the flow-sensitive analysis when a variable was used on
      both sides of the "=" operator

Checker Framework API
  New features:
    added the TypesUtils.toString() method for pretty-printing annotated types
    added AnnotationUtils, a utility class for working with annotations and
      their values
    added SourceChecker.getDefaultSkipPattern(), so that checkers can
      individually specify which classes to skip by default
    added preliminary support for suppressing checker warnings via
      the @SuppressWarnings annotation

  Bug fixes:
    fixed handling of annotations of field values
    InternalAnnotation now correctly uses defaults for annotation values
    improved support for annotations on class type parameter bounds
    fixed an assertion violation when compiling certain uses of arrays


Version 0.2.2 (16 Aug 2007)
---------------------------


Code Changes

* checkers.igj
    some bug fixes and improved documentation

* checkers.javari
    fixed standard return value to be @Mutable
    fixed generic and array handling of @ReadOnly
    fixed @RoMaybe resolution of receivers at method invocation
    fixed parsing of parenthesized trees and conditional trees
    added initial support for enhanced-for loop
    fixed constructor behavior on @ReadOnly classes
    added checks for annotations on primitive types inside arrays

* checkers.nonnull
    flow sensitive analysis supports System.exit, new class/array creation

* checkers.subtype
    fixes for method overriding and other generics-related bugs

* checkers.types
    added AnnotatedTypeMirror, a new representation for annotated types that
      might be moved to the compiler in later version
    added AnnotatedTypeScanner and AnnotatedTypeVisitor, visitors for types
    AnnotatedTypeFactory uses GenericsUtils for improved handing of annotated
      generic types

* checkers.util
    added AnnotatedTypes, a utility class for AnnotatedTypeMirror
    added GenericsUtils, a utility class for working with generic types

* tests
    modified output to print only missing and unexpected diagnostics
    added new test cases for the Javari Checker


Documentation Changes

* checkers/igj-checker.html
    improvements to page

* checkers/javari-checker.html
    examples now point to test suit files

Miscellaneous Changes

* checkers/build.xml
    Ant script fails if it doesn't find the correct JSR 308 javac version


Version 0.2.1 (1 Aug 2007)
--------------------------


Code Changes

* checkers.igj & checkers.igj.quals
    added an initial implementation for the IGJ language

* checkers.javari
    added a state parameter to the visitor methods
    added tests and restructured the test suite
    restructured and implemented RoMaybe
    modified return type to be mutable by default
    fixed mutability type handling for type casts and field access
    fixed bug, ensuring no primitives can be ReadOnly
    a method receiver type is now based on the correct annotation
    fixed parameter type-checking for overridden methods
    fixed bug on readonly field initialization
    added handling for unary trees

* checkers.nonnull
    added a tests for the flow-senstive analysis and varargs methods
    improved flow-sensitive analysis: else statements, asserts,
      return/throw statements, instanceof checks, complex conditionals with &&
    fixed a bug in the flow-sensitive analysis that incorrectly inferred
      @NonNull for some elements
    removed NonnullAnnotatedClassType, moving its functionality into
      NonnullAnnotatedTypeFactory

* checkers.source
    SourceChecker.getSupportedAnnotationTypes() returns ["*"], overriding
      AbstractProcessor.getSupportedAnnotationTypes(). This enables all
      checkers to run on unannotated code

* checkers.subtypes
    fixed a bug pertaining to method parameter checks for overriding methods
    fixed a bug that caused crashes when checking varargs methods

* checkers.types
    AnnotatedTypeFactory.getClass(Element) and getMethod(Element) use the
      tree of the passed Element if one exists
    AnnotatedClassType.includeAt, .execludeAt, .getAnnotationData were
      added and are public
    added constructor() and skipParens() methods to InternalUtils
    renamed getTypeArgumentLocations() to getAnnotatedTypeArgumentLocations()
      in AnnotatedClassType
    added AnnotationData to represent annotations instead of Class instances;
      primarily allows querying annotation arguments
    added switch for whether or not to use includes/excludes in
      AnnotatedClassType.hasAnnotationAt()

* checkers.util
    added utility classes
    added skeleton class generator utility for annotating external libraries


Documentation Changes

* checkers/nonnull-checker.html
    added a note about JML
    added a caveat about variable initialization

* checkers/README-checkers.html
    improvements to instructions


Version 0.2 (2 Jul 2007)
------------------------


Code Changes

* checkers.subtype
    subtype checker warns for annotated and redundant typecasts
    SubtypeVisitor checks for invalid return and parameter types in overriding
      methods
    added checks for compound assignments (like '+=')

* checkers.source
    SourceChecker honors the "checkers.skipClasses" property as a regex for
      suppressing warnings from unannotated code (property is "java.*" by
      default)
    SourceVisitor extends TreePathScanner<R,P> instead of
      TreeScanner<Void,Void>

* checkers.types
    AnnotatedClassType.isAnnotatedWith removed
    AnnotatedClassType.getInnerLocations renamed to getTypeArgumentLocations
    AnnotatedClassType.include now removes from the exclude list (and
      vice-versa)
    AnnotatedClassType.setElement and setTree methods are now public

* checkers.nonnull
    added a flow-sensitive analysis for inferring @NonNull in "if (var !=
      null)"-style checks
    added checks for prefix and postfix increment and decrement operations

* checkers.javari
    added initial implementation of a type-checker for the Javari language


Version 0.1.1 (7 Jun 2007)
--------------------------


Documentation Changes

* checkers/nonnull-checker.html
    created "Tiny examples" subsection
    created "Annotated library" subsection
    noted where to read @NonNull-annotated source
    moved instructions for unannotated code to README-checkers.html
    various minor corrections and clarifications

* checkers/README-checkers.html
    added cross-references to other Checker Framework documents
    removed redundant text
    moved instructions for unannotated code from nonnull-checker.html
    various minor corrections and clarifications

* checkers/creating-a-checker.html
    added note about getSupportedSourceVersion
    removed line numbers from @Interned example
    added section on SubtypeChecker/SubtypeVisitor
    various minor corrections and clarifications


Code Changes

* checkers.subtype
    removed deprecated getCheckedAnnotation() mechanism
    added missing package Javadocs
    package Javadocs reference relevant HTML documentation
    various improvements to Javadocs
    SubtypeVisitor and SubtypeChecker are now abstract classes
    updated with respect to preferred usages of
      AnnotatedClassType.hasAnnotationAt and AnnotatedClassType.annotateAt

* checkers.source
    added missing package Javadocs
    package Javadocs reference relevant HTML documentation

* checkers.types
    added missing package Javadocs
    package Javadocs reference relevant HTML documentation
    AnnotatedClassType.annotateAt now correctly handles
      AnnotationLocation.RAW argument
    AnnotatedClassType.annotate deprecated in favor of
      AnnotatedClassType.annotateAt with AnnotationLocation.RAW as an argument
    AnnotatedClassType.isAnnotatedWith deprecated in favor of
      AnnotatedClassType.hasAnnotationAt with AnnotationLocation.RAW as an
      argument
    Added fromArray and fromList methods to AnnotationLocation and made
      corresponding constructors private.

* checkers.quals
    added Javadocs and meta-annotations on annotation declarations where
      missing
    package Javadocs reference relevant HTML documentation

* checkers.nonnull
    various improvements to Javadocs
    package Javadocs reference relevant HTML documentation


Miscellaneous Changes

    improved documentation of ch examples
    Checker Framework build file now only attempts to compile .java files


Version 0.1.0 (1 May 2007)
--------------------------

Initial release.
