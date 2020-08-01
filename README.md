# AndroidRTNetworkUpdates
Android project with network update listener. Sets IPv4 address to a TextView as soon as it changes.

Using a `ConnectivityManager` instance we set a listener which reacts on any network change. Each change triggers an update which posts value to RX subject holding the latest network state. By subscribing to the subject we can track network state changes and assume the device had its address changed and thus we refresh IPv4 value displayed in a `TextView`. 
