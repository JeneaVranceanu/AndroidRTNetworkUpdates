package rt.network.updates

import android.os.Bundle
import android.widget.TextView
import androidx.appcompat.app.AppCompatActivity
import io.reactivex.rxjava3.android.schedulers.AndroidSchedulers
import io.reactivex.rxjava3.disposables.CompositeDisposable

class MainActivity : AppCompatActivity() {

    private val compositeDisposable = CompositeDisposable()
    private lateinit var textView: TextView

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)
        textView = findViewById(R.id.text_view)

        val service = NetworkReachabilityService.getService(application)
        service.resumeListeningNetworkChanges()

        subscribeToUpdates()
    }

    override fun onDestroy() {
        super.onDestroy()
        unsubscribeFromUpdates()
    }

    private fun unsubscribeFromUpdates() {
        compositeDisposable.dispose()
        compositeDisposable.clear()
    }

    private fun subscribeToUpdates() {
        val disposableSubscription =
            NetworkReachabilityService.NETWORK_REACHABILITY
                .observeOn(AndroidSchedulers.mainThread())
                .subscribe({ networkState ->
                    // We do not care about networkState right now
                    updateIPv4Address()
                }, {
                    // Handle the error
                    it.printStackTrace()
                })

        compositeDisposable.addAll(disposableSubscription)
    }

    private fun updateIPv4Address() {
        val service = NetworkReachabilityService.getService(application)
        textView.text = service.getIpv4HostAddress()
    }
}