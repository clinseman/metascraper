package com.beachape.metascraper.extractors.html

import com.beachape.metascraper.extractors.{ SchemaFactory, Schema }
import com.ning.http.client.Response
import dispatch.as.String
import org.jsoup.Jsoup
import org.jsoup.nodes.Document
import play.api.libs.ws.WSResponse

/**
 * Created by Lloyd on 2/15/15.
 */
trait HtmlSchema extends Schema {

  def doc: Document

  /**
   * Gets the non-empty content of a Document element.
   *
   * Returns None if it is empty
   */
  protected def nonEmptyContent(doc: Document, selector: String): Option[String] = Option {
    doc.select(selector).attr("content")
  }.filter(_.nonEmpty)

}

case class HtmlSchemas(schemas: (Document => HtmlSchema)*) extends SchemaFactory {

  def apply(resp: WSResponse, uri: String): Seq[HtmlSchema] = {
    val doc = Jsoup.parse(resp.body, uri)
    schemas.map(_.apply(doc))
  }

}