package com.grarak.ytfetcher.utils.server

import java.io.Closeable
import java.util.*
import java.util.concurrent.Executors

open class Server(private val url: String) : Closeable {
    companion object {
        private const val API_VERSION = "v1"
    }

    private val executor = Executors.newFixedThreadPool(20)
    private val requests = ArrayList<Request>()

    protected fun parseStatusCode(response: String): Int {
        val statusCode = Status.getStatusCode(response)
        return statusCode ?: Status.ServerOffline
    }

    protected fun getUrl(path: String): String {
        return "$url/api/$API_VERSION/$path"
    }

    protected operator fun get(url: String, requestCallback: Request.RequestCallback) {
        val request = Request()
        synchronized(this) {
            requests.add(request)
        }
        executor.execute {
            request.doRequest(url, null, null, object : Request.RequestCallback {
                override fun onConnect(request: Request, status: Int, url: String) {
                    requestCallback.onConnect(request, status, url)
                }

                override fun onSuccess(request: Request, status: Int,
                                       headers: Map<String, List<String>>, response: String) {
                    handleRequestCallbackSuccess(requestCallback, request, status, headers, response)
                }

                override fun onFailure(request: Request, e: Exception?) {
                    handleRequestCallbackFailure(requestCallback, request, e)
                }
            })
        }
    }

    protected fun post(url: String, data: String, requestCallback: Request.RequestCallback) {
        val request = Request()
        synchronized(this) {
            requests.add(request)
        }
        executor.execute {
            request.doRequest(url, "application/json", data, object : Request.RequestCallback {
                override fun onConnect(request: Request, status: Int, url: String) {
                    requestCallback.onConnect(request, status, url)
                }

                override fun onSuccess(request: Request, status: Int,
                                       headers: Map<String, List<String>>, response: String) {
                    handleRequestCallbackSuccess(requestCallback, request,
                            status, headers, response)
                }

                override fun onFailure(request: Request, e: Exception?) {
                    handleRequestCallbackFailure(requestCallback, request, e)
                }
            })
        }
    }

    private fun handleRequestCallbackSuccess(requestCallback: Request.RequestCallback,
                                             request: Request, status: Int,
                                             headers: Map<String, List<String>>, response: String) {
        synchronized(this) {
            requests.remove(request)
        }
        requestCallback.onSuccess(request, status, headers, response)
    }

    private fun handleRequestCallbackFailure(requestCallback: Request.RequestCallback,
                                             request: Request, e: Exception?) {
        synchronized(this) {
            requests.remove(request)
        }
        requestCallback.onFailure(request, e)
    }

    override fun close() {
        for (request in requests) {
            request.close()
        }
        requests.clear()
    }
}
