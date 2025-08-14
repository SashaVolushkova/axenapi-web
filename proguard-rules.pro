-target 17
-dontshrink
-dontoptimize
-dontobfuscate

-keep class pro.axenix_innovation.axenapi.web.AxenApiWebApplication {
    public static void main(java.lang.String[]);
}

-keepclassmembers class * {
    @org.springframework.beans.factory.annotation.Autowired *;
    @org.springframework.beans.factory.annotation.Value *;
}

-keep class * implements org.springframework.boot.CommandLineRunner {
    <methods>;
}

-keepclasseswithmembers public class * {
    public static void main(java.lang.String[]);
}

-keep public class * extends org.springframework.boot.web.servlet.support.SpringBootServletInitializer

-keepclassmembers enum * {
    public static **[] values();
    public static ** valueOf(java.lang.String);
}

-keepclassmembers class * {
    @org.springframework.context.annotation.Bean <methods>;
}

-keepclassmembers,allowshrinking class * {
    @org.springframework.context.annotation.Configuration *;
}

-keepclassmembers,allowshrinking class * {
    @javax.persistence.Entity <fields>;
    @javax.persistence.Entity <methods>;
}

-keepclassmembers,allowshrinking class * {
    @com.fasterxml.jackson.annotation.JsonCreator *;
    @com.fasterxml.jackson.annotation.JsonProperty *;
}

# Keep -names for transactional methods
-keepclassmembers,allowoptimization class * {
    @org.springframework.transaction.annotation.Transactional <methods>;
}

-keepattributes *Annotation*,Signature,EnclosingMethod,InnerClasses,RuntimeVisibleAnnotations,RuntimeVisibleParameterAnnotations,RuntimeInvisibleAnnotations,RuntimeInvisibleParameterAnnotations
-keepnames @org.springframework.context.annotation.Configuration class *
-keepnames class * implements org.springframework.boot.autoconfigure.condition.Condition
-keepnames class * {
    @org.springframework.context.annotation.Configuration *;
}
-keepnames class * {
    @org.springframework.boot.context.properties.ConfigurationProperties *;
}
-keepnames class * {
    @org.springframework.boot.autoconfigure.condition.ConditionalOnProperty *;
}

# For Logback
-keep class ch.qos.logback.** { *; }
-keep class org.slf4j.** { *; }

# For SnakeYAML
-keep class org.yaml.snakeyaml.** { *; }

# For Jackson
-keep class com.fasterxml.jackson.** { *; }
-keep interface com.fasterxml.jackson.** { *; }
-keep @com.fasterxml.jackson.annotation.JsonIgnoreProperties class * { *; }

# For h2
-keep class org.h2.** { *; }

# For JPA
-keep class javax.persistence.** { *; }
-keep class org.hibernate.** { *; }
-keepnames class * implements javax.persistence.AttributeConverter

# For Spring
-keep class org.springframework.** { *; }
-keep interface org.springframework.** { *; }
-dontwarn org.springframework.boot.SpringApplication
-dontwarn org.springframework.context.annotation.Configuration
-dontwarn org.springframework.beans.factory.annotation.Autowired
-dontwarn org.springframework.beans.factory.annotation.Value

# For docx4j
-keep class org.docx4j.** { *; }
-keep class org.glox4j.** { *; }
-keep class org.pptx4j.** { *; }
-keep class org.xlsx4j.** { *; }
-keep class org.plutext.** { *; }
-keep class org.apache.xmlgraphics.** { *; }
-keep class org.apache.fop.** { *; }
-keep class org.w3c.dom.** { *; }
-keep class org.xml.sax.** { *; }
-keep class javax.xml.** { *; }
-keep class com.sun.xml.** { *; }
-keep class com.sun.istack.** { *; }
-keep class org.jvnet.jaxb2_commons.** { *; }
-keep class org.glassfish.jaxb.** { *; }
-keep class jakarta.xml.bind.** { *; }

# For flexmark
-keep class com.vladsch.flexmark.** { *; }
-keep class org.nibor.autolink.** { *; }
-keep class com.ibm.icu.** { *; }

# For openhtmltopdf
-keep class com.openhtmltopdf.** { *; }

# For swagger
-keep class io.swagger.** { *; }
-keep class org.springdoc.** { *; }

# For gitlab4j
-keep class org.gitlab4j.** { *; }
-keep class org.eclipse.jgit.** { *; }

# For datafaker
-keep class net.datafaker.** { *; }

# For openapi-generator
-keep class org.openapitools.** { *; }
