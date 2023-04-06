package com.strukov.processor

import com.google.devtools.ksp.processing.*
import com.google.devtools.ksp.symbol.*
import com.strukov.processor.extensions.appendText
import java.io.OutputStream
import kotlin.properties.Delegates

internal class EnvironmentSettingsProcessor(
    private val codeGenerator: CodeGenerator,
    private val logger: KSPLogger,
    private val options: Map<String, String>
) : SymbolProcessor {
    override fun process(resolver: Resolver): List<KSAnnotated> {
        val symbols = resolver.getSymbolsWithAnnotation(checkNotNull(EnvironmentSettings::class.qualifiedName))

        symbols
            .filter {
                val classDeclaration = it as? KSClassDeclaration
                if (classDeclaration?.classKind != ClassKind.INTERFACE) {
                    logger.error("Use kotlin interface with @EnvironmentSettings!", classDeclaration)
                }
                classDeclaration?.classKind == ClassKind.INTERFACE
            }
            .forEach { it.accept(EnvironmentSettingsProcessorVisitor(), Unit) }

        return emptyList()
    }

    inner class EnvironmentSettingsProcessorVisitor : KSVisitorVoid() {
        private var file by Delegates.notNull<OutputStream>()

        override fun visitClassDeclaration(classDeclaration: KSClassDeclaration, data: Unit) {
            val packageName = classDeclaration.containingFile!!.packageName.asString()
            val className = "${classDeclaration.simpleName.asString()}Settings"

            file = codeGenerator.createNewFile(
                Dependencies(
                    false,
                    classDeclaration.containingFile!!
                ),
                packageName,
                className
            )

            file.appendText("package $packageName\n\n")
            file.appendText("internal class $className {\n")
            file.appendText(
                "\tval stage get() = Environment.PROD.env\n" +
                        "\n\tenum class Environment(val env: String) {"
            )

            var counter = 0

            options.forEach {
                file.appendText("\n\t\t${it.key}(\"${it.value}\")")
                if (options.size != ++counter) file.appendText(",")
            }
            file.appendText("\n\t}\n}")
            file.close()
        }
    }
}

internal class EnvironmentSettingsProcessorProvider : SymbolProcessorProvider {
    override fun create(environment: SymbolProcessorEnvironment): SymbolProcessor {
        return EnvironmentSettingsProcessor(environment.codeGenerator, environment.logger, environment.options)
    }
}