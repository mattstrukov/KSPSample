package com.strukov.processor

import com.google.devtools.ksp.processing.*
import com.google.devtools.ksp.symbol.*
import java.io.OutputStream
import kotlin.properties.Delegates

// Разобрать работу валидатора. Кастомный валидатор? gradlew :workload:build --no-daemon -Dorg.gradle.debug=true -Dkotlin.compiler.execution.strategy=in-process

class NetworkConfigProcessor(
    val codeGenerator: CodeGenerator,
    val logger: KSPLogger
) : SymbolProcessor {

    override fun process(resolver: Resolver): List<KSAnnotated> {
        val symbols = resolver.getSymbolsWithAnnotation(checkNotNull(EnvironmentConfig::class.qualifiedName))
        // Multiple round processing https://kotlinlang.org/docs/ksp-multi-round.html

        symbols
            .filter {
                val classDeclaration = it as? KSClassDeclaration
                if (classDeclaration?.classKind != ClassKind.INTERFACE) {
                    // если передавать classDeclaration, то будет ссылка на объявление в коде (удобно!)
                    logger.error("Use kotlin interface with @EnvironmentConfig!", classDeclaration)
                }
                classDeclaration?.classKind == ClassKind.INTERFACE
            }
            .forEach { it.accept(NetworkConfigVisitor(), Unit) }

        return emptyList()
    }

    inner class NetworkConfigVisitor : KSVisitorVoid() {
        private var file by Delegates.notNull<OutputStream>()

        override fun visitClassDeclaration(classDeclaration: KSClassDeclaration, data: Unit) {
            val properties = classDeclaration.getAllProperties()
            val packageName = classDeclaration.containingFile!!.packageName.asString()
            val className = "${classDeclaration.simpleName.asString()}Url"

            file = codeGenerator.createNewFile(
                Dependencies(
                    true,
                    classDeclaration.containingFile!!
                ),
                packageName,
                className
            )

            file.appendText("package $packageName\n\n")
            file.appendText("import com.strukov.processor.Environment\n\n")
            file.appendText("public class $className(private val environmentSettings: EnvironmentSettings) {\n")
            file.appendText("\tinternal val url get() = environments[environmentSettings.stage].orEmpty()\n")
            file.appendText("\tprivate val environments = mapOf<String, String>(")

            val iterator = properties.iterator()

            while (iterator.hasNext()) {
                iterator.next().accept(this, Unit)
                if (iterator.hasNext()) file.appendText(",")
            }

            file.appendText("\n\t)\n")

            file.appendText("}")
            file.close()
        }

        override fun visitPropertyDeclaration(property: KSPropertyDeclaration, data: Unit) {
            property.annotations.find { it.shortName.asString() == Url::class.java.simpleName }
                ?.accept(this, Unit)
                ?: logger.error("Use @Url for property!", property)
        }

        override fun visitAnnotation(annotation: KSAnnotation, data: Unit) {
            val environment = annotation.arguments.find { it.name?.asString() == "environment" }
                ?.value.toString().substringAfter("processor.")
            val name = annotation.arguments.find { it.name?.asString() == "name" }

            file.appendText("\n\t\t${environment}.env to \"${name!!.value as String}\"")
        }
    }
}

class NetworkConfigProcessorProvider : SymbolProcessorProvider {
    override fun create(environment: SymbolProcessorEnvironment): SymbolProcessor {
        return NetworkConfigProcessor(environment.codeGenerator, environment.logger)
    }
}

private fun OutputStream.appendText(str: String) {
    this.write(str.toByteArray())
}