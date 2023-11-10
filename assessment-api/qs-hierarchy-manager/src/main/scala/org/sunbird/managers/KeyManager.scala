package org.sunbird.managers

import org.slf4j.LoggerFactory
import org.sunbird.KeyData
import org.sunbird.common.Platform
import org.sunbird.utils.Base64Util

import java.io.FileInputStream
import java.nio.charset.StandardCharsets
import java.nio.file.{Files, Paths}
import java.security.spec.{PKCS8EncodedKeySpec, X509EncodedKeySpec}
import java.security.{KeyFactory, PrivateKey, PublicKey}
import java.util.Base64
import scala.collection.mutable.HashMap

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
        val publicKey = "-----BEGIN PUBLIC KEY-----\nMIIBIjANBgkqhkiG9w0BAQEFAAOCAQ8AMIIBCgKCAQEAypzjIoeWTbPJRYNlwhzv\nJ+6q9vXk9HJHvubsMJhdpXX77eEQojtgAkrk3vv5edyhJHs/XY69OJMu4o8qHyhY\nSsSiw0TIuPPIQ3+moZB+yY6MKY7xYiHbKp9xeB1XsFt38H+HtOGX32Q5bL/4CvDS\nHUq7bKoG5wg5dyPkMwQRU/F4T3z9fSnKuRNjsb4OkyyYglJ6tn7uWp+RjPzXXLnB\nnu2S8R6Enw2DPjtQlJmtI941UsONuHPdj7srb4t+2p7jtROhMARDeT3X1DtbqIdK\nNrMu/+Q9APhHSQ5jUgk2nttPFjH8d31pDrcnFjKL7pQytQZeAYIVUB4MQZLnSYVB\nLwIDAQAB\n-----END PUBLIC KEY-----"
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
        var privateKey = "-----BEGIN PRIVATE KEY-----\nMIIEvQIBADANBgkqhkiG9w0BAQEFAASCBKcwggSjAgEAAoIBAQDKnOMih5ZNs8lF\ng2XCHO8n7qr29eT0cke+5uwwmF2ldfvt4RCiO2ACSuTe+/l53KEkez9djr04ky7i\njyofKFhKxKLDRMi488hDf6ahkH7JjowpjvFiIdsqn3F4HVewW3fwf4e04ZffZDls\nv/gK8NIdSrtsqgbnCDl3I+QzBBFT8XhPfP19Kcq5E2Oxvg6TLJiCUnq2fu5an5GM\n/NdcucGe7ZLxHoSfDYM+O1CUma0j3jVSw424c92Puytvi37anuO1E6EwBEN5PdfU\nO1uoh0o2sy7/5D0A+EdJDmNSCTae208WMfx3fWkOtycWMovulDK1Bl4BghVQHgxB\nkudJhUEvAgMBAAECggEAB3Bz1u05vUTU84q/CwqkuoeE9HtgoQJFlLW5RcS0Arxb\nXmEaPlkSwRLLgE2p9WnLhHtLMmq1LOVOkX5mZaCsGT0XqTbJ1FMMu9m6Hj2GQjoY\nw5MRiHDFmAHxslJvFhuA3GFtjXX10+IQr7Seui9PouHleGuhXdmlKBtqKHgr3YEA\nxAkPoQl1Co/yafQ911RH13PM005UVVpCaD/2xwLnJPqrNxM3YktWNQKBAunDTdxo\nQ8JeCgTKNdWSJ03nBJ2u9wi6EGaZJfVG4uEF5B85lhEfo3O0kTh2B/3qdSl8Aknm\ngcOsoN1D8ivgld/IZebJ89NKgwSbiCP035cTYtS4jQKBgQDWsbb3016HE6AxUwGl\n/pmhGV4uWx0YjwgM2T30SXICNtQw7JWs9CXjoxD+ee3mYcB70Ag3+3g/CC82w/Lr\nz1sYNEx4HodvuIqo6XYYE5dg0QXkMehW+dxqx+VtCcwC0w7nGar9SfMtIwr8Wg/6\nqH5jcKxPaB2LMFtR/yCqBmbWowKBgQDxmCHT3RBE+VvmhAeM6i6EwLV1JS6+ZFf6\nOjDC3SRN2cMU/8wFEpZ12tDTTJnxrCKgWoZKUwdRm9upmGiMelq5M9mhJRPZeGrr\nlBs9I/vj2vJbu8QLCYVUC+JY8r3ezlnLp2Xr9dCebnECbgzQcAj+rq5tzedLhUbZ\nPsgC6HGwBQKBgDXjkauPEJETKgh3b1h9GY7IUU2NbTY24Kxo8xYYQVew734ARGmP\nNtt2mNNnQ4GqU6hARW/X3QzlPwSeFqF+AL2IkxEriI9QYO2Y/B16/Wo9zR7EMC90\ntBDRcBL4fI7Q71KurK67GyDfROimqpAeLutC4t1jotbHIoToZwiGZtXFAoGAHc/P\nBMy3kDtQ+s35/Ip9OQZqncz7yqSpMohxseoF69FeQD4cV9fmVx6sPBasvGSoVS82\neP9r3MclwPS8mfETNt1OEpN3spMoZm99OPsyvvgqheVSmKYRHMDmqmExysedzwKW\nEhrgJlysd0dLL4FTqtG1Vnlc/DWy+2XC2pECTl0CgYEAtzasBUsXp59IMhTSITKA\nZphiiBSTmEyhs6WptbbgOkAlLbT7YYhgpsCjKqFdLjGBZ+/Z9sICKlowgE8EueJ7\ntM0bnS/28Bgbd/QU1yJmglm3MbXzk9Q5tOE1Y3SNzwkT4kjp4e4AQvkVJt78ds+y\nPBvoTfmeV7/ov7/1OzRzHG0=\n-----END PRIVATE KEY-----"
        privateKey = privateKey
          .replaceAll("(-+BEGIN RSA PRIVATE KEY-+\\r?\\n|-+END RSA PRIVATE KEY-+\\r?\\n?)", "")
          .replaceAll("(-+BEGIN PRIVATE KEY-+\\r?\\n|-+END PRIVATE KEY-+\\r?\\n?)", "")

        val keySpec = new PKCS8EncodedKeySpec(Base64Util.decode(privateKey.getBytes("UTF-8"), Base64Util.DEFAULT))
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
