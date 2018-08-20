package com.outr.jefe.boot.command

import com.outr.jefe.boot.JefeBoot
import profig.{Profig, ProfigUtil}

object SetCommand extends Command {
  override def name: String = "set"
  override def description: String = "Sets a persistent variable in Jefe configuration (~/.jefe/config.json)"

  override def execute(): Unit = {
    Profig("arg2").opt[String].flatMap { key =>
      Profig("arg3").opt[String].map(value => key -> value)
    } match {
      case Some((key, value)) => {
        JefeBoot.config(key).merge(ProfigUtil.string2JSON(value))
        JefeBoot.save()
      }
      case None => {
        logger.info("Exactly two arguments must be included!")
        help()
      }
    }
  }

  override def help(): Unit = {
    logger.info("Usage: jefe set [key] [value]")
  }
}