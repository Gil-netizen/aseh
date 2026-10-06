package io.github.gilnetizen.aseh

import android.os.Bundle
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.appcompat.app.AppCompatActivity

class MainActivity : AppCompatActivity() {
  override fun onCreate(savedInstanceState: Bundle?) {
    super.onCreate(savedInstanceState)
    enableEdgeToEdge()

    val graph = (application as AsehApplication).appGraph
    setContent {
      AsehApp(
        clock = graph.clock,
        deviceTimeZone = graph.deviceTimeZone,
        interfacePreferencesRepository = graph.interfacePreferencesRepository,
        manualPlaceContextRepository = graph.manualPlaceContextRepository,
        onSelectedDestinationChanged = graph::setSelectedDestinationId,
      )
    }
  }
}
