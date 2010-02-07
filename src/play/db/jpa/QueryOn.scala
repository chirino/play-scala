package play.db.jpa
import scala.collection.mutable

/**
*  provides a mini DSL for Model objects
**/
trait QueryOn[T] {
  import JPQL.{instance => i}
  type M[T] = Manifest[T]
  implicit private def manifest2entity[T](m: M[T]): String = m.erasure.getName()
  def count(implicit m: M[T]) = i.count(m)

  def count(q: String, ps: AnyRef*)(implicit m: M[T]) = i.count(m, q, ps.toArray)
  def findAll(implicit m: M[T]) = i.findAll(m)
  def findById(id: Any)(implicit m: M[T]) = i.findById(m, id).asInstanceOf[T]
  def findBy(q: String, ps: AnyRef*)(implicit m: M[T]) = i.findBy(m, q, ps.toArray)
  def find(q: String, ps: AnyRef*)(implicit m: M[T]) = new ScalaQuery[T](i.find(m, q, ps.toArray))
  def all(implicit m: M[T]) = i.all(m)
  def delete(q: String, ps: AnyRef*)(implicit m: M[T]) = i.delete(m, q, ps.toArray)
  def deleteAll(implicit m: M[T]) = i.deleteAll(m)
  def findOneBy(q: String, ps: AnyRef*)(implicit m: M[T]): T = i.findOneBy(m, q, ps.toArray).asInstanceOf[T]
  def create(name: String, ps: play.mvc.Scope.Params)(implicit m: M[T]): T = i.create(m, name, ps).asInstanceOf[T]

}

/**
* provides Query functionality via companion object
**/
object QueryOn {
  private var queries = new mutable.HashMap[String, QueryHolder[_]]
  private def memoize[T](key:String ):QueryHolder[T] = {
     if (!queries.contains(key)) {
       queries += (key) -> (new QueryHolder[T])
     }
     queries(key).asInstanceOf[QueryHolder[T]]
  }
  def apply[T](implicit m: scala.reflect.Manifest[T]):QueryHolder[T] = memoize[T](m.toString)
}


private[jpa] class QueryHolder[T] extends QueryOn[T]

private[jpa] class ScalaQuery[T](val query: JPASupport.JPAQuery) {

    def first = query.first().asInstanceOf[T]
    def fetch() = asList[T](query.fetch())
    def all = fetch()
    def fetch(size: Int) = asList[T](query.fetch(size))
    def from(offset: Int) = {
        query.from(offset)
        this
    }

    private def asList[T](jlist: java.util.List[T]): List[T] = {
        import scala.collection.mutable.ListBuffer
        val buffer = ListBuffer[T]()
        for(e <- jlist.toArray) {
            buffer += e.asInstanceOf[T]
        }
        buffer.toList
    }
}
