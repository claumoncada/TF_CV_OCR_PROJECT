/* Copyright 2021 The TensorFlow Authors. All Rights Reserved.

Licensed under the Apache License, Version 2.0 (the "License");
you may not use this file except in compliance with the License.
You may obtain a copy of the License at

    http://www.apache.org/licenses/LICENSE-2.0

Unless required by applicable law or agreed to in writing, software
distributed under the License is distributed on an "AS IS" BASIS,
WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
See the License for the specific language governing permissions and
limitations under the License.
==============================================================================
*/

package org.tensorflow.lite.examples.ocr

import android.annotation.SuppressLint
import androidx.lifecycle.ViewModelProvider.AndroidViewModelFactory
import android.content.res.ColorStateList
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.os.Bundle
import androidx.appcompat.app.AppCompatActivity
import androidx.appcompat.widget.Toolbar
import android.util.Log
import android.view.MotionEvent
import android.view.View
import android.widget.Button
import android.widget.ImageView
import android.widget.Switch
import android.widget.TextView
import androidx.lifecycle.Observer
import com.bumptech.glide.Glide
import com.google.android.material.chip.Chip
import com.google.android.material.chip.ChipGroup
import java.io.IOException
import java.io.InputStream
import java.util.concurrent.Executors
import kotlinx.coroutines.MainScope
import kotlinx.coroutines.asCoroutineDispatcher
import kotlinx.coroutines.async
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock

private const val TAG = "MainActivity"

class MainActivity : AppCompatActivity() {

  private val tfImageName = "tensorflow.jpg" /* 1ST IMAGE */
  private val androidImageName = "custom.jpg"
  private val chromeImageName = "att.jpg"   //"chrome.jpg"
  private val customImageName = "covid.jpg" /* ---> CUSTOM IMAGE */
  private val churchImageName = "score.jpg" /* ---> CHURCH IMAGE */
  private val emiratesImageName = "emirates.jpg" /* ---> EMIRATES IMAGE */
  private val heritageImageName = "image7.jpg" /* ---> HERITAGE IMAGE */

  private lateinit var viewModel: MLExecutionViewModel
  private lateinit var resultImageView: ImageView

  private lateinit var tfImageView: ImageView /* TensorFlow Image view */
  private lateinit var androidImageView: ImageView
  private lateinit var chromeImageView: ImageView
  private lateinit var customImageView: ImageView /* --->  Custom Image view */
  private lateinit var churchImageView: ImageView
  private lateinit var emiratesImageView: ImageView
  private lateinit var heritageImageView: ImageView


  private lateinit var chipsGroup: ChipGroup
  private lateinit var runButton: Button
  private lateinit var textPromptTextView: TextView

  private var useGPU = false
  private var selectedImageName = "tensorflow.jpg"
  private var ocrModel: OCRModelExecutor? = null
  private val inferenceThread = Executors.newSingleThreadExecutor().asCoroutineDispatcher()
  private val mainScope = MainScope()
  private val mutex = Mutex()

  override fun onCreate(savedInstanceState: Bundle?) {
    super.onCreate(savedInstanceState)
    setContentView(R.layout.tfe_is_activity_main)

    val toolbar: Toolbar = findViewById(R.id.toolbar)
    setSupportActionBar(toolbar)
    supportActionBar?.setDisplayShowTitleEnabled(false)

    tfImageView = findViewById(R.id.tf_imageview) /* TENSORFLOW IMAGE VIEW */
    androidImageView = findViewById(R.id.android_imageview)
    chromeImageView = findViewById(R.id.chrome_imageview)
    customImageView = findViewById(R.id.custom_imageview) /* ---> CUSTOM IMAGE VIEW */
    churchImageView = findViewById(R.id.church_imageview)
    emiratesImageView = findViewById(R.id.emirates_imageview)
    heritageImageView = findViewById(R.id.heritage_imageview)

    val candidateImageViews = arrayOf<ImageView>(tfImageView, androidImageView, chromeImageView, customImageView, churchImageView, emiratesImageView, heritageImageView) /* ---> ADD CUSTOM IMAGE VIEW */

    val assetManager = assets
    try {
      val tfInputStream: InputStream = assetManager.open(tfImageName) /* 1ST IMAGE */
      val tfBitmap = BitmapFactory.decodeStream(tfInputStream)
      tfImageView.setImageBitmap(tfBitmap)

      val androidInputStream: InputStream = assetManager.open(androidImageName) /* 2ND IMAGE */
      val androidBitmap = BitmapFactory.decodeStream(androidInputStream)
      androidImageView.setImageBitmap(androidBitmap)

      val chromeInputStream: InputStream = assetManager.open(chromeImageName) /* 3rd IMAGE */
      val chromeBitmap = BitmapFactory.decodeStream(chromeInputStream)
      chromeImageView.setImageBitmap(chromeBitmap)

      val customInputStream: InputStream = assetManager.open(customImageName) /* ---> CUSTOM IMAGE */
      val customBitmap = BitmapFactory.decodeStream(customInputStream)
      customImageView.setImageBitmap(customBitmap)

      val churchInputStream: InputStream = assetManager.open(churchImageName) /* ---> CHURCH IMAGE */
      val churchBitmap = BitmapFactory.decodeStream(churchInputStream)
      churchImageView.setImageBitmap(churchBitmap)

      val emiratesInputStream: InputStream = assetManager.open(emiratesImageName) /* ---> EMIRATES IMAGE */
      val emiratesBitmap = BitmapFactory.decodeStream(emiratesInputStream)
      emiratesImageView.setImageBitmap(emiratesBitmap)

      val heritageInputStream: InputStream = assetManager.open(heritageImageName) /* ---> HERITAGE IMAGE */
      val heritageBitmap = BitmapFactory.decodeStream(heritageInputStream)
      heritageImageView.setImageBitmap(heritageBitmap)

    } catch (e: IOException) {
      Log.e(TAG, "Failed to open a test image")
    }

    for (iv in candidateImageViews) {
      setInputImageViewListener(iv)
    }

    resultImageView = findViewById(R.id.result_imageview)
    chipsGroup = findViewById(R.id.chips_group)
    textPromptTextView = findViewById(R.id.text_prompt)
    val useGpuSwitch: Switch = findViewById(R.id.switch_use_gpu)

    viewModel = AndroidViewModelFactory(application).create(MLExecutionViewModel::class.java)
    viewModel.resultingBitmap.observe(
      this,
      Observer { resultImage ->
        if (resultImage != null) {
          updateUIWithResults(resultImage)
        }
        enableControls(true)
      }
    )

    mainScope.async(inferenceThread) { createModelExecutor(useGPU) }

    useGpuSwitch.setOnCheckedChangeListener { _, isChecked ->
      useGPU = isChecked
      mainScope.async(inferenceThread) { createModelExecutor(useGPU) }
    }

    runButton = findViewById(R.id.rerun_button)
    runButton.setOnClickListener {
      enableControls(false)

      mainScope.async(inferenceThread) {
        mutex.withLock {
          if (ocrModel != null) {
            viewModel.onApplyModel(baseContext, selectedImageName, ocrModel, inferenceThread)
          } else {
            Log.d(
              TAG,
              "Skipping running OCR since the ocrModel has not been properly initialized ..."
            )
          }
        }
      }
    }

    setChipsToLogView(HashMap<String, Int>())
    enableControls(true)
  }

  @SuppressLint("ClickableViewAccessibility")
  private fun setInputImageViewListener(iv: ImageView) {
    iv.setOnTouchListener(
      object : View.OnTouchListener {
        override fun onTouch(v: View, event: MotionEvent?): Boolean {
          if (v.equals(tfImageView)) {
            selectedImageName = tfImageName /* Select image */
            textPromptTextView.setText(getResources().getString(R.string.tfe_using_first_image))
          } else if (v.equals(androidImageView)) {
            selectedImageName = androidImageName
            textPromptTextView.setText(getResources().getString(R.string.tfe_using_second_image))
          } else if (v.equals(chromeImageView)) {
            selectedImageName = chromeImageName
            textPromptTextView.setText(getResources().getString(R.string.tfe_using_third_image))
          }
          /* =====BEGINS CUSTOM IMAGE======================= */
           else if (v.equals(customImageView)) {
            selectedImageName = customImageName
            textPromptTextView.setText(getResources().getString(R.string.tfe_using_fourth_image))
          /* ===================ENDS CUSTOM IMAGE================================= */
          }

           else if (v.equals(churchImageView)) {
            selectedImageName = churchImageName
            textPromptTextView.setText(getResources().getString(R.string.tfe_using_fifth_image))
            /* ===================ENDS CUSTOM IMAGE================================= */
          }

           else if (v.equals(emiratesImageView)) {
            selectedImageName = emiratesImageName
            textPromptTextView.setText(getResources().getString(R.string.tfe_using_sixth_image))
            /* ===================ENDS CUSTOM IMAGE================================= */
          }

           else if (v.equals(heritageImageView)) {
            selectedImageName = heritageImageName
            textPromptTextView.setText(getResources().getString(R.string.tfe_using_seventh_image))
            /* ===================ENDS CUSTOM IMAGE================================= */
          }
          return false
        }
      }
    )
  }

  private suspend fun createModelExecutor(useGPU: Boolean) {
    mutex.withLock {
      if (ocrModel != null) {
        ocrModel!!.close()
        ocrModel = null
      }
      try {
        ocrModel = OCRModelExecutor(this, useGPU)
      } catch (e: Exception) {
        Log.e(TAG, "Fail to create OCRModelExecutor: ${e.message}")
        val logText: TextView = findViewById(R.id.log_view)
        logText.text = e.message
      }
    }
  }

  private fun setChipsToLogView(itemsFound: Map<String, Int>) {
    chipsGroup.removeAllViews()

    for ((word, color) in itemsFound) {
      val chip = Chip(this)
      chip.text = word
      chip.chipBackgroundColor = getColorStateListForChip(color)
      chip.isClickable = false
      chipsGroup.addView(chip)
    }
    val labelsFoundTextView: TextView = findViewById(R.id.tfe_is_labels_found)
    if (chipsGroup.childCount == 0) {
      labelsFoundTextView.text = getString(R.string.tfe_ocr_no_text_found)
    } else {
      labelsFoundTextView.text = getString(R.string.tfe_ocr_texts_found)
    }
    chipsGroup.parent.requestLayout()
  }

  private fun getColorStateListForChip(color: Int): ColorStateList {
    val states =
      arrayOf(
        intArrayOf(android.R.attr.state_enabled), // enabled
        intArrayOf(android.R.attr.state_pressed) // pressed
      )

    val colors = intArrayOf(color, color)
    return ColorStateList(states, colors)
  }

  private fun setImageView(imageView: ImageView, image: Bitmap) {
    Glide.with(baseContext).load(image).override(250, 250).fitCenter().into(imageView)
  }

  private fun updateUIWithResults(modelExecutionResult: ModelExecutionResult) {
    setImageView(resultImageView, modelExecutionResult.bitmapResult)
    val logText: TextView = findViewById(R.id.log_view)
    logText.text = modelExecutionResult.executionLog

    setChipsToLogView(modelExecutionResult.itemsFound)
    enableControls(true)
  }

  private fun enableControls(enable: Boolean) {
    runButton.isEnabled = enable
  }
}
