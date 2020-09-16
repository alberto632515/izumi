package izumi.distage.injector

import distage.{Id, Injector, ModuleDef}
import izumi.distage.gc.MkGcInjector
import izumi.distage.model.PlannerInput
import org.scalatest.wordspec.AnyWordSpec

class JSRAnnotationTest extends AnyWordSpec with MkGcInjector {
  import JSRAnnotationTest._
  "distage" should {
    "support JSR330 @Named anno" in {
      val definition = PlannerInput.noGC(new ModuleDef {
        make[Int].named("port").from(80)
        make[String].named("address").from("localhost")
        make[ServerConfig].from(ServerConfig)
      })

      val context = Injector.Standard().produce(definition).unsafeGet()

      assert(context.get[ServerConfig].port == context.get[Int]("port"))
      assert(context.get[ServerConfig].address == context.get[String]("address"))
    }
  }
}

object JSRAnnotationTest {
  type Port = Int @javax.inject.Named(value = "port")
  type Address = String @javax.inject.Named("address")

  final case class ServerConfig(port: Port, address: Address)
}
