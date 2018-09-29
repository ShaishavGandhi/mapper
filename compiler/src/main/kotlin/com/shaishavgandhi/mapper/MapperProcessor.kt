package com.shaishavgandhi.mapper

import com.google.auto.service.AutoService
import com.squareup.kotlinpoet.ClassName
import com.squareup.kotlinpoet.FileSpec
import com.squareup.kotlinpoet.FunSpec
import com.squareup.kotlinpoet.KModifier.VARARG
import com.squareup.kotlinpoet.PropertySpec
import com.squareup.kotlinpoet.TypeSpec
import com.squareup.kotlinpoet.asTypeName
import me.eugeniomarletti.kotlin.metadata.KotlinClassMetadata
import me.eugeniomarletti.kotlin.metadata.isDataClass
import me.eugeniomarletti.kotlin.metadata.kotlinMetadata
import java.io.File
import javax.annotation.processing.AbstractProcessor
import javax.annotation.processing.Filer
import javax.annotation.processing.Messager
import javax.annotation.processing.ProcessingEnvironment
import javax.annotation.processing.Processor
import javax.annotation.processing.RoundEnvironment
import javax.lang.model.SourceVersion
import javax.lang.model.element.TypeElement
import javax.lang.model.type.MirroredTypeException
import javax.lang.model.type.TypeMirror
import javax.lang.model.util.Elements
import javax.lang.model.util.Types
import javax.tools.Diagnostic.Kind.ERROR
import javax.tools.Diagnostic.Kind.WARNING
import kotlin.math.round

@AutoService(Processor::class)
class MapperProcessor: AbstractProcessor() {

  private lateinit var filer: Filer
  private lateinit var messager: Messager
  private lateinit var elements: Elements
  private lateinit var types: Types
  private lateinit var outputDir: File
  private lateinit var options: Map<String, String>

  override fun init(processingEnv: ProcessingEnvironment) {
    super.init(processingEnv)
    filer = processingEnv.filer
    messager = processingEnv.messager
    elements = processingEnv.elementUtils
    types = processingEnv.typeUtils
    options = processingEnv.options
    outputDir = options["kapt.kotlin.generated"]?.let(::File) ?: throw IllegalStateException(
        "No kapt.kotlin.generated option provided")
  }

  override fun process(annotations: MutableSet<out TypeElement>, roundedEnv: RoundEnvironment): Boolean {
    val elements = roundedEnv.getElementsAnnotatedWith(Mapper::class.java)
    for (element in elements) {
      if (!element.kind.isClass) {
        messager.printMessage(ERROR, "@Map can only be applied to a class")
      }

      val annotation = element.getAnnotation(Mapper::class.java)
      val name = element.simpleName.toString()
      val classValue = getValue(annotation)
      if (classValue?.toString()?.contains("DefaultMapper") == true) {
        continue
      }

      val file = FileSpec.builder("", "${element.simpleName}Extensions")

      val metaData = (element as TypeElement).kotlinMetadata
      if (metaData !is KotlinClassMetadata) {
        messager.printMessage(ERROR, "@Mapper can only be applied to Class")
        continue
      }

      val proto = metaData.data.classProto
      if (!proto.isDataClass) {
        messager.printMessage(ERROR, "@Mapper can only be applied to data classes")
        continue
      }

      val nameResolver = metaData.data.nameResolver

      proto.constructorList.map {
        it.valueParameterList.map {
          messager.printMessage(WARNING, "${nameResolver.getString(it.name)} ${nameResolver.getString(it.type.className)}")
        }
      }
      proto.typeParameterList.map {
        messager.printMessage(WARNING, nameResolver.getString(it.name))
      }

      file.addFunction(FunSpec.builder("to$name")
          .receiver(element.asType().asTypeName())
//          .returns(classValue?.asTypeName()!!)
          .build())
      file.build().writeTo(outputDir)
    }
    return true
  }

  fun getValue(annotation: Mapper): TypeMirror? {
    try {
      annotation.to
    } catch (exception: MirroredTypeException) {
      return exception.typeMirror
    }
    return null
  }

  override fun getSupportedSourceVersion(): SourceVersion {
    return SourceVersion.latestSupported()
  }

  override fun getSupportedAnnotationTypes(): MutableSet<String> {
    return mutableSetOf(Mapper::class.java.canonicalName)
  }
}
