package com.awkin.cjfeedback

import org.json._
import java.io._

import org.slf4j.Logger
import org.slf4j.LoggerFactory

import ch.qos.logback.classic.LoggerContext
import ch.qos.logback.classic.joran.JoranConfigurator
import ch.qos.logback.core.joran.spi.JoranException
import ch.qos.logback.core.util.StatusPrinter

import com.mongodb.casbah.Imports._
import com.mongodb.casbah.MongoConnection
import com.mongodb.casbah.MongoDB

object FeaDesc {
    def feaKeys: Map[String, FeaFetcher] = 
        Map("length_title" -> new IntFeaFetcher("length_title"), 
            "length_content" -> new IntFeaFetcher("length_content"), 
            "length_desc" -> new IntFeaFetcher("length_desc"), 
            "source" -> new StringFeaFetcher("source"))
}

abstract class FeaFetcher(val key: String) {
    def fetchFeature(obj: MongoDBObject): String
}
class IntFeaFetcher(key: String) extends FeaFetcher(key) {
    override def fetchFeature(obj: MongoDBObject): String = {
        obj.getAsOrElse[Int](key, 0).toString
    }
}
class StringFeaFetcher(key: String) extends FeaFetcher(key) {
    override def fetchFeature(obj: MongoDBObject): String = {
        obj.getAsOrElse[String](key, "")
    }
}

class FeaExtractor(val db: MongoDB) {
    val feaItemColl = db("feature")
    /*
    val itemColl = db("item")
    val channelColl = db("channel") */
    val logger: Logger = LoggerFactory.getLogger(classOf[FeaExtractor])

    /* get features of a specific item from db.
     * if nothing found, then return empty List */
    def getFeature(userid: String, itemid: String): List[Feature] = {
        /* check whether id are valid */
        val (uid: ObjectId, uidValid: Boolean) = 
            try {
                (new ObjectId(userid), true)
            } catch {
                case _ => (new ObjectId(), false)
            }
        val (oid: ObjectId, oidValid: Boolean) = 
            try {
                (new ObjectId(itemid), true)
            } catch {
                case _ => (new ObjectId(), false)
            }

        /* get features from db */
        val feaItemObjOption = 
            if (oidValid) {
                //val field = DBObject("title"->1, "channel"->1)
                val field = DBObject.empty
                feaItemColl.findOne(MongoDBObject("item"->oid), field)
            } else {
                None
            }
        /* contruct the feature list 
         * List() if nothing found 
         * Actually at most one feaItem could be found
         */
        (List[Feature]() /: feaItemObjOption) { (list, feaItemObj) =>
            list ++ 
            /* get feature according to the FeaDesc setting */
            (List[Feature]() /: FeaDesc.feaKeys.keys) { (fealist, key) =>
                val value = FeaDesc.feaKeys(key).fetchFeature(feaItemObj)
                //val value = feaItemObj.getAsOrElse[String](key, "")
                fealist ++ List(Feature(key, value))
            }
        }
    }
}

class Feature(val key: String, val value: String) { 
    override def toString(): String = "key: %s, value: %s".format(key, value)
}
object Feature {
    def apply(key: String, value: String) = {
        new Feature(key, value)
    }
}
