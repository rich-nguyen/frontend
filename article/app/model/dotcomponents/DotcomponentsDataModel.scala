package model.dotcomponents

import common.Edition
import conf.Configuration
import controllers.ArticlePage
import model.SubMetaLinks
import model.dotcomrendering.pageElements.PageElement
import navigation.NavMenu
import play.api.libs.json.{JsValue, Json, Writes}
import play.api.mvc.RequestHeader
import views.support.{CamelCase, GUDateTimeFormat}
import ai.x.play.json.Jsonx
import common.Maps.RichMap
import navigation.UrlHelpers.{Footer, Header, SideMenu, getReaderRevenueUrl}
import navigation.ReaderRevenueSite.{Support, SupportContribute, SupportSubscribe}
import ai.x.play.json.implicits.optionWithNull // Note, required despite Intellij saying otherwise

// We have introduced our own set of objects for serializing data to the DotComponents API,
// because we don't want people changing the core frontend models and as a side effect,
// making them incompatible with Dotcomponents. By having our own set of models, there's
// only one reason for change.
// exceptions: we do resuse the existing Nav & BlockElement classes right now

case class TagProperties(
    id: String,
    tagType: String,
    webTitle: String,
    twitterHandle: Option[String]
)
case class Tag(
    properties: TagProperties
)

case class Block(
    bodyHtml: String,
    elements: List[PageElement]
)

case class Blocks(
    main: Option[Block],
    body: List[Block]
)

case class ReaderRevenueLink(
  contribute: String,
  subscribe: String,
  support: String
)

case class ReaderRevenueLinks(
  header: ReaderRevenueLink,
  footer: ReaderRevenueLink,
  sideMenu: ReaderRevenueLink
)

case class PageData(
    author: String,
    pageId: String,
    pillar: Option[String],
    ajaxUrl: String,
    webPublicationDate: Long,
    webPublicationDateDisplay: String,
    section: Option[String],
    headline: String,
    webTitle: String,
    byline: String,
    contentId: Option[String],
    authorIds: Option[String],
    keywordIds: Option[String],
    toneIds: Option[String],
    seriesId: Option[String],
    isHosted: Boolean,
    beaconUrl: String,
    editionId: String,
    edition: String,
    contentType: Option[String],
    commissioningDesks: Option[String],
    subMetaLinks: SubMetaLinks,
    sentryHost: Option[String],
    sentryPublicApiKey: Option[String],
    switches: Map[String,Boolean],


    // AMP specific
    guardianBaseURL: String,
    webURL: String,
    shouldHideAds: Boolean,
    hasStoryPackage: Boolean,
    hasRelated: Boolean,
)

case class Config(
    isImmersive: Boolean,
    page: PageData,
    nav: NavMenu,
    readerRevenueLinks: ReaderRevenueLinks
)

case class ContentFields(
    standfirst: Option[String],
    main: String,
    body: String,
    blocks: Blocks
)

case class DotcomponentsDataModel(
    contentFields: ContentFields,
    config: Config,
    tags: List[Tag],
    version: Int
)

object Block {
  implicit val blockElementWrites: Writes[PageElement] = Json.writes[PageElement]
  implicit val writes = Json.writes[Block]
}

object Blocks {
  implicit val writes = Json.writes[Blocks]
}

object ContentFields {
  implicit val writes = Json.writes[ContentFields]
}

object TagProperties {
  implicit val writes = Json.writes[TagProperties]
}

object Tag {
  implicit val writes = Json.writes[Tag]
}

object ReaderRevenueLink {
  implicit val writes = Json.writes[ReaderRevenueLink]
}

object ReaderRevenueLinks {
  implicit val writes = Json.writes[ReaderRevenueLinks]
}

object PageData {
  // We use Jsonx here because PageData exceeds 22 fields and
  // regular Play JSON is unable to serialise this. See, e.g.
  //
  // * https://github.com/playframework/play-json/issues/3
  // * https://stackoverflow.com/questions/23571677/22-fields-limit-in-scala-2-11-play-framework-2-3-case-classes-and-functions/23588132#23588132
  implicit val formats = Jsonx.formatCaseClass[PageData]
}

object Config {
  implicit val writes = Json.writes[Config]
}

object DotcomponentsDataModel {

  val VERSION = 1

  def fromArticle(articlePage: ArticlePage, request: RequestHeader): DotcomponentsDataModel = {

    val article = articlePage.article

    val bodyBlocks: List[Block] = article.blocks match {
      case Some(bs) => bs.body.map(bb => Block(bb.bodyHtml, bb.dotcomponentsPageElements.toList)).toList
      case None => List()
    }

    val mainBlock: Option[Block] = article.blocks.flatMap(
      _.main.map(bb=>Block(bb.bodyHtml, bb.dotcomponentsPageElements.toList))
    )

    val dcBlocks = Blocks(mainBlock, bodyBlocks)

    val contentFields = ContentFields(
      article.fields.standfirst,
      article.fields.main,
      article.fields.body,
      dcBlocks
    )

    val jsConfig = (k: String) => articlePage.getJavascriptConfig.get(k).map(_.as[String])


    val jsPageData = Configuration.javascript.pageData mapKeys { key =>
      CamelCase.fromHyphenated(key.split('.').lastOption.getOrElse(""))
    }

    val switches = conf.switches.Switches.all.filter(_.exposeClientSide).foldLeft(Map.empty[String,Boolean])( (acc, switch) => {
      acc + (CamelCase.fromHyphenated(switch.name) -> switch.isSwitchedOn)
    })

    val pageData = PageData(
      article.tags.contributors.map(_.name).mkString(","),
      article.metadata.id,
      article.metadata.pillar.map(_.toString),
      Configuration.ajax.url,
      article.trail.webPublicationDate.getMillis,
      GUDateTimeFormat.formatDateTimeForDisplay(article.trail.webPublicationDate, request),
      article.metadata.section.map(_.value),
      article.trail.headline,
      article.metadata.webTitle,
      article.trail.byline.getOrElse(""),
      jsConfig("contentId"),   // source: content.scala
      jsConfig("authorIds"),   // source: meta.scala
      jsConfig("keywordIds"),  // source: tags.scala and meta.scala
      jsConfig("toneIds"),     // source: meta.scala
      jsConfig("seriesId"),    // source: content.scala
      article.metadata.isHosted,
      Configuration.debug.beaconUrl,
      Edition(request).id,
      Edition(request).displayName,
      jsConfig("contentType"),
      jsConfig("commissioningDesks"),
      article.content.submetaLinks,
      jsPageData.get("sentryHost"),
      jsPageData.get("sentryPublicApiKey"),
      switches,
      Configuration.site.host,
      article.metadata.webUrl,
      article.content.shouldHideAdverts,
      hasStoryPackage = articlePage.related.hasStoryPackage,
      hasRelated = article.content.showInRelated,
    )

    val tags = article.tags.tags.map(
      t => Tag(
        TagProperties(
          t.id,
          t.properties.tagType,
          t.properties.webTitle,
          t.properties.twitterHandle
        )
      )
    )

    val navMenu = NavMenu(articlePage, Edition(request))

    val headerReaderRevenueLink: ReaderRevenueLink = ReaderRevenueLink(
      getReaderRevenueUrl(SupportContribute, Header)(request),
      getReaderRevenueUrl(SupportSubscribe, Header)(request),
      getReaderRevenueUrl(Support, Header)(request)
    )

    val footerReaderRevenueLink: ReaderRevenueLink = ReaderRevenueLink(
      getReaderRevenueUrl(SupportContribute, Footer)(request),
      getReaderRevenueUrl(SupportSubscribe, Footer)(request),
      getReaderRevenueUrl(Support, Footer)(request)
    )

    val sideMenuReaderRevenueLink: ReaderRevenueLink = ReaderRevenueLink(
      getReaderRevenueUrl(SupportContribute, SideMenu)(request),
      getReaderRevenueUrl(SupportSubscribe, SideMenu)(request),
      getReaderRevenueUrl(Support, SideMenu)(request)
    )

    val readerRevenueLinks = ReaderRevenueLinks(
      headerReaderRevenueLink,
      footerReaderRevenueLink,
      sideMenuReaderRevenueLink
    )

    val config = Config(
      article.isImmersive,
      pageData,
      navMenu,
      readerRevenueLinks
    )

    DotcomponentsDataModel(
      contentFields,
      config,
      tags,
      VERSION
    )

  }

  def toJson(model: DotcomponentsDataModel): JsValue = {

    // make what we have look a bit closer to what dotcomponents currently expects

    implicit val DotComponentsDataModelWrites = new Writes[DotcomponentsDataModel] {
      def writes(model: DotcomponentsDataModel) = Json.obj(
        "config" -> model.config,
        "contentFields" -> Json.obj(
          "fields" -> model.contentFields
        ),
        "tags" -> Json.obj(
          "tags" -> model.tags
        ),
        "version" -> model.version
      )
    }

    Json.toJson(model)

  }


  def toJsonString(model: DotcomponentsDataModel): String = {
    Json.stringify(toJson(model))
  }

}
