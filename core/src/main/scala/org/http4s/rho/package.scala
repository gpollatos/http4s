package org.http4s

import scala.language.implicitConversions

import rho.bits.PathAST._
import rho.bits.HeaderAST._
import rho.bits.QueryAST._

import shapeless.{ HNil, :: }
import org.http4s.rho.bits._

import scala.reflect.runtime.universe.TypeTag

package object rho {

  val OPTIONS = Method.Options
  val GET = Method.Get
  val HEAD = Method.Head
  val POST = Method.Post
  val PUT = Method.Put
  val DELETE = Method.Delete
  val TRACE = Method.Trace
  val CONNECT = Method.Connect
  val PATCH = Method.Patch

  private val stringTag = implicitly[TypeTag[String]]

  implicit def method(m: Method): PathBuilder[HNil] = new PathBuilder(m, PathEmpty)

  implicit def pathMatch(s: String): TypedPath[HNil] = TypedPath(PathMatch(s))

  implicit def pathMatch(s: Symbol): TypedPath[String :: HNil] = {
    val capture = PathCapture(StringParser.strParser, stringTag)
    TypedPath(PathAST.MetaCons(capture, TextMeta(s.name, s"Param name: ${s.name}")))
  }

  /**
   * Defines a parameter in query string that should be bound to a route definition
   */
  def param[T](key: String, default: T)(implicit parser: QueryParser[T], m: TypeTag[T]): TypedQuery[T :: HNil] =
    param(key, Some(default))

  /**
   * Defines a parameter in query string that should be bound to a route definition
   */
  def param[T](key: String, default: Option[T] = None)(implicit parser: QueryParser[T], m: TypeTag[T]): TypedQuery[T :: HNil] =
    TypedQuery(QueryCapture(key, parser, default, m))

  /**
   * Defines a path variable of a URI that should be bound to a route definition
   */
  def pathVar[T](implicit parser: StringParser[T], m: TypeTag[T]): TypedPath[T :: HNil] =
    pathVar("unknown")(parser, m)

  /**
   * Defines a path variable of a URI that should be bound to a route definition
   */
  def pathVar[T](id: String)(implicit parser: StringParser[T], m: TypeTag[T]): TypedPath[T :: HNil] =
    TypedPath(PathAST.MetaCons(PathCapture(parser, stringTag), TextMeta(id, s"Param name: $id")))

  def * = CaptureTail()

  /////////////////////////////// Header helpers //////////////////////////////////////

  /* Checks that the header exists */
  def require(header: HeaderKey.Extractable): TypedHeader[HNil] = requireThat(header)(_ => true)

  /* Check that the header exists and satisfies the condition */
  def requireThat[H <: HeaderKey.Extractable](header: H)(f: H#HeaderT => Boolean = { _: H#HeaderT => true }): TypedHeader[HNil] =
    TypedHeader(HeaderRequire(header, f))

  /** requires the header and will pull this header from the pile and put it into the function args stack */
  def capture[H <: HeaderKey.Extractable](key: H): TypedHeader[H#HeaderT :: HNil] =
    TypedHeader(HeaderCapture[H#HeaderT](key))

  def requireMap[H <: HeaderKey.Extractable, R](key: H)(f: H#HeaderT => R): TypedHeader[R :: HNil] =
    TypedHeader(HeaderMapper[H, R](key, f))

}
