# Developer Guide

## To be compatible with BEAST 2.7.x framework

Either running application or unit tests from IDE or Gradle tasks 
must load the "version.xml" file, which defines the services 
to be added into BEASTClassLoader.

So the "version.xml" file in the project root is used for release, 
but those in each sub-project folders are used for unit tests only.

The BEAST2 packages in the dependencies have to be installed, 
which will provide the "version.xml" file. 
The set of jar files in the "lib" folder are used for IDE development.
