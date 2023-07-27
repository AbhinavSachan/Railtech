package com.dpr.railtech

import android.annotation.SuppressLint
import android.app.DownloadManager
import android.content.ContentValues
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.graphics.Bitmap
import android.net.ConnectivityManager
import android.net.Uri
import android.net.http.SslError
import android.os.Build
import android.os.Bundle
import android.os.Environment
import android.os.Handler
import android.os.Looper
import android.provider.MediaStore
import android.provider.MediaStore.Images.Media
import android.util.Log
import android.view.KeyEvent
import android.view.View
import android.webkit.RenderProcessGoneDetail
import android.webkit.SslErrorHandler
import android.webkit.ValueCallback
import android.webkit.WebChromeClient
import android.webkit.WebResourceError
import android.webkit.WebResourceRequest
import android.webkit.WebResourceResponse
import android.webkit.WebView
import android.webkit.WebViewClient
import android.widget.Toast
import androidx.annotation.RequiresApi
import androidx.appcompat.app.AlertDialog
import androidx.appcompat.app.AppCompatActivity
import androidx.core.app.ActivityCompat
import androidx.core.content.ContextCompat
import androidx.swiperefreshlayout.widget.SwipeRefreshLayout
import com.google.android.material.dialog.MaterialAlertDialogBuilder
import java.io.IOException
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale
import java.util.concurrent.ExecutorService
import java.util.concurrent.Executors


class MainActivity2 : AppCompatActivity() {
    private lateinit var loader: View
    private var isDialogShown: Boolean = false
    private var handler: Handler? = null
    private var service: ExecutorService? = null
    var mWebView: WebView? = null
    private var alertDialog: AlertDialog? = null

    private val CAMERA_PERMISSION_REQUEST_CODE = 1001

    private val INPUT_FILE_REQUEST_CODE = 1132
    private val CAMERA_CAP_REQUEST_CODE = 1531
    private var mCapturedImageURI: Uri? = null
    private var mFilePathCallback: ValueCallback<Array<Uri>>? = null
    private var dialog: AlertDialog? = null

    @Deprecated("Deprecated in Java")
    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        super.onActivityResult(requestCode, resultCode, data)
        if (resultCode == RESULT_CANCELED) {
            if (mFilePathCallback != null) {
                mFilePathCallback?.onReceiveValue(null)
                mFilePathCallback = null
            }
        }
        if (requestCode == INPUT_FILE_REQUEST_CODE && resultCode == RESULT_OK) {
            Log.d("URI", "result gallery")

            if (mFilePathCallback == null) return
            Log.d(
                "URI", "${data?.data} ${
                    WebChromeClient.FileChooserParams.parseResult(
                        resultCode, data
                    )
                }"
            )
            mFilePathCallback?.onReceiveValue(
                WebChromeClient.FileChooserParams.parseResult(
                    resultCode, data
                )
            )
            mFilePathCallback = null
        } else if (requestCode == CAMERA_CAP_REQUEST_CODE && resultCode == RESULT_OK) {
            Log.d("URI", "result camera")

            if (mFilePathCallback == null) return

            val imageUri: Uri? = mCapturedImageURI
            val result: Array<Uri?> = arrayOf(imageUri)
            mFilePathCallback?.onReceiveValue(result.toNonNullArray())

            mFilePathCallback = null
            mCapturedImageURI = null
        }
    }

    private fun getDialog(context: Context): AlertDialog {
        val builder = MaterialAlertDialogBuilder(context)
        builder.setTitle("Internet Unavailable")
        builder.setMessage("Please connect to the internet.\nIf this persists please try resetting your DNS settings")
        builder.setCancelable(false)
        builder.setPositiveButton("Refresh") { _, _ ->
            mWebView?.reload()
            isDialogShown = false
        }
        return builder.create()
    }

    private fun setNetworkListener() {
        service = Executors.newFixedThreadPool(1)
        alertDialog = getDialog(this)
        service?.execute {
            var finalIsNetworkAvailable = isNetworkAvailable

            // Check network availability continuously
            while (true) {
                if (finalIsNetworkAvailable) {
                    handler?.post {
                        if (isDialogShown) {
                            alertDialog?.cancel()
                            mWebView?.reload()
                            isDialogShown = false
                        }
                    }
                } else {
                    handler?.post {
                        if (!isDialogShown) {
                            alertDialog?.show()
                            isDialogShown = true
                        }
                    }
                }

                Thread.sleep(1000) // Wait for 1 second
                finalIsNetworkAvailable = isNetworkAvailable
            }
        }
    }

    /**
     * Checks whether internet available or not
     *
     * @return network status in boolean
     */
    private val isNetworkAvailable: Boolean
        get() {
            try {
                val connectivityManager =
                    this.getSystemService(CONNECTIVITY_SERVICE) as ConnectivityManager
                val activeNetworkInfo = connectivityManager.activeNetworkInfo
                return (activeNetworkInfo != null) && activeNetworkInfo.isConnected
            } catch (e: Exception) {
                e.printStackTrace()
            }
            return false
        }


    private fun Array<Uri?>?.toNonNullArray(): Array<Uri> {
        return this?.filterNotNull()?.toTypedArray() ?: emptyArray()
    }

    @Throws(IOException::class)
    private fun createImageFileName(): String {
        // Create an image file name
        val timeStamp: String =
            SimpleDateFormat("yyyyMMdd_HHmmss", Locale.getDefault()).format(Date())
        val imageFileName = "RAILTECH_" + timeStamp + "_TEMP"
        return imageFileName
    }

    override fun onKeyDown(keyCode: Int, event: KeyEvent?): Boolean {
        if (keyCode == KeyEvent.KEYCODE_BACK && mWebView?.canGoBack() == true) {
            mWebView?.goBack()
            return true
        }
        return super.onKeyDown(keyCode, event)
    }

    override fun onDestroy() {
        super.onDestroy()
        service?.shutdown()
    }

    private fun showLoader() {
        loader.visibility = View.VISIBLE
    }

    private fun hideLoader() {
        loader.visibility = View.GONE
    }

    @SuppressLint("SetJavaScriptEnabled")
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)
        mWebView = findViewById(R.id.webview)
        loader = findViewById(R.id.loader)

        val webSettings = mWebView!!.settings
        webSettings.javaScriptEnabled = true
        handler = Handler(Looper.getMainLooper())

        setNetworkListener()

        mWebView!!.settings.allowFileAccess = true
        mWebView!!.settings.domStorageEnabled = true
        mWebView!!.settings.allowContentAccess = true
        mWebView!!.settings.useWideViewPort = true
        mWebView!!.webViewClient = object : WebViewClient() {

            override fun onPageStarted(view: WebView?, url: String?, favicon: Bitmap?) {
                super.onPageStarted(view, url, favicon)
            }

            override fun onPageFinished(view: WebView?, url: String?) {
                super.onPageFinished(view, url)
            }

            override fun onPageCommitVisible(view: WebView?, url: String?) {
                super.onPageCommitVisible(view, url)
            }

            override fun onReceivedError(
                view: WebView?, request: WebResourceRequest?, error: WebResourceError?
            ) {
                Log.d(
                    "DEBUG",
                    "onReceivedError description: ${error?.description}, errorCode: ${error?.errorCode}, headers: $error"
                )

                super.onReceivedError(view, request, error)
            }

            override fun onReceivedHttpError(
                view: WebView?, request: WebResourceRequest?, errorResponse: WebResourceResponse?
            ) {
                Log.d(
                    "DEBUG",
                    "onReceivedHttpError reasonPhrase: ${errorResponse?.reasonPhrase}, code: ${errorResponse?.statusCode}, headers: ${errorResponse?.responseHeaders}"
                )
                super.onReceivedHttpError(view, request, errorResponse)
            }

            @RequiresApi(Build.VERSION_CODES.O)
            override fun onRenderProcessGone(
                view: WebView?, detail: RenderProcessGoneDetail?
            ): Boolean {
                if (detail?.didCrash() == true) {
                    Toast.makeText(this@MainActivity2, "Low on resources", Toast.LENGTH_SHORT)
                        .show()
                    return true
                }
                return super.onRenderProcessGone(view, detail)
            }

            override fun onReceivedSslError(
                view: WebView?, handler: SslErrorHandler?, error: SslError?
            ) {
                val builder = AlertDialog.Builder(this@MainActivity2)
                builder.setMessage("Invalid SSL certificate")
                builder.setPositiveButton(
                    "continue"
                ) { dialog, which -> handler!!.proceed() }
                builder.setNegativeButton(
                    "cancel"
                ) { dialog, which -> handler!!.cancel() }
                val dialog = builder.create()
                dialog.show()
                super.onReceivedSslError(view, handler, error)
            }

            override fun shouldOverrideUrlLoading(
                view: WebView, request: WebResourceRequest
            ): Boolean {
                val uri = request.url
                if (uri.toString().startsWith("mailto:")) {
                    //Handle mail Urls
                    startActivity(Intent(Intent.ACTION_SENDTO, uri))
                } else if (uri.toString().startsWith("tel:")) {
                    //Handle telephony Urls
                    startActivity(Intent(Intent.ACTION_DIAL, uri))
                } else {
                    //Handle Web Urls
                    view.loadUrl(uri.toString())
                }
                return true
            }

        }
        mWebView?.setDownloadListener { url, _, _, _, _ ->
            val request = DownloadManager.Request(
                Uri.parse(url)
            )
            request.allowScanningByMediaScanner()
            request.setNotificationVisibility(DownloadManager.Request.VISIBILITY_VISIBLE_NOTIFY_COMPLETED) //Notify client once download is completed!
            request.setDestinationInExternalPublicDir(
                Environment.DIRECTORY_DOWNLOADS, createImageFileName() + ".pdf"
            )
            val dm = getSystemService(DOWNLOAD_SERVICE) as DownloadManager
            dm.enqueue(request)
            Toast.makeText(
                applicationContext,
                "Downloading File",  //To notify the Client that the file is being downloaded
                Toast.LENGTH_LONG
            ).show()
        }
        mWebView?.webChromeClient = object : WebChromeClient() {

            // For Lollipop 5.0+ Devices
            override fun onShowFileChooser(
                mWebView: WebView,
                filePathCallback: ValueCallback<Array<Uri>>,
                fileChooserParams: FileChooserParams
            ): Boolean {
                if (mFilePathCallback != null) {
                    mFilePathCallback?.onReceiveValue(null)
                    mFilePathCallback = null
                }
                mFilePathCallback = filePathCallback
                getPickIntent()

                return true
            }
        }

        mWebView!!.loadUrl("https://dpr.railtech.co.in/managerlogin.aspx")

        val swipeRefreshLayout: SwipeRefreshLayout = findViewById(R.id.swiperefreshlayout)

        swipeRefreshLayout.setOnRefreshListener {
            swipeRefreshLayout.isRefreshing = false
            mWebView?.reload()
        }
        requestCameraPermission()

    }

    private fun requestCameraPermission() {
        if (ContextCompat.checkSelfPermission(
                this, android.Manifest.permission.CAMERA
            ) != PackageManager.PERMISSION_GRANTED
        ) {
            ActivityCompat.requestPermissions(
                this, arrayOf(android.Manifest.permission.CAMERA), CAMERA_PERMISSION_REQUEST_CODE
            )
        }
    }

    override fun onRequestPermissionsResult(
        requestCode: Int, permissions: Array<out String>, grantResults: IntArray
    ) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults)
        if (requestCode == CAMERA_PERMISSION_REQUEST_CODE) {
            if (grantResults.isNotEmpty() && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
                // Camera permission granted
                // Perform camera-related operations

            }
        }
    }

    private fun getPickIntent() {
        val builder = AlertDialog.Builder(this)
        val view = layoutInflater.inflate(R.layout.intent_picker, null)
        builder.setView(view)
        val camera: View = view.findViewById(R.id.camera)
        val gallery: View = view.findViewById(R.id.gallery)
        camera.setOnClickListener {
            dialog?.dismiss()

            val values = ContentValues().apply {
                put(Media.DISPLAY_NAME, createImageFileName())
                put(Media.MIME_TYPE, "image/jpeg")
                put(Media.RELATIVE_PATH, Environment.DIRECTORY_PICTURES)
            }

            val resolver = applicationContext.contentResolver
            mCapturedImageURI = resolver.insert(Media.EXTERNAL_CONTENT_URI, values)

            val captureIntent = Intent(MediaStore.ACTION_IMAGE_CAPTURE)
            captureIntent.putExtra(MediaStore.EXTRA_OUTPUT, mCapturedImageURI)

            captureIntent.putExtra("android.intent.extras.CAMERA_FACING", 0)
            startActivityForResult(captureIntent, CAMERA_CAP_REQUEST_CODE)
        }
        gallery.setOnClickListener {
            dialog?.dismiss()
            val cint = Intent(
                Intent.ACTION_PICK, Media.EXTERNAL_CONTENT_URI
            )
            startActivityForResult(cint, INPUT_FILE_REQUEST_CODE)
        }
        builder.setOnCancelListener {
            if (mFilePathCallback != null) {
                mFilePathCallback?.onReceiveValue(null)
                mFilePathCallback = null
            }
        }
        dialog = builder.create()
        dialog?.show()
    }
}
