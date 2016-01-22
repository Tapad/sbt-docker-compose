package com.tapad.docker

import sbt._

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
}