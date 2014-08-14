package com.adobe

// Scala.
import scala.collection.mutable.StringBuilder

object Helpers {
  implicit class MySB(val sb: StringBuilder) {
    def appendLn(s: String) = sb.append(s + "\n")
  }
}
