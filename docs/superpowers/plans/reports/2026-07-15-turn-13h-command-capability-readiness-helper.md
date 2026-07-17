# TURN-13H Command Capability Readiness Architecture Preflight

> Role: non-binding architecture helper
>
> Scope: source-only preflight for an inert Cloud wiring card. This report is not a review verdict and does not
> change Java, card status, runtime activation, or build state.

## 1. Audited source facts

1. `CloudBrainServer.start(...)` currently creates one `CloudTurnExchange` inline and passes it to
   `CloudTurnRoutes.create(...)`. The returned bundle is then used only to register the two HTTP handlers.
2. `CloudTurnRoutes.Bundle` already retains that exact exchange through its `CloudTurnCommandPort` field, but
   `commandPort()` is package-private. `CloudBrainServer` is in the parent package and cannot consume it.
3. `CloudServiceHost.create(scope, stateRoot)` builds a separate `AnnotationConfigApplicationContext`. Before
   refresh it registers only `CloudServiceScope` and `CloudServiceStorage`; therefore a scanned service cannot
   constructor-inject `CloudTurnCommandPort` today.
4. `CloudServiceConfiguration` scans only `com.bot.dhxy.service`. A future client placed directly under
   `com.yueyunfe.dhxy.cloudbrain.turn` would not be discovered. Scanning the entire existing `turn` package would
   be too broad because that package also owns exchange, HTTP handlers, routes, frames, and transport machinery.
5. There are currently no Java call sites of `CloudServiceHost.create(...)`. Requiring a command capability in its
   construction API therefore does not require a compatibility overload.
6. `CloudTurnActionFactory` is stateless and already validates caller-supplied action IDs and typed actions. It is
   not currently a Spring bean.

## 2. Recommended exact write set

Repository root: `D:\mavenProject\dhxy-cloud-brain`.

| File | Narrow change only |
|---|---|
| `src/main/java/com/yueyunfe/dhxy/cloudbrain/turn/CloudTurnRoutes.java` | Expose the bundle's already-retained `CloudTurnCommandPort`; do not alter exchange/handler construction. |
| `src/main/java/com/yueyunfe/dhxy/cloudbrain/CloudBrainServer.java` | Retain the capability obtained from the same route bundle for later TURN-40 activation; do not construct a host or add an endpoint. |
| `src/main/java/com/yueyunfe/dhxy/cloudbrain/host/CloudServiceHost.java` | Require the command capability as an explicit create argument and register that exact instance as a bean before configuration refresh. |
| `src/main/java/com/yueyunfe/dhxy/cloudbrain/host/CloudServiceConfiguration.java` | Add only the dedicated future client package to component scan and provide the stateless `CloudTurnActionFactory` bean. |

No DHXY file, protocol DTO, Service implementation, HTTP handler, exchange state machine, Task, route, properties,
or application launcher belongs in this card.

## 3. Recommended API shape

### 3.1 Route capability

`CloudTurnRoutes.Bundle` should expose only the existing typed capability:

```java
public CloudTurnCommandPort commandPort() {
    return commandPort;
}
```

It must return the field initialized from the exact `CloudTurnExchange` supplied to `CloudTurnRoutes.create(...)`.
It must not return a new exchange, a wrapper with its own state, or an HTTP-ingress object.

### 3.2 Server retention without activation

`CloudBrainServer` should retain `turnRoutes.commandPort()` as a final capability field (or retain the bundle and
derive the same capability from it). The constructor receives it during the existing `start(...)` assembly.

This card should not add any call to `CloudServiceHost.create(...)`, any host collection, any public activation
endpoint, or any close/start behavior for a host. TURN-40 remains the only place that may authenticate a user scope,
choose the state root, create the host, and own its eventual close.

### 3.3 Dormant host construction

Replace the unused two-argument construction method with the required capability form:

```java
public static CloudServiceHost create(
        CloudServiceScope scope,
        Path stateRoot,
        CloudTurnCommandPort commandPort) {
    CloudTurnCommandPort requiredCommandPort =
            Objects.requireNonNull(commandPort, "commandPort");
    // Existing scope/storage construction remains unchanged.
    context.registerBean(CloudTurnCommandPort.class, () -> requiredCommandPort);
    context.register(CloudServiceConfiguration.class);
    context.refresh();
    ...
}
```

The exact supplied object is registered before `CloudServiceConfiguration` refresh. Do not preserve a two-argument
fallback that could create a host without the command capability.

### 3.4 Narrow client discovery

Extend `CloudServiceConfiguration` scanning to exactly these two roots:

```text
com.bot.dhxy.service
com.yueyunfe.dhxy.cloudbrain.turn.client
```

Do not scan all of `com.yueyunfe.dhxy.cloudbrain.turn` or all of `com.yueyunfe.dhxy.cloudbrain`. TURN-14/15/16
clients should move to the dedicated `turn.client` package and use constructor injection.

The same configuration may expose the existing stateless factory:

```java
@Bean
public CloudTurnActionFactory cloudTurnActionFactory() {
    return new CloudTurnActionFactory();
}
```

No generic bean lookup, class-name dispatch, reflection, static holder, or manually cached application context is
needed.

## 4. Dependency and acceptance gates

Recommended card metadata:

```text
cardId: TURN-13H
type: INFRA / INTEGRATION
countDelta: 0
startDependsOn: TURN-02, TURN-05, TURN-13 source delivery
approval/build dependency: stable Cloud Java writer window
runtime dependency: none; TURN-40 remains future and explicit
```

Source acceptance points for the parent manager:

1. Exactly one `CloudTurnExchange` is created for the registered `/turn` handler and retained command capability.
2. `Bundle.commandPort()` returns that same object identity; no second exchange or state wrapper exists.
3. `CloudServiceHost.create(...)` requires non-null scope, state root, and command port, and registers the supplied
   command port before context refresh.
4. `CloudBrainServer.start(...)` does not create `CloudServiceHost`, refresh a service host context, start a loop,
   or expose user activation.
5. Component scan adds only `turn.client`; HTTP handlers/routes/exchange are not component-scanned into each host.
6. No `@PostConstruct`, `@Scheduled`, executor, thread, timer, retry, owner, permit, session, ledger, static holder,
   singleton, or service locator is introduced.
7. No existing `CloudTurnExchange`, protocol validation, action fencing, HTTP route, or close behavior changes.
8. After all concurrent Cloud writers are stable, run the existing Cloud gate:

```powershell
cd D:\mavenProject\dhxy-cloud-brain
mvn -q clean package
```

Do not start the application/server/host/loop and do not add or run tests for this card.

## 5. TURN-14/15/16 readiness conditions

TURN-14/15/16 become source-implementable through this boundary only when the parent has confirmed all of the
following:

1. The exact `CloudTurnCommandPort` bean and `CloudTurnActionFactory` bean are constructible in the dormant host
   context.
2. Their new clients use these exact mutually exclusive files:
   - TURN-14: `turn/client/CloudBagLocalServiceClient.java` plus its already-declared service/caller files.
   - TURN-15: `turn/client/CloudUiCleanerLocalServiceClient.java` plus its already-declared port file.
   - TURN-16: `turn/client/CloudGiveItemLocalServiceClient.java` plus `DialogService.java`.
3. Each client is a normal constructor-injected component. It may consume `CloudTurnCommandPort` and
   `CloudTurnActionFactory`; it must not inject or instantiate `CloudTurnExchange`, HTTP handlers, routes,
   `CloudServiceHost`, or the old broker/client lifecycle.
4. Caller-provided `actionId`, device/window identity, ordered mechanics, and timeout remain explicit inputs from the
   existing Cloud task/caller boundary. TURN-13H must not manufacture a new identity/session/owner source.
5. TURN-14/15/16 preserve their baseline business order and typed terminal mapping. This preflight only enables the
   transport capability; it does not authorize changing business fallback, retry, timing, or local-Service behavior.
6. Host creation and user-visible execution remain unavailable until TURN-40 supplies authenticated scope,
   state-root selection, explicit start, and lifecycle close ownership.

## 6. Integration risks to keep visible

- Keeping `Bundle.commandPort()` package-private would force TURN-40 or a client to bypass the bundle and retain a
  parallel exchange reference. The explicit accessor is the smaller same-instance boundary.
- Keeping the old two-argument `CloudServiceHost.create(...)` would allow a host graph that cannot satisfy the new
  clients and would weaken the construction invariant.
- Scanning the entire `turn` package could instantiate transport objects per tenant host or create duplicate exchange
  state. Only the dedicated `turn.client` package should be added.
- Letting TURN-14/15/16 each modify `CloudServiceConfiguration` would collide on one shared file and defeat their
  intended parallel write sets. TURN-13H should establish the scan/factory Bean boundary once.
- A successful compile/package proves Java assembly only. Because this card intentionally does not refresh a real
  host at runtime, actual authenticated host creation remains a separate TURN-40 acceptance point.

## 7. Scope statement

This preflight proposes only an inert construction and Bean-injection boundary. It does not alter business logic,
does not migrate a caller, does not increment the 407 ledger, and does not activate any Cloud host, server, loop,
capture, input, UI, Task, or retry path.

