java_file = BitMeshDataCollection
c_file = ./BitMeshDataCollection.c

all: jni c

c: 
	gcc -Wall -shared -fpic -o libbitmeshpcap.so -I/usr/lib/jvm/java-1.7.0-openjdk-amd64/include $(c_file) -lpcap
jni:
	javac $(java_file).java 
	javah -jni $(java_file)
run:
	java -Djava.library.path=. $(java_file)

