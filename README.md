# 頂你武肺
Scan 個 QR code 咋嘛，自己黎啦。

Forked from  [BinaryEye](https://github.com/markusfisch/BinaryEye).

Binary Eye uses the [ZXing][zxing] ("Zebra Crossing") barcode scanning
library.

## Cap圖

<img src="fastlane/metadata/android/en-US/images/phoneScreenshots/screencap-scanning.png"
	alt="Screenshot Gallery" width="160"/>
<img src="fastlane/metadata/android/en-US/images/phoneScreenshots/screencap-scanning-cropped.png"
	alt="Screenshot Gallery" width="160"/>
<img src="fastlane/metadata/android/en-US/images/phoneScreenshots/screencap-decoded.png"
	alt="Screenshot Theme" width="160"/>
<img src="fastlane/metadata/android/en-US/images/phoneScreenshots/screencap-compose-barcode.png"
	alt="Screenshot Editor" width="160"/>

## 下載


## RenderScript

This app uses [RenderScript][rs] to resize and rotate the camera image.
Unfortunately, RenderScript has some nasty gotchas.

### RenderScript.forceCompat()

It's necessary to call `RenderScript.forceCompat()` on some devices/roms.

`RenderScript.forceCompat()` needs to be run before any other RenderScript
function and unfortunately there is no way to know if invoking `forceCompat()`
is necessary or not.

If `RenderScript.forceCompat()` is necessary, a `RSRuntimeException` will
be thrown and the only option is to restart the app, this time with calling
`forceCompat()` first.

Calling `RenderScript.forceCompat()` means the processing is done in
software so you probably don't want to enable it by default.

### 2D barcodes

If you want to fork this and are only interested in reading 2D barcodes
(like QR or Aztec), you may want to remove the custom rotation kernel
altogether as ZXing can read 2D barcodes in any orientation.

This will make your app a bit simpler and saves you from compiling a
custom RenderScript kernel for each architecture you want to support.

[play]: https://play.google.com/store/search?q=barcode%20scanner&c=apps
[zxing]: https://github.com/zxing/zxing
[kotlin]: http://kotlinlang.org/
[aztec]: https://en.wikipedia.org/wiki/Aztec_Code
[codabar]: https://en.wikipedia.org/wiki/Codabar
[code_39]: https://en.wikipedia.org/wiki/Code_39
[code_93]: https://en.wikipedia.org/wiki/Code_93
[code_128]: https://en.wikipedia.org/wiki/Code_128
[data_matrix]: https://en.wikipedia.org/wiki/Data_Matrix
[ean_8]: https://en.wikipedia.org/wiki/EAN-8
[ean_13]: https://en.wikipedia.org/wiki/International_Article_Number
[itf]: https://en.wikipedia.org/wiki/Interleaved_2_of_5
[maxicode]: https://en.wikipedia.org/wiki/MaxiCode
[pdf417]: https://en.wikipedia.org/wiki/PDF417
[qr_code]: https://en.wikipedia.org/wiki/QR_code
[rss]: https://en.wikipedia.org/wiki/GS1_DataBar
[upc_a]: https://en.wikipedia.org/wiki/Universal_Product_Code
[upc_e]: https://en.wikipedia.org/wiki/Universal_Product_Code#UPC-E
[upc_ean]: https://en.wikipedia.org/wiki/Universal_Product_Code#EAN-13
[77]: https://github.com/markusfisch/BinaryEye/issues/77
[rs]: https://developer.android.com/guide/topics/renderscript/compute
