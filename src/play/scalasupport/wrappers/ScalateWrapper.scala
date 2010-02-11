package play.scalasupport.wrappers
import play.mvc.{Http,Scope}
import play.classloading.enhancers.LocalvariablesNamesEnhancer.LocalVariablesNamesTracer
import play.Play
import play.exceptions.{UnexpectedException,PlayException,TemplateNotFoundException}
import play.data.validation.Validation
import org.fusesource.scalate._
import java.io.{StringWriter,PrintWriter}
import scala.collection.JavaConversions._
private[wrappers] object ScalateWrapper  {
  val engine = new TemplateEngine

  def renderOrProvideTemplate(args:Array[AnyRef]):String = {
    //determine template
    val templateName:String =
        if (args.length > 0 && args(0).isInstanceOf[String] && 
            LocalVariablesNamesTracer.getAllLocalVariableNames(args(0)).isEmpty) {
            discardLeadingAt(args(0).toString)
        } else {
          determineURI()
        }

    if (shouldRenderWithScalate(templateName)) {
      renderScalateTemplate(templateName,args)
      null
    } else {
      templateName
    }  

  }

  //determine if we need to render with scalate
  def shouldRenderWithScalate(template:String):Boolean = {
    val ignore = Play.configuration.getProperty("scalate.ignore") 
    if (Play.configuration.containsKey("scalate")) {
      if (ignore != null) {
         ignore.split(",").filter(template.startsWith(_)).size == 0
      } else true
    } else false 
  }

  //render with scalate
  def renderScalateTemplate(templateName:String, args:Array[AnyRef]) = {
    val renderMode = Play.configuration.getProperty("scalate") 
    //loading template
    val lb = new scala.collection.mutable.ListBuffer[Binding]
    val buffer = new StringWriter()
    var context = new DefaultRenderContext(engine, new PrintWriter(buffer))
    val templateBinding = Scope.RenderArgs.current()
    // try to fill context
    for (o <-args) {
      for (name <-LocalVariablesNamesTracer.getAllLocalVariableNames(o).iterator) {
        context.attributes += name -> o
        lb += Binding(name,o.getClass.getName)
      }
    }
    context.attributes += "session" -> Scope.Session.current
    //lb += Binding("session",Scope.Session.current.getClass.getName)
    context.attributes += "request" -> Http.Request.current
    //lb += Binding("request", Http.Request.current.getClass.getName)
    context.attributes += "flash" -> Scope.Flash.current
    //lb += Binding("flash", Scope.Flash.current.getClass.getName)
    context.attributes += "params" ->  Scope.Params.current
    //lb += Binding("params", Scope.Params.current.getClass.getName)
    try {
       context.attributes +="errors" -> Validation.errors()
    } catch { case ex:Exception => throw new UnexpectedException(ex)}
    try {
          println("LB:"+lb.toList)
          engine.bindings=lb.toList
          val template = engine.load(Play.applicationPath+"/app/views/"+templateName.replaceAll(".html","."+renderMode))
          template.render(context)
          throw new RenderScalateTemplate(buffer.toString,templateName)
    } catch { 
        case ex:TemplateNotFoundException => {
          if(ex.isSourceAvailable) {
            throw ex
          }
          val element = PlayException.getInterestingStrackTraceElement(ex)
          if (element != null) {
             throw new TemplateNotFoundException(templateName, Play.classes.getApplicationClass(element.getClassName()), element.getLineNumber());
          } else {
             throw ex
          }
       }
    }
  }
  
  def discardLeadingAt(templateName:String):String = {
        if(templateName.startsWith("@")) {
            if(!templateName.contains(".")) {
                determineURI(Http.Request.current().controller + "." + templateName.substring(1))
            }
            determineURI(templateName.substring(1))
        } else templateName
  }

  def determineURI(template:String = Http.Request.current().action):String = {
     template.replace(".", "/") + "." + 
     (if (Http.Request.current().format == null)  "html" else Http.Request.current().format)
  }
}
