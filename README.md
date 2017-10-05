# CompilerProject

## Compile Instructions:
To compiler the parser run the following java command 
javac IntermediateCodeGenerator.java

To execute the parser run the following command in the terminal:
java IntermediateCodeGenerator <inputFileName>

An example of the parser execution is:
java IntermediateCodeGenerator foo.c

The output will be stored in a file with _gen appended to original name at the same path as original.

## Important files:
IntermediateCodeGenerator.java: Its the main class used to call scanner, parser and code generators. 
