package org.sunbird.utils


sealed trait JWTokenType {
  def algorithmName: String
  def tokenType: String
}

object JWTokenType {
  case object HS256 extends JWTokenType {
    val algorithmName: String = "HmacSHA256"
    val tokenType: String = "HS256"
  }

  case object RS256 extends JWTokenType {
    val algorithmName: String = "SHA256withRSA"
    val tokenType: String = "RS256"
  }
}
