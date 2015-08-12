## Overview ##
**cltool4j** is a simple framework intended to speed development of
command-line tools in Java.

Some systems, especially scripting languages, handle argument parsing and I/O
intrinsically, leaving the developer to concentrate on the actual
task. Java provides a huge array of libraries and tools, but simple
argument and I/O handling in Java require considerably more code than
in Awk, Python, Perl, etc. So a Java developer is often torn between
quick development in a scripting language and desire to reuse existing
Java code (without resorting to copy-and-pasting (yet again) the
boilerplate code from previous projects).

The fundamental design goal of cltool4j is to simplify development of
command-line Java tools.

The `style' of tool comes from the standard Unix toolset:
  * Input is read from any files specified on the command-line, or from `STDIN` if no input files are specified.
  * Output is written to `STDOUT`

We recommend use of the [genjar2](http://code.google.com/p/genjar2) project to simplify packaging standalone tools. The [Downloads](http://code.google.com/p/cltool4j/downloads/list) page includes an Ant build target to automate this task (see [GenjarTargets](GenjarTargets.md)). Command-line arguments are parsed with a parsing system derived from the [Args4J](http://weblogs.java.net/blog/kohsuke/archive/2005/05/parsing_command.html) system. The annotations are described in the [Annotations](Annotations.md) wiki page. For further information, see the complete [JavaDoc](http://wiki.cltool4j.googlecode.com/hg/javadoc/index.html)

With no further ado, on to an example; a simple pattern matcher,
analagous to the ubiquitous 'grep', supporting match inversion (a'la
`grep -v`, but implemented here with `-i`, since `-v` is used by the
superclass), and trailing context (`-C`):

```
import cltool4j.BaseCommandlineTool;
import cltool4j.args4j.Argument;
import cltool4j.args4j.Option;

// A simple pattern matcher
public class Jgrep extends BaseCommandlineTool {

    @Option(name = "-C", metaVar = "lines", usage = "Print lines of context following a match")
    private int contextLines;

    // Use -i for invert; -v is used for in superclass for 'verbose'
    @Option(name = "-i", usage = "invert match")
    private boolean inverse;

    // Use the first (0th) argument as the pattern to match. Any other
    // arguments are assumed to be input files
    @Argument(index = 0, required = true, metaVar = "pattern")
    private String pattern;

    @Override
    protected void run() throws Exception {

        int currentContextLines = 0;

        for (String line : inputLines()) {

            if ((inverse && !line.contains(pattern)) || (!inverse && line.contains(pattern))) {
                System.out.println(line);
                currentContextLines = contextLines;

            } else if (currentContextLines > 0) {
                // Print out lines of context (for simplicity, only _after_ matches)
                System.out.println(line);
                currentContextLines--;
            }
        }
    }

    // We have to define main() and call the superclass' run() method
    public static void main(String[] args) {
        run(args);
    }
}
```

### Usage Information ###
Help / usage information is generated automatically using the
parameter annotations. e.g.:

```
$ java Jgrep -?
Usage: Jgrep [-help] [-O option / file] [-v level] [-version] [-C lines] [-i] <pattern> [files]
 -help (--help,-?)    : Print detailed usage information
 -O option / file     : Option or option file (file in Java properties format or option as key=value)
 -v level             : Verbosity  (all,+5,5; finest,+4,4; finer,+3,3; fine,+2,2,debug; config,+1,1; info,0;
                        warning,-1; severe,-2; off,-3)   Default = info
 -version (--version) : Print version information
 -C lines             : Print lines of context following a match
 -i                   : invert match
```

We see several options provided and supported by the base class and
the `-C` and `-i` options implemented in Jgrep. Also note that in the
usage line, the 'pattern' argument is denoted as required with `<...>`;
optional parameters use `[...]`. This is controlled by the `required`
parameter to the `@Option` and `@Argument` annotations.

### Options in `BaseCommendlineTool` ###
  * `-help`: Prints out usage information
  * `-readme`: Prints out the `META-INF/readme.txt` file, if present in CLASSPATH. If `META-INF/readme.txt` is not present, the option is hidden in help.
  * `-license`: Similar to `-readme`, but prints `META-INF/license.txt`.
  * `-O`: Options. Specifies configuration options, either using `-O option=value` format or by specifying a file in standard Java property file format. Multiple `-O` options are allowed.
  * `-v`: Verbosity. Controls the verbosity level of the base `java.util.logging.Logger` instance (accessible using `Logger.getLogger("")` or as `BaseLogger.singleton()`.
  * `-version`: Prints the 'Version' attribute from META-INF/MANIFEST.MF (if present)
  * `-xt`: Maximum threads (see Threading, below). Only enabled if the class is annotated `@Threadable`


### Threading ###

Threading is handled through a class annotation. If your
code is thread-safe and your task is large enough to benefit, simply
annotate your class with `@Threadable`, and the `-xt` option will be
enabled. By default, classes annotated `@Threadable` will be executed
with the same number of threads as available processors
(`Runtime.availableProcessors()`). You can change this behavior with the
`defaultThreads` parameter of the `@Threadable` annotation
(e.g. `@Threadable(defaultThreads = 2)`.


### Multi-file input ###

As already mentioned, the motivation for cltool4j is to enable easy
development of tools which "feel" like Unix tools. Of course, there is
some variation, even in the Unix toolset itself. For example, `cat foo
bar` outputs the contents of foo and bar, without denoting file
boundaries. `wc foo bar`, however, outputs separate counts for each
file. The normal usage of cltool4j matches the `cat` example - all
input is read as a single stream. However, it does include an optional
callback mechanism to notify the tool when a new input file is
encountered. This enables implementing tools of the `wc` style,
although that need is somewhat rare.
