package io.github.gilnetizen.aseh

import android.content.Context
import io.github.gilnetizen.aseh.core.database.InterfacePreferencesRepository
import io.github.gilnetizen.aseh.core.database.InterfacePreferencesRepositoryFactory
import io.github.gilnetizen.aseh.core.database.OperationalStore
import io.github.gilnetizen.aseh.core.database.OperationalStoreFactory
import java.io.Closeable
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.cancel
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.launch

/** Application-scoped manual composition root selected in proposed ADR-0006. */
class AppGraph(context: Context) : Closeable {
  private val applicationScope = CoroutineScope(SupervisorJob() + Dispatchers.IO)
  private val selectedDestinationUpdates = Channel<String>(capacity = Channel.CONFLATED)

  val operationalStore: OperationalStore =
    OperationalStoreFactory.create(context.applicationContext)

  val interfacePreferencesRepository: InterfacePreferencesRepository =
    InterfacePreferencesRepositoryFactory.create(
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
