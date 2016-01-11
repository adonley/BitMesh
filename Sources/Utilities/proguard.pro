-injars       out/artifacts/bitmeshutil_1_0_1/bitmeshutil-1.0.1.jar
-outjars      ../../Libraries/bitmeshutil-1.0.0.jar
-libraryjars  <java.home>/lib/rt.jar
-libraryjars  lib/commons-codec-1.4.jar
-libraryjars  lib/slf4j-api-1.7.6.jar
-libraryjars  lib/bcprov-jdk15on-152.jar
-libraryjars  lib/slf4j-simple-1.7.6.jar
-printmapping out/artifacts/bitmeshutil_1_0_1/bitmesh-util.map
-overloadaggressively
-optimizationpasses 5

-keepparameternames
-renamesourcefileattribute SourceFile
-keepattributes Exceptions,InnerClasses,Signature,Deprecated,
                SourceFile,LineNumberTable,*Annotation*,EnclosingMethod

-keep public class * {
    public protected *;
}

-keepclassmembernames class * {
    java.lang.Class class$(java.lang.String);
    java.lang.Class class$(java.lang.String, boolean);
}

-keepclasseswithmembernames,includedescriptorclasses class * {
    native <methods>;
}

-keepclassmembers,allowoptimization enum * {
    public static **[] values();
    public static ** valueOf(java.lang.String);
}
