@file:Suppress("DEPRECATION")

package de.markusfisch.android.yuvshot.activity

import android.annotation.SuppressLint
import de.markusfisch.android.yuvshot.R

import de.markusfisch.android.cameraview.widget.CameraView

import android.app.Activity
import android.content.pm.PackageManager
import android.hardware.Camera
import android.os.Build
import android.os.Bundle
import android.os.Environment
import android.view.MotionEvent
import android.view.View
import android.view.Window
import android.view.WindowManager
import android.widget.Toast

import java.io.File

class MainActivity : Activity() {
	private lateinit var cameraView: CameraView

	private var frameData: ByteArray? = null
	private var frameWidth: Int = 0
	private var frameHeight: Int = 0
	private var frameOrientation: Int = 0

	override fun onRequestPermissionsResult(
		requestCode: Int,
		permissions: Array<String>,
		grantResults: IntArray
	) {
		when (requestCode) {
			REQUEST_PERMISSIONS -> for (result in grantResults) {
				if (result != PackageManager.PERMISSION_GRANTED) {
					Toast.makeText(
						this,
						R.string.no_camera_no_fun,
						Toast.LENGTH_SHORT
					).show()
					finish()
					return
				}
			}
		}
	}

	override fun onCreate(state: Bundle?) {
		super.onCreate(state)
		requestWindowFeature(Window.FEATURE_NO_TITLE)
		window.addFlags(WindowManager.LayoutParams.FLAG_FULLSCREEN)
		setContentView(R.layout.activity_main)

		cameraView = findViewById(R.id.camera_view)
		findViewById<View>(R.id.take_photo).setOnClickListener { _ ->
			val yuvData = frameData
			yuvData?.let {
				val path = saveYuv(
					yuvData,
					frameWidth,
					frameHeight,
					frameOrientation
				)
				Toast.makeText(
					this@MainActivity,
					String.format(getString(R.string.file_saved), path),
					Toast.LENGTH_LONG
				).show()
			}
		}
		findViewById<View>(R.id.switch_camera).setOnClickListener {
			stopCamera()
			frontFacing = frontFacing xor true
			startCamera()
		}

		initCameraView()
	}

	override fun onResume() {
		super.onResume()
		startCamera()
	}

	override fun onPause() {
		super.onPause()
		stopCamera()
	}

	private fun initCameraView() {
		cameraView.setUseOrientationListener(true)
		cameraView.setOnCameraListener(object : CameraView.OnCameraListener {
			override fun onConfigureParameters(parameters: Camera.Parameters) {
				val sceneModes = parameters.supportedSceneModes
				sceneModes?.let {
					for (mode in sceneModes) {
						if (mode.equals(Camera.Parameters.SCENE_MODE_BARCODE)) {
							parameters.sceneMode = mode
							break
						}
					}
				}
				CameraView.setAutoFocus(parameters)
			}

			override fun onCameraError() {
				Toast.makeText(
					this@MainActivity,
					R.string.camera_error,
					Toast.LENGTH_SHORT
				).show()
				finish()
			}

			override fun onCameraReady(camera: Camera) {
				frameWidth = cameraView.frameWidth
				frameHeight = cameraView.frameHeight
				frameOrientation = cameraView.frameOrientation
				camera.setPreviewCallback { data, _ -> frameData = data }
			}

			override fun onPreviewStarted(camera: Camera) {
				frameData = null
			}

			override fun onCameraStopping(camera: Camera) {
				camera.setPreviewCallback(null)
			}
		})
		setTapToFocus()
	}

	@SuppressLint("ClickableViewAccessibility")
	private fun setTapToFocus() {
		val runnable = Runnable {
			cameraView.setFocusArea(null)
		}
		cameraView.setOnTouchListener({ v: View, event: MotionEvent ->
			if (event.getActionMasked() == MotionEvent.ACTION_UP) {
				val camera = cameraView.camera
				camera?.cancelAutoFocus()
				cameraView.setFocusArea(
					cameraView.calculateFocusRect(
						event.x, event.y, 100
					)
				)
				camera?.autoFocus({ _: Boolean, _: Camera ->
					cameraView.removeCallbacks(runnable)
					cameraView.postDelayed(runnable, 3000)
				})
				v.performClick()
			}
			true
		})
	}

	private fun startCamera() {
		System.gc()
		if (hasPermissions()) {
			cameraView.openAsync(
				CameraView.findCameraId(
					if (frontFacing) {
						Camera.CameraInfo.CAMERA_FACING_FRONT
					} else {
						Camera.CameraInfo.CAMERA_FACING_BACK
					}
				)
			)
		}
	}

	private fun stopCamera() {
		cameraView.close()
	}

	private fun hasPermissions(): Boolean {
		if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
			val permissions = arrayOf(
				android.Manifest.permission.CAMERA,
				android.Manifest.permission.WRITE_EXTERNAL_STORAGE
			)
			val requests = mutableListOf<String>()

			for (permission in permissions) {
				if (checkSelfPermission(permission) !=
					PackageManager.PERMISSION_GRANTED
				) {
					requests.add(permission)
				}
			}

			if (requests.isNotEmpty()) {
				requestPermissions(
					requests.toTypedArray(),
					REQUEST_PERMISSIONS
				)
				return false
			}
		}

		return true
	}

	private fun saveYuv(
		yuvData: ByteArray,
		width: Int,
		height: Int,
		orientation: Int
	): String {
		val name = "${System.currentTimeMillis()}-${width}x$height-${orientation}deg.yuv"
		val dir = File(Environment.getExternalStorageDirectory(), "YuvShot")
		if (!dir.exists() && !dir.mkdir()) {
			throw RuntimeException("Cannot create output directory")
		}
		val file = File(dir, name)
		file.writeBytes(yuvData)
		return file.absolutePath
	}

	companion object {
		private const val REQUEST_PERMISSIONS = 1

		private var frontFacing = true
	}
}
