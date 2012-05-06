package com.awkin.cjfeedback

import java.net.URL
import java.util.Date
import java.io._

import org.slf4j.Logger
import org.slf4j.LoggerFactory

import org.json._

import ch.qos.logback.classic.LoggerContext
import ch.qos.logback.classic.joran.JoranConfigurator
import ch.qos.logback.core.joran.spi.JoranException
import ch.qos.logback.core.util.StatusPrinter

import scala.io.Source
import scala.xml._
import scala.actors._
import Actor._

class LoggerService(val filename: String) extends Actor {
    val logger: Logger = LoggerFactory.getLogger(classOf[LoggerService])
    val writer = new FileWriter(filename)
    //val writer = Source.fromFile(filename)
    //implicit val codec = scalax.io.Codec.UTF8

    def act() {
        while(true) {
            receive {
                case logData: JSONObject =>
                    logger.debug("log for user:%s, item:%s, action:%s".format(
                                    logData.optString("uid", "null"),
                                    logData.optString("oid", "null"),
                                    logData.optString("action", "null")))
                    /* log the action */
                    writeLogger(logData)
                case (caller : Actor, "quit") =>
                    logger.info("ready to quit")
                    exit
                case _ =>
                    logger.warn("logger svr receive invalid msg")
            }
        }
    }
    private def writeLogger(logData: JSONObject) {
        try {
            writer.write(generateLog(logData))
            writer.flush
        } catch {
            case ex => logger.warn("Fail to write action log: {}", ex.toString)
        }
    }
    private def generateLog(logData: JSONObject) = {
        "%s,%s,%s,%s,%s,%s,%s\n".format(
            logData.optString("action", "null"),
            logData.optString("uid", "-1"),
            logData.optString("oid", "-1"),
            logData.optString("source", "null"),
            logData.optString("length_title", "null"),
            logData.optString("length_desc", "null"),
            logData.optString("length_content", "null")
        )
    }
}
