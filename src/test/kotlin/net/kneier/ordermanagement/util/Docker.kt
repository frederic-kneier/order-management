package net.kneier.ordermanagement.util

import org.testcontainers.containers.MongoDBContainer

object Docker {
    val Mongo = MongoDBContainer("mongo:5.0.8").apply {
        start()
    }
}