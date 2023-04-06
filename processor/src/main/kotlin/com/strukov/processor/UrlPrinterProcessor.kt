package com.strukov.processor

import com.google.devtools.ksp.processing.*
import com.google.devtools.ksp.symbol.*
import com.google.devtools.ksp.validate
import com.strukov.processor.extensions.appendText
import java.io.OutputStream
import kotlin.properties.Delegates

internal class UrlPrinterProcessor(
    val codeGenerator: CodeGenerator,
    private val logger: KSPLogger
) : SymbolProcessor {
    override fun process(resolver: Resolver): List<KSAnnotated> {
        val symbols = resolver.getSymbolsWithAnnotation(checkNotNull(UrlPrinter::class.qualifiedName))

        return symbols
            .filter {
                val classDeclaration = it as? KSClassDeclaration
                if (classDeclaration?.classKind != ClassKind.INTERFACE) {
                    logger.error("Use kotlin interface with @UrlPrinter!", classDeclaration)
                }
                classDeclaration?.classKind == ClassKind.INTERFACE
            }
            .mapNotNull {
                if (it.validate()) {
                    it.accept(UrlPrinterProcessorVisitor(), Unit)
                    null
                } else {
                    it
                }
            }
            .toList()
    }

    inner class UrlPrinterProcessorVisitor : KSVisitorVoid() {
        private var file by Delegates.notNull<OutputStream>()

        override fun visitClassDeclaration(classDeclaration: KSClassDeclaration, data: Unit) {
            val properties = classDeclaration.getAllProperties()
            val packageName = classDeclaration.containingFile!!.packageName.asString()
            val className = "${classDeclaration.simpleName.asString()}Printer"

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
            file.appendText("\tfun print() {")

            val iterator = properties.iterator()

            while (iterator.hasNext()) {
                iterator.next().accept(this, Unit)
                if (iterator.hasNext()) file.appendText("\n")
            }

            file.appendText("\n\t}\n}")
            file.close()
        }

        override fun visitPropertyDeclaration(property: KSPropertyDeclaration, data: Unit) {
            file.appendText("\n\t\tprintln(${property.type}(SampleEnvironmentSettings()).url)")
        }
    }
}

internal class UrlPrinterProcessorProvider : SymbolProcessorProvider {
    override fun create(environment: SymbolProcessorEnvironment): SymbolProcessor {
        return UrlPrinterProcessor(environment.codeGenerator, environment.logger)
    }
}