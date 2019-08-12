package com.tapad.docker

case class OutputTable(table: List[List[String]]) {

  val defBorderSeparator = "+"
  val defBorderFiller = "-"
  val defRowSeparator = "|"

  // List that contains the max number of characters per column.
  val columnWidthList = table.transpose.map(_.map(_.length).reduceOption(math.max))

  def mkBorder(borderSeparator: String = defBorderSeparator, borderFiller: String = defBorderFiller): String = {
    columnWidthList
      .map(borderFiller * _.getOrElse(0))
      .mkString(s"$borderSeparator$borderFiller", s"$borderFiller$borderSeparator$borderFiller", s"$borderFiller$borderSeparator")
  }

  def mkRow(row: List[String], separator: String = defRowSeparator): String = {
    row.zip(columnWidthList)
      .map {
        case (cellString, Some(width)) => cellString + " " * (width - cellString.length)
        case _ => ""
      }
      .mkString(s"$separator ", s" $separator ", s" $separator")
  }

  override def toString: String = {
    table match {
      case Nil => ""
      case header :: rows =>
        List(
          mkBorder(),
          mkRow(header),
          mkBorder(borderFiller = "="),
          rows.map(mkRow(_)).mkString("\n"),
          mkBorder()).mkString("\n")
    }
  }
}