package izumi.distage.bootstrap

import izumi.distage.bootstrap.DynamicProxyBootstrap.DynamicProxyProvider
import izumi.distage.model.*
import izumi.distage.model.definition.*
import izumi.distage.model.definition.errors.ProvisionerIssue
import izumi.distage.model.plan.ExecutableOp.InstantiationOp
import izumi.distage.model.plan.*
import izumi.distage.model.planning.*
import izumi.distage.model.provisioning.PlanInterpreter.FinalizerFilter
import izumi.distage.model.provisioning.proxies.ProxyProvider
import izumi.distage.model.provisioning.proxies.ProxyProvider.ProxyProviderFailingImpl
import izumi.distage.model.provisioning.strategies.*
import izumi.distage.model.provisioning.{OperationExecutor, PlanInterpreter}
import izumi.distage.model.reflection.{DIKey, MirrorProvider}
import izumi.distage.planning.*
import izumi.distage.planning.sequential.{ForwardingRefResolverDefaultImpl, FwdrefLoopBreaker, SanityCheckerDefaultImpl}
import izumi.distage.planning.solver.SemigraphSolver.SemigraphSolverImpl
import izumi.distage.planning.solver.{GraphPreparations, PlanSolver, SemigraphSolver}
import izumi.distage.provisioning.*
import izumi.distage.provisioning.strategies.*
import izumi.fundamentals.platform.functional.Identity

object BootstrapLocator {
  /**
    * Create an initial bootstrap locator from a module with recipes for `Planner`, `PlanInterpreter` & `BootstrapModule`
    *
    * Workings of the `Injector` can be customized by changing the bootstrap module,
    * e.g. by adding members to [[izumi.distage.model.planning.PlanningHook]] Set.
    *
    * The passed activation will affect _only_ the bootstrapping of the injector itself (see [[izumi.distage.bootstrap.BootstrapLocator]]),
    * to set activation choices, pass `Activation` to [[izumi.distage.model.Planner#plan]] or [[izumi.distage.model.PlannerInput]].
    *
    * @param bootstrapBase Initial bootstrap context module, such as [[izumi.distage.bootstrap.BootstrapLocator.defaultBootstrap]]
    * @param bootstrapActivation A map of axes of configuration to choices along these axes
    * @param overrides Overrides of Injector's own bootstrap environment - injector itself is constructed with DI.
    *                  They can be used to customize the Injector, e.g. by adding members to [[izumi.distage.model.planning.PlanningHook]] Set.
    */
  def bootstrap(
    bootstrapBase: BootstrapContextModule,
    bootstrapActivation: Activation,
    overrides: Seq[BootstrapModule],
    parent: Option[Locator],
  ): Locator = {
    val bindings0 = bootstrapBase overriddenBy overrides.merge
    // BootstrapModule & bootstrap plugins cannot modify `Activation` after 1.0, it's solely under control of `PlannerInput` now.
    // Please open an issue if you need the ability to override Activation using BootstrapModule
    val bindings = bindings0 ++ BootstrapLocator.selfReflectionModule(bindings0, bootstrapActivation)

    val plan =
      BootstrapLocator.bootstrapPlanner
        .plan(bindings, bootstrapActivation, Roots.Everything).getOrThrow()

    val resource =
      BootstrapLocator.bootstrapProducer
        .run[Identity](plan, parent.getOrElse(Locator.empty), FinalizerFilter.all)

    resource.unsafeGet().throwOnFailure()
  }

  private[this] final val mirrorProvider = MirrorProvider.Impl
  private[this] final val fullStackTraces = izumi.distage.DebugProperties.`izumi.distage.interpreter.full-stacktraces`.boolValue(true)

  private final val bootstrapPlanner: Planner = {
    val bootstrapObserver = new PlanningObserverAggregate(Set.empty)

    val mp = mirrorProvider
    val hook = new PlanningHookAggregate(Set.empty)
    val loopBreaker = new FwdrefLoopBreaker.FwdrefLoopBreakerDefaultImpl(mp)
    val forwardingRefResolver = new ForwardingRefResolverDefaultImpl(loopBreaker)
    val sanityChecker = new SanityCheckerDefaultImpl()
    val resolver = new PlanSolver.Impl(
      new SemigraphSolverImpl[DIKey, Int, InstantiationOp](),
      new GraphPreparations(new BindingTranslator.Impl()),
    )

    new PlannerDefaultImpl(
      forwardingRefResolver = forwardingRefResolver,
      sanityChecker = sanityChecker,
      planningObserver = bootstrapObserver,
      hook = hook,
      resolver = resolver,
    )
  }

  private val bootstrapExecutor = new OperationExecutorImpl(
    setStrategy = new SetStrategyDefaultImpl,
    proxyStrategy = new ProxyStrategyFailingImpl,
    providerStrategy = new ProviderStrategyDefaultImpl,
    instanceStrategy = new InstanceStrategyDefaultImpl,
    effectStrategy = new EffectStrategyDefaultImpl,
    resourceStrategy = new ResourceStrategyDefaultImpl,
    contextStrategy = new ContextStrategyDefaultImpl(bootstrapPlanner),
  )

  private final val bootstrapProducer: PlanInterpreter = {
    new PlanInterpreterNonSequentialRuntimeImpl(
      planner = bootstrapPlanner,
      importStrategy = new ImportStrategyDefaultImpl,
      operationExecutor = bootstrapExecutor,
      verifier = new ProvisionOperationVerifier.Default(mirrorProvider),
      fullStackTraces = fullStackTraces,
    )
  }

  final val defaultBootstrap: BootstrapContextModule = new BootstrapContextModuleDef {
    make[Boolean].named("izumi.distage.interpreter.full-stacktraces").fromValue(fullStackTraces)

    make[ProvisionOperationVerifier].from[ProvisionOperationVerifier.Default]

    make[MirrorProvider].fromValue(mirrorProvider)

    make[PlanSolver].from[PlanSolver.Impl]
    make[GraphPreparations]

    make[SemigraphSolver[DIKey, Int, InstantiationOp]].from[SemigraphSolverImpl[DIKey, Int, InstantiationOp]]

    make[ForwardingRefResolver].from[ForwardingRefResolverDefaultImpl]
    make[SanityChecker].from[SanityCheckerDefaultImpl]

    make[Planner].from[PlannerDefaultImpl]
    make[OperationExecutor].from[OperationExecutorImpl]
    make[PlanInterpreter].from[PlanInterpreterNonSequentialRuntimeImpl]

    make[SetStrategy].from[SetStrategyDefaultImpl]
    make[ProviderStrategy].from[ProviderStrategyDefaultImpl]
    make[ImportStrategy].from[ImportStrategyDefaultImpl]
    make[InstanceStrategy].from[InstanceStrategyDefaultImpl]
    make[ContextStrategy].from[ContextStrategyDefaultImpl]
    make[EffectStrategy].from[EffectStrategyDefaultImpl]
    make[ResourceStrategy].from[ResourceStrategyDefaultImpl]

    many[PlanningObserver]
    many[PlanningHook]

    make[PlanningObserver].from[PlanningObserverAggregate]
    make[PlanningHook].from[PlanningHookAggregate]

    make[BindingTranslator].from[BindingTranslator.Impl]

    make[ProxyProvider].tagged(Cycles.Proxy).fromValue(DynamicProxyProvider)
    make[ProxyProvider].fromValue(new ProxyProviderFailingImpl(ProvisionerIssue.ProxyFailureCause.ProxiesDisabled()))

    make[ProxyStrategy].tagged(Cycles.Disable).from[ProxyStrategyFailingImpl]
    make[ProxyStrategy].from[ProxyStrategyDefaultImpl]

    make[FwdrefLoopBreaker].from[FwdrefLoopBreaker.FwdrefLoopBreakerDefaultImpl]
  }

  final val defaultBootstrapActivation: Activation = Activation(
    Cycles -> Cycles.Proxy
  )

  private def selfReflectionModule(bindings0: BootstrapContextModule, bootstrapActivation: Activation): BootstrapModuleDef = {
    new BootstrapModuleDef {
      make[Activation].named("bootstrapActivation").fromValue(bootstrapActivation)
      make[BootstrapModule].fromValue(bindings0)
    }
  }

  lazy val selfReflectionKeys: Set[DIKey] = {
    // passing nulls as values to prevent key list getting out of sync
    val reflectModule = selfReflectionModule(null, null.asInstanceOf[Activation])
    val onlyKeys = reflectModule.keys
    onlyKeys
  }
}
