package com.icecreamapp.sweethearts.ui

import android.app.Application
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import com.icecreamapp.sweethearts.data.DropoffRepository
import com.icecreamapp.sweethearts.data.DropoffRequest
import com.icecreamapp.sweethearts.data.DropoffRequestDisplay
import com.icecreamapp.sweethearts.data.IceCreamMenuItem
import com.icecreamapp.sweethearts.data.IceCreamRepository
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
    private val _dropoffDisplays = MutableStateFlow<List<DropoffRequestDisplay>>(emptyList())
    val dropoffDisplays: StateFlow<List<DropoffRequestDisplay>> = _dropoffDisplays.asStateFlow()

    private val _dropoffLoadError = MutableStateFlow<String?>(null)
    val dropoffLoadError: StateFlow<String?> = _dropoffLoadError.asStateFlow()

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
    }

    fun updateCurrentLocation(latitude: Double, longitude: Double) {
        _currentLocation.value = latitude to longitude
    }

    fun markDropoffDone(dropoffId: String) {
        viewModelScope.launch {
            dropoffRepository.markDropoffDone(dropoffId)
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
