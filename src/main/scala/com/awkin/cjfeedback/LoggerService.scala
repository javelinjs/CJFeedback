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

class LoggerService extends Actor {
    val logger: Logger = LoggerFactory.getLogger(classOf[LoggerService])
    def act() {
        while(true) {
            receive {
                case logData: JSONObject =>
                    logger.debug("log for user:%s, item:%s, action:%s".format(
                                    logData.optString("uid", "null"),
                                    logData.optString("oid", "null"),
                                    logData.optString("action", "null")))
                    /* TODO */
                    logger.info("ACTION {}", logData.toString)
                case (caller : Actor, "quit") =>
                    logger.info("ready to quit")
                    exit
                case _ =>
                    logger.warn("logger svr receive invalid msg")
            }
        }
    }
}
