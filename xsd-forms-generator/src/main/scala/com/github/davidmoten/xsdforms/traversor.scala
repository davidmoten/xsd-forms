package com.github.davidmoten.xsdforms {

  import xsd._
  import javax.xml.namespace.QName
  import scalaxb._

  /**
   * **************************************************************
   *
   *   SchemaTraversor
   *
   *
   * **************************************************************
   */

  class SchemaTraversor(s: Schema, rootElement: Option[String], visitor: Visitor) {
    import Util._
    import XsdUtil._

    val extensionStack = new scala.collection.mutable.Stack[ExtensionTypable]
    val extensionsIncludedInBaseSequence = new scala.collection.mutable.Stack[Boolean]

    private val topLevelElements =
      s.schemasequence1.flatMap(_.schemaTopOption1.value match {
        case y: TopLevelElement => Some(y)
        case _ => None
      })

    private val topLevelComplexTypes = s
      .schemasequence1
      .flatMap(_.schemaTopOption1.value match {
        case y: TopLevelComplexType => Some(y)
        case _ => None
      })

    private val topLevelSimpleTypes = s
      .schemasequence1.flatMap(_.schemaTopOption1.value match {
        case y: TopLevelSimpleType => Some(y)
        case _ => None
      })

    private val targetNs = s.targetNamespace.getOrElse(
      unexpected("schema must have targetNamespace attribute")).toString

    private val schemaTypes =
      (topLevelComplexTypes.map(x => (qn(targetNs, x.name.get), x))
        ++ (topLevelSimpleTypes.map(x => (qn(targetNs, x.name.get), x)))).toMap;

    private val baseTypes =
      Set(XsdDecimal, XsdString, XsdInteger, XsdDate, XsdDateTime, XsdTime,
        XsdBoolean, XsdInt, XsdLong, XsdShort, XsdPositiveInteger,
        XsdNegativeInteger, XsdNonPositiveInteger, XsdNonNegativeInteger,
        XsdDouble, XsdFloat)
        .map(qn(_))

    private def getType(q: QName): AnyRef = {
      schemaTypes.get(q) match {
        case Some(x: Annotatedable) => return x
        case _ =>
          if (baseTypes contains q) return BaseType(q)
          else unexpected("unrecognized type: " + q)
      }
    }

    private def matches[T](d: DataRecord[T], q: QName) =
      toQName(d).equals(q)

    private case class MyType(typeValue: AnyRef)

    /**
     * Visits the element definition tree.
     */
    def traverse {
      topLevelAnnotations
      val element =
        if (rootElement.isDefined)

          topLevelElements.find(
            _.name match {
              case Some(y) => y equals rootElement.get
              case None => false
            }).getOrElse(unexpected("did not find element " + rootElement.get))
        else if (topLevelElements.length == 0)
          unexpected("no top level elements specified in schema!")
        else
          topLevelElements(0)

      process(element)
    }

    private def topLevelAnnotations {
      //first get the children of the annotation - appinfo element
      val children = s.schemaoption
        .filter(toQName(_) == qn(XsdAnnotation))
        .map(_.value)
        .map(x => x match {
          case y: Annotation => y
          case _ => unexpected
        })
        .map(_.annotationoption)
        .flatten
        .filter(toQName(_) == qn("appinfo"))
        .map(_.value)
        .map(x => x match {
          case y: Appinfo => y
          case _ => unexpected
        })
        .map(x => x.mixed)
        .flatten

      def extract(elementName: String) =
        children
          .filter(_.key.isDefined)
          .filter(toQName(_) == qn(AppInfoSchema, elementName))
          .map(_.value)
          .map(x => scala.xml.XML.loadString(x.toString))
          .map(x => x.text).headOption

      val header = extract("header")
      val footer = extract("footer")
      val extraImports = extract("extraImports")
      val extraScript = extract("extraScript")
      val extraCss = extract("extraCss")
      val configuration = Configuration(header, footer, extraImports, extraScript, extraCss)
      visitor.configuration(configuration)
    }

    private def process(e: Element) {
      e.typeValue match {
        case Some(x: QName) => process(e, MyType(getType(x)))
        case None => {
          e.elementoption match {
            case Some(x: DataRecord[ElementOption]) => {
              x.value match {
                case y: LocalComplexType => process(e, y)
                case y: LocalSimpleType => process(e, y)
                case _ => unexpected
              }
            }
            case _ => unexpected("unsupported " + e)
          }
        }
        case _ => unexpected
      }
    }

    private def process(e: Element, typeValue: MyType) {
      typeValue.typeValue match {
        case x: TopLevelSimpleType => process(e, x)
        case x: TopLevelComplexType => process(e, x)
        case x: BaseType => process(e, x)
      }
    }

    private def process(e: Element, x: SimpleType) {
      visitor.simpleType(e, x)
    }

    private def process(e: Element, c: ComplexType) {
      c.complexTypeModelOption3.value match {
        case x: ComplexContent =>
          process(e, x)
        case x: SimpleContent => {
          val q = toQName(x.simplecontentoption)
          if (qn("extension") == q) {
            x.simplecontentoption.value match {
              case t: SimpleExtensionType =>
                process(e, t)
              case _ => unexpected
            }
          } else unexpected
        }
        case x: ComplexTypeModelSequence1 => {
          //sequence or choice
          process(e, x.typeDefParticleOption1.getOrElse(unexpected))
        }
      }
    }

    private def process(e: Element, x: DataRecord[TypeDefParticleOption]) {
      x.value match {
        case y: GroupRef =>
          unexpected
        case y: ExplicitGroupable =>
          if (matches(x, qn("sequence")))
            process(e, Sequence(y))
          else if (matches(x, qn("choice")))
            process(e, Choice(y))
          else unexpected
        case _ => unexpected
      }
    }

    private def process(e: Element, cc: ComplexContent) {
      val q = toQName(cc.complexcontentoption)

      val value = cc.complexcontentoption.value
      println("cc " + q + "=" + value)
      if (qn("extension") == q)
        value match {
          case et: ExtensionType => {
            process(e, et)
          }
          case _ => unexpected
        }
      else unexpected
    }

    private def process(e: Element, et: ExtensionTypable) {
      println("startExtension")
      extensionStack.push(et)
      process(e, MyType(getType(et.base)))
      println("stopExtension")
    }

    private def process(e: Element, x: BaseType) {
      visitor.baseType(e, x)
    }

    private def process(e: Element, x: Sequence) {
      val wrapWithSequence =
        extensionsIncludedInBaseSequence.isEmpty || !extensionsIncludedInBaseSequence.top
      if (wrapWithSequence) {
        visitor.startSequence(e)
      }
      val extensions = extensionStack.toList
      extensionStack.clear
      extensionsIncludedInBaseSequence.push(false)
      x.group.particleOption3.foreach(y => process(e, toQName(y), y.value))
      extensionsIncludedInBaseSequence.pop
      extensionsIncludedInBaseSequence.push(true)
      extensions.foreach { y =>
        y.typeDefParticleOption3 match {
          case Some(t) => process(e, t)
          case None => {}
        }
      }
      extensionsIncludedInBaseSequence.pop
      if (wrapWithSequence)
        visitor.endSequence
    }

    private def process(e: Element, x: Choice) {
      val wrapWithSequence = !extensionStack.isEmpty
      val extensions = extensionStack.toList
      extensionStack.clear
      if (wrapWithSequence) {
        visitor.startSequence(e)
      }
      val anon = MyElement()
      val subElement = if (wrapWithSequence) anon else e
      visitor.startChoice(subElement, x)
      var index = 0
      extensionsIncludedInBaseSequence.push(false)
      x.group.particleOption3.foreach(y => {
        index = index + 1
        visitor.startChoiceItem(subElement, y.value, index)
        process(subElement, toQName(y), y.value)
        visitor.endChoiceItem
      })
      extensionsIncludedInBaseSequence.pop
      visitor.endChoice
      extensionsIncludedInBaseSequence.push(true)
      extensions.foreach { y =>
        y.typeDefParticleOption3 match {
          case Some(t) => process(anon, t)
          case None => {}
        }
      }
      extensionsIncludedInBaseSequence.pop
      if (wrapWithSequence)
        visitor.endSequence
    }

    private def process(e: Element, q: QName, x: ParticleOption) {
      if (q == qn("element")) {
        x match {
          case y: LocalElementable => process(y)
          case _ => unexpected
        }
      } else if (q == qn("choice")) {
        x match {
          case y: ExplicitGroupable => process(e, Choice(y))
          case _ => unexpected
        }
      } else if (q == qn("sequence")) {
        x match {
          case y: ExplicitGroupable => process(MyElement(), Sequence(y))
          case _ => unexpected
        }
      } else unexpected(q + x.toString)
    }
  }
}