package play.scalasupport.wrappers
import play.mvc.Http.{Request,Response}
import play.mvc.results.Result
import play.templates.Template
import play.exceptions.UnexpectedException
import play.libs.MimeTypes

class RenderScalateTemplate(content:String,template:String) extends Result {

 def apply(request:Request, response:Response) = {
     try {
            Result.setContentTypeIfNotSet(response, MimeTypes.getContentType(template, "text/plain"))
            response.out.write(content.getBytes("utf-8"))
        } catch {
            case e:Exception => throw new UnexpectedException(e)
        }
  }
}
