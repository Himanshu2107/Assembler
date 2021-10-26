# Assembler
A two pass assembler that assembles assembly language programs to machine language. This assembler is for Morris Mano's Basic Computer <sup>1</sup>. 
The two pass assembler assembles a program using two passes. In the first pass, all of the labels' addresses are stored in a symbol table, called the address symbol 
table. In the second pass the actual translations take place. 

The details of the assembly language and the machine on which it runs can be found in [1]. An additional restriction this assembler assumes is that labels are always 
three letters long. 

The program takes as input a .txt file. It outputs the machine code as a string of 1's and 0's in a .txt file, named as a.txt. Each instruction in the output consists 
of two parts. First the address of the instruction in memory and after that its actual binary code. The project also contains two test files which you can use to test
the working of the assembler. The program does not check for errors in the input and assumes that the input is a valid Basic Computer assembly program. The behaviour
is unpredictable in case of invalid input.

## Compilation

`javac Assembler.java`

## Usage

The file you want to translate should be provided as a command line argument, as follows.

`java Assembler fileName.txt`

The input must be a correct Basic Computer assembly language program.

## References

1. Computer System Architecture 3e, Morris M. Mano. `
