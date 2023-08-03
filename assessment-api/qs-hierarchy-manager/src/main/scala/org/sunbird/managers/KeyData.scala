package org.sunbird.managers

import java.security.PrivateKey
import java.security.PublicKey

class KeyData(private var keyId: String, private var privateKey: PrivateKey, private var publicKey: PublicKey) {

  def getKeyId: String = keyId

  def setKeyId(keyId: String): Unit = {
    this.keyId = keyId
  }

  def getPrivateKey: PrivateKey = privateKey

  def setPrivateKey(privateKey: PrivateKey): Unit = {
    this.privateKey = privateKey
  }

  def getPublicKey: PublicKey = publicKey

  def setPublicKey(publicKey: PublicKey): Unit = {
    this.publicKey = publicKey
  }
}