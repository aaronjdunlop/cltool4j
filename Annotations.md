Command-line arguments are parsed according to the `@Option` and `@Argument` annotations on instance variables or methods.

For example:
```
    @Option(name = "-a", metaVar = "value", usage = "Option a")
    private int a;

    @Argument(index = 0, required = true, metaVar = "Argument b")
    private String b;

    @Argument(index = 1, required = false, metaVar = "Argument c")
    public void setC(float c) {
        ...
```

In this example, the `-a` flag is optional, but the program requires (at least) one argument (`b`). If a second argument is supplied, it is parsed as a float and passed to `setC(float c)`.

By default, `@Option` and `@Argument` support the following types. The object wrapper types for all primitive types are also supported (e.g. `java.lang.Integer`). It is relatively straightforward to implement custom parsers for other types.
  * `byte`
  * `char`
  * `double`
  * `java.io.File`
  * `float`
  * `int`
  * `long`
  * `java.lang.String`
  * `java.net.URI`
  * `java.net.URL`
  * `java.util.Calendar`
  * `java.util.Date`

Note: The patterns supported for `java.util.Calendar` and `java.util.Date` are listed in `cltool4j.args4j.CalendarParser`.

Details of the parameters supported by these annotations are available in the JavaDoc for [@Option](http://wiki.cltool4j.googlecode.com/hg/javadoc/cltool4j/args4j/Option.html) and [@Argument](http://wiki.cltool4j.googlecode.com/hg/javadoc/cltool4j/args4j/Argument.html)