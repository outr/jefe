package com.outr.jefe.boot.command

import com.outr.jefe.boot.JefeBoot
import com.outr.jefe.resolve.{Credentials, MavenRepository}
import profig.Profig

object RepositoryCommand extends Command {
  override def name: String = "repository"
  override def description: String = "Adds or removes a repository used to resolve Maven dependencies"

  override def execute(): Unit = Profig("arg2").opt[String].getOrElse("") match {
    case "" => help()
    case "add" => {
      val name = Profig("arg3").opt[String].getOrElse(fail("Both the name and URL for the Maven repository must be provided"))
      val url = Profig("arg4").opt[String].getOrElse(fail("Both the name and URL for the Maven repository must be provided"))
      val username = Profig("username").opt[String]
      val password = Profig("password").opt[String]
      if (name.nonEmpty && url.nonEmpty) {
        val repositories = remove(name)
        val credentials = List(username, password).flatten match {
          case un :: pw :: Nil => Some(Credentials(un, pw))
          case _ => None
        }
        val repository = MavenRepository(name, url, credentials)
        JefeBoot.config("repositories").store(repositories ::: List(repository))
        JefeBoot.save()
      }
    }
    case "remove" => remove(Profig("arg3").opt[String].getOrElse(fail("The name for the Maven repository must be provided")))
    case arg => fail(s"The argument '$arg' is unsupported")
  }

  private def fail(message: String): String = {
    logger.info(message)
    logger.info("")
    help()
    ""
  }

  def remove(name: String): List[MavenRepository] = {
    val repositories = JefeBoot.additionalRepositories.filterNot(_.name == name)
    JefeBoot.config("repositories").store(repositories)
    JefeBoot.save()
    repositories
  }

  override def help(): Unit = {
    logger.info(s"Usage: jefe repository [add|remove] [repository name] (repository URL)")
    logger.info("")
    logger.info("Name and URL must be specified if adding a repository, but only name is required for removal.")
    logger.info("Arguments:")
    logger.info("  --username=???: Sets the username credential for authentication on this repository")
    logger.info("  --password=???: Sets the password credential for authentication on this repository")
  }
}