package com.beachape.metascraper

import com.beachape.metascraper.Messages._
import akka.actor.{ ActorLogging, ActorRef, Actor, Props }
import com.google.inject.Inject
import dispatch._
import java.util.concurrent.Executors
import com.ning.http.client.{ AsyncHttpClientConfig, AsyncHttpClient }
import play.api.libs.ws.WSClient

import scala.util.{ Failure, Success }

/**
 * Companion object for instantiating ScaperActors
 */
object ScraperActor {

  /**
   * Factory method for the params required to instantiate a MonitorActor
   *
   * @param httpExecutorThreads Int number of threads to use for this actor's async HTTP executor service
   * @param maxConnectionsPerHost Int max connections at a time per host
   * @param connectionTimeoutInMs Int time in milliseconds before timing out when trying to make a connection to a host
   * @param requestTimeoutInMs Int time in milliseconds before timing out when waiting for a request to complete after connection
   * @return Props for instantiating a ScaperActor
   */
  def apply(httpExecutorThreads: Int = 10,
    maxConnectionsPerHost: Int = 30,
    connectionTimeoutInMs: Int = 10000,
    requestTimeoutInMs: Int = 15000) =
    Props(
      classOf[ScraperActor],
      httpExecutorThreads,
      maxConnectionsPerHost,
      connectionTimeoutInMs,
      requestTimeoutInMs)
}

/**
 * Actor for scraping metadata from websites at URLs
 *
 * Should be instantiated with Props provided via companion object factory
 * method
 */
class ScraperActor(wsClient: WSClient)(
  httpExecutorThreads: Int = 10,
  maxConnectionsPerHost: Int = 30,
  connectionTimeoutInMs: Int = 10000,
  requestTimeoutInMs: Int = 15000)
    extends Actor with ActorLogging {

  import context.dispatcher

  // Validator
  val validSchemas = Seq("http", "https")
  // Http client config
  val followRedirects = true
  val connectionPooling = true
  val compressionEnabled = true

  private val executorService = Executors.newFixedThreadPool(httpExecutorThreads)

  private val scraper = new Scraper(wsClient, validSchemas)

  override def postStop() {
    wsClient.close()
    executorService.shutdown()
  }

  def receive = {

    case message: ScrapeUrl => {
      val zender = sender()
      val fScrapedData = scraper.fetch(message)
      fScrapedData onComplete {
        case Success(data) => zender ! Right(data)
        case Failure(e) => logAndForwardErrorAsLeft(e, zender)
      }
    }

    case _ => log.error("Scraper Actor received an unexpected message :( !")
  }

  /**
   * Helper function that logs an error and forwards the throwable
   *
   * @param throwable Throwable
   * @param sendToRef Actor to send the message to
   */
  def logAndForwardErrorAsLeft(throwable: Throwable, sendToRef: ActorRef) {
    log.error(throwable.getMessage)
    sendToRef ! Left(throwable)
  }

}
