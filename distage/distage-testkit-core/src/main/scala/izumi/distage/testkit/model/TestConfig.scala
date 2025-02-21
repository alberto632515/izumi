package izumi.distage.testkit.model

import distage.*
import distage.config.AppConfig
import izumi.distage.framework.config.PlanningOptions
import izumi.distage.model.definition.Axis.AxisChoice
import izumi.distage.plugins.PluginConfig
import izumi.distage.testkit.model.TestConfig.PriorAxisDIKeys.MaxLevel
import izumi.distage.testkit.model.TestConfig.{AxisDIKeys, Parallelism, PriorAxisDIKeys}
import izumi.distage.testkit.runner.impl.services.BootstrapFactory
import izumi.logstage.api.Log

import scala.annotation.nowarn
import scala.collection.compat.immutable.ArraySeq
import scala.language.implicitConversions

/**
  * User-defined test settings
  *
  * General options:
  *
  * @param pluginConfig          Source of module definitions from which to build object graphs for each test.
  *                              Changes to [[izumi.distage.plugins.PluginConfig]] that alter implementations of components in [[memoizationRoots]]
  *                              OR their dependencies will cause the test to execute in a new memoization environment,
  *                              check the initial log output in tests for information about the memoization environments in your tests.
  *                              Components specified in `memoizationRoots` will be memoized only for the tests in the same memoization environment.
  * @param bootstrapPluginConfig Same as [[pluginConfig]], but for [[izumi.distage.model.definition.BootstrapModule]].
  *                              Every distinct `bootstrapPluginConfig` will create a distinct memoization environment.
  * @param activation            Chosen activation axes. Changes in [[izumi.distage.model.definition.Activation]] that alter implementations of components in [[memoizationRoots]]
  *                              OR their dependencies will cause the test to execute in a new memoization environment,
  *                              check the initial log output in tests for information about the memoization environments in your tests.
  * @param memoizationRoots      Specifies the components that will be created *once* and shared across all tests within
  *                              the same memoization environment (i.e. with the same [[TestConfig]])
  *                              Every distinct set of `memoizationRoots` will create a distinct memoization environment
  * @param forcedRoots Specifies components that will be treated as if they are a dependency of every test within
  *                    this memoization environment. Components designated as forced roots will not be garbage
  *                    collected even if there are no components or tests that depend on them.
  *                    When combined with `memoizationRoots`, a [[distage.Lifecycle]] binding can be used to
  *                    implement a global start/stop action (beforeAll/afterAll) for all tests within this memoization environment.
  *                    Changes to `forcedRoots` that alter implementations of components in [[memoizationRoots]]
  *                    OR their dependencies will cause the test to execute in a new memoization environment,
  *                    check the initial log output in tests for information about the memoization environments in your tests.
  * @param moduleOverrides    Override loaded plugins with a given [[Module]]. As long as overriden bindings are not memoized,
  *                           or dependencies of memoized components, using overrides will NOT create a new memoization environment.
  *                           Changes to `moduleOverrides` that alter implementations of components in [[memoizationRoots]]
  *                           OR their dependencies will cause the test to execute in a new memoization environment,
  *                           check the initial log output in tests for information about the memoization environments in your tests.
  * @param bootstrapOverrides Same as [[moduleOverrides]], but for [[BootstrapModule]]
  *                           Every distinct `bootstrapPluginConfig` will create a distinct memoization environment.
  *
  *                           Parallelism options:
  * @param parallelEnvs       [[Parallelism]] of distinct memoization environments run, default: [[Parallelism.Unlimited]].
  *                           Sequential envs will run in sequence after the parallel ones.
  * @param parallelSuites     [[Parallelism]] of test suites run, default: [[Parallelism.Unlimited]].
  *                           Sequential suites will run in sequence after the parallel ones.
  * @param parallelTests      [[Parallelism]] of test cases run, default: [[Parallelism.Unlimited]].
  *                           Sequential tests will run in sequence after the parallel ones.
  *
  *                           Other options, Tests with different other options will run in distinct memoization environments:
  * @param configBaseName     Search for config in HOCON resource files with names `\$configBaseName.conf`,
  *                           `\$configBaseName-reference.conf`, `\$configBaseName-reference-dev.conf`
  *                           (see [[izumi.distage.framework.services.ConfigLoader]]
  * @param configOverrides    Overriding definitions on top of main loaded config, default `None`
  * @param bootstrapFactory   [[BootstrapFactory]], controls config loading & initial modules
  * @param planningOptions       [[PlanningOptions]], debug options for [[distage.Planner]]
  * @param logLevel              Log level for the [[logstage.IzLogger]] used in testkit and provided to the tests (will be overriden by plugin / module bindings if exist)
  * @param debugOutput           Print testkit debug messages, including those helping diagnose memoization environment issues,
  *                              default: `false`, also controlled by [[DebugProperties.`izumi.distage.testkit.debug`]] system property
  */
final case class TestConfig(
  // general options
  pluginConfig: PluginConfig,
  bootstrapPluginConfig: PluginConfig = PluginConfig.empty,
  activation: Activation = StandardAxis.testProdActivation,
  moduleOverrides: Module = Module.empty,
  bootstrapOverrides: BootstrapModule = BootstrapModule.empty,
  memoizationRoots: PriorAxisDIKeys = PriorAxisDIKeys.empty,
  forcedRoots: AxisDIKeys = AxisDIKeys.empty,
  // parallelism options
  parallelEnvs: Parallelism = Parallelism.Unlimited,
  parallelSuites: Parallelism = Parallelism.Unlimited,
  parallelTests: Parallelism = Parallelism.Unlimited,
  // other options
  configBaseName: String,
  configOverrides: Option[AppConfig] = None,
  bootstrapFactory: BootstrapFactory = BootstrapFactory.Impl,
  planningOptions: PlanningOptions = PlanningOptions(),
  logLevel: Log.Level = Log.Level.Info,
  debugOutput: Boolean = false,
  activationStrategy: TestActivationStrategy = TestActivationStrategy.LoadConfig(ignoreUnknown = false, warnUnset = true),
  unusedValidAxisChoices: Set[AxisChoice] = Set.empty,
) {
  def activate(changeActivation: Activation => Activation): TestConfig = {
    this.copy(activation = changeActivation(this.activation))
  }

}
object TestConfig {
  def forSuite(clazz: Class[?]): TestConfig = {
    val packageName = clazz.getPackage.getName
    TestConfig(
      pluginConfig = PluginConfig.cached(Seq(packageName)),
      configBaseName = s"${packageName.split('.').last}-test",
    )
  }

  final case class AxisDIKeys(keyMap: Map[Set[AxisChoice], Set[DIKey]]) extends AnyVal {
    /**
      * Consider a section to be activated if for each Axis, one of the Axis Choices in the section is present in the Activation
      *
      *  - empty section is always activated
      *  - axis values for _different_ axes are implicitly under an AND relationship - all axes must be present in Activation to pass
      *  - axis values for _the same_ axis are implicitly under an OR relationship - Activation must contain one of the axis choices for the same axis to pass
      *
      *  Note that this rule currently differs from the rule of activation for bindings themselves, specifically
      *  you cannot specify multiple axis values for _the same_ axis on bindings.
      *
      *  @see [[izumi.distage.model.planning.ActivationChoices#allValid]]
      */
    def getActiveKeys(activation: Activation): Set[DIKey] = {
      def activatedSection(axisValues: Set[AxisChoice], activation: Activation): Boolean = {
        axisValues
          .groupBy(_.axis)
          .forall {
            case (axis, axisValues) =>
              activation.activeChoices.get(axis).exists(axisValues.contains)
          }
      }
      keyMap.iterator.flatMap {
        case (axisValues, keys) if activatedSection(axisValues, activation) => keys
        case _ => Nil
      }.toSet
    }

    def ++(that: AxisDIKeys): AxisDIKeys = {
      val allKeys = ArraySeq.unsafeWrapArray((this.keyMap.iterator ++ that.keyMap.iterator).toArray)
      val updatedKeys = allKeys.groupBy(_._1).map { case (k, kvs) => k -> kvs.iterator.flatMap(_._2).toSet }
      AxisDIKeys(updatedKeys)
    }

    def +(key: DIKey): AxisDIKeys = {
      this ++ Set(key)
    }
    def +(axisKey: (AxisChoice, DIKey)): AxisDIKeys = {
      this ++ Map(axisKey)
    }
    def +(setAxisKey: (Set[AxisChoice], DIKey))(implicit d: DummyImplicit): AxisDIKeys = {
      this ++ Map(setAxisKey)
    }
  }
  object AxisDIKeys {
    def empty: AxisDIKeys = AxisDIKeys(Map.empty)

    @inline implicit def fromSet(set: Set[? <: DIKey]): AxisDIKeys =
      AxisDIKeys(Map(Set.empty -> set.toSet[DIKey]))

    @inline implicit def fromSetMap(map: Iterable[(Set[? <: AxisChoice], Set[? <: DIKey])]): AxisDIKeys =
      AxisDIKeys(map.iterator.map { case (k, v) => k.toSet[AxisChoice] -> v.toSet[DIKey] }.toMap)

    @inline implicit def fromSingleMap(map: Iterable[(AxisChoice, DIKey)]): AxisDIKeys =
      AxisDIKeys(map.iterator.map { case (k, v) => Set(k) -> Set(v) }.toMap)

    @inline implicit def fromSingleToSetMap(map: Iterable[(AxisChoice, Set[? <: DIKey])]): AxisDIKeys =
      AxisDIKeys(map.iterator.map { case (k, v) => Set(k) -> v.toSet[DIKey] }.toMap)

    @inline implicit def fromSetToSingleMap(map: Iterable[(Set[? <: AxisChoice], DIKey)]): AxisDIKeys =
      AxisDIKeys(map.iterator.map { case (k, v) => k.toSet[AxisChoice] -> Set(v) }.toMap)
  }

  final case class PriorAxisDIKeys(keys: Map[Int, AxisDIKeys]) extends AnyVal {
    def ++(that: PriorAxisDIKeys): PriorAxisDIKeys = {
      val allKeys = ArraySeq.unsafeWrapArray((this.keys.iterator ++ that.keys.iterator).toArray)
      val updatedKeys = allKeys.groupBy(_._1).map { case (k, kvs) => k -> kvs.iterator.map(_._2).reduce(_ ++ _) }
      PriorAxisDIKeys(updatedKeys)
    }
    def ++(that: AxisDIKeys)(implicit d: DummyImplicit): PriorAxisDIKeys = {
      this ++ PriorAxisDIKeys(Map(MaxLevel -> that))
    }
    def ++[A](elem: (Int, A))(implicit toAxisDIKeys: A => AxisDIKeys): PriorAxisDIKeys = {
      addToLevel(elem._1, toAxisDIKeys(elem._2))
    }

    def +(key: DIKey): PriorAxisDIKeys = addToLevel(MaxLevel, Set(key))
    def +(priorKey: (Int, DIKey)): PriorAxisDIKeys = addToLevel(priorKey._1, Set(priorKey._2))

    def addToLevel(level: Int, keys: AxisDIKeys): PriorAxisDIKeys = {
      this ++ PriorAxisDIKeys(Map(level -> keys))
    }
  }
  object PriorAxisDIKeys {
    def empty: PriorAxisDIKeys = PriorAxisDIKeys(Map.empty)

    final val MaxLevel = Int.MaxValue

    @inline implicit def fromSet(set: Set[? <: DIKey]): PriorAxisDIKeys =
      PriorAxisDIKeys(Map(MaxLevel -> AxisDIKeys.fromSet(set)))

    @inline implicit def fromPriorSet(map: Map[Int, Set[? <: DIKey]]): PriorAxisDIKeys =
      PriorAxisDIKeys(map.map { case (i, v) => i -> AxisDIKeys.fromSet(v) })

    @inline implicit def fromAxisDIKeys[A](set: A)(implicit toAxisDIKeys: A => AxisDIKeys): PriorAxisDIKeys =
      PriorAxisDIKeys(Map(MaxLevel -> toAxisDIKeys(set)))

    @nowarn("msg=Unused import")
    @inline implicit def fromPriorAxisDIKeys[A](map: Map[Int, A])(implicit toAxisDIKeys: A => AxisDIKeys): PriorAxisDIKeys = {
      import scala.collection.compat.*
      PriorAxisDIKeys(map.view.mapValues(toAxisDIKeys).toMap)
    }
  }

  @deprecated("renamed to Parallelism", "1.1.0")
  type ParallelLevel = Parallelism

  @deprecated("renamed to Parallelism", "1.1.0")
  final val ParallelLevel = Parallelism

  sealed trait Parallelism

  object Parallelism {
    final case class Fixed(n: Int) extends Parallelism

    case object Unlimited extends Parallelism

    case object Sequential extends Parallelism
  }
}
