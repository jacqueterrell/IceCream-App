package com.icecreamapp.sweethearts.ui

import android.util.Log
import android.app.Application
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import com.icecreamapp.sweethearts.data.DropoffRepository
import com.icecreamapp.sweethearts.data.DropoffRequest
import com.icecreamapp.sweethearts.data.DropoffRequestDisplay
import com.icecreamapp.sweethearts.data.DropoffWithEta
import com.icecreamapp.sweethearts.data.IceCreamMenuItem
import com.icecreamapp.sweethearts.data.IceCreamRepository
import com.icecreamapp.sweethearts.util.decodePolyline
import com.icecreamapp.sweethearts.util.distanceMeters
import com.icecreamapp.sweethearts.util.formatDistance
import com.icecreamapp.sweethearts.util.reverseGeocode
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

/**
 * ViewModel for the ice cream screen (MVVM).
 * Holds UI state and forwards actions to the repository.
 */
class IceCreamViewModel(
    private val application: Application,
    private val repository: IceCreamRepository = IceCreamRepository(),
    private val dropoffRepository: DropoffRepository = DropoffRepository(),
) : ViewModel() {

    private val _menu = MutableStateFlow<List<IceCreamMenuItem>>(emptyList())
    val menu: StateFlow<List<IceCreamMenuItem>> = _menu.asStateFlow()

    private val _loading = MutableStateFlow(false)
    val loading: StateFlow<Boolean> = _loading.asStateFlow()

    private val _message = MutableStateFlow<String?>(null)
    val message: StateFlow<String?> = _message.asStateFlow()

    private val _dropoffLoading = MutableStateFlow(false)
    val dropoffLoading: StateFlow<Boolean> = _dropoffLoading.asStateFlow()

    private val _dropoffSuccess = MutableStateFlow(false)
    val dropoffSuccess: StateFlow<Boolean> = _dropoffSuccess.asStateFlow()

    private val _dropoffError = MutableStateFlow(false)
    val dropoffError: StateFlow<Boolean> = _dropoffError.asStateFlow()

    private val _dropoffErrorMessage = MutableStateFlow<String?>(null)
    val dropoffErrorMessage: StateFlow<String?> = _dropoffErrorMessage.asStateFlow()

    private val _currentLocation = MutableStateFlow<Pair<Double, Double>?>(null)
    val currentLocation: StateFlow<Pair<Double, Double>?> = _currentLocation.asStateFlow()

    private val _dropoffDisplays = MutableStateFlow<List<DropoffRequestDisplay>>(emptyList())
    val dropoffDisplays: StateFlow<List<DropoffRequestDisplay>> = _dropoffDisplays.asStateFlow()

    private val _dropoffLoadError = MutableStateFlow<String?>(null)
    val dropoffLoadError: StateFlow<String?> = _dropoffLoadError.asStateFlow()

    private val _optimizedDropoffsWithEta = MutableStateFlow<List<DropoffWithEta>>(emptyList())
    val optimizedDropoffsWithEta: StateFlow<List<DropoffWithEta>> = _optimizedDropoffsWithEta.asStateFlow()

    private val _routePolyline = MutableStateFlow<List<Pair<Double, Double>>>(emptyList())
    val routePolyline: StateFlow<List<Pair<Double, Double>>> = _routePolyline.asStateFlow()

    private val _routeLoading = MutableStateFlow(false)
    val routeLoading: StateFlow<Boolean> = _routeLoading.asStateFlow()

    private val _routeError = MutableStateFlow<String?>(null)
    val routeError: StateFlow<String?> = _routeError.asStateFlow()

    /** Admin screen: optimized route polyline for pending dropoffs only. */
    private val _adminRoutePolyline = MutableStateFlow<List<Pair<Double, Double>>>(emptyList())
    val adminRoutePolyline: StateFlow<List<Pair<Double, Double>>> = _adminRoutePolyline.asStateFlow()

    /** Admin screen: pending dropoffs in route order with ETA (5 min dwell per stop). */
    private val _adminDropoffsWithEta = MutableStateFlow<List<DropoffWithEta>>(emptyList())
    val adminDropoffsWithEta: StateFlow<List<DropoffWithEta>> = _adminDropoffsWithEta.asStateFlow()

    private val _adminRouteLoading = MutableStateFlow(false)
    val adminRouteLoading: StateFlow<Boolean> = _adminRouteLoading.asStateFlow()

    /** IDs to hide from admin list immediately when Approve/Cancel is tapped. */
    private val _hiddenFromAdminList = MutableStateFlow<Set<String>>(emptySet())
    val hiddenFromAdminList: StateFlow<Set<String>> = _hiddenFromAdminList.asStateFlow()

    companion object {
        private const val DWELL_SECONDS = 5 * 60L
        private const val TAG_DIRECTIONS = "DirectionsAPI"
    }

    init {
        loadMenu()
        viewModelScope.launch {
            dropoffRepository.dropoffRequestsFlow()
                .combine(_currentLocation) { result, location ->
                    result to location
                }
                .collect { (result, location) ->
                    _dropoffLoadError.value = result.loadError
                    val list = withContext(Dispatchers.IO) {
                        result.requests.map { req ->
                            val address = reverseGeocode(application, req.latitude, req.longitude)
                            val dist = location?.let { (lat, lng) ->
                                distanceMeters(lat, lng, req.latitude, req.longitude)
                            } ?: 0.0
                            DropoffRequestDisplay(request = req, address = address, distanceMeters = dist)
                        }
                    }
                    _dropoffDisplays.value = list
                }
        }
        viewModelScope.launch {
            combine(_dropoffDisplays, _currentLocation) { displays, location ->
                displays to location
            }.collect { (displays, location) ->
                if (displays.isEmpty()) {
                    Log.d(TAG_DIRECTIONS, "Route: skip (no dropoffs)")
                    _optimizedDropoffsWithEta.value = emptyList()
                    _routePolyline.value = emptyList()
                    _routeError.value = null
                    return@collect
                }
                if (location == null) {
                    Log.d(TAG_DIRECTIONS, "Route: skip (no location) dropoffs=${displays.size}")
                    _routeError.value = null
                    _optimizedDropoffsWithEta.value = displays.map { DropoffWithEta(display = it, etaSecondsFromNow = -1L) }
                    _routePolyline.value = emptyList() // Only draw route when we have Directions API (follows streets)
                    return@collect
                }
                _routeLoading.value = true
                _routeError.value = null
                Log.d(TAG_DIRECTIONS, "Route: fetching (location set, dropoffs=${displays.size})")
                dropoffRepository.getOptimizedRoute(
                    originLat = location.first,
                    originLng = location.second,
                    waypoints = displays.map { it.request },
                )
                    .onSuccess { response ->
                        val order = response.waypointOrder
                        val durations = response.legDurationsSeconds
                        val n = minOf(order.size, durations.size)
                        val decoded = decodePolyline(response.encodedPolyline)
                        _routeError.value = null
                        if (decoded.isNotEmpty()) {
                            _routePolyline.value = decoded
                        } else {
                            _routePolyline.value = emptyList()
                        }
                        if (n == 0) {
                            Log.d(TAG_DIRECTIONS, "Route: polylinePoints=${decoded.size} but no waypoint order (n=0)")
                            _optimizedDropoffsWithEta.value = displays.map { DropoffWithEta(display = it, etaSecondsFromNow = -1L) }
                        } else {
                            Log.d(TAG_DIRECTIONS, "Route: success polylinePoints=${decoded.size} waypoints=$n")
                            var accSeconds = 0L
                            val withEta = (0 until n).map { index ->
                                val orderIndex = order[index]
                                val display = displays.getOrNull(orderIndex)
                                    ?: return@map null
                                accSeconds += durations.getOrElse(index) { 0L }
                                val eta = accSeconds
                                accSeconds += DWELL_SECONDS
                                DropoffWithEta(display = display, etaSecondsFromNow = eta)
                            }.filterNotNull()
                            _optimizedDropoffsWithEta.value = withEta
                        }
                    }
                    .onFailure { e ->
                        Log.w(TAG_DIRECTIONS, "Route: failure", e)
                        _routeError.value = e.message ?: "Could not load route"
                        _optimizedDropoffsWithEta.value = displays.map { DropoffWithEta(display = it, etaSecondsFromNow = -1L) }
                        _routePolyline.value = emptyList() // No line when API fails; only Directions API draws on streets
                    }
                _routeLoading.value = false
            }
        }
    }

    fun updateCurrentLocation(latitude: Double, longitude: Double) {
        _currentLocation.value = latitude to longitude
    }

    fun markDropoffDone(dropoffId: String) {
        _hiddenFromAdminList.value = _hiddenFromAdminList.value + dropoffId
        viewModelScope.launch {
            dropoffRepository.markDropoffDone(dropoffId)
        }
    }

    fun updateDropoffStatus(dropoffId: String, status: String) {
        viewModelScope.launch {
            dropoffRepository.updateDropoffStatus(dropoffId, status)
                .onSuccess {
                    if (status == "Canceled") {
                        _hiddenFromAdminList.value = _hiddenFromAdminList.value + dropoffId
                    }
                }
        }
    }

    fun clearHiddenFromAdminList() {
        _hiddenFromAdminList.value = emptySet()
    }

    /**
     * Load optimized route for the admin screen using current location and the given
     * pending dropoffs. Sets [adminRoutePolyline] and [adminDropoffsWithEta].
     * Route line is only set when Directions API succeeds (so it follows streets); otherwise ETAs show as —.
     */
    fun loadAdminRoute(pendingDisplays: List<DropoffRequestDisplay>) {
        viewModelScope.launch {
            if (pendingDisplays.isEmpty()) {
                Log.d(TAG_DIRECTIONS, "Admin route: skip (no pending dropoffs)")
                _adminRoutePolyline.value = emptyList()
                _adminDropoffsWithEta.value = emptyList()
                return@launch
            }
            val location = _currentLocation.value
            if (location == null) {
                Log.d(TAG_DIRECTIONS, "Admin route: skip (no location) pending=${pendingDisplays.size}")
                _adminRoutePolyline.value = emptyList() // Only draw route when we have Directions API (follows streets)
                _adminDropoffsWithEta.value = pendingDisplays.map { DropoffWithEta(display = it, etaSecondsFromNow = -1L) }
                return@launch
            }
            _adminRouteLoading.value = true
            Log.d(TAG_DIRECTIONS, "Admin route: fetching pending=${pendingDisplays.size}")
            dropoffRepository.getOptimizedRoute(
                originLat = location.first,
                originLng = location.second,
                waypoints = pendingDisplays.map { it.request },
            )
                .onSuccess { response ->
                    val order = response.waypointOrder
                    val durations = response.legDurationsSeconds
                    val n = minOf(order.size, durations.size)
                    val decoded = decodePolyline(response.encodedPolyline)
                    _adminRoutePolyline.value = if (decoded.isEmpty()) emptyList() else decoded
                    if (n == 0) {
                        Log.d(TAG_DIRECTIONS, "Admin route: API returned no order/legs")
                        _adminDropoffsWithEta.value = pendingDisplays.map { DropoffWithEta(display = it, etaSecondsFromNow = -1L) }
                    } else {
                        Log.d(TAG_DIRECTIONS, "Admin route: success polylinePoints=${decoded.size} waypoints=$n")
                        var accSeconds = 0L
                        val withEta = (0 until n).map { index ->
                            val orderIndex = order[index]
                            val display = pendingDisplays.getOrNull(orderIndex) ?: return@map null
                            accSeconds += durations.getOrElse(index) { 0L }
                            val eta = accSeconds
                            accSeconds += DWELL_SECONDS
                            DropoffWithEta(display = display, etaSecondsFromNow = eta)
                        }.filterNotNull()
                        _adminDropoffsWithEta.value = withEta
                    }
                }
                .onFailure { e ->
                    Log.w(TAG_DIRECTIONS, "Admin route: failure", e)
                    _adminRoutePolyline.value = emptyList() // No line when API fails; only Directions API draws on streets
                    _adminDropoffsWithEta.value = pendingDisplays.map { DropoffWithEta(display = it, etaSecondsFromNow = -1L) }
                }
            _adminRouteLoading.value = false
        }
    }

    fun loadMenu() {
        viewModelScope.launch {
            _loading.value = true
            _message.value = null
            repository.getMenu()
                .onSuccess { _menu.value = it }
                .onFailure { _message.value = it.message ?: "Failed to load menu" }
            _loading.value = false
        }
    }

    fun requestIceCream(item: IceCreamMenuItem) {
        viewModelScope.launch {
            _loading.value = true
            _message.value = null
            repository.requestIceCream(item.id, item.name)
                .onSuccess { resp ->
                    _message.value = if (resp.success) resp.message else resp.message
                }
                .onFailure { _message.value = it.message ?: "Request failed" }
            _loading.value = false
        }
    }

    fun clearMessage() {
        _message.value = null
    }

    fun requestDropoff(name: String, phoneNumber: String, latitude: Double, longitude: Double) {
        viewModelScope.launch {
            _dropoffLoading.value = true
            _dropoffSuccess.value = false
            _dropoffError.value = false
            _dropoffErrorMessage.value = null
            repository.requestIceCreamDropoff(name, phoneNumber, latitude, longitude)
                .onSuccess {
                    _dropoffSuccess.value = true
                }
                .onFailure { e ->
                    _dropoffError.value = true
                    _dropoffErrorMessage.value = e.message ?: "Request failed"
                }
            _dropoffLoading.value = false
        }
    }

    fun clearDropoffSuccess() {
        _dropoffSuccess.value = false
    }

    fun clearDropoffError() {
        _dropoffError.value = false
        _dropoffErrorMessage.value = null
    }
}
