//package com.outr.jefe.runner
//
//import java.io.File
//
//import com.outr.jefe.repo._
//
//object ConfigCreator {
//  def main(args: Array[String]): Unit = {
//    val repositories = Repositories().withMaven("RoundEights", "http://maven.spikemark.net/roundeights")
//    val config = Configuration("com.zooxoos" %% "synchronizer" % "latest", "com.zooxoos.synchronizer.Main", repositories = repositories)
//    Configuration.save(config, new File("config.zooxoos"))
//  }
//}
