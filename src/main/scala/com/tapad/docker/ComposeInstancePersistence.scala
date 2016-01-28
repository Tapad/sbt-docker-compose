package com.tapad.docker

import java.io._

import com.tapad.docker.DockerComposeKeys._
import sbt.{ State, _ }

import scala.collection.Seq
import scala.util.Try

/**
 * Trait for defining how to save Docker Compose RunningInstanceInfo to both the sbt session state and to the disk for
 * longer term persistence across sbt sessions
 */
trait ComposeInstancePersistence extends SettingsHelper {
  val settingsFileName = "dockerComposeInstances.bin"
  val settingsFile = if (new java.io.File("/tmp").exists) {
    s"/tmp/$settingsFileName"
  } else {
    s"${System.getProperty("java.io.tmpdir")}$settingsFileName"
  }
  private var initialized = false

  /**
   * getPersistedState loads any saved dockerCompose instances from previous sbt sessions. It will only be loaded on the
   * initial call.
   * @param state  The current application state which contains the set of instances running
   * @return The updated application state containing any running instances from exited sbt sessions
   */
  def getPersistedState(implicit state: State): State = {
    if (!initialized) {
      initialized = true
      getAttribute(runningInstances) match {
        case Some(_) => state
        case None =>
          Try {
            if (new File(settingsFile).exists) {
              val ois = new ObjectInputStream(new FileInputStream(settingsFile)) {
                override def resolveClass(desc: ObjectStreamClass): Class[_] = {
                  try {
                    Class.forName(desc.getName, false, getClass.getClassLoader)
                  } catch {
                    case ex: ClassNotFoundException => super.resolveClass(desc)
                  }
                }
              }
              return setAttribute(runningInstances, ois.readObject().asInstanceOf[List[RunningInstanceInfo]])
            }
          }
          state
      }
    } else state
  }

  /**
   * saveInstanceState will write out the current docker instance information to a temporary file so that it this
   * information can be used between sbt sessions. If the there are no instances then remove the file.
   * @param state The current application state which contains the set of instances running
   */
  def saveInstanceState(implicit state: State): Unit = {
    Try(getAttribute(runningInstances) match {
      case Some(s) =>
        val oos = new ObjectOutputStream(new FileOutputStream(settingsFile))
        try { oos.writeObject(s) } finally { oos.close() }
      case None =>
        new File(settingsFile).delete()
    })
  }

  /**
   * Gets the sequence of running instance Id's for this sbt project
   * @param state The current application state which contains the set of instances running
   * @return Sequence of running instance Id's for this sbt project
   */
  def getServiceRunningInstanceIds(implicit state: State): Seq[String] = {
    //By default if no arguments are passed return all instances from current sbt project
    val serviceName = getSetting(composeServiceName).toLowerCase
    getAttribute(runningInstances) match {
      case Some(launchedInstances) =>
        //Get the instance names that map to the current sbt projects defined service
        launchedInstances.filter(_.composeServiceName == serviceName).map(_.instanceName)
      case None =>
        Seq.empty
    }
  }

  /**
   * Gets the sequence of running instance Id's for all instances
   * @param state The current application state which contains the set of instances running
   * @return Sequence of running instance Id's for this sbt project
   */
  def getAllRunningInstanceIds(implicit state: State): Seq[String] = {
    getAttribute(runningInstances) match {
      case Some(launchedInstances) =>
        //Get the instance names that map to the current sbt projects defined service
        launchedInstances.map(_.instanceName)
      case None =>
        Seq.empty
    }
  }

  /**
   * Updates the sbt session information into includes the new RunningInstanceInfo object
   * @param state The current application state which contains the set of instances running
   * @param instance The instance information to save
   * @return The updated State which includes the new RunningInstnaceInfo object
   */
  def saveInstanceToSbtSession(implicit state: State, instance: RunningInstanceInfo): State = {
    //Save a list of generated Service Names and port mappings so that it can be used for the dockerComposeStop command
    getAttribute(runningInstances) match {
      case Some(names) => setAttribute(runningInstances, names ::: List(instance))
      case None => setAttribute(runningInstances, List(instance))
    }
  }
}
