package org.sunbird.utils

import com.fasterxml.jackson.databind.ObjectMapper

import java.lang.reflect.Type
import java.util.Map

object JsonUtil {
  private val objectMapper: ObjectMapper = new ObjectMapper()

  def fromJson[C](json: String, classOfC: Class[C]): C = {
    objectMapper.readValue(json, classOfC)
  }

  def fromJson[T](json: String, `type`: Type): T = {
    objectMapper.readValue(json, objectMapper.constructType(`type`))
  }

  def fromJson[T](json: String, classOfT: Class[T], exceptionMessage: String): T = {
    objectMapper.readValue(json, classOfT)
  }

  def fromMap[C](map: Map[_, _], classOfC: Class[C]): C = {
    objectMapper.convertValue(map, classOfC)
  }

  def toJson(obj: Any): String = {
    objectMapper.writeValueAsString(obj)
  }
}