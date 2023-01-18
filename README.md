# OCR (Optical Character Recognition) Android App.

OCR is the process of recognizing characters from images using computer vision
and machine learning techniques. The current implementation uses the EAST detection model and the Keras-OCR text recognition model as a
pipeline to recognize texts.

## Requirements

*   Android Studio 4.2 (installed on a Linux, Mac or Windows machine)
*   An Android device, or an Android Emulator

## Build and run

### Step 1. Clone the TensorFlow examples source code

Clone the TF_CV_OCR_PROJECT GitHub repository to your computer to get the demo
application.

```
git clone https://github.com/claumoncada/TF_CV_OCR_PROJECT
```

### Step 2. Import the sample app to Android Studio

Open the TensorFlow source code in Android Studio. To do this, open Android
Studio and select `Import Projects (Gradle, Eclipse ADT, etc.)`, setting the
folder to `TF_CV_OCR_PROJECT`

### Step 3. Run the Android app

Connect the Android device to the computer and be sure to approve any ADB
permission prompts that appear on your phone. Select `Run -> Run app.` Select
the deployment target in the connected devices to the device on which the app
will be installed. This will install the app on the device.

To test the app, open the app called `TFL OCR` on your device. Re-installing the
app may require you to uninstall the previous installations.

For gradle CLI, running `./gradlew build` can create APKs for both solutions
under `app/build/outputs/apk`.

# Download the APK-DEMO
Feel free to download the OCR demo application directly from google drive at: https://drive.google.com/file/d/1YxZNUN5SP73qWeoGEgN1mYVyykc9ckaI/view
This apk returns the same results as the source code presented in this repository.


## Limitations

*   The current text recognition model is an improvement of the original
    [keras-ocr model](https://tfhub.dev/tulasiram58827/lite-model/keras-ocr/float16/2) trained with English letters and numbers.
    In the same way as the original, the new model  was trained using synthetic data but adding new characters to the alphabet.

*   The current model, similar to the original, is not general enough for OCR in the wild (say, random images
    taken by a smartphone camera in a low lighting condition).



## References

*   OpenCV text detection/recognition:
    https://github.com/opencv/opencv/blob/master/samples/dnn/text_detection.py
*   OCR TFLite community project by commnuity contributors @Tulasi123789 and
    @risingsayak https://github.com/tulasiram58827/ocr_tflite
*   OpenCV text detection:
    https://www.pyimagesearch.com/2018/08/20/opencv-text-detection-east-text-detector/
*   Deep Learning based Text Detection Using OpenCV:
    https://learnopencv.com/deep-learning-based-text-detection-using-opencv-c-python/
*   Optical Character Recognition (OCR):
    https://www.tensorflow.org/lite/examples/optical_character_recognition/overview

