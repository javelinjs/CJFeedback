package com.awkin.cjfeedback

import java.net.URL
import java.io._
import java.util.Date
import java.text.SimpleDateFormat
import java.util.Locale
import java.util.TimeZone

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
    val writer = new FileWriter(filename, true) //true means append mode
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
        //val sformat = new SimpleDateFormat("yyyy M d HH:mm:ss", Locale.US)
        //sformat setTimeZone TimeZone.getTimeZone("+0800")
        "%s,%s,%d,%s,%d,%s,%d,%s,%s,%s,%s\n".format(
            logData.optString("action", "null"),
            logData.optString("uid", "-1"),
            logData.optString("uid", "-1").hashCode,
            logData.optString("oid", "-1"),
            logData.optString("oid", "-1").hashCode,
            logData.optString("source", "null"),
            logData.optString("source", "null").hashCode,
            logData.optString("length_title", "-1"),
            logData.optString("length_desc", "-1"),
            logData.optString("length_content", "-1"),
            (new Date()).getTime
        )
    }
}
