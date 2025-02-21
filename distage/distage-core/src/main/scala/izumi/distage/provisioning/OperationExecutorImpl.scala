package izumi.distage.provisioning

import izumi.distage.model.definition.errors.ProvisionerIssue
import izumi.functional.quasi.QuasiIO
import ProvisionerIssue.ProvisionerExceptionIssue.UnexpectedStepProvisioning
import izumi.distage.model.plan.ExecutableOp.{CreateSet, MonadicOp, NonImportOp, ProxyOp, WiringOp}
import izumi.distage.model.provisioning.strategies.*
import izumi.distage.model.provisioning.{NewObjectOp, OperationExecutor, ProvisioningKeyProvider}
import izumi.reflect.TagK

class OperationExecutorImpl(
  setStrategy: SetStrategy,
  proxyStrategy: ProxyStrategy,
  providerStrategy: ProviderStrategy,
  instanceStrategy: InstanceStrategy,
  effectStrategy: EffectStrategy,
  resourceStrategy: ResourceStrategy,
  contextStrategy: ContextStrategy,
) extends OperationExecutor {

  override def execute[F[_]: TagK](
    context: ProvisioningKeyProvider,
    step: NonImportOp,
  )(implicit F: QuasiIO[F]
  ): F[Either[ProvisionerIssue, Seq[NewObjectOp]]] = {
    F.definitelyRecoverWithTrace(
      executeUnsafe(context, step)
    )((_, trace) => F.pure(Left(UnexpectedStepProvisioning(step, trace.unsafeAttachTraceOrReturnNewThrowable()))))
  }

  private[this] def executeUnsafe[F[_]: TagK](
    context: ProvisioningKeyProvider,
    step: NonImportOp,
  )(implicit F: QuasiIO[F]
  ): F[Either[ProvisionerIssue, Seq[NewObjectOp]]] = step match {
    case op: CreateSet =>
      setStrategy.makeSet(context, op)

    case op: WiringOp.UseInstance =>
      instanceStrategy.getInstance(context, op)

    case op: WiringOp.ReferenceKey =>
      instanceStrategy.getInstance(context, op)

    case op: WiringOp.CallProvider =>
      providerStrategy.callProvider(context, op)

    case op: WiringOp.LocalContext =>
      contextStrategy.prepareContext(context, op)

    case op: ProxyOp.MakeProxy =>
      proxyStrategy.makeProxy(context, op)

    case op: ProxyOp.InitProxy =>
      proxyStrategy.initProxy(context, this, op)

    case op: MonadicOp.ExecuteEffect =>
      effectStrategy.executeEffect[F](context, op)

    case op: MonadicOp.AllocateResource =>
      resourceStrategy.allocateResource[F](context, op)
  }

}
