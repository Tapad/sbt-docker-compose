package com.tapad.docker

import sbt._

import scala.collection.Seq

/**
 * Access all SBT project settings and attributes through this trait so that the values can be mocked under test
 */
trait SettingsHelper {
  def getSetting[T](setting: SettingKey[T])(implicit state: State): T = {
    val extracted = Project.extract(state)
    extracted.get(setting)
  }

  def getAttribute[T](attribute: AttributeKey[T])(implicit state: State): Option[T] = {
    state.get(attribute)
  }

  def setAttribute[T](attribute: AttributeKey[T], value: T)(implicit state: State): State = {
    state.put(attribute, value)
  }

  def removeAttribute[T](attribute: AttributeKey[T])(implicit state: State): State = {
    state.copy(attributes = state.attributes.remove(attribute))
  }

  def containsArg(arg: String, args: Seq[String]): Boolean = {
    args != null && args.exists(_.contains(arg))
  }

  /**
   * Given an input argument of the format <arg>:<value> this function will return the Option[<value>] if it exists otherwise None
   * @param arg The argument name of the value to retrieve
   * @param args The set of arguments from the command line
   * @return None if the argument value is malformed or not found. Otherwise, an Option[String] with the argument value is returned.
   */
  def getArgValue(arg: String, args: Seq[String]): Option[String] = {
    val argList = args.filter(_.contains(arg))
    val debugPort = if (argList.nonEmpty) {
      val argValues = argList.head.split(':')
      if (argValues.length == 2) {
        Some(argValues(1))
      } else
        None
    } else
      None

    debugPort
  }
}