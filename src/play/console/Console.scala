package play.console
import scala.tools.nsc._
import java.io.File
import play.Play
import play.db.jpa.JPAPlugin
import jline.ConsoleReader
/**
* provides a simple REPL while keeping play's classpath
**/
object Console {
   def main(args : Array[String]) {
     val root = new File(System.getProperty("application.path"));
     Play.init(root, System.getProperty("play.id", ""));
     println("~")
     println("~ Starting up, please be patient")
     println("~ Ctrl+D to stop")
     println("~")
     Play.start()
     JPAPlugin.startTx(false)  
     try {
       //launch readline loop using play's classloader
       val command = new GenericRunnerCommand(Nil, (error:String) => println(error))
       command.settings.classpath.value = System.getProperty("java.class.path")
       JLine.withJLine {
            val loop = new InterpreterLoop {
              override def createInterpreter() = {
                super.createInterpreter()
              }
            }
            loop.main(command.settings)
        }     
     } catch {
	      case e:Exception=> e.printStackTrace()
     }
     // After the repl exits, kill the scala script
     JPAPlugin.closeTx(false)
     exit(0)
   }
}

/**
* lifted from <a href="http://github.com/harrah/sbt/blob/master/src/main/scala/sbt/LineReader.scala">sbt</a>
* credit goes to Mark Harrah 
**/
private object JLine
{
  def terminal = jline.Terminal.getTerminal
  def createReader() =
    terminal.synchronized
    {
      val cr = new ConsoleReader
      terminal.enableEcho()
      cr.setBellEnabled(false)
      cr
    }
  def withJLine[T](action: => T): T =
  {
    val t = terminal
    t.synchronized
    {
      t.disableEcho()
      try { action }
      finally { t.enableEcho() }
    }
  }
}
