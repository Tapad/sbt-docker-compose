package com.tapad.docker

import scala.util.parsing.combinator.RegexParsers

case class Version(major: Int, minor: Int, release: Int)

object Version extends RegexParsers {
  def apply(version: String): Version = {
    parseVersion(version)
  }

  def parseVersion(version: String): Version = {
    parse(parser, version) match {
      case Success(ver, _) => ver
      case NoSuccess(msg, _) => throw new RuntimeException(s"Could not parse Version from $version: $msg")
    }
  }

  private val positiveWholeNumber: Parser[Int] = {
    ("0".r | """[1-9]?\d*""".r).map(_.toInt).withFailureMessage("non-negative integer value expected")
  }

  private val parser: Parser[Version] = {
    positiveWholeNumber ~ ("." ~> positiveWholeNumber) ~ ("." ~> positiveWholeNumber) ^^ {
      case major ~ minor ~ release => Version(major, minor, release)
    }
  }
}