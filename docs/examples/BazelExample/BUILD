java_binary(
    name = "example",
    srcs = ["BazelExample.java"],
    main_class = "BazelExample",
    deps = ["@maven//:io_github_eisop_checker_qual"],
    plugins = ["nullness"],
    javacopts = [
        "-Xlint:-options",
        "-Werror",
    ],
)

java_plugin(
    name = "nullness",
    srcs = ["BazelExample.java"],
    processor_class = "org.checkerframework.checker.nullness.NullnessChecker",
    deps = [
        "@maven//:io_github_eisop_checker_qual",
        "@maven//:io_github_eisop_checker",
    ],
)
