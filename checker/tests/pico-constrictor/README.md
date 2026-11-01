# Annotation Conversion

| Constrictor | PICO |
| ----- | -------- |
| @Immutable Class | @Immutable Class and all its subclass|
| @Immutable method | @Readonly receiver parameter |
| @Viewmethod method | @Readonly receiver parameter|
| Field not returned in @Viewmethod | @Assignable or @Mutable Field annotation |
