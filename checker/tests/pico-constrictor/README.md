- [done] inheritence_benchmarks
  - [done]Lists, found what constrictor found.
  - [done]Points, found what constrictor found.
  - [done]Sets, found what constrictor found.
    - Also found the error missed by constrictor in WrongImplMoveFrontListSet.java GOOD!
    - The MoveToFrontListSet result seems to be correct but spurious according to constrictor paper.
  - [done]Shapes, almost found what constrictor found.
    - EvilMemoizedRectangle and LeakyMemoizedRectangle does not work as expected because PICO does not check observability. BAD!
    - Something could be done for MemoizedRectangle to improve above case, but does not solve the problem.
- [done] non_inheritence_benchmarks
  - CachedList.java, the code is wrong? cached_val should not be in the abstract state but marked as viewmethod? Anyway, constrictor found an error and they claim there should be no error. I add @Assignable to both caching fields.
  - DefaultDict.java, what should be in the abstract state? Constrictor said viewmethod should not modify the abstract state.....
  - EvilBinarySearchTree.java, found error while constrictor time out. GOOD!
  - EvilUnionFind, PICO found the error, but not sure if the same error constrictor found....
  - LongLoopMutator.java, PICO found the error while constrictor time out. GOOD!
  - StringShuffler.java, how can this be fine???
  - UnionFind, PICO found the error, constrictor says no error should be found....
  - UnreachablyMutating.java, both PICO and constrictor found the error (fails) but can be improved by dataflow analysis in PICO.
  - VariableTypesMatter.java, PICO is not precise enough to know whether one assignment is observable or not.

annotation conversion:
| Constrictor | PICO |
| ----- | -------- |
| @Immutable Class | @Immutable Class and all its subclass|
| @Immutable method | @Readonly receiver parameter |
| @Viewmethod method | @Readonly receiver parameter|
| Field not returned in @Viewmethod | @Assignable or @Mutable Field annotation |
