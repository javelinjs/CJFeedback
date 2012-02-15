package com.awkin.cjfeedback

import java.util.Date

import org.json._

import com.mongodb.casbah.Imports._
import com.mongodb.casbah.MongoConnection
import com.mongodb.casbah.MongoDB

import scala.actors._
import Actor._

class Command(val mongoDB: MongoDB, val loggerService: Actor) {
    private var command: JSONObject = new JSONObject().put("valid", 0)
    private var statusStr: String = _
    /* return to the client */
    var response: JSONObject = new JSONObject()

    private var logData: JSONObject = new JSONObject()
    private val feaExtractor = new FeaExtractor(mongoDB)

    def this (cmd: String, db: MongoDB, logger: Actor) {
        this(db, logger)
        putCmd(cmd)
    }

    def putCmd(cmd: String) = {
        command = 
            try {
                new JSONObject(cmd).put("valid", 1)
            } catch {
                case ex: JSONException => new JSONObject().put("valid", 0)
            }
        this
    }

    def run(): JSONObject = {
        val res: JSONObject = new JSONObject()
        command match {
        case CmdValid() =>
            val data = command getJSONObject "data"
            /* extract the item features and other things need to log */
            if (parseIncomeData(data) == true) {
                command match {
                case CmdClick() =>
                    this.logData.put("action", "click")
                    /* log the feedback data */
                    loggerService ! logData
                    this.statusStr = "success"
                    res.put("success", 1)
                case _ =>
                    this.statusStr = "Unknown request"
                    res.put("success", 0)
                    res.put("err", statusStr)
                }
            } else {
                this.statusStr = "Cannot parse 'data' field"
                res.put("success", 0)
                res.put("err", statusStr)
            }
        case CmdInvalid() => 
            this.statusStr = "Invalid request format"
            res.put("success", 0)
            res.put("err", statusStr)
        }
        this.response = res
        this.response
    }

    private def parseIncomeData(jsonData: JSONObject): Boolean = {
        try {
            /* features from the front */
            val userJson = jsonData getJSONObject "user"
            val itemJson = jsonData getJSONObject "item"

            val userid = userJson getString "user_id"
            val itemid = itemJson getString "item_id"

            /* get features from db */
            val features = feaExtractor.getFeature(userid, itemid)

            /* generate the JSON log */
            val jsonLog = new JSONObject()
            jsonLog.put("oid", itemid)
            jsonLog.put("uid", userid)
            for (feature <- features) {
                jsonLog.put(feature.key, feature.value)
            }

            /* change the logData !important */
            this.logData = jsonLog

            /* successfully parsed */
            true
        } catch {
            case ex: JSONException => 
                this.statusStr = "Invalid data format"
                false
            case _ =>
                this.statusStr = "Unknown"
                false
        }
    }
}

/* case Objects */
object CmdInvalid {
    def apply(cmd: JSONObject) : Boolean = {
        unapply(cmd)
    }

    def unapply(cmd: JSONObject) : Boolean = {
        try {
            cmd.getInt("valid") == 0 && 
                cmd.getString("data") != "" &&
                cmd.getString("cmd") != ""
        } catch {
            case _ => false
        }
    }
}

object CmdValid {
    def apply(cmd: JSONObject) : Boolean = !CmdInvalid(cmd)
    def unapply(cmd: JSONObject) : Boolean = apply(cmd)
}

object CmdClick {
    def apply(cmd: JSONObject) : Boolean = {
        unapply(cmd)
    }

    def unapply(cmd: JSONObject) : Boolean = {
        try {
            cmd.getString("cmd") == "click"
        } catch {
            case _ => false    
        }
    }
}

object ErrCode {
    val errCode = Map("401"->"Invalid request format",
                        "402"->"Invalid data format",
                        "409"->"Unknown")
}
