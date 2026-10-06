package io.github.gilnetizen.aseh

import android.content.Context
import io.github.gilnetizen.aseh.core.database.InterfacePreferencesRepository
import io.github.gilnetizen.aseh.core.database.InterfacePreferencesRepositoryFactory
import io.github.gilnetizen.aseh.core.database.ManualPlaceContextRepository
import io.github.gilnetizen.aseh.core.database.ManualPlaceContextRepositoryFactory
import io.github.gilnetizen.aseh.core.database.OperationalStore
import io.github.gilnetizen.aseh.core.database.OperationalStoreFactory
import io.github.gilnetizen.aseh.location.AndroidDeviceLocationClient
import io.github.gilnetizen.aseh.location.DeviceLocationClient
import java.io.Closeable
import java.time.Clock
import java.time.ZoneId
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.cancel
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.launch

/** Application-scoped manual composition root selected in accepted ADR-0006. */
class AppGraph(context: Context) : Closeable {
  private val applicationScope = CoroutineScope(SupervisorJob() + Dispatchers.IO)
  private val selectedDestinationUpdates = Channel<String>(capacity = Channel.CONFLATED)

  val operationalStore: OperationalStore =
    OperationalStoreFactory.create(context.applicationContext)

  val clock: Clock = Clock.systemUTC()

  val deviceTimeZone: () -> ZoneId = ZoneId::systemDefault

  val deviceLocationClient: DeviceLocationClient =
    AndroidDeviceLocationClient(context.applicationContext)

  val interfacePreferencesRepository: InterfacePreferencesRepository =
    InterfacePreferencesRepositoryFactory.create(
      context = context.applicationContext,
      scope = applicationScope,
    )

  val manualPlaceContextRepository: ManualPlaceContextRepository =
    ManualPlaceContextRepositoryFactory.create(
      context = context.applicationContext,
      scope = applicationScope,
    )

  init {
    applicationScope.launch {
      for (destinationId in selectedDestinationUpdates) {
        interfacePreferencesRepository.setSelectedDestinationId(destinationId)
      }
    }
  }

  fun setSelectedDestinationId(destinationId: String) {
    check(selectedDestinationUpdates.trySend(destinationId).isSuccess) {
      "The application preference writer is closed"
    }
  }

  override fun close() {
    selectedDestinationUpdates.close()
    operationalStore.close()
    applicationScope.cancel()
  }
}
