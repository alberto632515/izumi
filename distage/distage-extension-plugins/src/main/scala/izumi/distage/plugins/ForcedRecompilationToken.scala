package izumi.distage.plugins

import scala.language.experimental.macros
import scala.reflect.api.Universe
import scala.reflect.macros.whitebox

/**
  * This macro enables `distage`'s compile-time checks to work well with Scala's incremental compilation,
  * it forces recompilation of the macro that performs compile-time plan checking every time a PluginDef,
  * or a constructor bound in PluginDef changes. It does that by generating a new unique type for each
  * compiler session and assigning it to a class inheriting PluginDef. The "change of super type" of a plugin
  * forces recompilation of all code that references it, and specifically the [[izumi.distage.staticinjector.plugins.StaticPluginChecker]] macro.
  * This allows compile-time checking macro to provide rapid feedback during development.
  *
  * @see [[https://izumi.7mind.io/distage/distage-framework.html#compile-time-checks Compile-time checks]]
  */
final abstract class ForcedRecompilationToken[T]

object ForcedRecompilationToken {
  implicit def materialize[T]: ForcedRecompilationToken[T] = macro UniqueRecompilationTokenMacro.whiteboxMaterializeImpl

  object UniqueRecompilationTokenMacro {
    final val compilerLaunchId = java.util.UUID.randomUUID().toString
    var cachedTypedTree: Universe#Tree = null

    def whiteboxMaterializeImpl(c: whitebox.Context): c.Tree = {
      import c.universe._

      UniqueRecompilationTokenMacro.synchronized {
        if (cachedTypedTree eq null) {
          val tree = {
            def charToType(c: Char): Tree = c match {
              case '1' => tq"_root_.scala.Product1.type"
              case '2' => tq"_root_.scala.Product2.type"
              case '3' => tq"_root_.scala.Product3.type"
              case '4' => tq"_root_.scala.Product4.type"
              case '5' => tq"_root_.scala.Product5.type"
              case '6' => tq"_root_.scala.Product6.type"
              case '7' => tq"_root_.scala.Product7.type"
              case '8' => tq"_root_.scala.Product8.type"
              case '9' => tq"_root_.scala.Product9.type"
              case '0' => tq"_root_.scala.Product10.type"
              case 'a' => tq"_root_.scala.Int"
              case 'b' => tq"_root_.scala.Long"
              case 'c' => tq"_root_.scala.Short"
              case 'd' => tq"_root_.scala.Byte"
              case 'e' => tq"_root_.scala.Double"
              case 'f' => tq"_root_.scala.Boolean"
            }
            val uuidEncodedAsEithersTpe = compilerLaunchId.filterNot(_ == '-').foldRight(tq"_root_.scala.Unit": c.Tree) {
              (c, t) => tq"_root_.scala.util.Either[${charToType(c)}, $t]"
            }

            c.typecheck(q"null : _root_.izumi.distage.plugins.ForcedRecompilationToken[$uuidEncodedAsEithersTpe]")
          }
          cachedTypedTree = tree
        }
        cachedTypedTree.asInstanceOf[c.Tree]
      }
    }
  }

  // an implementation for better days!
  // As of IDEA 2020.3 EAP, Intellij "Incrementality type: IDEA" doesn't regard changes in singleton types as "real",
  // so instead we have to use the trick above to encode the UUID as Eithers.
  // For IJ 2020.3 EAP on "Incrementality type: Zinc", the situation is even worse as it doesn't even recognize full-blown
  // changes to a class' super-class, but there's nothing we could do workaround that.
  // See bugs on IJ bugtracker:
  //   - https://youtrack.jetbrains.com/issue/SCL-18301
  //   - https://youtrack.jetbrains.com/issue/SCL-18302

//  object UniqueRecompilationTokenMacro {
//    final val compilerLaunchId = java.util.UUID.randomUUID().toString
//    var cachedTypedTree: Universe#Tree = null
//
//    def whiteboxMaterializeImpl(c: whitebox.Context): c.Tree = {
//      import c.universe._
//      UniqueRecompilationTokenMacro.synchronized {
//        if (cachedTypedTree eq null) {
//          val uuidStrConstantType = internal.constantType(Constant(compilerLaunchId))
//          val tree = c.typecheck(q"null : _root_.izumi.distage.plugins.ForcedRecompilationToken[$uuidStrConstantType]")
//          cachedTypedTree = tree
//        }
//        cachedTypedTree.asInstanceOf[c.Tree]
//      }
//    }
//  }
}
