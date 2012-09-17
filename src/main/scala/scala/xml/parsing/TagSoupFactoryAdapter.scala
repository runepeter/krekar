package scala.xml.parsing

import scala.xml.factory.NodeFactory
import scala.xml.{Elem, MetaData, NamespaceBinding, Node, Text, TopScope}
import org.ccil.cowan.tagsoup.jaxp.SAXFactoryImpl

class TagSoupFactoryAdapter extends FactoryAdapter with NodeFactory[Elem] {

  private val parserFactory = new SAXFactoryImpl
  parserFactory.setNamespaceAware(true)

  val emptyElements = Set("area", "base", "br", "col", "hr", "img", "input", "link", "meta", "param")

  override def nodeContainsText(localName: String) = !(emptyElements contains localName)

  def getReader() = parserFactory.newSAXParser().getXMLReader()

  override def load(source: scala.xml.InputSource): scala.xml.Node = {
    val reader = getReader()
    reader.setContentHandler(this)
    scopeStack.push(TopScope)
    reader.parse(source)
    scopeStack.pop
    rootElem
  }

  protected def create(pre: String, label: String,
                       attrs: MetaData, scpe: NamespaceBinding,
                       children: Seq[Node]): Elem = Elem(pre, label, attrs, scpe, children: _*)

  def createNode(pre: String, label: String,
                 attrs: MetaData, scpe: NamespaceBinding,
                 children: List[Node]): Elem = Elem(pre, label, attrs, scpe, children: _*)

  def createText(text: String) = Text(text)

  def createProcInstr(target: String, data: String) = makeProcInstr(target, data)
}
