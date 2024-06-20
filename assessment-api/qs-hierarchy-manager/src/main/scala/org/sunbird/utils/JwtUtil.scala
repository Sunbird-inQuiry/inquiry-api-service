package org.sunbird.utils

import com.fasterxml.jackson.databind.ObjectMapper
import com.fasterxml.jackson.module.scala.DefaultScalaModule
import org.sunbird.managers.{KeyData, KeyManager}

import java.nio.charset.StandardCharsets
import java.security.PrivateKey
import java.util
import java.util.{Base64, HashMap, Map}

object JwtUtils {

  private val SEPARATOR = "."
  private val objectMapper: ObjectMapper = new ObjectMapper().registerModule(DefaultScalaModule)
  def createRS256Token(key: String, privateKey: PrivateKey, headerOptions: Map[String, String]): String = {
    val tokenType = JWTokenType.RS256
    val payLoad = createHeader(tokenType, headerOptions) + SEPARATOR + createClaims(key)
    val signature = encodeToBase64Uri(CryptoUtil.generateRSASign(payLoad, privateKey, tokenType.algorithmName))
    payLoad + SEPARATOR + signature
  }


  private def createHeader(tokenType: JWTokenType, headerOptions: Map[String, String]): String = {
    val headerData = new HashMap[String, String]()
    if (headerOptions != null)
      headerData.putAll(headerOptions)
    headerData.put("alg", tokenType.tokenType)
    encodeToBase64Uri(JsonUtil.toJson(headerData).getBytes)
  }

  private def createClaims(subject: String): String = {
    val payloadData = new HashMap[String, Any]()
    payloadData.put("data", subject)
    payloadData.put("iat", System.currentTimeMillis / 1000)
    encodeToBase64Uri(JsonUtil.toJson(payloadData).getBytes)
  }
  private def encodeToBase64Uri(data: Array[Byte]): String = {
    Base64.getUrlEncoder.encodeToString(data)
  }
  def decodeFromBase64(data: String): Array[Byte] = {
    Base64.getDecoder.decode(data)
  }
  def verifyRS256Token(token: String, keyManager: KeyManager): (Boolean, Map[String, Any])= {
    val tokenElements = token.split("\\.")
    val header = tokenElements(0)
    val header_decode=payload(header)
    val body = tokenElements(1)
    val signature = tokenElements(2)
    val payLoad = header + SEPARATOR + body
    var keyData: KeyData = null
    var isValid = false
    keyData = keyManager.getValueFromKeyMap(header_decode.get("keyId").asInstanceOf[String])
    if (keyData != null) {
      isValid = CryptoUtil.verifyRSASign(payLoad, decodeFromBase64(signature), keyData.getPublicKey, "SHA256withRSA")
    }
    if(isValid)
      (isValid,payload(body))
    else
      (isValid,new util.HashMap[String,Any]())
  }
//
//  def decodeFromBase64(data: String): Array[Byte] = {
//    Base64Util.decode(data, 11)
//  }

  def payload(encodedPayload: String): Map[String, Any] = {
    val decodedPayload = new String(Base64.getDecoder.decode(encodedPayload), StandardCharsets.UTF_8)
    objectMapper.readValue(decodedPayload, classOf[Map[String, Any]])
  }
}
