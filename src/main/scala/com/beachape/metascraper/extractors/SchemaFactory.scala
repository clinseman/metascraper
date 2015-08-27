package com.beachape.metascraper.extractors

import play.api.libs.ws.WSResponse

/**
 * Created by Lloyd on 2/15/15.
 */
trait SchemaFactory extends ((WSResponse, String) => Seq[Schema]) {

  /**
   * Based on a [[WSResponse]], returns a list of [[Schema]]
   */
  def apply(s: WSResponse, uri: String): Seq[Schema]

}
