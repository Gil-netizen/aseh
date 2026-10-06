package io.github.gilnetizen.aseh

import android.app.Application

class AsehApplication : Application() {
  private val appGraphDelegate = lazy(LazyThreadSafetyMode.SYNCHRONIZED) {
    AppGraph(applicationContext)
  }
  val appGraph: AppGraph
    get() = appGraphDelegate.value

  override fun onTerminate() {
    if (appGraphDelegate.isInitialized()) {
      appGraph.close()
    }
    super.onTerminate()
  }
}
