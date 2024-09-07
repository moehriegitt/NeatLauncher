# Development

## Gradle Wrapper

I refuse to check in .jar files into git.  It is wrong.  If it was a .java file to be compiled, OK, it it is a .jar.

To start developing, the missing gradle wrapper files can be generated with gradle.  E.g., I use something like:

```sh
$HOME/.gradle/wrapper/dists/gradle-8.7-bin/*/gradle-8.7/bin/gradle wrapper
```

## Size

This is supposed to stay small.  Try not to add unnecessary features, particularly no bells ands whistles.
