package org.sunbird.managers
import org.slf4j.LoggerFactory
import org.sunbird.common.Platform

import java.io.FileInputStream
import java.nio.charset.StandardCharsets
import java.security.{KeyFactory, PrivateKey, PublicKey}
import java.security.spec.{PKCS8EncodedKeySpec, X509EncodedKeySpec}
import scala.collection.mutable.HashMap
import java.util.Base64
import java.nio.file.{Files, Paths}

class KeyManager(private val basePath: String, private val keyPrefix: String, private val keyCount: Int) {
  private val log = LoggerFactory.getLogger(this.getClass)

  private val keyMap: HashMap[String, KeyData] = new HashMap[String, KeyData]()

  private val loadHardcodedKeys = Platform.getBoolean("useHardcodedKeys",true);

  init()


  private def init(): Unit = {
    loadKeys()
  }

  private def loadKeys(): Unit = {
    for (i <- 1 until keyCount) {
      val keyId = keyPrefix + i
      log.info("Private key loaded - " + basePath + keyId)
      keyMap.put(keyId, new KeyData(keyId, getPrivateKey(basePath + keyId ), loadPublicKey(basePath + keyId + Platform.getString("public.key.suffix",".pub"))))
    }
  }

  private def loadPublicKey(path: String): PublicKey = {
    try {
      if (!loadHardcodedKeys) {
        val in = new FileInputStream(path)
        val keyBytes = new Array[Byte](in.available())
        in.read(keyBytes)
        in.close()

        val publicKey = new String(keyBytes, StandardCharsets.UTF_8)
          .replaceAll("(-+BEGIN RSA PUBLIC KEY-+\\r?\\n|-+END RSA PUBLIC KEY-+\\r?\\n?)", "")
          .replaceAll("(-+BEGIN PUBLIC KEY-+\\r?\\n|-+END PUBLIC KEY-+\\r?\\n?)", "")
          //      .replaceAll("-", "")
          .replaceAll("\\s", "")


        val publicBytes: Array[Byte] = Base64.getMimeDecoder.decode(publicKey)
        val keySpec: X509EncodedKeySpec = new X509EncodedKeySpec(publicBytes)
        val keyFactory: KeyFactory = KeyFactory.getInstance("RSA")
        log.info("Public key loaded from filesystem - " + path)
        keyFactory.generatePublic(keySpec)
      } else {
        val publicKey = Platform.getString("api.jwt.publickey","")
          .replaceAll("(-+BEGIN RSA PUBLIC KEY-+\\r?\\n|-+END RSA PUBLIC KEY-+\\r?\\n?)", "")
          .replaceAll("(-+BEGIN PUBLIC KEY-+\\r?\\n|-+END PUBLIC KEY-+\\r?\\n?)", "")
          //      .replaceAll("-", "")
          .replaceAll("\\s", "")

        val publicBytes = Base64.getDecoder.decode(publicKey)
        val keySpec = new X509EncodedKeySpec(publicBytes)
        val keyFactory = KeyFactory.getInstance("RSA")
        keyFactory.generatePublic(keySpec)
      }
    } catch {
      case e: Exception =>
        throw new Exception("failed to load public key", e)
    }
  }

  def getRandomKey(): KeyData = {
    val keyId = keyPrefix + (Math.random() * keyCount).toInt
    keyMap.getOrElse(keyId, null)
  }

  private def getPrivateKey(path: String): PrivateKey = {
    try {
      if (!loadHardcodedKeys) {
        val privateKeyContent: String = new String(Files.readAllBytes(Paths.get(path)))
        val privateKeyPEM: String = privateKeyContent
          .replace("-----BEGIN PRIVATE KEY-----", "")
          .replace("-----END PRIVATE KEY-----", "")
          .replaceAll("\\s", "")
        val keyBytesDecoded: Array[Byte] = Base64.getDecoder.decode(privateKeyPEM)

        val spec: PKCS8EncodedKeySpec = new PKCS8EncodedKeySpec(keyBytesDecoded)
        val keyFactory: KeyFactory = KeyFactory.getInstance("RSA")
        log.info("loading private key from filesystem", path)
        keyFactory.generatePrivate(spec)

      } else {
        var privateKey = Platform.getString("api.jwt.privatekey", "")
        privateKey = privateKey
          .replaceAll("(-+BEGIN RSA PRIVATE KEY-+\\r?\\n|-+END RSA PRIVATE KEY-+\\r?\\n?)", "")
          .replaceAll("(-+BEGIN PRIVATE KEY-+\\r?\\n|-+END PRIVATE KEY-+\\r?\\n?)", "")
          .replaceAll("\\s", "")
        val privateBytes = Base64.getDecoder.decode(privateKey)
        val keySpec = new PKCS8EncodedKeySpec(privateBytes)
        val keyFactory = KeyFactory.getInstance("RSA")
        keyFactory.generatePrivate(keySpec)
      }
    } catch {
      case e: Exception =>
        throw new Exception("Failed to retrieve private key", e)
    }
  }

  def getValueFromKeyMap(keyId: String): KeyData = {
    keyMap.getOrElse(keyId, throw new NoSuchElementException(s"KeyData not found for keyId: $keyId"))
  }

}
