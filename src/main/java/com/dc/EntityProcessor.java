package com.dc;

import javax.annotation.processing.AbstractProcessor;
import javax.annotation.processing.Filer;
import javax.annotation.processing.RoundEnvironment;
import javax.lang.model.SourceVersion;
import javax.lang.model.element.*;
import javax.lang.model.type.DeclaredType;
import javax.lang.model.type.TypeKind;
import javax.lang.model.type.TypeMirror;
import javax.lang.model.util.Elements;
import javax.tools.Diagnostic;
import javax.tools.JavaFileObject;
import java.io.IOException;
import java.io.Writer;
import java.text.SimpleDateFormat;
import java.util.*;

/**
 * 表字段常量生成
 * @Author dengchen
 * @Date 2023/11/22
 */
public class EntityProcessor extends AbstractProcessor {

    @Override
    public Set<String> getSupportedAnnotationTypes() {
        return Set.of("javax.persistence.Entity","jakarta.persistence.Entity");
    }

    @Override
    public SourceVersion getSupportedSourceVersion() {
        return SourceVersion.latestSupported();
    }

    @Override
    public boolean process(Set<? extends TypeElement> annotations, RoundEnvironment roundEnv) {
        Set<Element> elements = new HashSet<>();
        for (TypeElement annotation : annotations) {
            elements.addAll(roundEnv.getElementsAnnotatedWith(annotation));
        }
        for (Element element : elements) {
            if (element.getKind() == ElementKind.CLASS) {
                TypeElement typeElement = (TypeElement) element;
                generateFieldConstants(typeElement);
            }
        }
        return false;
    }

    /**
     * 生成字段常量信息
     * @param typeElement
     */
    private void generateFieldConstants(TypeElement typeElement) {
        String packageName = processingEnv.getElementUtils().getPackageOf(typeElement).getQualifiedName().toString();
        String className = "Dc" + typeElement.getSimpleName();
        try {
            Filer filer = processingEnv.getFiler();
            JavaFileObject sourceFile = filer.createSourceFile(packageName + "." + className);
            try (Writer writer = sourceFile.openWriter()) {
                writer.write("package " + packageName + ";\n\n");
                writer.write("import javax.annotation.processing.Generated;\n\n");
                writer.write("@Generated(value =\"" + this.getClass().getName() + "\", date =\"" +
                        new SimpleDateFormat("yyyy-MM-dd HH:mm:ss").format(new Date()) +
                        "\", comments =\"根据" + typeElement.getQualifiedName().toString() + "自动生成\")\n");
                writer.write("public class " + className + " {\n\n");
                // 生成字段常量
                processFields(typeElement, writer);
                writer.write("}\n");
            }
        } catch (IOException e) {
            processingEnv.getMessager().printMessage(Diagnostic.Kind.ERROR,e.toString());
        }
    }

    /**
     * 处理字段
     * @param typeElement
     * @param writer
     * @throws IOException
     */
    private void processFields(TypeElement typeElement, Writer writer) throws IOException {
        // 先生成父类字段
        TypeMirror superClassType = typeElement.getSuperclass();
        if (superClassType.getKind() == TypeKind.DECLARED) {
            DeclaredType declaredSuperClass = (DeclaredType) superClassType;
            Element superClassElement = declaredSuperClass.asElement();
            if (superClassElement instanceof TypeElement) {
                processFields((TypeElement) superClassElement, writer);
            }
        }
        // 生成当前类字段
        for (Element enclosedElement : typeElement.getEnclosedElements()) {
            if (enclosedElement.getKind() == ElementKind.FIELD) {
                String fieldName = enclosedElement.getSimpleName().toString();
                if (!shouldBeIgnored(enclosedElement)) {
                    String constantName = constantName(fieldName);
                    String constantValue = constantValue(enclosedElement).orElseGet(() -> constantValue(fieldName));
                    writer.write("    "+"/**\n     *"+getJavadoc(enclosedElement)+"     */\n");
                    writer.write("    public static final String " + constantName + " = \"" + constantValue + "\";\n\n");
                }
            }
        }
    }

    /**
     * 生成字段常量名
     * @param fieldName
     * @return
     */
    private String constantName(String fieldName) {
        return constantValue(fieldName).toUpperCase();
    }

    /**
     * 解析元素上指定的字段常量值
     * @param fieldElement
     * @return
     */
    private Optional<String> constantValue(Element fieldElement) {
        Optional<? extends AnnotationMirror> annotationMirrorOptional = getAnnotationMirror(fieldElement, "javax.persistence.Column");
        annotationMirrorOptional = annotationMirrorOptional.equals(Optional.empty()) ? getAnnotationMirror(fieldElement,"jakarta.persistence.Column") : annotationMirrorOptional;
        if (annotationMirrorOptional.isPresent()) {
            Map<? extends ExecutableElement, ? extends AnnotationValue> elementValues = annotationMirrorOptional.get().getElementValues();
            for (Map.Entry<? extends ExecutableElement, ? extends AnnotationValue> entry : elementValues.entrySet()) {
                if ("name".equals(entry.getKey().getSimpleName().toString())) {
                    return Optional.of(entry.getValue().getValue().toString());
                }
            }
        }
        return Optional.empty();
    }

    /**
     * 生成字段常量值
     * @param fieldName
     * @return
     */
    private String constantValue(String fieldName) {
        StringBuilder result = new StringBuilder();
        boolean isFirst = true;
        for (int i = 0; i < fieldName.length(); i++) {
            char currentChar = fieldName.charAt(i);
            if (Character.isUpperCase(currentChar)) {
                if (!isFirst) {
                    result.append("_");
                }
                result.append(Character.toLowerCase(currentChar));
            } else {
                result.append(currentChar);
            }
            isFirst = false;
        }
        return result.toString();
    }

    /**
     * 获取指定的注解元素
     * @param element
     * @param annotationName
     * @return
     */
    private Optional<? extends AnnotationMirror> getAnnotationMirror(Element element, String annotationName) {
        for (AnnotationMirror annotationMirror : element.getAnnotationMirrors()) {
            if (annotationMirror.getAnnotationType().toString().equals(annotationName)) {
                return Optional.of(annotationMirror);
            }
        }
        return Optional.empty();
    }

    /**
     * 应该忽略的元素
     * @param fieldElement
     * @return
     */
    private boolean shouldBeIgnored(Element fieldElement) {
        return fieldElement.getModifiers().contains(Modifier.STATIC) ||
                fieldElement.getModifiers().contains(Modifier.FINAL) ||
                hasAnnotation(fieldElement, "javax.persistence.Transient") ||
                hasAnnotation(fieldElement, "jakarta.persistence.Transient");
    }

    /**
     * 是否包含某个注解
     * @param fieldElement
     * @param annotation
     * @return
     */
    private boolean hasAnnotation(Element fieldElement, String annotation) {
        for (AnnotationMirror annotationMirror : fieldElement.getAnnotationMirrors()) {
            DeclaredType annotationType = annotationMirror.getAnnotationType();
            String annotationName = annotationType.toString();
            if (annotationName.equals(annotation)) {
                return true;
            }
        }
        return false;
    }

    /**
     * 获取文档注释
     * @param element
     * @return
     */
    private String getJavadoc(Element element) {
        Elements elementUtils = processingEnv.getElementUtils();
        return elementUtils.getDocComment(element);
    }

}
