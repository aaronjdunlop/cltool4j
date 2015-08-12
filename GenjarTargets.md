# Simple example #
```
    <import file="genjar-targets.xml" />

...

    <target name="foo" depends="compile">
        <antcall target="package-tool">
            <param name="root-class" value="org.foo.Foo" />
            <param name="tool-name" value="foo" />
        </antcall>
    </target>
```

At a minimum, we need only specify the tool class (`root-class`) and the name of the tool. The tool will be generated into `foo.jar`, and a wrapper script into `foo`. The wrapper script specifies the most common Java VM arguments, including `-server`, heap size, and garbage collection methods. It supports most Unix variants, including Linux, Mac OS, Solaris, and Cygwin. If you wish to use the wrapper script, it should be deployed in the same directory as the jar file.

# Detailed example #

The `package-tool` target offers many other configuration options as well. e.g.:
```
    <target name="foo" depends="compile">
        <antcall target="package-tool">
            <param name="root-class" value="org.foo.Foo" />
            <param name="tool-name" value="foo" />
            <param name="heap-size" value="1500m" />
            <param name="package-gpl-libs" value="true" />
            <param name="additional-file-root" value="src" />
            <param name="additional-file-includes" value="**/*.cl" />
            <param name="license-file" value="java/tools/license.txt" />
            <param name="readme-file" value="java/tools/README.txt" />
            <param name="default-options" value="java/tools/defaults.properties" />
            <param name="srcjar" value="true" />
        </antcall>
    </target>
```

Arguments:
  * root-class - Primary root classname (required)
  * additional-root-classes - Additional root classnames, comma-delimited (optional)
  * additional-libs (optional) List of any additional jars which should be included in their entirety
  * additional-file-root, additional-file-includes (optional) Fileset specifying any additional files which should be included
  * heap-size (optional - default=128m) Heap size (e.g. 20m, 1g)
  * package-gpl-libs (optional) Package GPL and LGPL libraries in generated jar
  * reference-lgpl-libs (optional) Reference LGPL libraries in generated jar classpath. For non-GPL code, we don't want to package LGPL libraries directly into the generated jar file. For GPL code, see package-gpl-libs.
  * version (optional) Version number to include in jar file MANIFEST
  * readme-file (optional) Location of a README file for the tool. If the file exists, it will be packaged as META-INF/README.txt
  * license-file (optional) Location of a license file for the tool. If the file exists, it will be packaged as META-INF/LICENSE.txt
  * default-options (optional) Location of a properties file specifying default options for the tool. If the file exists, it will be packaged as META-INF/defaults.properties
  * srcjar (optional) If true, build a source jar as well as the class-file jar

The following properties must be configured:
  * dir.build
  * dir.dist
  * dir.bsd.lib
  * dir.lgpl.lib
  * dir.gpl.lib
  * dir.src (if srcjar is specified)