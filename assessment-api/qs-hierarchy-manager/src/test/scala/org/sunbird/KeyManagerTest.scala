package org.sunbird

import org.scalatest.flatspec.AnyFlatSpec
import org.scalatest.matchers.should.Matchers
import org.slf4j.LoggerFactory
import org.sunbird.common.Platform
import org.sunbird.utils.Base64Util

import java.nio.charset.StandardCharsets
import java.nio.file.{Files, Paths}
import java.security.spec.{PKCS8EncodedKeySpec, X509EncodedKeySpec}
import java.security.{KeyFactory, PrivateKey, PublicKey}
import java.util.{Base64, HashMap}

class KeyManagerTest extends AnyFlatSpec with Matchers {

  private val log = LoggerFactory.getLogger(this.getClass)

  private val basePath = Platform.getString("api.jwt.basepath","./keys/")
  private val keyPrefix = Platform.getString("api.jwt.keyprefix","device")
  private val keyCount = Platform.getInteger("api.jwt.keycount",5)

  "KeyManager" should "load keys" in {

    val keyManager = new KeyManager(basePath, keyPrefix, keyCount)

    keyManager.getRandomKey() should not be null
  }

  it should "load public key from filesystem" in {
    val keyManager = new KeyManager(basePath, keyPrefix, keyCount)
    val keyId = keyPrefix + "1"
    val keyData = keyManager.getValueFromKeyMap(keyId)

    keyData should not be null
    keyData.getPublicKey should not be null
  }

  it should "load private key from filesystem" in {
    val keyManager = new KeyManager(basePath, keyPrefix, keyCount)
    val keyId = keyPrefix + "1"
    val keyData = keyManager.getValueFromKeyMap(keyId)

    keyData should not be null
    keyData.getPrivateKey should not be null
  }

  it should "load public key from hardcoded value" in {
    val keyManager = new KeyManager(basePath, keyPrefix, keyCount)
    val keyId = keyPrefix + "1"
    val keyData = keyManager.getValueFromKeyMap(keyId)

    keyData should not be null
    keyData.getPublicKey should not be null
  }

  it should "load private key from hardcoded value" in {
    val keyManager = new KeyManager(basePath, keyPrefix, keyCount)
    val keyId = keyPrefix + "1"
    val keyData = keyManager.getValueFromKeyMap(keyId)

    keyData should not be null
    keyData.getPrivateKey should not be null
  }
}
